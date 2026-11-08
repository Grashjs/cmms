import * as FileSystem from 'expo-file-system';
import { File as NativeFile } from 'expo-file-system/next';
import * as Crypto from 'expo-crypto';
import type { IFile } from '../models/file';
import { OfflineRepository } from './OfflineRepository';
import {
  DownloadDescriptor,
  FileTarget,
  LocalFile,
  OfflineError,
  OFFLINE_LIMITS
} from './types';
import { offlineStorageDirectory } from './storage';
import { openOfflineDocument, protectOfflineFile } from './documentViewer';

// Account changes can overlap teardown. A single installation-wide filesystem
// lane keeps quota checks, copies and deletion from racing across contexts.
let fileLane: Promise<unknown> = Promise.resolve();
function exclusive<T>(action: () => Promise<T>): Promise<T> {
  const result = fileLane.then(action);
  fileLane = result.catch(() => undefined);
  return result;
}

export class OfflineFileStore {
  private directory = '';
  private root = '';
  private download: FileSystem.DownloadResumable | null = null;
  constructor(
    private readonly repository: OfflineRepository,
    private readonly isCurrent: () => boolean,
    private readonly canRead: () => Promise<boolean>
  ) {}

  private current() {
    if (!this.isCurrent()) throw new OfflineError('CONTEXT_CHANGED');
  }
  private async directories() {
    if (this.directory) return;
    this.root = `${await offlineStorageDirectory()}files/`;
    const key = await Crypto.digestStringAsync(
      Crypto.CryptoDigestAlgorithm.SHA256,
      this.repository.contextKey
    );
    this.directory = `${this.root}${key}/`;
    await FileSystem.makeDirectoryAsync(this.directory, {
      intermediates: true
    });
    await protectOfflineFile(this.directory);
  }
  private assertPrivate(uri: string) {
    if (
      !uri.startsWith(this.directory) ||
      uri.slice(this.directory.length).includes('/') ||
      uri.includes('..')
    )
      throw new OfflineError('FILE_IDENTITY');
  }
  private async usedBytes(directory: string): Promise<number> {
    let total = 0;
    for (const name of await FileSystem.readDirectoryAsync(directory)) {
      const uri = `${directory}${name}`;
      const info = await FileSystem.getInfoAsync(uri);
      if (info.exists)
        total += info.isDirectory ? await this.usedBytes(`${uri}/`) : info.size;
    }
    return total;
  }
  private async capacity(size: number, maxBytes = OFFLINE_LIMITS.fileBytes) {
    if (!Number.isSafeInteger(size) || size < 0 || size > maxBytes)
      throw new OfflineError('FILE_LIMIT');
    if (
      (await this.usedBytes(this.root)) + size >
        OFFLINE_LIMITS.localFileBytes ||
      (await FileSystem.getFreeDiskStorageAsync()) - size <
        OFFLINE_LIMITS.freeDiskReserve
    )
      throw new OfflineError('LOCAL_STORAGE_LIMIT');
  }
  private paths(id: string, name: string) {
    // Preserve only a short safe extension; the original name is never a path.
    const extension = name.match(/\.[a-zA-Z0-9]{1,12}$/)?.[0] || '';
    return {
      uri: `${this.directory}${id}${extension}`,
      temporaryUri: `${this.directory}${id}${extension}.part`
    };
  }
  async verify(file: LocalFile): Promise<void> {
    await this.directories();
    this.assertPrivate(file.uri);
    if (file.status !== 'READY') throw new OfflineError('FILE_NOT_READY');
    const actual = await this.fingerprint(file.uri);
    if (actual.size !== file.size || actual.sha256 !== file.sha256)
      throw new OfflineError('FILE_INTEGRITY');
  }
  private async fingerprint(uri: string) {
    const info = await FileSystem.getInfoAsync(uri);
    if (
      !info.exists ||
      info.isDirectory ||
      info.size > OFFLINE_LIMITS.fileBytes
    )
      throw new OfflineError('FILE_INTEGRITY');
    // Bounded to one <=35 MiB file; no base64 copies or whole multipart buffers.
    const bytes = await new NativeFile(uri).bytes();
    if (bytes.byteLength !== info.size)
      throw new OfflineError('FILE_INTEGRITY');
    const digest = await Crypto.digest(
      Crypto.CryptoDigestAlgorithm.SHA256,
      bytes as unknown as BufferSource
    );
    return {
      size: info.size,
      sha256: Array.from(new Uint8Array(digest), (byte) =>
        byte.toString(16).padStart(2, '0')
      ).join('')
    };
  }

  importFiles(
    workOrderId: number,
    target: FileTarget,
    inputs: IFile[],
    maxBytes?: number
  ): Promise<void> {
    return exclusive(async () => {
      this.current();
      await this.directories();
      const reserved: LocalFile[] = [];
      for (const input of inputs) {
        this.current();
        if (!/^(file|content):\/\//.test(input.uri))
          throw new OfflineError('FILE_INTEGRITY');
        const info = await FileSystem.getInfoAsync(input.uri);
        if (!info.exists || info.isDirectory)
          throw new OfflineError('FILE_COPY_FAILED');
        await this.capacity(
          info.size + reserved.reduce((sum, file) => sum + file.size, 0),
          OFFLINE_LIMITS.localFileBytes
        );
        if (
          info.size >
          Math.min(
            maxBytes ?? OFFLINE_LIMITS.fileBytes,
            OFFLINE_LIMITS.fileBytes
          )
        )
          throw new OfflineError('FILE_LIMIT');
        if (
          (target.kind === 'TASK' || target.image) &&
          !input.type?.startsWith('image/')
        )
          throw new OfflineError('FILE_INTEGRITY');
        const id = Crypto.randomUUID();
        const file: LocalFile = {
          id,
          name: input.name,
          mimeType: input.type || 'application/octet-stream',
          ...this.paths(id, input.name),
          size: info.size,
          sha256: '',
          status: 'IMPORTING',
          workOrderIds: [],
          target,
          importWorkOrderId: workOrderId,
          sourceUri: input.uri
        };
        reserved.push(file);
      }
      for (const file of reserved)
        await this.repository.createFileSelection(file, workOrderId, target);
      let firstError: unknown;
      for (const file of reserved) {
        try {
          await this.copySelection(file);
        } catch (error) {
          firstError ||= error;
        }
      }
      if (firstError) throw firstError;
      await this.cleanupNow();
    });
  }

  private async copySelection(file: LocalFile): Promise<void> {
    for (const id of file.transferIds || [file.id]) {
      const operation = await this.repository.getOperation(id);
      if (operation && !['REJECTED', 'ABANDONED'].includes(operation.status))
        throw new OfflineError('RESULT_UNKNOWN');
    }
    const copying = {
      ...file,
      status: 'IMPORTING' as const,
      temporaryUri: file.temporaryUri || `${file.uri}.part`,
      error: undefined
    };
    await this.repository.putFile(copying);
    try {
      this.current();
      if (!file.sourceUri) throw new OfflineError('FILE_COPY_FAILED');
      await FileSystem.copyAsync({
        from: file.sourceUri,
        to: copying.temporaryUri
      });
      await protectOfflineFile(copying.temporaryUri);
      const actual = await this.fingerprint(copying.temporaryUri);
      if (actual.size !== file.size) throw new OfflineError('FILE_INTEGRITY');
      this.current();
      await FileSystem.moveAsync({ from: copying.temporaryUri, to: file.uri });
      await protectOfflineFile(file.uri);
      await this.repository.putFile({
        ...file,
        ...actual,
        status: 'READY',
        sourceUri: undefined,
        temporaryUri: undefined,
        error: undefined
      });
    } catch (error) {
      await this.repository.putFile({
        ...copying,
        status: 'ERROR',
        error: error instanceof OfflineError ? error.code : 'FILE_COPY_FAILED'
      });
      throw error;
    }
  }

  retryFileImport(id: string): Promise<void> {
    return exclusive(async () => {
      this.current();
      await this.directories();
      const file = (await this.repository.getFiles([id]))[0];
      if (!file || !file.sourceUri || file.status === 'READY')
        throw new OfflineError('FILE_COPY_FAILED');
      await this.capacity(file.size);
      await this.copySelection(file);
    });
  }

  downloadFile(
    workOrderId: number,
    descriptor: DownloadDescriptor
  ): Promise<void> {
    return exclusive(async () => {
      this.current();
      if (!(await this.canRead())) throw new OfflineError('OFFLINE_DISABLED');
      await this.directories();
      if (!/^https?:\/\//i.test(descriptor.url))
        throw new OfflineError('INVALID_RESPONSE');
      const existing = await this.repository.getServerFile(
        workOrderId,
        descriptor.fileId
      );
      if (existing?.status === 'READY') {
        await this.verify(existing);
        return;
      }
      await this.capacity(descriptor.size);
      const id =
        existing && !existing.importWorkOrderId
          ? existing.id
          : Crypto.randomUUID();
      const file: LocalFile = {
        id,
        ...(existing && !existing.importWorkOrderId
          ? {
              uri: existing.uri,
              temporaryUri: existing.temporaryUri || `${existing.uri}.part`
            }
          : this.paths(id, descriptor.name)),
        name: descriptor.name,
        mimeType: descriptor.mimeType,
        size: descriptor.size,
        sha256: '',
        status: 'IMPORTING',
        serverFileId: descriptor.fileId,
        workOrderIds: [workOrderId]
      };
      await this.repository.putFile(file);
      try {
        let exceeded = false;
        if (!(await this.canRead()))
          throw new OfflineError('OFFLINE_DISABLED');
        const task = FileSystem.createDownloadResumable(
          descriptor.url,
          file.temporaryUri!,
          { sessionType: FileSystem.FileSystemSessionType.FOREGROUND },
          (progress) => {
            if (
              progress.totalBytesWritten > descriptor.size ||
              progress.totalBytesWritten > OFFLINE_LIMITS.fileBytes
            ) {
              exceeded = true;
              void task.cancelAsync().catch(() => undefined);
            }
          }
        );
        this.download = task;
        const timeout = setTimeout(() => {
          void task.cancelAsync().catch(() => undefined);
        }, 120_000);
        let response: FileSystem.FileSystemDownloadResult | undefined;
        try {
          response = await task.downloadAsync();
        } finally {
          clearTimeout(timeout);
        }
        this.download = null;
        if (exceeded) throw new OfflineError('FILE_LIMIT');
        if (!response || response.status < 200 || response.status >= 300)
          throw new OfflineError('NETWORK_UNAVAILABLE');
        this.current();
        await protectOfflineFile(file.temporaryUri!);
        const actual = await this.fingerprint(file.temporaryUri!);
        if (actual.size !== descriptor.size)
          throw new OfflineError('FILE_INTEGRITY');
        // A revoke/removal during a download must not publish the new copy.
        const target = await this.repository.getWorkOrderProjection(
          workOrderId
        );
        if (!target?.available) throw new OfflineError('TARGET_UNAVAILABLE');
        await FileSystem.moveAsync({ from: file.temporaryUri!, to: file.uri });
        await protectOfflineFile(file.uri);
        await this.repository.putFile({
          ...file,
          ...actual,
          status: 'READY',
          temporaryUri: undefined
        });
      } catch (error) {
        this.download = null;
        await FileSystem.deleteAsync(file.temporaryUri!, {
          idempotent: true
        }).catch(() => undefined);
        await this.repository.putFile({
          ...file,
          status: 'ERROR',
          error: error instanceof OfflineError ? error.code : 'FILE_COPY_FAILED'
        });
        throw error;
      }
    });
  }

  async openFile(id: string): Promise<void> {
    this.current();
    if (!(await this.canRead())) throw new OfflineError('OFFLINE_DISABLED');
    const file = (await this.repository.getFiles([id]))[0];
    if (!file) throw new OfflineError('TARGET_UNAVAILABLE');
    await this.verify(file);
    this.current();
    if (!(await this.canRead())) throw new OfflineError('OFFLINE_DISABLED');
    // Re-check target access too: verification may overlap an authoritative revoke.
    if (!(await this.repository.getFiles([id])).length)
      throw new OfflineError('TARGET_UNAVAILABLE');
    if (!(await this.canRead())) throw new OfflineError('OFFLINE_DISABLED');
    await openOfflineDocument(file.uri, file.name, file.mimeType);
  }
  removeLocalCopy(id: string): Promise<void> {
    return exclusive(async () => {
      this.current();
      await this.directories();
      await this.cleanupNow(id);
    });
  }
  cleanup(): Promise<void> {
    return exclusive(async () => {
      await this.directories();
      await this.cleanupNow();
    });
  }
  private async cleanupNow(explicitId?: string) {
    for (const file of await this.repository.allFiles()) {
      if (file.status !== 'READY' || !file.temporaryUri) continue;
      this.assertPrivate(file.temporaryUri);
      await FileSystem.deleteAsync(file.temporaryUri, { idempotent: true });
      await this.repository.putFile({ ...file, temporaryUri: undefined });
    }
    for (const file of await this.repository.queueUnusedFiles(explicitId)) {
      for (const uri of [file.uri, file.temporaryUri]) {
        if (!uri) continue;
        this.assertPrivate(uri);
        await FileSystem.deleteAsync(uri, { idempotent: true });
      }
      await this.repository.acknowledgeFileDeletion(file.id);
    }
  }
  recover(): Promise<void> {
    return exclusive(async () => {
      await this.directories();
      for (const file of await this.repository.allFiles()) {
        if (file.status !== 'IMPORTING' && file.status !== 'ERROR') continue;
        // A move may have completed just before the READY SQLite commit was
        // interrupted. Recover the private complete file, never a picker URI.
        const finalInfo = await FileSystem.getInfoAsync(file.uri);
        if (finalInfo.exists && !finalInfo.isDirectory) {
          try {
            this.assertPrivate(file.uri);
            const actual = await this.fingerprint(file.uri);
            if (
              actual.size === file.size &&
              (!file.sha256 || actual.sha256 === file.sha256)
            ) {
              await protectOfflineFile(file.uri);
              await this.repository.putFile({
                ...file,
                ...actual,
                status: 'READY',
                error: undefined,
                sourceUri: undefined
              });
              continue;
            }
          } catch {
            /* Keep an unusable copy visible as an error, never submit it. */
          }
        }
        // No automatic re-import from a transient picker URI after a restart.
        // Remove only incomplete temporary bytes, keeping the selection visible.
        if (file.temporaryUri) {
          this.assertPrivate(file.temporaryUri);
          await FileSystem.deleteAsync(file.temporaryUri, { idempotent: true });
        }
        await this.repository.putFile({
          ...file,
          status: 'ERROR',
          temporaryUri: undefined,
          error: 'FILE_COPY_FAILED'
        });
      }
      await this.cleanupNow();
    });
  }
  cancelTransfers() {
    void this.download?.cancelAsync().catch(() => undefined);
  }
}
