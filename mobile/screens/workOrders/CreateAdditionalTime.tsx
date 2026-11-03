import { ScrollView } from 'react-native';

import { View } from '../../components/Themed';
import { useTranslation } from 'react-i18next';
import { IField } from '../../models/form';
import * as Yup from 'yup';
import Form from '../../components/form';
import { formatSelect } from '../../utils/formatters';
import { RootStackScreenProps } from '../../types';
import { useDispatch } from '../../store';
import { createLabor } from '../../slices/labor';
import { getErrorMessage } from '../../utils/api';
import { useContext, useEffect, useRef, useState } from 'react';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { Button, HelperText, Text, TextInput } from 'react-native-paper';
import CustomDateTimePicker from '../../components/CustomDateTimePicker';
import { useOffline } from '../../offline/OfflineProvider';
import { useOfflineTime } from '../../offline/useOfflineTime';
import type { ManualTimeDraft, ManualTimeInput } from '../../offline/types';
import {
  assertSessionGeneration,
  sessionGeneration
} from '../../offline/sessionBridge';

export default function CreateAdditionalTime({
  navigation,
  route
}: RootStackScreenProps<'AddAdditionalTime'>) {
  const offline = useOffline();
  const prepared =
    offline.enabled &&
    offline.prepared.some((item) => item.id === route.params.workOrderId);
  const time = useOfflineTime(route.params.workOrderId, prepared);
  if (prepared && !route.params.connectedOnly) {
    if (!time.view?.available) return <View />;
    return (
      <PersonalTimeForm
        key={`${offline.repository?.contextKey}:${route.params.workOrderId}`}
        workOrderId={route.params.workOrderId}
        initialDraft={time.view.manualDraft}
        allowed={time.view.permissions.createManualTime}
        navigation={navigation}
      />
    );
  }
  return <ConnectedTimeForm navigation={navigation} route={route} />;
}

function ConnectedTimeForm({
  navigation,
  route
}: RootStackScreenProps<'AddAdditionalTime'>) {
  const offline = useOffline();
  const connected = !offline.enabled || offline.networkAvailable;
  const { t } = useTranslation();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const inputRevision = useRef(0);
  const mounted = useRef(true);
  useEffect(
    () => () => {
      mounted.current = false;
    },
    []
  );
  const fields: Array<IField> = [
    {
      name: 'assignedTo',
      type: 'select',
      label: t('assigned_to'),
      type2: 'user',
      midWidth: true
    },
    {
      name: 'hourlyRate',
      type: 'number',
      label: t('hourly_rate'),
      midWidth: true
    },
    {
      name: 'includeToTotalTime',
      type: 'switch',
      label: t('include_time'),
      helperText: t('include_time_description')
    },
    {
      name: 'startedAt',
      type: 'date',
      label: t('work_started_at')
    },
    {
      name: 'timeCategory',
      type: 'select',
      label: t('category'),
      type2: 'category',
      category: 'time-categories'
    },
    {
      name: 'duration',
      type: 'titleGroupField',
      label: t('duration')
    },
    {
      name: 'hours',
      type: 'number',
      label: t('hours'),
      midWidth: true,
      required: true
    },
    {
      name: 'minutes',
      type: 'number',
      label: t('minutes'),
      midWidth: true,
      required: true
    }
  ];
  const shape = {
    hours: Yup.number().required(t('required_hours')),
    minutes: Yup.number().required(t('required_minutes')),
    startedAt: Yup.date().required(t('required_field'))
  };
  return (
    <View style={{ flex: 1 }}>
      <Form
        readOnly={!connected}
        submitDisabled={!connected}
        fields={fields}
        navigation={navigation}
        validation={Yup.object().shape(shape)}
        submitText={t('add')}
        values={{ includeToTotalTime: true }}
        onChange={() => {
          inputRevision.current += 1;
        }}
        onSubmit={async (values) => {
          const submittedRevision = inputRevision.current;
          const formattedValues = { ...values };
          formattedValues.assignedTo = formatSelect(formattedValues.assignedTo);
          formattedValues.timeCategory = formatSelect(
            formattedValues.timeCategory
          );
          formattedValues.duration = values.hours * 3600 + values.minutes * 60;
          if (!connected) return;
          const generation = sessionGeneration();
          try {
            const result = await dispatch(
              createLabor(route.params.workOrderId, formattedValues)
            );
            assertSessionGeneration(generation);
            if (!mounted.current) return;
            if (result?.localUpdateError)
              showSnackBar(t('offline_time_saved_refresh_pending'), 'error');
            if (inputRevision.current === submittedRevision)
              navigation.goBack();
          } catch (error) {
            if (mounted.current && generation === sessionGeneration())
              showSnackBar(getErrorMessage(error), 'error');
          }
        }}
      />
    </View>
  );
}

/** A personal draft has no remote selectors and is never submitted by reconnection. */
function PersonalTimeForm({
  workOrderId,
  initialDraft,
  allowed,
  navigation
}: {
  workOrderId: number;
  initialDraft: ManualTimeDraft | null;
  allowed: boolean;
  navigation: RootStackScreenProps<'AddAdditionalTime'>['navigation'];
}) {
  const offline = useOffline();
  const { t } = useTranslation();
  const [values, setValues] = useState<ManualTimeInput>(
    () =>
      initialDraft || {
        startedAt: new Date().toISOString(),
        hours: '0',
        minutes: '0'
      }
  );
  const [saving, setSaving] = useState(false);
  const [failed, setFailed] = useState(false);
  const [busy, setBusy] = useState(false);
  const [saved, setSaved] = useState(!!initialDraft);
  const valuesRef = useRef(values);
  const draft = useRef(initialDraft);
  const tail = useRef<Promise<unknown>>(Promise.resolve());
  const sequence = useRef(0);
  const durableSequence = useRef(0);
  const failedRef = useRef(false);
  const busyRef = useRef(false);
  const mounted = useRef(true);
  const generation = useRef(sessionGeneration());
  useEffect(
    () => () => {
      mounted.current = false;
    },
    []
  );
  const current = () =>
    mounted.current && generation.current === sessionGeneration();
  const editable = allowed && offline.writable;
  const change = (patch: Partial<ManualTimeInput>) => {
    if (!editable) return;
    const next = { ...valuesRef.current, ...patch };
    valuesRef.current = next;
    setValues(next);
    const revision = ++sequence.current;
    setSaving(true);
    setFailed(false);
    const pending = tail.current.then(async () => {
      assertSessionGeneration(generation.current);
      const result = await offline.saveManualTimeDraft(
        workOrderId,
        next,
        draft.current?.revision ?? null
      );
      draft.current = result;
      durableSequence.current = revision;
      failedRef.current = false;
      if (current() && sequence.current === revision) {
        failedRef.current = false;
        setFailed(false);
        setSaving(false);
        setSaved(true);
      }
    });
    tail.current = pending.catch((error) => {
      failedRef.current = true;
      if (current() && sequence.current === revision) {
        failedRef.current = true;
        setSaving(false);
        setFailed(true);
        offline.reportError(error);
      }
    });
  };
  const submit = async () => {
    if (!editable || busyRef.current) return;
    busyRef.current = true;
    setBusy(true);
    const submittedSequence = sequence.current;
    const pending = tail.current.then(async () => {
      assertSessionGeneration(generation.current);
      if (
        failedRef.current ||
        !draft.current ||
        durableSequence.current !== submittedSequence
      )
        return;
      await offline.submitManualTimeDraft(workOrderId, draft.current.revision);
      draft.current = null;
      if (current() && submittedSequence === sequence.current)
        navigation.goBack();
    });
    // Later typing is queued after this exact revision is consumed.
    tail.current = pending.catch((error) => {
      if (current()) offline.reportError(error);
    });
    await tail.current;
    if (current()) {
      busyRef.current = false;
      setBusy(false);
    }
  };
  const duration = Number(values.hours) * 3600 + Number(values.minutes) * 60;
  const valid =
    /^\d+$/.test(values.hours) &&
    /^\d+$/.test(values.minutes) &&
    Number.isSafeInteger(duration) &&
    duration > 0 &&
    Number.isFinite(Date.parse(values.startedAt));
  return (
    <ScrollView
      contentContainerStyle={{ padding: 16, gap: 16 }}
      keyboardShouldPersistTaps="handled"
    >
      <Text variant="titleMedium">{t('offline_time_personal')}</Text>
      <View pointerEvents={editable ? 'auto' : 'none'}>
        <CustomDateTimePicker
          label={t('work_started_at')}
          value={new Date(values.startedAt)}
          onChange={(date) => change({ startedAt: date.toISOString() })}
        />
      </View>
      <TextInput
        label={t('hours')}
        value={values.hours}
        keyboardType="numeric"
        editable={editable}
        onChangeText={(hours) =>
          change({ hours: hours.replace(/[^0-9]/g, '') })
        }
      />
      <TextInput
        label={t('minutes')}
        value={values.minutes}
        keyboardType="numeric"
        editable={editable}
        onChangeText={(minutes) =>
          change({ minutes: minutes.replace(/[^0-9]/g, '') })
        }
      />
      {(saving || failed || saved) && (
        <HelperText type={failed ? 'error' : 'info'}>
          {t(
            saving
              ? 'offline_saving'
              : failed
              ? 'offline_save_failed'
              : 'offline_draft_saved'
          )}
        </HelperText>
      )}
      <Button
        mode="contained"
        loading={busy}
        disabled={!editable || busy || failed || !valid || (!saved && !saving)}
        onPress={() => void submit()}
      >
        {t('offline_time_save')}
      </Button>
    </ScrollView>
  );
}
