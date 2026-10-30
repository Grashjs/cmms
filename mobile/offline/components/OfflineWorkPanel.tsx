import React, { useState } from 'react';
import { Alert, View } from 'react-native';
import { Button, Card, Text } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { useOffline } from '../OfflineProvider';
import { usePendingWork } from '../hooks';

/** Metadata-only recovery stays reachable when the server withdrew the target. */
export function OfflineWorkPanel({ workOrderId }: { workOrderId?: number }) {
  const offline = useOffline();
  const items = usePendingWork(workOrderId);
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
        <Text variant="titleMedium">{t('offline_pending')}</Text>
        {!items.length && <Text>{t('offline_no_local_work')}</Text>}
        {items.map((item) => (
          <View key={`${item.kind}:${item.id}`} style={{ marginTop: 8 }}>
            <Text>{item.label || t('offline_orphan')}</Text>
            <Text>
              {t(
                item.kind === 'DRAFT'
                  ? 'offline_draft_saved'
                  : item.status === 'UNKNOWN' || item.status === 'SENDING'
                  ? 'offline_unknown'
                  : item.status === 'CONFLICT'
                  ? 'offline_conflict'
                  : item.status === 'REJECTED'
                  ? 'offline_rejected'
                  : 'offline_queued'
              )}
            </Text>
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
                              ? offline.repository.abandonDraft(
                                  item.taskId,
                                  item.draftRevision
                                )
                              : offline.repository.abandonOperation(item.id)
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
        {workOrderId !== undefined && !items.length && (
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
  if (!offline.enabled && !offline.prepared.length && !offline.error)
    return null;
  return (
    <View style={{ padding: 10 }}>
      {offline.error && <Text accessibilityRole="alert">{offline.error}</Text>}
      {!offline.readable && !!offline.prepared.length && (
        <Text>{t('offline_disabled')}</Text>
      )}
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
