import { StyleSheet } from 'react-native';
import { View } from '../../components/Themed';
import { useTranslation } from 'react-i18next';
import { IField } from '../../models/form';
import * as Yup from 'yup';
import Form from '../../components/form';
import { formatSelect } from '../../utils/formatters';
import { editLabor } from '../../slices/labor';
import { RootStackScreenProps } from '../../types';
import { useDispatch } from '../../store';
import api, { getErrorMessage } from '../../utils/api';
import { useContext, useEffect, useRef, useState } from 'react';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { Button, Text } from 'react-native-paper';
import Labor from '../../models/labor';
import { useOffline } from '../../offline/OfflineProvider';
import {
  assertSessionGeneration,
  sessionGeneration
} from '../../offline/sessionBridge';

export default function EditAdditionalTime({
  navigation,
  route
}: RootStackScreenProps<'EditAdditionalTime'>) {
  const { t } = useTranslation();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const { workOrderId, labor } = route.params;
  const offline = useOffline();
  const connected = !offline.enabled || offline.networkAvailable;
  const [baseLabor, setBaseLabor] = useState<Labor>(() => ({ ...labor }));
  const [conflict, setConflict] = useState(false);
  const inputRevision = useRef(0);
  const mounted = useRef(true);
  useEffect(
    () => () => {
      mounted.current = false;
    },
    []
  );
  const [reviewed, setReviewed] = useState<Labor | null>(null);

  const hours = Math.floor(labor.duration / 3600);
  const minutes = Math.floor((labor.duration % 3600) / 60);

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
      {conflict && (
        <View style={{ padding: 12 }}>
          <Text accessibilityRole="alert">{t('labor_changed_review')}</Text>
          <Button
            disabled={!connected}
            onPress={async () => {
              const generation = sessionGeneration();
              try {
                const current = await api.get<Labor>(`labors/${baseLabor.id}`);
                assertSessionGeneration(generation);
                if (mounted.current) setReviewed(current);
              } catch (error) {
                if (mounted.current && generation === sessionGeneration())
                  showSnackBar(getErrorMessage(error), 'error');
              }
            }}
          >
            {t('labor_review_current')}
          </Button>
          {reviewed && (
            <View>
              <Text>
                {t('labor_server_duration')}: {reviewed.duration / 60}{' '}
                {t('minutes')}
              </Text>
              <Text>
                {t('work_started_at')}:{' '}
                {new Date(reviewed.startedAt).toLocaleString()}
              </Text>
              <Text>
                {t('assigned_to')}: {reviewed.assignedTo?.firstName}{' '}
                {reviewed.assignedTo?.lastName}
              </Text>
              <Text>
                {t('hourly_rate')}: {reviewed.hourlyRate} · {t('category')}:{' '}
                {reviewed.timeCategory?.name || '—'} · {t('include_time')}:{' '}
                {String(reviewed.includeToTotalTime)}
              </Text>
              <Button
                onPress={() => {
                  setBaseLabor(reviewed);
                  setReviewed(null);
                  setConflict(false);
                }}
              >
                {t('labor_review_confirm')}
              </Button>
            </View>
          )}
        </View>
      )}
      <Form
        readOnly={!connected}
        submitDisabled={!connected || conflict}
        fields={fields}
        navigation={navigation}
        validation={Yup.object().shape(shape)}
        submitText={t('save')}
        values={{
          assignedTo: labor.assignedTo
            ? {
                label: `${labor.assignedTo.firstName} ${labor.assignedTo.lastName}`,
                value: labor.assignedTo.id
              }
            : null,
          hourlyRate: labor.hourlyRate?.toString(),
          includeToTotalTime: labor.includeToTotalTime,
          startedAt: labor.startedAt ? new Date(labor.startedAt) : null,
          timeCategory: labor.timeCategory
            ? {
                label: labor.timeCategory.name,
                value: labor.timeCategory.id
              }
            : null,
          hours: hours.toString(),
          minutes: minutes.toString()
        }}
        onChange={() => {
          inputRevision.current += 1;
        }}
        onSubmit={async (values) => {
          if (!connected || conflict) return;
          const generation = sessionGeneration();
          const submittedRevision = inputRevision.current;
          const formattedValues: any = {
            ...values,
            version: baseLabor.version
          };
          formattedValues.assignedTo = formatSelect(formattedValues.assignedTo);
          formattedValues.timeCategory = formatSelect(
            formattedValues.timeCategory
          );
          formattedValues.duration = values.hours * 3600 + values.minutes * 60;
          try {
            const result = await dispatch(
              editLabor(baseLabor.id, workOrderId, formattedValues)
            );
            assertSessionGeneration(generation);
            if (!mounted.current) return;
            if (result?.localUpdateError)
              showSnackBar(t('offline_time_saved_refresh_pending'), 'error');
            if (result?.labor) setBaseLabor(result.labor);
            if (inputRevision.current === submittedRevision)
              navigation.goBack();
          } catch (error) {
            if (mounted.current && generation === sessionGeneration()) {
              setConflict(error?.message === 'conflict_error');
              showSnackBar(getErrorMessage(error), 'error');
            }
          }
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({});
