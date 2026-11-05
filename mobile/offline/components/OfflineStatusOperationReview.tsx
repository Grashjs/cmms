import React, { useRef, useState } from 'react';
import { Alert, Image, View } from 'react-native';
import { Button, Text } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { useOffline } from '../OfflineProvider';
import { useOfflineStatus } from '../useOfflineStatus';
import { isStatusOperation, OfflineError, PendingWork } from '../types';
import { offlineErrorKey } from '../errors';
import { sessionGeneration } from '../sessionBridge';

export function OfflineStatusOperationReview({ item }: { item: PendingWork }) {
  const offline = useOffline();
  const { view } = useOfflineStatus(item.workOrderId);
  const { t } = useTranslation();
  const [busy, setBusy] = useState(false);
  const context = useRef({
    repository: offline.repository,
    workOrderId: item.workOrderId
  });
  context.current = {
    repository: offline.repository,
    workOrderId: item.workOrderId
  };
  const currentGuard = () => {
    const observed = context.current,
      generation = sessionGeneration();
    return () =>
      generation === sessionGeneration() &&
      observed.repository === context.current.repository &&
      observed.workOrderId === context.current.workOrderId;
  };
  const operation = view?.operations.find((op) => op.id === item.id);
  if (item.kind === 'DRAFT' && view?.draft) {
    const draft = view.draft;
    return (
      <View>
        <Text>
          {t('offline_status_requested', { status: t(draft.status) })}
        </Text>
        {!!draft.feedback && <Text>{draft.feedback}</Text>}
        {!!draft.signature && (
          <Image
            source={{ uri: draft.signature }}
            style={{ height: 120, width: '100%' }}
            resizeMode="contain"
          />
        )}
        <Button
          disabled={busy || !offline.writable}
          onPress={async () => {
            const isCurrent = currentGuard();
            setBusy(true);
            try {
              await offline.submitStatusDraft(item.workOrderId, draft.revision);
            } catch (error) {
              if (isCurrent()) offline.reportError(error);
            } finally {
              if (isCurrent()) setBusy(false);
            }
          }}
        >
          {t(
            draft.status === 'COMPLETE'
              ? 'offline_closure_submit'
              : 'offline_status_submit'
          )}
        </Button>
      </View>
    );
  }
  if (!operation || !isStatusOperation(operation.request)) return null;
  const change = operation.request.statusChange;
  const reviewable = item.canAbandon && item.status !== 'APPLIED';
  return (
    <View>
      <Text>{t('offline_status_requested', { status: t(change.status) })}</Text>
      {!!change.feedback && <Text>{change.feedback}</Text>}
      {!!change.signature && (
        <Image
          source={{ uri: change.signature }}
          style={{ height: 120, width: '100%' }}
          resizeMode="contain"
        />
      )}
      {operation.blockedReason && (
        <Text accessibilityRole="alert">
          {t(offlineErrorKey({ code: operation.blockedReason }))}
        </Text>
      )}
      {['CONFLICT', 'REJECTED'].includes(operation.status) &&
        operation.receipt?.code && (
          <Text accessibilityRole="alert">
            {t(offlineErrorKey({ code: operation.receipt.code }))}
          </Text>
        )}
      {reviewable && item.targetAvailable && (
        <Button
          disabled={busy || !offline.writable}
          onPress={async () => {
            const isCurrent = currentGuard();
            setBusy(true);
            try {
              // A refused/conflicting request must be reviewed against a new
              // server read, not the copy that preceded its failed attempt.
              // Never-attempted requests remain reviewable offline.
              if (operation.attemptCount > 0) {
                if (!offline.networkAvailable)
                  throw new OfflineError('NETWORK_UNAVAILABLE');
                await offline.refresh(item.workOrderId);
              }
              if (!isCurrent()) return;
              const current = await offline.repository.getStatusView(
                item.workOrderId
              );
              if (!isCurrent()) return;
              if (!current.available)
                throw new OfflineError('TARGET_UNAVAILABLE');
              const expectedDraft = current.draft?.revision ?? null;
              const expectedServer = current.serverRevision;
              Alert.alert(
                t('offline_status_review'),
                `${t('offline_status_review_confirm', {
                  local: t(change.status),
                  server: t(current.serverStatus || '')
                })}\n${t('work_order_status_review_value', {
                  status: t(current.serverStatus || ''),
                  count: current.activeTimerRuns.length
                })}`,
                [
                  { text: t('cancel'), style: 'cancel' },
                  {
                    text: t('offline_status_review'),
                    onPress: async () => {
                      if (!isCurrent()) return;
                      setBusy(true);
                      try {
                        await offline.reviewStatusOperation(
                          operation.id,
                          expectedDraft,
                          expectedServer,
                          current.activeTimerRuns
                        );
                      } catch (error) {
                        if (isCurrent()) offline.reportError(error);
                      } finally {
                        if (isCurrent()) setBusy(false);
                      }
                    }
                  }
                ]
              );
            } catch (error) {
              if (isCurrent()) offline.reportError(error);
            } finally {
              if (isCurrent()) setBusy(false);
            }
          }}
        >
          {t('offline_status_review')}
        </Button>
      )}
    </View>
  );
}
