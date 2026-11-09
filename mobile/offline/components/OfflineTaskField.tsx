import React, { useEffect, useRef, useState } from 'react';
import { Alert, View } from 'react-native';
import { Button, HelperText, Text, TextInput } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { useOffline } from '../OfflineProvider';
import { useOfflineTask } from '../hooks';
import { SheetManager } from 'react-native-actions-sheet';
import { operationText } from '../types';
import type { TaskDraft, TaskField } from '../types';

export function OfflineTaskField({
  workOrderId,
  taskId,
  editable,
  field = 'value'
}: {
  workOrderId: number;
  taskId: number;
  editable: boolean;
  field?: TaskField;
}) {
  const offline = useOffline();
  const task = useOfflineTask(taskId, field);
  const { t } = useTranslation();
  const [text, setText] = useState('');
  const [saving, setSaving] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(false);
  const sequence = useRef(0);
  const savingRef = useRef(false);
  const savedDraft = useRef<TaskDraft | null>(null);
  const busyRef = useRef(false);
  useEffect(() => {
    if (!savingRef.current && !error && task?.task) {
      setText(String(task.task[field] ?? ''));
      savedDraft.current = task.draft;
    }
  }, [task?.task?.[field], task?.draft?.revision, field, error]);

  const save = (value: string) => {
    setText(value);
    setSaving(true);
    savingRef.current = true;
    setError(false);
    const revision = ++sequence.current;
    const pending = offline.saveDraft(workOrderId, taskId, value, field);
    void pending
      .then((draft) => {
        if (revision === sequence.current) {
          savedDraft.current = draft;
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
    if (busyRef.current) return;
    busyRef.current = true;
    setBusy(true);
    try {
      await action();
    } catch (failure) {
      offline.reportError(failure);
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  };
  const submit = () => {
    if (savingRef.current || error) return;
    // Capture the durable revision at the tap, before any asynchronous work.
    const revision = savedDraft.current?.revision ?? task?.draft?.revision;
    if (revision !== undefined)
      void run(() => offline.submitDraft(taskId, revision, field));
  };
  const operation = task?.operation;
  const blocked =
    operation &&
    ['QUEUED', 'SENDING', 'UNKNOWN', 'CONFLICT', 'REJECTED'].includes(
      operation.status
    );
  const conflict =
    operation &&
    (['CONFLICT', 'REJECTED'].includes(operation.status) ||
      !!operation.blockedReason);
  const needsReview = conflict || !!task?.draft?.blockedReason;
  const status = saving
    ? 'offline_saving'
    : error
    ? 'offline_save_failed'
    : task?.draft
    ? 'offline_draft_saved'
    : null;
  const sendStatus =
    operation?.status === 'APPLIED'
      ? 'offline_applied'
      : operation?.status === 'SENDING'
      ? 'offline_sending'
      : operation?.status === 'UNKNOWN'
      ? 'offline_unknown'
      : operation?.status === 'QUEUED'
      ? 'offline_queued'
      : operation?.status === 'CONFLICT'
      ? 'offline_conflict'
      : operation?.status === 'REJECTED'
      ? 'offline_rejected'
      : null;

  const type = task?.serverTask?.taskBase.taskType;
  const numeric = field === 'value' && (type === 'NUMBER' || type === 'METER');
  const numberValid = !numeric || /^(?:\d+\.?\d*|\.\d+)$/.test(text);
  const options =
    field === 'value' && type === 'SUBTASK'
      ? ['OPEN', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETE'].map((value) => ({
          value,
          label: t(value)
        }))
      : field === 'value' && type === 'INSPECTION'
      ? ['PASS', 'FLAG', 'FAIL'].map((value) => ({ value, label: t(value) }))
      : field === 'value' && type === 'MULTIPLE'
      ? (task?.serverTask?.taskBase.options ?? []).map((option) => ({
          value: option.label,
          label: option.label
        }))
      : null;
  const optionValid =
    !options || options.some((option) => option.value === text);
  const disabled = !editable || !task?.canEdit || !offline.writable;

  if (!task?.available) return <Text>{t('offline_orphan')}</Text>;
  return (
    <View>
      {options ? (
        <Button
          mode="outlined"
          disabled={disabled}
          onPress={() =>
            SheetManager.show('dropdown-sheet', {
              payload: {
                value: text,
                items: options,
                setValue: (value: string) => save(value)
              }
            })
          }
          testID={`offline-task-${field}-${taskId}`}
        >
          {options.find((option) => option.value === text)?.label ||
            text ||
            t('value')}
        </Button>
      ) : (
        <TextInput
          mode="outlined"
          label={t(field)}
          value={text}
          onChangeText={(value) =>
            save(
              numeric
                ? value.replace(/[^0-9.]/g, '').replace(/(\..*)\./g, '$1')
                : value
            )
          }
          maxLength={255}
          multiline={!numeric}
          keyboardType={numeric ? 'decimal-pad' : 'default'}
          disabled={disabled}
          testID={`offline-task-${field}-${taskId}`}
        />
      )}
      <HelperText type={error ? 'error' : 'info'}>
        {t(
          status ||
            (field === 'notes' ? 'offline_max_notes' : 'offline_max_text')
        )}
      </HelperText>
      {task.draft && !numberValid && (
        <HelperText type="error">{t('offline_invalid_number')}</HelperText>
      )}
      {task.draft && !optionValid && (
        <HelperText type="error">{t('offline_invalid_choice')}</HelperText>
      )}
      {sendStatus && (
        <View>
          <Text>
            {t('offline_submission')}: {t(sendStatus)}
          </Text>
          {operation.status !== 'APPLIED' && (
            <Text>{operationText(operation.request)}</Text>
          )}
        </View>
      )}
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
            !numberValid ||
            !optionValid ||
            disabled
          }
          loading={busy}
          testID={`offline-task-submit-${field}-${taskId}`}
        >
          {t(field === 'notes' ? 'offline_submit_notes' : 'offline_submit')}
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
            {t('offline_server_value')}:{' '}
            {String(task.serverTask?.[field] ?? '')}
          </Text>
          <Text>{t('offline_resume_after_conflict')}</Text>
          <Button
            disabled={busy || saving || error || disabled}
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
                          serverVersion,
                          field
                        )
                      )
                  }
                ]
              );
            }}
          >
            {t('offline_keep_draft')}
          </Button>
          <Button
            disabled={
              busy ||
              saving ||
              error ||
              disabled ||
              (!!operation &&
                (operation.status === 'UNKNOWN' ||
                  operation.status === 'SENDING' ||
                  (operation.status === 'QUEUED' &&
                    operation.attemptCount > 0)))
            }
            onPress={() => {
              const expectedRevision = task.draft?.revision ?? null;
              const operationId = operation?.id ?? null;
              const serverVersion = task.serverTask.version;
              Alert.alert(
                t('offline_use_server'),
                t('offline_discard_confirm'),
                [
                  { text: t('cancel'), style: 'cancel' },
                  {
                    text: t('offline_use_server'),
                    style: 'destructive',
                    onPress: () =>
                      void run(() =>
                        offline.discardTaskField(
                          taskId,
                          field,
                          expectedRevision,
                          operationId,
                          serverVersion
                        )
                      )
                  }
                ]
              );
            }}
          >
            {t('offline_use_server')}
          </Button>
        </View>
      )}
    </View>
  );
}
