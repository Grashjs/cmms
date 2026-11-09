import React, { useState } from 'react';
import { Alert, View } from 'react-native';
import { Button, Card, Text } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { useOffline } from '../OfflineProvider';
import { usePendingWork } from '../hooks';
import { OfflineFileOperationReview } from './OfflineFileOperationReview';
import { OfflineStatusOperationReview } from './OfflineStatusOperationReview';

/** Metadata-only recovery stays reachable when the server withdrew the target. */
export function OfflineWorkPanel({ workOrderId }: { workOrderId?: number }) {
  const offline = useOffline();
  const items = usePendingWork(workOrderId);
  const pending = items.filter((item) => item.status !== 'APPLIED');
  const refreshes = items.filter((item) => item.status === 'APPLIED');
  const { t } = useTranslation();
  const [busy, setBusy] = useState(false);
  const run = async (action: () => Promise<unknown>) => {
    setBusy(true);
    try {
      await action();
    } catch (error) {
      offline.reportError(error);
    } finally {
      setBusy(false);
    }
  };
  if (!offline.repository || (!items.length && workOrderId === undefined))
    return null;
  return (
    <Card style={{ marginVertical: 10 }}>
      <Card.Content>
        <Text variant="titleMedium">
          {t(
            items.length && !pending.length
              ? 'offline_refresh_pending'
              : 'offline_pending'
          )}
        </Text>
        {!items.length && <Text>{t('offline_no_local_work')}</Text>}
        {[...pending, ...refreshes].map((item, index) => (
          <View key={`${item.kind}:${item.id}`} style={{ marginTop: 8 }}>
            {!!pending.length && index === pending.length && (
              <Text variant="titleMedium">{t('offline_refresh_pending')}</Text>
            )}
            <Text>
              {item.label ||
                (item.operationType === 'UPLOAD_FILE' && item.targetAvailable
                  ? item.value
                  : null) ||
                t(
                  item.targetAvailable && item.field === 'status'
                    ? 'offline_status_request'
                    : item.targetAvailable && item.field === 'time'
                    ? 'time'
                    : item.targetAvailable && item.field === 'comment'
                    ? 'comments'
                    : item.targetAvailable && item.field === 'attachments'
                    ? 'files'
                    : 'offline_orphan'
                )}
            </Text>
            {item.targetAvailable && (
              <Text>
                {t(
                  item.field === 'status'
                    ? 'offline_status_request'
                    : item.field === 'comment'
                    ? 'comments'
                    : item.field === 'attachments'
                    ? 'files'
                    : item.field
                )}
              </Text>
            )}
            <Text>
              {t(
                item.kind === 'DRAFT'
                  ? 'offline_draft_saved'
                  : item.status === 'APPLIED'
                  ? item.field === 'status'
                    ? 'offline_status_saved_refresh_pending'
                    : item.field === 'time'
                    ? 'offline_time_saved_refresh_pending'
                    : 'offline_comment_refresh_pending'
                  : item.status === 'SENDING'
                  ? 'offline_sending'
                  : item.status === 'UNKNOWN'
                  ? 'offline_unknown'
                  : item.status === 'CONFLICT'
                  ? 'offline_conflict'
                  : item.status === 'REJECTED'
                  ? 'offline_rejected'
                  : 'offline_queued'
              )}
            </Text>
            {item.reason === 'TRANSFER_FAILED' && (
              <Text accessibilityRole="alert">
                {t('offline_file_dependency_failed')}
              </Text>
            )}
            {item.kind === 'OPERATION' &&
              item.operationType !== 'UPLOAD_FILE' &&
              item.field !== 'time' &&
              item.field !== 'status' && (
                <OfflineFileOperationReview item={item} />
              )}
            {item.targetAvailable && item.field === 'status' && (
              <OfflineStatusOperationReview item={item} />
            )}
            {item.canAbandon ? (
              <Button
                disabled={busy}
                onPress={() => {
                  Alert.alert(
                    t('offline_discard'),
                    t('offline_discard_confirm'),
                    [
                      { text: t('cancel'), style: 'cancel' },
                      {
                        text: t('offline_discard'),
                        style: 'destructive',
                        onPress: () =>
                          run(() =>
                            item.kind === 'DRAFT'
                              ? item.field === 'status'
                                ? offline.abandonStatusDraft(
                                    item.workOrderId,
                                    item.draftRevision
                                  )
                                : item.field === 'time'
                                ? item.id.endsWith(':manual-time')
                                  ? offline.abandonManualTimeDraft(
                                      item.workOrderId,
                                      item.draftRevision
                                    )
                                  : offline.abandonTimeDraft(
                                      item.workOrderId,
                                      item.draftRevision
                                    )
                                : item.field === 'comment'
                                ? offline.abandonCommentDraft(
                                    item.workOrderId,
                                    item.draftRevision
                                  )
                                : item.field === 'attachments'
                                ? offline.abandonAttachmentDraft(
                                    item.workOrderId,
                                    item.draftRevision,
                                    item.taskId
                                  )
                                : offline.repository.abandonDraft(
                                    item.taskId,
                                    item.draftRevision,
                                    item.field
                                  )
                              : offline.abandonOperation(item.id)
                          )
                      }
                    ]
                  );
                }}
              >
                {t('offline_discard')}
              </Button>
            ) : (
              <Button
                disabled={busy || !offline.networkAvailable}
                onPress={() => run(() => offline.sync())}
              >
                {t('offline_sync')}
              </Button>
            )}
          </View>
        ))}
        {workOrderId !== undefined && !pending.length && (
          <Button
            disabled={busy}
            onPress={() =>
              Alert.alert(t('offline_remove'), t('offline_remove_confirm'), [
                { text: t('cancel'), style: 'cancel' },
                {
                  text: t('offline_remove'),
                  onPress: () =>
                    run(() => offline.removePreparation(workOrderId))
                }
              ])
            }
          >
            {t('offline_remove')}
          </Button>
        )}
      </Card.Content>
    </Card>
  );
}

export function OfflineStatus() {
  const offline = useOffline();
  const { t } = useTranslation();
  if (!offline.enabled) return null;
  return (
    <View style={{ padding: 10 }}>
      {offline.error && <Text accessibilityRole="alert">{offline.error}</Text>}
      <Button
        loading={offline.syncing}
        onPress={() => offline.sync()}
        disabled={!offline.repository || offline.syncing}
      >
        {t(offline.syncing ? 'offline_syncing' : 'offline_sync')}
      </Button>
    </View>
  );
}
