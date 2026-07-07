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
import { getErrorMessage } from '../../utils/api';
import { useContext } from 'react';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';

export default function EditAdditionalTime({
  navigation,
  route
}: RootStackScreenProps<'EditAdditionalTime'>) {
  const { t } = useTranslation();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const { workOrderId, labor } = route.params;

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
      <Form
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
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          const formattedValues = { ...values };
          formattedValues.assignedTo = formatSelect(formattedValues.assignedTo);
          formattedValues.timeCategory = formatSelect(
            formattedValues.timeCategory
          );
          formattedValues.duration = values.hours * 3600 + values.minutes * 60;
          return dispatch(
            editLabor(labor.id, workOrderId, formattedValues)
          )
            .catch((err) => showSnackBar(getErrorMessage(err), 'error'))
            .finally(() => navigation.goBack());
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({});
