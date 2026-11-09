import { useEffect, useState } from 'react';
import { View } from 'react-native';
import { Button, Text } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { useOffline } from '../OfflineProvider';
import { useOfflineWorkOrder } from '../hooks';
import { LocalOperation, PendingWork, transferIds } from '../types';
import { OfflineFileItem } from './OfflineFileItem';
import { sessionGeneration } from '../sessionBridge';

/** A rejected attachment is reviewed before it becomes an editable draft again. */
export function OfflineFileOperationReview({ item }: { item: PendingWork }) {
  const offline = useOffline();
  const prepared = useOfflineWorkOrder(item.workOrderId);
  const { t } = useTranslation();
  const [operation, setOperation] = useState<LocalOperation | null>(null);
  const [reviewing, setReviewing] = useState(false);
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    setOperation(null);
    setReviewing(false);
    if (!offline.repository || !offline.readable) return;
    let active = true;
    void offline.repository
      .getOperation(item.id)
      .then((value) => {
        if (active) setOperation(value);
      })
      .catch(offline.reportError);
    return () => {
      active = false;
    };
  }, [offline.repository, offline.readable, item.id, item.status]);
  const request = operation?.request;
  if (
    !request ||
    !prepared?.available ||
    !item.targetAvailable ||
    ![
      'CREATE_COMMENT',
      'ADD_TASK_IMAGES',
      'UPDATE_WORK_ORDER_ATTACHMENTS'
    ].includes(request.type) ||
    !(
      item.status === 'CONFLICT' ||
      item.status === 'REJECTED' ||
      (item.status === 'QUEUED' &&
        item.attemptCount === 0 &&
        item.reason === 'TRANSFER_FAILED')
    )
  )
    return null;
  const imageChange =
    request.type === 'UPDATE_WORK_ORDER_ATTACHMENTS' && request.imageChange;
  return (
    <View>
      <Button
        disabled={busy || !offline.writable}
        onPress={() => setReviewing(!reviewing)}
      >
        {t('offline_review_files')}
      </Button>
      {reviewing && (
        <View>
          {request.type === 'CREATE_COMMENT' && (
            <Text selectable>{request.content}</Text>
          )}
          {transferIds(request).map((localId) => (
            <OfflineFileItem
              key={localId}
              localId={localId}
              workOrderId={item.workOrderId}
            />
          ))}
          {imageChange && (
            <View>
              <Text>{t('offline_server_value')}</Text>
              {prepared.workOrder.image ? (
                <OfflineFileItem
                  workOrderId={item.workOrderId}
                  file={prepared.workOrder.image}
                />
              ) : (
                <Text>—</Text>
              )}
            </View>
          )}
          <Text>{t('offline_restore_files_draft')}</Text>
          <Button
            disabled={busy || !offline.writable}
            onPress={async () => {
              const generation = sessionGeneration();
              setBusy(true);
              try {
                // null requires an empty composer/selection; the provider refuses any newer draft.
                await offline.restoreFileOperation(
                  item.id,
                  null,
                  imageChange ? prepared.workOrder.image?.id ?? null : undefined
                );
                if (generation === sessionGeneration()) setReviewing(false);
              } catch (error) {
                if (generation === sessionGeneration())
                  offline.reportError(error);
              } finally {
                if (generation === sessionGeneration()) setBusy(false);
              }
            }}
          >
            {t('offline_keep_file_selection')}
          </Button>
        </View>
      )}
    </View>
  );
}
