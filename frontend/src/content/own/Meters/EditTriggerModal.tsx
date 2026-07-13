import { Dialog, DialogContent, DialogTitle, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import Form from '../components/form';
import * as Yup from 'yup';
import { IField } from '../type';
import { formatSelect, formatSelectMultiple } from '../../../utils/formatters';
import { useDispatch } from '../../../store';
import { editWorkOrderMeterTrigger } from '../../../slices/workOrderMeterTrigger';
import { getWOBaseFields, getWOBaseValues } from '../../../utils/woBase';
import Meter from '../../../models/owns/meter';
import WorkOrderMeterTrigger from '../../../models/owns/workOrderMeterTrigger';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import { getImageAndFiles } from '../../../utils/overall';

interface EditTriggerProps {
  open: boolean;
  onClose: () => void;
  meter: Meter;
  workOrderMeterTrigger: WorkOrderMeterTrigger;
}
export default function EditTriggerModal({
  open,
  onClose,
  meter,
  workOrderMeterTrigger
}: EditTriggerProps) {
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
        { label: 'Greater than', value: 'MORE_THAN' },
        { label: 'Lower than', value: 'LESS_THAN' }
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
  const onEditSuccess = () => {
    onClose();
    showSnackBar(t('wo_trigger_edit_success'), 'success');
  };
  const onEditFailure = (err) =>
    showSnackBar(t('wo_trigger_edit_failure'), 'error');

  const shape = {
    name: Yup.string().required(t('Trequired_trigger_name')),
    title: Yup.string().required(t('required_wo_title')),
    value: Yup.number().required(t('required_value')),
    triggerCondition: Yup.object().required(t('required_trigger_condition'))
  };
  const formatValues = (values) => {
    const newValues = { ...values };
    newValues.primaryUser = formatSelect(newValues.primaryUser);
    newValues.location = formatSelect(newValues.location);
    newValues.team = formatSelect(newValues.team);
    newValues.category = formatSelect(newValues.category);
    newValues.asset = formatSelect(newValues.asset);
    newValues.assignedTo = formatSelectMultiple(newValues.assignedTo);
    newValues.priority = newValues.priority?.value;
    newValues.triggerCondition = newValues.triggerCondition.value;
    return newValues;
  };
  return (
    <Dialog fullWidth maxWidth="sm" open={open} onClose={onClose}>
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {t('edit_wo_trigger')}
        </Typography>
        <Typography variant="subtitle2">
          {t('edit_wo_trigger_description')}
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
          submitText={t('save')}
          values={{
            ...workOrderMeterTrigger,
            triggerCondition: {
              label: t(workOrderMeterTrigger?.triggerCondition),
              value: workOrderMeterTrigger?.triggerCondition
            },
            ...getWOBaseValues(t, workOrderMeterTrigger)
          }}
          onChange={({ field, e }) => {}}
          onSubmit={async (values) => {
            let formattedValues = formatValues(values);
            const files = formattedValues.files.find((file) => file.id)
              ? []
              : formattedValues.files;
            return new Promise<void>((resolve, rej) => {
              uploadFiles(files, formattedValues.image)
                .then((files) => {
                  const imageAndFiles = getImageAndFiles(
                    files,
                    workOrderMeterTrigger.image
                  );
                  formattedValues = {
                    ...formattedValues,
                    image: imageAndFiles.image,
                    files: [
                      ...workOrderMeterTrigger.files,
                      ...imageAndFiles.files
                    ]
                  };
                  dispatch(
                    editWorkOrderMeterTrigger(
                      meter.id,
                      workOrderMeterTrigger.id,
                      formattedValues
                    )
                  )
                    .then(() => {
                      onEditSuccess();
                      resolve();
                    })
                    .catch((err) => {
                      onEditFailure(err);
                      rej();
                    });
                })
                .catch((err) => {
                  onEditFailure(err);
                  rej();
                });
            });
          }}
        />
      </DialogContent>
    </Dialog>
  );
}
