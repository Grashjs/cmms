import { StyleSheet } from 'react-native';
import * as Yup from 'yup';
import { View } from '../../components/Themed';
import { RootStackScreenProps } from '../../types';
import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useRef, useState } from 'react';
import { IField } from '../../models/form';
import Form from '../../components/form';
import api, { getErrorMessage } from '../../utils/api';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { Button, HelperText, Text, TextInput } from 'react-native-paper';
import SignaturePad, { SignaturePadHandle, SignatureSaveState } from '../../components/form/SignaturePad';
import { useOffline } from '../../offline/OfflineProvider';
import { useOfflineStatus } from '../../offline/useOfflineStatus';
import { assertSessionGeneration, sessionGeneration } from '../../offline/sessionBridge';
import { OfflineError, StatusDraft, StatusInput } from '../../offline/types';
import { offlineErrorKey } from '../../offline/errors';
import WorkOrder, { WorkOrderStatusGuard } from '../../models/workOrder';
import Labor from '../../models/labor';

export default function CompleteWorkOrderModal({
  navigation,
  route
}: RootStackScreenProps<'CompleteWorkOrder'>) {
  const { onComplete, fieldsConfig } = route.params;
  const { t }: { t: any } = useTranslation();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const offline = useOffline();
  const connectedRevision = useRef(0);
  const connectedSignatureDirty = useRef(false);
  const [signatureDirty, setSignatureDirty] = useState(false);
  const connectedGeneration = useRef(sessionGeneration()).current;
  const mounted = useRef(true);
  const [conflict, setConflict] = useState(false);
  const [reviewed, setReviewed] = useState<{ workOrder: WorkOrder; guard: WorkOrderStatusGuard } | null>(null);
  useEffect(() => {
    mounted.current = true;
    return () => { mounted.current = false; };
  }, []);

  if (route.params.workOrderId !== undefined) {
    return (
      <PreparedClosure
        key={`${offline.repository?.contextKey}:${sessionGeneration()}:${route.params.workOrderId}`}
        workOrderId={route.params.workOrderId}
        navigation={navigation}
      />
    );
  }

  const getFieldsAndShape = (): [Array<IField>, { [key: string]: any }] => {
    let fields: IField[] = [];
    let shape = {};
    if (fieldsConfig.feedback) {
      fields.push({
        name: 'feedback',
        type: 'text',
        label: t('feedback'),
        placeholder: t('feedback_description'),
        multiple: true
      });
      shape = { feedback: Yup.string().required(t('required_feedback')) };
    }
    if (fieldsConfig.signature) {
      fields.push({
        name: 'signature',
        type: 'signature',
        label: t('signature')
      });
      shape = {
        ...shape,
        signature: Yup.string().required(t('required_signature'))
      };
    }
    return [fields, shape];
  };
  return (
    <View style={styles.container}>
      {conflict && (
        <>
          <HelperText type="error">{t('work_order_status_review')}</HelperText>
          <Button onPress={async () => {
            try {
              assertSessionGeneration(connectedGeneration);
              const [workOrder, labors] = await Promise.all([
                api.get<WorkOrder>(`work-orders/${route.params.connectedWorkOrderId}`),
                api.get<Labor[]>(`labors/work-order/${route.params.connectedWorkOrderId}`)
              ]);
              assertSessionGeneration(connectedGeneration);
              if (!mounted.current) return;
              setReviewed({ workOrder, guard: {
                baseRevision: workOrder.timerStatusRevision,
                activeTimerRuns: labors.filter((labor) => labor.logged && labor.status === 'RUNNING')
                  .map((labor) => ({ laborId: labor.id, timerRunId: labor.timerRunId }))
              } });
            } catch (error) {
              if (mounted.current && connectedGeneration === sessionGeneration()) showSnackBar(t(getErrorMessage(error)), 'error');
            }
          }}>{t('work_order_status_review_current')}</Button>
          {reviewed && (
            <>
              <Text>{t('work_order_status_review_value', {
                status: t(reviewed.workOrder.status), count: reviewed.guard.activeTimerRuns.length
              })}</Text>
              <Button onPress={() => {
                assertSessionGeneration(connectedGeneration);
                route.params.onReviewStatus?.(reviewed.guard);
                setReviewed(null);
                setConflict(false);
              }}>{t('work_order_status_review_confirm')}</Button>
            </>
          )}
        </>
      )}
      {signatureDirty && <HelperText type="info">{t('offline_signature_unsaved')}</HelperText>}
      <Form
        fields={getFieldsAndShape()[0]}
        validation={Yup.object().shape(getFieldsAndShape()[1])}
        submitText={t('complete_work_order')}
        values={{}}
        navigation={navigation}
        submitDisabled={conflict || signatureDirty}
        onChange={({ field }) => {
          connectedRevision.current += 1;
          if (field === 'signature') {
            connectedSignatureDirty.current = false;
            setSignatureDirty(false);
          }
        }}
        onSignatureInput={() => {
          connectedRevision.current += 1;
          connectedSignatureDirty.current = true;
          setSignatureDirty(true);
        }}
        onSubmit={async (values) => {
          if (conflict || connectedSignatureDirty.current) return;
          const submittedRevision = connectedRevision.current;
          try {
            assertSessionGeneration(connectedGeneration);
            await onComplete(values.signature, values.feedback);
            assertSessionGeneration(connectedGeneration);
            if (mounted.current && submittedRevision === connectedRevision.current) navigation.navigate('Root');
          } catch (err) {
            if (mounted.current && connectedGeneration === sessionGeneration()) {
              if ((err?.status === 409 || err?.message === 'conflict_error') && route.params.onReviewStatus) setConflict(true);
              showSnackBar(t(getErrorMessage(err)), 'error');
            }
          }
        }}
      />
    </View>
  );
}

function PreparedClosure({ workOrderId, navigation }) {
  const status = useOfflineStatus(workOrderId);
  if (!status.view) return <View style={styles.container} />;
  return <PreparedClosureEditor status={status} navigation={navigation} />;
}

function PreparedClosureEditor({
  status,
  navigation
}: {
  status: ReturnType<typeof useOfflineStatus>;
  navigation: RootStackScreenProps<'CompleteWorkOrder'>['navigation'];
}) {
  const offline = useOffline();
  const { t } = useTranslation();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const generation = useRef(sessionGeneration()).current;
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    return () => { mounted.current = false; };
  }, []);
  const initial = useRef(status.view.draft?.status === 'COMPLETE' ? status.view.draft : null).current;
  const [input, setInput] = useState<StatusInput>({
    status: 'COMPLETE', feedback: initial?.feedback ?? '', signature: initial?.signature ?? ''
  });
  const current = useRef(input);
  const inputRevision = useRef(0);
  const gestureRevision = useRef(0);
  const savedDraft = useRef<StatusDraft | null>(initial);
  const writes = useRef<Promise<StatusDraft | null>>(Promise.resolve(initial));
  const signature = useRef<SignaturePadHandle>(null);
  const [saving, setSaving] = useState(false);
  const [signatureState, setSignatureState] = useState<SignatureSaveState>(initial?.signature ? 'saved' : 'dirty');
  const [saveFailed, setSaveFailed] = useState(false);
  const [saved, setSaved] = useState(!!initial);
  const [submitting, setSubmitting] = useState(false);
  const submittingRef = useRef(false);
  const requirements = status.view.closure;
  const closureChecks = status.view.closureChecks;
  const missingRequirements = closureChecks.some((check) => check.state === 'missing');
  const writable = offline.writable && status.view.available && !!requirements?.canChangeStatus;

  const save = (next: StatusInput) => {
    assertSessionGeneration(generation);
    current.current = next;
    setInput(next);
    const revision = ++inputRevision.current;
    setSaving(true);
    setSaveFailed(false);
    setSaved(false);
    // Keep all accepted inputs in the hook's ordered, context-checked write lane.
    const write = status.save(next);
    writes.current = write;
    return write.then((draft) => {
      assertSessionGeneration(generation);
      if (mounted.current && revision === inputRevision.current) {
        savedDraft.current = draft;
        setSaved(true);
      }
      return draft;
    }).catch((error) => {
      if (mounted.current && generation === sessionGeneration() && revision === inputRevision.current) setSaveFailed(true);
      throw error;
    }).finally(() => {
      if (mounted.current && generation === sessionGeneration() && revision === inputRevision.current) setSaving(false);
    });
  };

  const submit = async () => {
    if (submittingRef.current) return;
    submittingRef.current = true;
    setSubmitting(true);
    try {
      assertSessionGeneration(generation);
      let write: typeof writes.current;
      do {
        await signature.current?.waitForSave();
        write = writes.current;
        await write;
      } while (write !== writes.current);
      await signature.current?.waitForSave();
      assertSessionGeneration(generation);
      if (!mounted.current) return;
      if (!savedDraft.current) await save(current.current);
      const revision = inputRevision.current;
      const submittedGesture = gestureRevision.current;
      const fields: Record<string, any> = {};
      if (requirements.askFeedback) fields.feedback = Yup.string().required(t('required_feedback'));
      if (requirements.requiredSignature) fields.signature = Yup.string().required(t('required_signature'));
      Yup.object(fields).validateSync(current.current);
      if (saveFailed) throw new OfflineError('DRAFT_SAVE_FAILED');
      await status.submit(savedDraft.current.revision);
      assertSessionGeneration(generation);
      if (!mounted.current) return;
      showSnackBar(t('offline_queued'), 'success');
      // A newer input belongs to a later draft, even when its text is identical.
      if (revision === inputRevision.current && submittedGesture === gestureRevision.current && !signature.current?.isPending()) navigation.goBack();
    } catch (error) {
      if (mounted.current && generation === sessionGeneration()) showSnackBar(t(error instanceof OfflineError ? offlineErrorKey(error) : getErrorMessage(error)), 'error');
    } finally {
      submittingRef.current = false;
      if (mounted.current && generation === sessionGeneration()) setSubmitting(false);
    }
  };

  const fields: IField[] = [
    ...(requirements?.askFeedback ? [{ name: 'feedback', type: 'text' as const, label: t('feedback') }] : []),
    ...(requirements?.requiredSignature ? [{ name: 'signature', type: 'signature' as const, label: t('signature') }] : [])
  ];
  return (
    <View style={styles.container}>
      <Text>{t('offline_closure_local')}</Text>
      {closureChecks.filter((check) => check.state !== 'satisfied').map((check) => (
        <HelperText key={check.field} type={check.state === 'missing' ? 'error' : 'info'}>
          {check.state === 'missing'
            ? t(check.messageKey)
            : `${t(check.labelKey)} — ${t('offline_status_dependency_pending')}`}
        </HelperText>
      ))}
      {saving && <Text>{t('offline_saving')}</Text>}
      {!saving && saved && !saveFailed && (!requirements?.requiredSignature || signatureState === 'saved') && <Text>{t('offline_draft_saved')}</Text>}
      {saveFailed && signatureState !== 'error' && (
        <>
          <HelperText type="error">{t('offline_closure_save_failed')}</HelperText>
          <Button disabled={!writable || saving} onPress={async () => {
            try {
              if (mounted.current) await save(current.current);
            } catch {
              // Keep the current input and its warning on a failed local write.
            }
          }}>
            {t('retry')}
          </Button>
        </>
      )}
      <Form
        fields={fields}
        navigation={navigation}
        values={{}}
        validation={Yup.object()}
        submitText={t('offline_closure_submit')}
        submitDisabled={!writable || missingRequirements || saving || saveFailed || status.busy || submitting ||
          (!!requirements?.requiredSignature && signatureState !== 'saved') ||
          (!!requirements?.requiredSignature && !requirements.canCaptureSignature)}
        fieldOverrides={{
          feedback: (
            <TextInput label={t('feedback')} multiline value={input.feedback ?? ''} disabled={!writable}
              onChangeText={(feedback) => void save({ ...current.current, feedback }).catch(() => undefined)} />
          ),
          signature: (
            <SignaturePad ref={signature} label={t('signature')} durable value={initial?.signature ?? ''}
              onInputStart={() => { gestureRevision.current += 1; }}
              disabled={!writable || !requirements?.canCaptureSignature}
              onStateChange={setSignatureState}
              onChange={async (value) => {
                await save({ ...current.current, signature: value });
                assertSessionGeneration(generation);
              }} />
          )
        }}
        onSubmit={submit}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    marginTop: 'auto'
  }
});
