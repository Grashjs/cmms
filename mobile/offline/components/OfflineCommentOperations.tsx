import React, { useState } from 'react';
import { Alert, TextInput, View } from 'react-native';
import { Button, IconButton, Text } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { operationText } from '../types';
import type { LocalOperation } from '../types';
import { OfflineFileItem } from './OfflineFileItem';
import { useOffline } from '../OfflineProvider';

function PendingComment({ operation }: { operation: LocalOperation }) {
  const offline = useOffline();
  const { t } = useTranslation();
  const [editing, setEditing] = useState(false);
  const [content, setContent] = useState(operationText(operation.request));
  const [busy, setBusy] = useState(false);
  const canChange = operation.request.type === 'CREATE_COMMENT' &&
    operation.status === 'QUEUED' && operation.attemptCount === 0 && offline.writable;
  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    try { await action(); }
    catch (error) { offline.reportError(error); }
    finally { setBusy(false); }
  };
  return (
    <View style={{ marginVertical: 8, padding: 12 }}>
      {editing ? (
        <View>
          <TextInput value={content} onChangeText={setContent} multiline
            style={{ minHeight: 60, color: 'black' }} />
          <View style={{ flexDirection: 'row' }}>
            <Button disabled={busy || !content.trim()} onPress={() => void run(async () => {
              await offline.editQueuedComment(operation.id, content);
              setEditing(false);
            })}>{t('save')}</Button>
            <Button onPress={() => setEditing(false)}>{t('cancel')}</Button>
          </View>
        </View>
      ) : (
        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
          <Text selectable style={{ flex: 1 }}>
            {operationText(operation.request).replace(
              /@\[([^\]]+)\]\(user:\d+\)/g, '@$1'
            )}
          </Text>
          {canChange && (
            <View style={{ flexDirection: 'row' }}>
              <IconButton icon="pencil" size={20} disabled={busy}
                onPress={() => { setContent(operationText(operation.request)); setEditing(true); }} />
              <IconButton icon="delete" size={20} disabled={busy}
                onPress={() => Alert.alert(t('confirmation'), t('confirm_delete_comment'), [
                  { text: t('cancel'), style: 'cancel' },
                  { text: t('to_delete'), style: 'destructive',
                    onPress: () => void run(() => offline.abandonOperation(operation.id)) }
                ])} />
            </View>
          )}
        </View>
      )}
      {operation.request.type === 'CREATE_COMMENT' &&
        operation.request.fileTransferIds?.map((localId) => (
          <OfflineFileItem key={localId}
            workOrderId={operation.request.workOrderId} localId={localId} compact />
        ))}
    </View>
  );
}

/** Keep published comments visible until the server copy contains them. */
export function OfflineCommentOperations({
  operations
}: {
  operations: LocalOperation[];
}) {
  return (
    <View>
      {operations.map((operation) => (
        <PendingComment key={operation.id} operation={operation} />
      ))}
    </View>
  );
}
