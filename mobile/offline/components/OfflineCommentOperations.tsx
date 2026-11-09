import React from 'react';
import { View } from 'react-native';
import { Button, Text } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { useOffline } from '../OfflineProvider';
import { operationText } from '../types';
import type { LocalOperation } from '../types';
import { OfflineFileItem } from './OfflineFileItem';

/** Unsent text is local work; APPLIED rows only report a pending local refresh. */
export function OfflineCommentOperations({
  operations
}: {
  operations: LocalOperation[];
}) {
  const offline = useOffline();
  const { t } = useTranslation();
  return (
    <View>
      {operations.map((operation) => {
        const applied = operation.status === 'APPLIED';
        const status = applied
          ? 'offline_comment_refresh_pending'
          : operation.status === 'REJECTED'
          ? 'offline_rejected'
          : operation.status === 'CONFLICT'
          ? 'offline_conflict'
          : operation.status === 'SENDING'
          ? 'offline_sending'
          : operation.status === 'UNKNOWN'
          ? 'offline_unknown'
          : 'offline_queued';
        return (
          <View key={operation.id} style={{ marginVertical: 8, padding: 12 }}>
            {!applied && (
              <Text variant="titleSmall">{t('offline_comment_pending')}</Text>
            )}
            <Text>{t(status)}</Text>
            {!applied && (
              <Text selectable>
                {operationText(operation.request).replace(
                  /@\[([^\]]+)\]\(user:\d+\)/g,
                  '@$1'
                )}
              </Text>
            )}
            {!applied &&
              operation.request.type === 'CREATE_COMMENT' &&
              operation.request.fileTransferIds?.map((localId) => (
                <OfflineFileItem
                  key={localId}
                  workOrderId={operation.request.workOrderId}
                  localId={localId}
                />
              ))}
            {(applied || operation.status === 'UNKNOWN') && (
              <Button
                disabled={!offline.networkAvailable || offline.syncing}
                onPress={() => void offline.sync().catch(offline.reportError)}
              >
                {t('offline_sync')}
              </Button>
            )}
          </View>
        );
      })}
    </View>
  );
}
