import React, { useEffect, useRef, useState } from 'react';
import { Alert, View } from 'react-native';
import { Button, HelperText, Text, TextInput } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { useOffline } from '../OfflineProvider';
import { useOfflineTask } from '../hooks';

export function OfflineTaskValue({
  workOrderId,
  taskId,
  editable
}: {
  workOrderId: number;
  taskId: number;
  editable: boolean;
}) {
  const offline = useOffline();
  const task = useOfflineTask(taskId);
  const { t } = useTranslation();
  const [text, setText] = useState('');
  const [saving, setSaving] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(false);
  const sequence = useRef(0);
  const savingRef = useRef(false);
  const latestWrite = useRef<Promise<unknown>>(Promise.resolve());
  useEffect(() => {
    if (!savingRef.current && !error && task?.task)
      setText(String(task.task.value ?? ''));
  }, [task?.task?.value, task?.draft?.revision, error]);

  const save = (value: string) => {
    setText(value);
    setSaving(true);
    savingRef.current = true;
    setError(false);
    const revision = ++sequence.current;
    const pending = offline.saveDraft(workOrderId, taskId, value);
    latestWrite.current = pending;
    void pending
      .then(() => {
        if (revision === sequence.current) {
          savingRef.current = false;
          setSaving(false);
        }
      })
      .catch(() => {
        if (revision === sequence.current) {
          savingRef.current = false;
          setSaving(false);
          setError(true);
        }
      });
  };
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
  const submit = () =>
    run(async () => {
      await latestWrite.current;
      const current = await offline.repository.getTask(taskId);
      if (current?.draft)
        await offline.submitDraft(taskId, current.draft.revision);
    });
  const operation = task?.operation;
  const blocked =
    operation &&
    ['QUEUED', 'SENDING', 'UNKNOWN', 'CONFLICT', 'REJECTED'].includes(
      operation.status
    );
  const conflict =
    operation && ['CONFLICT', 'REJECTED'].includes(operation.status);
  const needsReview = conflict || !!task?.draft?.blockedReason;
  const status = saving
    ? 'offline_saving'
    : error
    ? 'offline_save_failed'
    : task?.draft
    ? 'offline_draft_saved'
    : operation?.status === 'APPLIED'
    ? 'offline_applied'
    : operation?.status === 'UNKNOWN' || operation?.status === 'SENDING'
    ? 'offline_unknown'
    : operation?.status === 'QUEUED'
    ? 'offline_queued'
    : operation?.status === 'CONFLICT'
    ? 'offline_conflict'
    : operation?.status === 'REJECTED'
    ? 'offline_rejected'
    : null;

  if (!task?.available) return <Text>{t('offline_orphan')}</Text>;
  return (
    <View>
      <TextInput
        mode="outlined"
        label={t('value')}
        value={text}
        onChangeText={save}
        maxLength={255}
        multiline
        disabled={!editable || !task.canEdit || !offline.writable}
        testID={`offline-task-value-${taskId}`}
      />
      <HelperText type={error ? 'error' : 'info'}>
        {t(status || 'offline_max_text')}
      </HelperText>
      {!!task.draft && (
        <Button
          mode="contained"
          onPress={submit}
          disabled={
            saving ||
            error ||
            busy ||
            !!blocked ||
            !!task.draft.blockedReason ||
            !editable ||
            !offline.writable
          }
          loading={busy}
          testID={`offline-task-submit-${taskId}`}
        >
          {t('offline_submit')}
        </Button>
      )}
      {!!task.draft && !!blocked && <Text>{t('offline_wait_previous')}</Text>}
      {operation?.status === 'UNKNOWN' && (
        <Button onPress={() => offline.sync()}>{t('offline_sync')}</Button>
      )}
      {needsReview && (
        <View style={{ marginTop: 8 }}>
          <Text>
            {t(
              operation?.status === 'REJECTED'
                ? 'offline_rejected'
                : 'offline_conflict'
            )}
          </Text>
          <Text>
            {t('offline_server_value')}: {String(task.serverTask?.value ?? '')}
          </Text>
          <Text>{t('offline_resume_after_conflict')}</Text>
          <Button
            disabled={busy || !offline.writable}
            onPress={() => {
              const expectedRevision = task.draft?.revision;
              const serverVersion = task.serverTask?.version;
              Alert.alert(
                t('offline_keep_draft'),
                t('offline_resume_after_conflict'),
                [
                  { text: t('cancel'), style: 'cancel' },
                  {
                    text: t('offline_keep_draft'),
                    onPress: () =>
                      run(() =>
                        offline.resolveConflict(
                          taskId,
                          expectedRevision,
                          serverVersion
                        )
                      )
                  }
                ]
              );
            }}
          >
            {t('offline_keep_draft')}
          </Button>
        </View>
      )}
    </View>
  );
}
