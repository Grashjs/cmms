import { Dialog, DialogContent, DialogTitle, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import Form from '../components/form';
import * as Yup from 'yup';
import { IField } from '../type';
import { formatSelect, formatSelectMultiple } from '../../../utils/formatters';
import { useDispatch } from '../../../store';
import { createWorkOrderMeterTrigger } from '../../../slices/workOrderMeterTrigger';
import { getWOBaseFields } from '../../../utils/woBase';
import Meter from '../../../models/owns/meter';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import { getImageAndFiles } from '../../../utils/overall';

interface AddTriggerProps {
  open: boolean;
  onClose: () => void;
  meter: Meter;
}
export default function AddTriggerModal({
  open,
  onClose,
  meter
}: AddTriggerProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { uploadFiles } = useContext(CompanySettingsContext);
  const fields: Array<IField> = [
    {
      name: 'name',
      type: 'text',
      label: t('trigger_name'),
      required: true
    },
    {
      name: 'triggerCondition',
      type: 'select',
      label: t('when_reading_is'),
      items: [
        { label: t('greater_than'), value: 'MORE_THAN' },
        { label: t('lower_than'), value: 'LESS_THAN' }
      ],
      midWidth: true,
      required: true
    },
    {
      name: 'value',
      type: 'number',
      label: t('value') + '(' + meter.unit + ')',
      midWidth: true,
      required: true
    },
    {
      name: 'workOrderConfig',
      type: 'titleGroupField',
      label: t('wo_configuration')
    },
    ...getWOBaseFields(t)
  ];
  const shape = {
    name: Yup.string().required(t('required_trigger_name')),
    title: Yup.string().required(t('required_wo_title')),
    value: Yup.number().required(t('required_value')),
    triggerCondition: Yup.object().required(t('required_trigger_condition'))
  };
  const formatValues = (values) => {
    const newValues = { ...values };
    newValues.primaryUser = formatSelect(newValues.primaryUser);
    newValues.location = formatSelect(newValues.location);
    newValues.team = formatSelect(newValues.team);
    newValues.asset = formatSelect(newValues.asset);
    newValues.category = formatSelect(newValues.category);
    newValues.assignedTo = formatSelectMultiple(newValues.assignedTo);
    newValues.priority = newValues.priority?.value;
    newValues.triggerCondition = newValues.triggerCondition.value;
    return newValues;
  };
  const onCreationSuccess = () => {
    onClose();
    showSnackBar(t('wo_trigger_create_success'), 'success');
  };
  const onCreationFailure = (err) =>
    showSnackBar(t('wo_trigger_create_failure'), 'error');

  return (
    <Dialog fullWidth maxWidth="sm" open={open} onClose={onClose}>
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {t('add_wo_trigger')}
        </Typography>
        <Typography variant="subtitle2">
          {t('add_wo_trigger_description')}
        </Typography>
      </DialogTitle>
      <DialogContent
        dividers
        sx={{
          p: 3
        }}
      >
        <Form
          fields={fields}
          validation={Yup.object().shape(shape)}
          submitText={t('add')}
          values={{ dueDate: null }}
          onChange={({ field, e }) => {}}
          onSubmit={async (values) => {
            let formattedValues = formatValues(values);
            return new Promise<void>((resolve, rej) => {
              uploadFiles(formattedValues.files, formattedValues.image)
                .then((files) => {
                  const imageAndFiles = getImageAndFiles(files);
                  formattedValues = {
                    ...formattedValues,
                    image: imageAndFiles.image,
                    files: imageAndFiles.files
                  };
                  dispatch(
                    createWorkOrderMeterTrigger(meter.id, formattedValues)
                  )
                    .then(() => {
                      onCreationSuccess();
                      resolve();
                    })
                    .catch((err) => {
                      onCreationFailure(err);
                      rej();
                    });
                })
                .catch((err) => {
                  onCreationFailure(err);
                  rej();
                });
            });
          }}
        />
      </DialogContent>
    </Dialog>
  );
}
