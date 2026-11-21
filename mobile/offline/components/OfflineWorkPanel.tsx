import React, { useEffect, useState } from 'react';
import { Alert, View } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { Button, Card, Dialog, Portal, Text } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { useOffline } from '../OfflineProvider';
import { usePendingWork } from '../hooks';
import { offlineErrorKey } from '../errors';
import { sessionGeneration } from '../sessionBridge';
import type { PendingWork, TaskField } from '../types';
import { OfflineFileOperationReview } from './OfflineFileOperationReview';
import { OfflineStatusOperationReview } from './OfflineStatusOperationReview';

function summary(items: PendingWork[]) {
  const result = new Map<string, PendingWork>();
  for (const item of items) {
    if (item.kind === 'DRAFT') {
      // A non-closure status can be explicitly reopened after a server refusal.
      if (item.field === 'status' && item.value !== 'COMPLETE')
        result.set(item.id, item);
      continue;
    }
    // Uploads and their attachment are one user action. Keep individual failed uploads accessible.
    if (
      item.operationType === 'UPLOAD_FILE' &&
      !['CONFLICT', 'REJECTED'].includes(item.status)
    )
      continue;
    if (
      item.operationType === 'START_TIMER' &&
      items.some(
        (other) =>
          other.operationType === 'STOP_TIMER' &&
          item.periodId === other.periodId
      )
    )
      continue;
    const key =
      item.taskId && ['value', 'notes'].includes(item.field)
        ? item.taskId + ':' + item.field
        : item.field === 'status' &&
          ['QUEUED', 'UNKNOWN', 'SENDING'].includes(item.status)
        ? 'status'
        : item.id;
    const previous = result.get(key);
    const status =
      previous?.status === 'CONFLICT' || previous?.status === 'REJECTED'
        ? previous.status
        : item.status;
    result.set(key, {
      ...item,
      oldValue: previous?.oldValue ?? item.oldValue,
      reason:
        previous && ['CONFLICT', 'REJECTED'].includes(previous.status)
          ? previous.reason
          : item.reason,
      status
    });
  }
  return [...result.values()];
}

export function OfflineWorkPanel({ workOrderId }: { workOrderId: number }) {
  const offline = useOffline();
  const items = summary(usePendingWork(workOrderId));
  const { t } = useTranslation();
  const [busy, setBusy] = useState(false);
  const [comparison, setComparison] = useState<{
    item: PendingWork;
    server: string;
    version: number;
    generation: number;
  } | null>(null);
  const [commentComparison, setCommentComparison] = useState<{
    item: PendingWork;
    server: string;
    updatedAt: string | null;
    generation: number;
  } | null>(null);
  useEffect(() => {
    setComparison(null);
    setCommentComparison(null);
  }, [workOrderId, offline.repository]);
  const run = async (action: () => Promise<unknown>) => {
    setBusy(true);
    try {
      await action();
    } catch (failure) {
      offline.reportError(failure);
    } finally {
      setBusy(false);
    }
  };
  if (!offline.repository || !offline.readable) return null;
  const compare = async (item: PendingWork) => {
    const generation = sessionGeneration();
    await offline.refresh(workOrderId);
    const view = await offline.repository.getTask(
      item.taskId,
      item.field as TaskField
    );
    if (generation === sessionGeneration() && view.available && view.serverTask)
      setComparison({
        item,
        server: String(view.serverTask[item.field as TaskField] ?? ''),
        version: view.serverTask.version,
        generation
      });
  };
  const resolve = (keepLocal: boolean) => {
    const { item, version, generation } = comparison;
    setComparison(null);
    if (generation === sessionGeneration())
      void run(() =>
        offline.settleTaskField(
          workOrderId,
          item.taskId,
          item.field as TaskField,
          item.id,
          item.value,
          version,
          keepLocal
        )
      );
  };
  const compareComment = async (item: PendingWork) => {
    const generation = sessionGeneration();
    await offline.refresh(workOrderId);
    const view = await offline.repository.getWorkOrderProjection(workOrderId);
    const server = view?.comments.find((comment) => comment.id === item.commentId);
    if (generation === sessionGeneration() && view?.available)
      setCommentComparison({ item, server: server?.content ?? '',
        updatedAt: server?.updatedAt ?? null, generation });
  };
  const resolveComment = (keepLocal: boolean) => {
    const selected = commentComparison;
    setCommentComparison(null);
    if (selected && selected.generation === sessionGeneration())
      void run(() => offline.resolveCommentMutation(
        selected.item.id, selected.updatedAt, keepLocal));
  };
  const discard = (item: PendingWork) =>
    Alert.alert(t('offline_discard'), t('offline_discard_confirm'), [
      { text: t('cancel'), style: 'cancel' },
      {
        text: t('offline_discard'),
        style: 'destructive',
        onPress: () => void run(() => offline.abandonOperation(item.id))
      }
    ]);
  return (
    <Card style={{ marginVertical: 10 }}>
      <Card.Content>
        {!!items.length && (
          <Text variant="titleMedium">{t('offline_pending')}</Text>
        )}
        {!items.length && <Text>{t('offline_nothing_to_sync')}</Text>}
        {items.map((item) => {
          if (item.kind === 'DRAFT')
            return <OfflineStatusOperationReview key={item.id} item={item} />;
          const taskField = item.field === 'value' || item.field === 'notes';
          const issue =
            item.status === 'CONFLICT' ||
            item.status === 'REJECTED' ||
            item.reason === 'PREDECESSOR_FAILED' ||
            item.reason === 'TRANSFER_FAILED';
          const label =
            item.field === 'time' && item.minutes != null
              ? t('offline_time_added')
              : item.label ||
                t(
                  item.field === 'attachments'
                    ? 'files'
                    : item.field === 'comment'
                    ? 'comments'
                    : item.field
                );
          const state =
            item.status === 'APPLIED'
              ? 'offline_refresh_pending'
              : item.status === 'CONFLICT'
              ? taskField
                ? 'offline_server_changed'
                : 'offline_badge_conflict'
              : item.status === 'REJECTED'
              ? 'offline_badge_refused'
              : item.field === 'status' && item.value === 'COMPLETE'
              ? 'offline_closure_pending'
              : 'offline_badge_pending';
          const value =
            item.field === 'status'
              ? [item.oldValue, item.value]
                  .filter(Boolean)
                  .map((v) => t(v))
                  .join(' → ')
              : item.field === 'time'
              ? item.minutes != null
                ? item.minutes + ' ' + t('minutes')
                : t('offline_time_start')
              : item.operationType === 'DELETE_COMMENT'
              ? t('to_delete')
              : item.value;
          return (
            <View key={item.id} style={{ paddingVertical: 6 }}>
              <Text>
                {item.targetAvailable
                  ? `${[label, item.label && item.field === 'notes' ? t('notes') : '']
                      .filter(Boolean).join(' · ')} → ${value || ''}`
                  : t('offline_unavailable')}
              </Text>
              <Text variant="labelSmall">{t(state)}</Text>
              {issue && item.targetAvailable && item.reason && (
                <Text>{t(offlineErrorKey({ code: item.reason }))}</Text>
              )}
              {issue && item.targetAvailable && taskField && (
                <Button
                  disabled={busy || !offline.networkAvailable}
                  onPress={() => void run(() => compare(item))}
                >
                  {t('offline_compare')}
                </Button>
              )}
              {item.status === 'CONFLICT' && item.targetAvailable &&
                (item.operationType === 'UPDATE_COMMENT' ||
                  item.operationType === 'DELETE_COMMENT') && (
                  <Button disabled={busy || !offline.networkAvailable}
                    onPress={() => void run(() => compareComment(item))}>
                    {t('offline_compare')}
                  </Button>
                )}
              {issue && item.targetAvailable && item.field === 'status' && (
                <OfflineStatusOperationReview item={item} />
              )}
              {issue && item.targetAvailable && !taskField && (
                <OfflineFileOperationReview item={item} />
              )}
              {issue && !taskField && item.canAbandon && (
                <Button disabled={busy} onPress={() => discard(item)}>
                  {t('offline_discard')}
                </Button>
              )}
            </View>
          );
        })}
        <Button
          disabled={busy || offline.syncing || !offline.networkAvailable}
          loading={offline.syncing}
          onPress={() => void run(offline.sync)}
        >
          {t(offline.syncing ? 'offline_syncing' : 'offline_sync')}
        </Button>
        <Button
          disabled={busy}
          onPress={() =>
            Alert.alert(t('offline_remove'), t('offline_withdraw_warning'), [
              { text: t('cancel'), style: 'cancel' },
              {
                text: t('offline_remove'),
                style: 'destructive',
                onPress: () =>
                  void run(() => offline.removePreparation(workOrderId))
              }
            ])
          }
        >
          {t('offline_remove')}
        </Button>
      </Card.Content>
      {comparison &&
        items.some(
          (item) => item.id === comparison.item.id && item.targetAvailable
        ) && (
          <Portal>
            <Dialog visible onDismiss={() => setComparison(null)}>
              <Dialog.Title>{t('offline_compare')}</Dialog.Title>
              <Dialog.Content>
                <Text>
                  {t('offline_server_value')} · {comparison.server}
                </Text>
                <Text>
                  {t('offline_submission')} · {comparison.item.value}
                </Text>
              </Dialog.Content>
              <Dialog.Actions style={{ flexWrap: 'wrap' }}>
                <Button onPress={() => setComparison(null)}>
                  {t('cancel')}
                </Button>
                <Button onPress={() => resolve(false)}>
                  {t('offline_use_server')}
                </Button>
                <Button onPress={() => resolve(true)}>
                  {t('offline_submit')}
                </Button>
              </Dialog.Actions>
            </Dialog>
          </Portal>
        )}
      {commentComparison &&
        items.some((item) => item.id === commentComparison.item.id && item.targetAvailable) && (
          <Portal>
            <Dialog visible onDismiss={() => setCommentComparison(null)}>
              <Dialog.Title>{t('offline_compare')}</Dialog.Title>
              <Dialog.Content>
                <Text>{t('offline_server_value')} · {commentComparison.updatedAt
                  ? commentComparison.server : t('offline_unavailable')}</Text>
                <Text>{t('offline_submission')} · {commentComparison.item.operationType === 'DELETE_COMMENT'
                  ? t('to_delete') : commentComparison.item.value}</Text>
              </Dialog.Content>
              <Dialog.Actions style={{ flexWrap: 'wrap' }}>
                <Button onPress={() => setCommentComparison(null)}>{t('cancel')}</Button>
                <Button onPress={() => resolveComment(false)}>{t('offline_use_server')}</Button>
                {!!commentComparison.updatedAt &&
                  <Button onPress={() => resolveComment(true)}>{t('offline_submit')}</Button>}
              </Dialog.Actions>
            </Dialog>
          </Portal>
        )}
    </Card>
  );
}

export function OfflineStatus() {
  const offline = useOffline();
  const { t } = useTranslation();
  return offline.initialized && !offline.networkAvailable ? (
    <View style={{ flexDirection: 'row', alignItems: 'center', padding: 8, gap: 5 }}>
      <MaterialCommunityIcons name="wifi-off" size={16} />
      <Text variant="labelMedium">{t('offline_network_label')}</Text>
    </View>
  ) : null;
}
