import { OfflineApi, OfflineApiError } from './OfflineApi';
import { OfflineRepository } from './OfflineRepository';
import type { OfflineFileStore } from './OfflineFileStore';
import {
  OfflineEligibility,
  OfflineError,
  OfflineTask,
  operationField,
  TaskField,
  OFFLINE_LIMITS,
  isTaskOperation,
  transferIds
} from './types';

interface SyncState {
  status: 'idle' | 'syncing' | 'paused' | 'authentication_required' | 'error';
}
interface SyncPolicy {
  enabled: () => boolean;
  online: () => boolean;
  sessionIsCurrent: () => boolean;
  onEligibility?: (eligibility: OfflineEligibility) => void | Promise<void>;
  onStateChanged?: (state: SyncState) => void;
  onError?: (error: unknown) => void;
}
interface CoordinatorTimers {
  setTimeout: (
    callback: () => void,
    delay: number
  ) => ReturnType<typeof setTimeout>;
  clearTimeout: (timer: ReturnType<typeof setTimeout>) => void;
}

/** All foreground network work for an immutable account context shares this lane. */
export class SyncCoordinator {
  private tail: Promise<void> = Promise.resolve();
  private suspended = false;
  private requested = false;
  private syncPromise: Promise<void> | null = null;
  private state: SyncState = { status: 'idle' };
  private retryTimer: ReturnType<typeof setTimeout> | null = null;
  private retryIndex = 0;
  private permissionConfirmed = false;
  private lastEligibility: OfflineEligibility | null = null;

  constructor(
    public readonly repository: OfflineRepository,
    public readonly api: OfflineApi,
    private readonly policy: SyncPolicy,
    private readonly timers: CoordinatorTimers = { setTimeout, clearTimeout },
    public readonly files?: OfflineFileStore
  ) {}

  private clearRetry() {
    if (this.retryTimer !== null) this.timers.clearTimeout(this.retryTimer);
    this.retryTimer = null;
  }
  private retryAfter(error: unknown) {
    if (
      !(error instanceof OfflineApiError) ||
      !this.connected() ||
      !(
        error.code === 'NETWORK_UNAVAILABLE' ||
        error.status === 429 ||
        error.status >= 500
      )
    )
      return;
    this.clearRetry();
    const delays = [5000, 15000, 60000, 300000];
    const delay = delays[Math.min(this.retryIndex++, delays.length - 1)];
    this.retryTimer = this.timers.setTimeout(() => {
      this.retryTimer = null;
      if (this.connected()) void this.trigger('retry').catch(() => undefined);
    }, delay);
    // Node tests should not be kept alive by a mobile foreground retry timer.
    (this.retryTimer as any)?.unref?.();
  }

  private current() {
    return !this.suspended && this.policy.sessionIsCurrent();
  }
  private connected() {
    return this.current() && this.policy.online();
  }
  private assertConnected() {
    if (!this.current()) throw new OfflineError('CONTEXT_CHANGED');
    if (!this.policy.online()) throw new OfflineError('NETWORK_UNAVAILABLE');
  }
  private emit(state: SyncState) {
    this.state = { ...this.state, ...state };
    this.policy.onStateChanged?.(this.state);
  }
  private enqueue<T>(action: () => Promise<T>): Promise<T> {
    const operation = this.tail.then(action);
    this.tail = operation.then(
      () => undefined,
      () => undefined
    );
    return operation;
  }
  private async authorize(): Promise<boolean> {
    if (!this.connected() || !this.policy.enabled()) return false;
    const eligibility = await this.api.eligibility();
    if (!this.current()) return false;
    await this.policy.onEligibility?.(eligibility);
    this.lastEligibility = eligibility;
    this.permissionConfirmed = eligibility.enabled && eligibility.eligible;
    return (
      this.connected() && this.policy.enabled() && this.permissionConfirmed
    );
  }
  private async authorizationRevoked() {
    this.permissionConfirmed = false;
    if (this.lastEligibility && this.current()) {
      this.lastEligibility = {
        ...this.lastEligibility,
        eligible: false,
        authorizedUntil: undefined
      };
      await this.policy.onEligibility?.(this.lastEligibility);
    }
  }
  private async releases() {
    for (const id of await this.repository.getPendingReleases()) {
      if (!this.connected()) return;
      await this.api.release(id);
      if (!this.current()) return;
      await this.repository.acknowledgeRelease(id);
    }
  }
  private async refreshOne(workOrderId: number) {
    try {
      const revision = await this.repository.getAvailabilityRevision(
        workOrderId
      );
      // Reserve a durable compensating release before a new server reservation
      // can be committed without a locally acknowledged snapshot.
      const preparationToken = await this.repository.beginPreparation(
        workOrderId
      );
      const snapshot = await this.api.prepare(workOrderId);
      if (this.current())
        await this.repository.prepareSnapshot(
          snapshot,
          revision,
          preparationToken
        );
    } catch (error) {
      // Only an authoritative target response is a durable unavailability.
      // Timeouts, 5xx, over-limit and invalid/partial snapshots keep the old copy.
      if (
        this.current() &&
        error instanceof OfflineApiError &&
        error.code === 'OFFLINE_NOT_AUTHORIZED'
      ) {
        await this.authorizationRevoked();
      } else if (
        this.current() &&
        error instanceof OfflineApiError &&
        (error.status === 403 || error.status === 404)
      ) {
        await this.repository.markUnavailable(
          workOrderId,
          error.status === 403 ? 'ACCESS_REVOKED' : 'TARGET_NOT_FOUND'
        );
      }
      throw error;
    }
  }

  trigger(_reason = 'manual'): Promise<void> {
    if (!this.current()) return Promise.resolve();
    this.clearRetry();
    this.requested = true;
    if (this.syncPromise) return this.syncPromise;
    this.syncPromise = this.enqueue(async () => {
      try {
        while (this.requested && this.current()) {
          this.requested = false;
          if (!this.connected()) {
            this.emit({ status: 'paused' });
            return;
          }
          this.emit({ status: 'syncing' });
          await this.syncPass();
        }
        if (this.current()) {
          this.retryIndex = 0;
          this.emit({ status: 'idle' });
        }
      } catch (error) {
        this.requested = false;
        if (this.current()) {
          this.emit({
            status:
              error instanceof OfflineApiError && error.status === 401
                ? 'authentication_required'
                : 'error'
          });
          this.policy.onError?.(error);
          this.retryAfter(error);
        }
        throw error;
      } finally {
        this.syncPromise = null;
      }
    });
    return this.syncPromise;
  }

  private async syncPass() {
    const attempted = [
      ...(await this.repository.getOperationsToSync()).filter(
        (operation) => operation.attemptCount > 0
      ),
      ...(await this.repository.getCommentReceiptsToRefresh()),
      ...(await this.repository.getFileReceiptsToRefresh())
    ];
    // Receipts are read even while the feature is disabled. Missing is NOT an
    // assurance of non-application, and never makes an operation abandonable.
    for (
      let start = 0;
      start < attempted.length;
      start += OFFLINE_LIMITS.receiptIds
    ) {
      if (!this.connected()) return;
      const batch = attempted.slice(start, start + OFFLINE_LIMITS.receiptIds);
      const results = await this.api.receipts(
        batch.map((operation) => operation.id)
      );
      if (!this.current()) return;
      for (const receipt of results.results)
        await this.repository.applyReceipt(receipt);
      for (const id of results.missingOperationIds) {
        // Missing does not undo a durable success, nor permit its replay.
        if (
          batch.some(
            (operation) => operation.id === id && operation.status !== 'APPLIED'
          )
        )
          await this.repository.markUnknown(id);
      }
    }
    await this.releases();
    if (!(await this.authorize())) return;
    let firstRefreshError: unknown;
    for (const workOrder of await this.repository.getPreparedWorkOrderSummaries()) {
      if (!this.connected() || !this.policy.enabled()) return;
      try {
        await this.refreshOne(workOrder.id);
      } catch (error) {
        if (
          error instanceof OfflineApiError &&
          (error.status === 0 ||
            error.status === 401 ||
            error.code === 'OFFLINE_NOT_AUTHORIZED')
        )
          throw error;
        firstRefreshError ||= error;
        // An oversized/inaccessible intervention must not block independent work.
      }
    }
    const commentsToRefresh = new Set<number>();
    for (const operation of await this.repository.getOperationsToSync()) {
      if (
        !this.connected() ||
        !this.policy.enabled() ||
        !this.permissionConfirmed
      )
        return;
      const request = operation.request;
      if (
        request.type === 'UPLOAD_FILE' &&
        operation.attemptCount === 0 &&
        !(await this.repository.uploadHasParent(operation.id))
      )
        continue;
      if (
        isTaskOperation(request) &&
        request.predecessorOperationId &&
        operation.attemptCount === 0
      ) {
        const predecessor = await this.repository.getOperation(
          request.predecessorOperationId
        );
        if (predecessor?.status !== 'APPLIED') continue;
      }
      if (
        operation.attemptCount === 0 &&
        (
          await Promise.all(
            transferIds(request).map((id) => this.repository.getOperation(id))
          )
        ).some(
          (dependency) =>
            dependency?.status !== 'APPLIED' ||
            [
              'FILE_DELETED',
              'TARGET_UNAVAILABLE',
              'WORK_ORDER_UNAVAILABLE'
            ].includes(dependency.receipt?.visibility || '')
        )
      )
        continue;
      const target =
        !isTaskOperation(request) &&
        'taskId' in request &&
        request.taskId != null
          ? await this.repository.getTask(request.taskId, 'notes')
          : !isTaskOperation(request)
          ? await this.repository.getWorkOrderProjection(request.workOrderId)
          : await this.repository.getTask(
              request.taskId,
              operationField(request) as TaskField
            );
      // A missing receipt cannot prove that a previous attempt had no effect.
      // Replay that immutable request so current server rights can produce a
      // terminal receipt, even when the local target was removed or is read-only.
      if (
        operation.attemptCount === 0 &&
        (!target?.available ||
          !(request.type === 'CREATE_COMMENT'
            ? 'canComment' in target && target.canComment
            : target.canEdit))
      )
        continue;
      let localFile =
        request.type === 'UPLOAD_FILE'
          ? await this.repository.getTransferFile(operation.id)
          : null;
      if (request.type === 'UPLOAD_FILE') {
        try {
          if (!localFile || !this.files)
            throw new OfflineError('FILE_NOT_READY');
          await this.files.verify(localFile);
        } catch (error) {
          if (localFile)
            await this.repository.putFile({
              ...localFile,
              status: 'ERROR',
              error: 'FILE_INTEGRITY'
            });
          firstRefreshError ||= error;
          continue;
        }
      }
      if (!this.connected()) return;
      const attempt = await this.repository.beginAttempt(operation.id);
      try {
        const result =
          attempt.request.type === 'UPLOAD_FILE'
            ? await this.api.uploadFile(attempt.request, localFile!)
            : await this.api.sendOperation(attempt.request);
        if (!this.current()) return; // reconciled through its receipt next login
        await this.repository.applyReceipt(result);
        if (
          request.type !== 'UPLOAD_FILE' &&
          !isTaskOperation(request) &&
          result.status === 'APPLIED'
        )
          commentsToRefresh.add(request.workOrderId);
      } catch (error) {
        await this.repository.markUnknown(operation.id);
        if (
          error instanceof OfflineApiError &&
          error.code === 'OFFLINE_NOT_AUTHORIZED'
        )
          await this.authorizationRevoked();
        throw error;
      }
    }
    // A refresh failure must never turn a durable creation receipt into UNKNOWN.
    for (const workOrderId of commentsToRefresh) {
      if (!this.connected() || !this.policy.enabled()) return;
      try {
        await this.refreshOne(workOrderId);
      } catch (error) {
        firstRefreshError ||= error;
      }
    }
    await this.files?.cleanup();
    if (firstRefreshError) throw firstRefreshError;
  }

  prepare(workOrderId: number): Promise<void> {
    return this.enqueue(async () => {
      this.assertConnected();
      if (!(await this.authorize())) throw new OfflineError('OFFLINE_DISABLED');
      await this.releases();
      await this.refreshOne(workOrderId);
    });
  }
  refresh(workOrderId: number): Promise<void> {
    return this.prepare(workOrderId);
  }

  runConnected<T>(action: () => Promise<T>): Promise<T> {
    return this.enqueue(async () => {
      this.assertConnected();
      return action();
    });
  }

  removeServerAttachment(
    workOrderId: number,
    fileId: number,
    image: boolean
  ): Promise<{ localUpdateError?: unknown }> {
    return this.enqueue(async () => {
      this.assertConnected();
      if (!this.policy.enabled()) throw new OfflineError('OFFLINE_DISABLED');
      const view = await this.repository.getWorkOrderProjection(workOrderId);
      if (!view?.available || !view.canEdit)
        throw new OfflineError('EDIT_FORBIDDEN');
      if (
        image
          ? view.workOrder?.image?.id !== fileId
          : !view.workOrder?.files.some((file) => file.id === fileId)
      )
        throw new OfflineError('SERVER_CHANGED');
      this.assertConnected();
      await this.api.removeWorkOrderAttachment(workOrderId, fileId, image);
      if (!this.current()) throw new OfflineError('CONTEXT_CHANGED');
      // Server success is final. A failed snapshot can only be refreshed; it
      // must not make this connected mutation look eligible for a retry.
      try {
        this.assertConnected();
        await this.refreshOne(workOrderId);
        if (!this.current()) throw new OfflineError('CONTEXT_CHANGED');
        return {};
      } catch (localUpdateError) {
        if (!this.current()) throw new OfflineError('CONTEXT_CHANGED');
        return { localUpdateError };
      }
    });
  }

  patchTask(
    workOrderId: number,
    taskId: number,
    patch: { notes?: string; value?: string | number }
  ): Promise<OfflineTask> {
    return this.enqueue(async () => {
      this.assertConnected();
      const view = await this.repository.getTask(taskId);
      const workOrder = await this.repository.getWorkOrderProjection(
        workOrderId
      );
      if (
        !view.serverTask ||
        !view.available ||
        !view.canEdit ||
        !workOrder?.tasks.some((task) => task.id === taskId)
      ) {
        throw new OfflineError('TARGET_UNAVAILABLE');
      }
      const canonical = await this.api.patchTask(taskId, {
        ...patch,
        version: view.serverTask.version
      });
      if (this.current())
        await this.repository.ingestServerTask(workOrderId, canonical);
      return canonical;
    });
  }

  removePreparation(workOrderId: number): Promise<void> {
    return this.enqueue(async () => {
      if (!this.current()) throw new OfflineError('CONTEXT_CHANGED');
      await this.repository.removePreparation(workOrderId);
      await this.files?.cleanup();
      // Release is a durable housekeeping action; its failure does not undo an
      // explicit, safe local withdrawal. It is retried before the next prepare.
      if (this.connected()) {
        try {
          await this.releases();
        } catch (error) {
          this.policy.onError?.(error);
        }
      }
    });
  }

  downloadFile(workOrderId: number, fileId: number): Promise<void> {
    return this.enqueue(async () => {
      this.assertConnected();
      if (!(await this.authorize()) || !this.files)
        throw new OfflineError('OFFLINE_DISABLED');
      const descriptor = await this.api.fileDescriptor(workOrderId, fileId);
      this.assertConnected();
      if (descriptor.fileId !== fileId)
        throw new OfflineError('TARGET_MISMATCH');
      await this.files.downloadFile(workOrderId, descriptor);
    });
  }

  cancelTransfers(): void {
    this.api.cancelTransfers();
    this.files?.cancelTransfers();
  }

  suspend(): void {
    this.cancelTransfers();
    this.clearRetry();
    this.suspended = true;
    this.requested = false;
    this.emit({ status: 'paused' });
  }
}
