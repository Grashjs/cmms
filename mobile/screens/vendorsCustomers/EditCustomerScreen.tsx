import { RootStackScreenProps } from '../../types';
import { View } from '../../components/Themed';
import Form from '../../components/form';
import * as Yup from 'yup';
import { StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import { useContext } from 'react';
import { useDispatch } from '../../store';
import { editCustomer } from '../../slices/customer';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { formatCustomerValues, getCustomerFields } from '../../utils/fields';
import {
  emailRegExp,
  phoneRegExp,
  websiteRegExp
} from '../../utils/validators';

export default function EditCustomerScreen({
  navigation,
  route
}: RootStackScreenProps<'EditCustomer'>) {
  const { t } = useTranslation();
  const { customer } = route.params;
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const shape = {
    name: Yup.string().required('required_customer_name'),
    phone: Yup.string()
      .matches(phoneRegExp, t('invalid_phone'))
      .required(t('required_phone')),
    website: Yup.string()
      .matches(websiteRegExp, t('invalid_website'))
      .nullable(),
    email: Yup.string().matches(emailRegExp, t('invalid_email')).nullable()
  };

  const onEditSuccess = () => {
    showSnackBar(t('changes_saved_success'), 'success');
    navigation.goBack();
  };
  const onEditFailure = (err) =>
    showSnackBar(t('customer_edit_failure'), 'error');

  return (
    <View style={styles.container}>
      <Form
        fields={getCustomerFields(t)}
        validation={Yup.object().shape(shape)}
        navigation={navigation}
        submitText={t('save')}
        values={{
          ...customer,
          billingCurrency: customer?.billingCurrency
            ? {
                label: customer.billingCurrency.name,
                value: customer.billingCurrency.id
              }
            : null
        }}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          const formattedValues = formatCustomerValues(values);
          return dispatch(editCustomer(customer.id, formattedValues))
            .then(onEditSuccess)
            .catch(onEditFailure);
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
