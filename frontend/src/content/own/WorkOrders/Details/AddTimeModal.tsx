import {
  Box,
  Button,
  Dialog,
  DialogContent,
  DialogTitle,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import Form from '../../components/form';
import * as Yup from 'yup';
import { IField } from '../../type';
import {
  formatSelect,
  getHoursAndMinutesAndSeconds
} from '../../../../utils/formatters';
import { useDispatch } from '../../../../store';
import { createLabor, editLabor } from '../../../../slices/labor';
import useAuth from '../../../../hooks/useAuth';
import FeatureErrorMessage from '../../components/FeatureErrorMessage';
import { PlanFeature } from '../../../../models/owns/subscriptionPlan';
import api, { getErrorMessage } from '../../../../utils/api';
import { useContext, useEffect, useRef, useState } from 'react';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import Labor from '../../../../models/owns/labor';

interface AddTimeProps {
  open: boolean;
  onClose: () => void;
  workOrderId: number;
  labor?: Labor;
}

export default function AddTimeModal({
  open,
  onClose,
  workOrderId,
  labor
}: AddTimeProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { hasFeature } = useAuth();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [baseLabor, setBaseLabor] = useState<Labor | null>(null);
  const [conflict, setConflict] = useState(false);
  const [reviewed, setReviewed] = useState<Labor | null>(null);
  const wasOpen = useRef(false);
  const inputRevision = useRef(0);
  const activeFormSession = useRef(0);
  const [formSession, setFormSession] = useState(0);
  useEffect(() => {
    if (open && !wasOpen.current) {
      activeFormSession.current += 1;
      setBaseLabor(labor ? { ...labor } : null);
      setFormSession((value) => value + 1);
      setConflict(false);
      setReviewed(null);
    }
    wasOpen.current = open;
  }, [open, labor]);
  const isEdit = !!labor;
  const [editHours, editMinutes] = baseLabor
    ? getHoursAndMinutesAndSeconds(baseLabor.duration)
    : [0, 0];
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
  const defaultValues = baseLabor
    ? {
        ...baseLabor,
        assignedTo: baseLabor.assignedTo
          ? {
              label: `${baseLabor.assignedTo.firstName} ${baseLabor.assignedTo.lastName}`,
              value: baseLabor.assignedTo.id
            }
          : null,
        timeCategory: baseLabor.timeCategory
          ? {
              label: baseLabor.timeCategory.name,
              value: baseLabor.timeCategory.id
            }
          : null,
        hours: editHours,
        minutes: editMinutes
      }
    : { includeToTotalTime: true };
  return (
    <Dialog fullWidth maxWidth="sm" open={open} onClose={onClose}>
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {isEdit ? t('edit_time') : t('add_time')}
        </Typography>
        <Typography variant="subtitle2">{t('add_time_description')}</Typography>
      </DialogTitle>
      <DialogContent
        dividers
        sx={{
          p: 3
        }}
      >
        {conflict && (
          <Box>
            <Typography color="error">{t('labor_changed_review')}</Typography>
            <Button
              onClick={async () => {
                const reviewedSession = activeFormSession.current;
                try {
                  const current = await api.get<Labor>(
                    `labors/${baseLabor.id}`
                  );
                  if (
                    wasOpen.current &&
                    reviewedSession === activeFormSession.current
                  )
                    setReviewed(current);
                } catch (error) {
                  if (
                    wasOpen.current &&
                    reviewedSession === activeFormSession.current
                  )
                    showSnackBar(getErrorMessage(error), 'error');
                }
              }}
            >
              {t('labor_review_current')}
            </Button>
            {reviewed && (
              <Box>
                <Typography>
                  {t('labor_server_duration')}: {reviewed.duration / 60}{' '}
                  {t('minutes')}
                </Typography>
                <Typography>
                  {t('work_started_at')}:{' '}
                  {new Date(reviewed.startedAt).toLocaleString()}
                </Typography>
                <Typography>
                  {t('assigned_to')}: {reviewed.assignedTo?.firstName}{' '}
                  {reviewed.assignedTo?.lastName}
                </Typography>
                <Typography>
                  {t('hourly_rate')}: {reviewed.hourlyRate} · {t('category')}:{' '}
                  {reviewed.timeCategory?.name || '—'} · {t('include_time')}:{' '}
                  {String(reviewed.includeToTotalTime)}
                </Typography>
                <Button
                  onClick={() => {
                    setBaseLabor(reviewed);
                    setReviewed(null);
                    setConflict(false);
                  }}
                >
                  {t('labor_review_confirm')}
                </Button>
              </Box>
            )}
          </Box>
        )}
        {hasFeature(PlanFeature.ADDITIONAL_TIME) ? (
          (!isEdit || baseLabor) && (
            <Form
              key={formSession}
              fields={fields}
              validation={Yup.object().shape(shape)}
              submitText={isEdit ? t('edit') : t('add')}
              values={defaultValues}
              enableReinitialize={false}
              onChange={() => {
                inputRevision.current += 1;
              }}
              onSubmit={async (values) => {
                if (conflict) return;
                const submittedRevision = inputRevision.current;
                const submittedSession = activeFormSession.current;
                const formattedValues: any = {
                  ...values,
                  ...(isEdit ? { version: baseLabor.version } : {})
                };
                formattedValues.assignedTo = formatSelect(
                  formattedValues.assignedTo
                );
                formattedValues.timeCategory = formatSelect(
                  formattedValues.timeCategory
                );
                formattedValues.duration =
                  values.hours * 3600 + values.minutes * 60;
                try {
                  if (isEdit) {
                    const result = await dispatch(
                      editLabor(baseLabor.id, workOrderId, formattedValues)
                    );
                    if (
                      wasOpen.current &&
                      submittedSession === activeFormSession.current
                    )
                      setBaseLabor(result);
                  } else
                    await dispatch(createLabor(workOrderId, formattedValues));
                  if (
                    wasOpen.current &&
                    submittedSession === activeFormSession.current &&
                    submittedRevision === inputRevision.current
                  )
                    onClose();
                } catch (error) {
                  if (
                    !wasOpen.current ||
                    submittedSession !== activeFormSession.current
                  )
                    return;
                  setConflict(isEdit && error?.message === 'conflict_error');
                  showSnackBar(getErrorMessage(error), 'error');
                }
              }}
            />
          )
        ) : (
          <FeatureErrorMessage message="Upgrade to add Itemized time tracking to your Work Orders. " />
        )}
      </DialogContent>
    </Dialog>
  );
}
