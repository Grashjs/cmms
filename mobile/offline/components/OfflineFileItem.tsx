import { useEffect, useState } from 'react';
import { Image, TouchableOpacity, View } from 'react-native';
import { Button, Text } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import type File from '../../models/file';
import type { LocalFile } from '../types';
import { useOffline } from '../OfflineProvider';
import { offlineErrorKey } from '../errors';
import { sessionGeneration } from '../sessionBridge';

/** A server URL is never used as evidence that the bytes are on this device. */
export function OfflineFileItem({
  workOrderId,
  file,
  localId,
  compact = false,
  onRemove
}: {
  workOrderId: number;
  file?: Pick<File, 'id' | 'name'>;
  localId?: string;
  compact?: boolean;
  onRemove?: () => Promise<void>;
}) {
  const offline = useOffline();
  const { t } = useTranslation();
  const [local, setLocal] = useState<LocalFile | null>(null);
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    setLocal(null);
    setBusy(false);
    if (!offline.repository || !offline.readable) return;
    const subscription = localId
      ? offline.repository.observeFiles([localId]).subscribe({
          next: (files) => setLocal(files[0] ?? null),
          error: () => setLocal(null)
        })
      : offline.repository.observeServerFile(workOrderId, file.id).subscribe({
          next: setLocal,
          error: () => setLocal(null)
        });
    return () => subscription.unsubscribe();
  }, [offline.repository, offline.readable, localId, workOrderId, file?.id]);
  const run = async (action: () => Promise<unknown>) => {
    if (busy) return;
    const generation = sessionGeneration();
    setBusy(true);
    try {
      await action();
    } catch (error) {
      if (generation === sessionGeneration()) offline.reportError(error);
    } finally {
      if (generation === sessionGeneration()) setBusy(false);
    }
  };
  const ready = offline.readable && local?.status === 'READY';
  if (file && !localId) return (
    <View style={{ marginVertical: 6 }}>
      {(!compact || (ready && !local?.mimeType?.startsWith('image/'))) && (
        <TouchableOpacity disabled={!ready || busy}
          onPress={() => void run(() => offline.openFile(local!.id))}>
          <Text>{file.name}</Text>
        </TouchableOpacity>
      )}
      {ready && local.mimeType?.startsWith('image/') && (
        <TouchableOpacity disabled={busy}
          onPress={() => void run(() => offline.openFile(local.id))}>
          <Image source={{ uri: local.uri }}
            style={{ height: 150, borderRadius: 5 }} />
        </TouchableOpacity>
      )}
      {!ready && <Text>{t(local?.status === 'IMPORTING' || offline.syncing
        ? 'offline_saving' : 'offline_file_failed')}</Text>}
      {!!local?.error &&
        <Text accessibilityRole="alert">{t(offlineErrorKey({ code: local.error }))}</Text>}
    </View>
  );
  return (
    <View style={{ marginVertical: 6 }}>
      {(!compact || (ready && !local?.mimeType?.startsWith('image/'))) && (
        <TouchableOpacity disabled={!ready || busy || !compact}
          onPress={() => void run(() => offline.openFile(local!.id))}>
          <Text>{file?.name ?? local?.name ?? t('files')}</Text>
        </TouchableOpacity>
      )}
      {ready && local.mimeType?.startsWith('image/') && (
        <TouchableOpacity
          disabled={busy}
          onPress={() => void run(() => offline.openFile(local.id))}
        >
          <Image
            source={{ uri: local.uri }}
            style={{ height: 150, width: compact ? 150 : '100%', borderRadius: 5 }}
          />
        </TouchableOpacity>
      )}
      {!compact && (
        <Text>
          {t(
            ready
              ? 'offline_file_available'
              : local?.status === 'IMPORTING'
              ? 'offline_saving'
              : local?.status === 'ERROR'
              ? 'offline_file_failed'
              : 'offline_files_not_downloaded'
          )}
        </Text>
      )}
      {!compact && local?.transferStatus === 'APPLIED' && (
        <Text>{t('offline_file_transfer_only')}</Text>
      )}
      {!!local?.error && (
        <Text accessibilityRole="alert">
          {t(offlineErrorKey({ code: local.error }))}
        </Text>
      )}
      {local?.status === 'ERROR' && !local.serverFileId && (
        <Button
          disabled={busy || !offline.writable}
          onPress={() => void run(() => offline.retryFileImport(local.id))}
        >
          {t('offline_retry_file_import')}
        </Button>
      )}
      {ready
        ? !compact && (
            <Button
              disabled={busy}
              onPress={() => void run(() => offline.openFile(local.id))}
            >
              {t('offline_open_file')}
            </Button>
          )
        : file && (
            <Button
              loading={busy}
              disabled={busy || !offline.networkAvailable || !offline.writable}
              onPress={() =>
                void run(() => offline.downloadFile(workOrderId, file.id))
              }
            >
              {t('offline_download_file')}
            </Button>
          )}
      {onRemove ? (
        <Button
          disabled={busy || !offline.writable}
          onPress={() => void run(onRemove)}
        >
          {t('offline_remove_file_selection')}
        </Button>
      ) : (
        !compact &&
        !!local &&
        file && (
          <Button
            disabled={busy || !offline.writable}
            onPress={() => void run(() => offline.removeLocalCopy(local.id))}
          >
            {t('offline_remove_local_copy')}
          </Button>
        )
      )}
    </View>
  );
}
