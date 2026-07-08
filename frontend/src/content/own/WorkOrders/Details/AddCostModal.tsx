import { Dialog, DialogContent, DialogTitle, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import Form from '../../components/form';
import * as Yup from 'yup';
import { IField } from '../../type';
import { formatSelect } from '../../../../utils/formatters';
import { useDispatch } from '../../../../store';
import {
  createAdditionalCost,
  editAdditionalCost
} from '../../../../slices/additionalCost';
import useAuth from '../../../../hooks/useAuth';
import FeatureErrorMessage from '../../components/FeatureErrorMessage';
import { PlanFeature } from '../../../../models/owns/subscriptionPlan';
import { getErrorMessage } from '../../../../utils/api';
import { useContext } from 'react';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import AdditionalCost from '../../../../models/owns/additionalCost';

interface AddCostProps {
  open: boolean;
  onClose: () => void;
  workOrderId: number;
  additionalCost?: AdditionalCost;
}
export default function AddCostModal({
  open,
  onClose,
  workOrderId,
  additionalCost
}: AddCostProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { hasFeature } = useAuth();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const isEdit = !!additionalCost;

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
  const defaultValues = additionalCost
    ? {
        ...additionalCost,
        assignedTo: additionalCost.assignedTo
          ? {
              label: `${additionalCost.assignedTo.firstName} ${additionalCost.assignedTo.lastName}`,
              value: additionalCost.assignedTo.id
            }
          : null,
        category: additionalCost.category
          ? {
              label: additionalCost.category.name,
              value: additionalCost.category.id
            }
          : null
      }
    : { includeToTotalCost: true };
  return (
    <Dialog fullWidth maxWidth="sm" open={open} onClose={onClose}>
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {isEdit ? t('edit_cost') : t('add_cost')}
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
            submitText={isEdit ? t('edit') : t('add')}
            values={defaultValues}
            enableReinitialize={isEdit}
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              const formattedValues = { ...values };
              formattedValues.assignedTo = formatSelect(
                formattedValues.assignedTo
              );
              formattedValues.category = formatSelect(formattedValues.category);
              if (isEdit) {
                return dispatch(
                  editAdditionalCost(
                    workOrderId,
                    additionalCost.id,
                    formattedValues
                  )
                )
                  .then(() => onClose())
                  .catch((err) => showSnackBar(getErrorMessage(err), 'error'));
              } else
                return dispatch(
                  createAdditionalCost(workOrderId, formattedValues)
                )
                  .then(() => onClose())
                  .catch((err) => showSnackBar(getErrorMessage(err), 'error'));
            }}
          />
        ) : (
          <FeatureErrorMessage message="Upgrade to add Itemized cost tracking to your Work Orders. " />
        )}
      </DialogContent>
    </Dialog>
  );
}
