import { Dialog, DialogContent, DialogTitle, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import Form from '../../components/form';
import * as Yup from 'yup';
import { IField } from '../../type';
import { formatSelect } from '../../../../utils/formatters';
import { useDispatch } from '../../../../store';
import { createLabor } from '../../../../slices/labor';
import useAuth from '../../../../hooks/useAuth';
import FeatureErrorMessage from '../../components/FeatureErrorMessage';
import { PlanFeature } from '../../../../models/owns/subscriptionPlan';

interface AddTimeProps {
  open: boolean;
  onClose: () => void;
  workOrderId: number;
}

export default function AddTimeModal({
                                       open,
                                       onClose,
                                       workOrderId
                                     }: AddTimeProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { hasFeature } = useAuth();
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
    <Dialog fullWidth maxWidth="sm" open={open} onClose={onClose}>
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {t('add_time')}
        </Typography>
        <Typography variant="subtitle2">{t('add_time_description')}</Typography>
      </DialogTitle>
      <DialogContent
        dividers
        sx={{
          p: 3
        }}
      >
        {hasFeature(PlanFeature.ADDITIONAL_TIME) ? (
          <Form
            fields={fields}
            validation={Yup.object().shape(shape)}
            submitText={t('add')}
            values={{ includeToTotalTime: true }}
            onChange={({ field, e }) => {
            }}
            onSubmit={async (values) => {
              const formattedValues = { ...values };
              formattedValues.assignedTo = formatSelect(
                formattedValues.assignedTo
              );
              formattedValues.timeCategory = formatSelect(
                formattedValues.timeCategory
              );
              formattedValues.duration =
                values.hours * 3600 + values.minutes * 60;
              return dispatch(
                createLabor(workOrderId, formattedValues)
              ).finally(() => onClose());
            }}
          />
        ) : (
          <FeatureErrorMessage message="Upgrade to add Itemized time tracking to your Work Orders. " />
        )}
      </DialogContent>
    </Dialog>
  );
}
