import { StyleSheet } from 'react-native';
import { View } from '../../components/Themed';
import { useTranslation } from 'react-i18next';
import { IField } from '../../models/form';
import * as Yup from 'yup';
import Form from '../../components/form';
import { formatSelect } from '../../utils/formatters';
import { editAdditionalCost } from '../../slices/additionalCost';
import { RootStackScreenProps } from '../../types';
import { useDispatch } from '../../store';
import { getErrorMessage } from '../../utils/api';
import { useContext } from 'react';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';

export default function EditAdditionalCost({
  navigation,
  route
}: RootStackScreenProps<'EditAdditionalCost'>) {
  const { t } = useTranslation();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const { workOrderId, additionalCost } = route.params;

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
    <View style={{ flex: 1 }}>
      <Form
        fields={fields}
        navigation={navigation}
        validation={Yup.object().shape(shape)}
        submitText={t('save')}
        values={{
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
            : null,
          date: additionalCost.date ? new Date(additionalCost.date) : null,
          cost: additionalCost.cost?.toString()
        }}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          const formattedValues = { ...values };
          formattedValues.assignedTo = formatSelect(formattedValues.assignedTo);
          formattedValues.category = formatSelect(formattedValues.category);
          return dispatch(
            editAdditionalCost(workOrderId, additionalCost.id, formattedValues)
          )
            .catch((err) => showSnackBar(getErrorMessage(err), 'error'))
            .finally(() => navigation.goBack());
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({});
