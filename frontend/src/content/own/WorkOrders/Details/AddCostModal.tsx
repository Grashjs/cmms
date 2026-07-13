import { Dialog, DialogContent, DialogTitle, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import Form from '../../components/form';
import * as Yup from 'yup';
import { IField } from '../../type';
import { formatSelect } from '../../../../utils/formatters';
import { useDispatch } from '../../../../store';
import { createAdditionalCost } from '../../../../slices/additionalCost';
import useAuth from '../../../../hooks/useAuth';
import FeatureErrorMessage from '../../components/FeatureErrorMessage';
import { PlanFeature } from '../../../../models/owns/subscriptionPlan';

interface AddCostProps {
  open: boolean;
  onClose: () => void;
  workOrderId: number;
}
export default function AddCostModal({
  open,
  onClose,
  workOrderId
}: AddCostProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { hasFeature } = useAuth();
  const fields: Array<IField> = [
    {
      name: 'description',
      type: 'text',
      label: t('cost_description'),
      required: true
    },
    {
      name: 'assignedTo',
      type: 'select',
      label: t('assigned_to'),
      type2: 'user',
      midWidth: true
    },
    {
      name: 'category',
      type: 'select',
      label: t('category'),
      type2: 'category',
      category: 'cost-categories',
      midWidth: true
    },
    {
      name: 'date',
      type: 'date',
      label: t('date'),
      midWidth: true
    },
    {
      name: 'cost',
      type: 'number',
      label: t('cost'),
      midWidth: true
    },
    {
      name: 'includeToTotalCost',
      type: 'switch',
      label: t('include_cost'),
      helperText: t('include_cost_description')
    }
  ];
  const shape = {
    description: Yup.string().required(t('required_cost_description')),
    cost: Yup.number().required(t('required_cost'))
  };
  return (
    <Dialog fullWidth maxWidth="sm" open={open} onClose={onClose}>
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {t('add_cost')}
        </Typography>
        <Typography variant="subtitle2">{t('add_cost_description')}</Typography>
      </DialogTitle>
      <DialogContent
        dividers
        sx={{
          p: 3
        }}
      >
        {hasFeature(PlanFeature.ADDITIONAL_COST) ? (
          <Form
            fields={fields}
            validation={Yup.object().shape(shape)}
            submitText={t('add')}
            values={{ includeToTotalCost: true }}
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              const formattedValues = { ...values };
              formattedValues.assignedTo = formatSelect(
                formattedValues.assignedTo
              );
              formattedValues.category = formatSelect(formattedValues.category);
              return dispatch(
                createAdditionalCost(workOrderId, formattedValues)
              ).finally(() => onClose());
            }}
          />
        ) : (
          <FeatureErrorMessage message="Upgrade to add Itemized cost tracking to your Work Orders. " />
        )}
      </DialogContent>
    </Dialog>
  );
}
