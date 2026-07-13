import { RootStackScreenProps } from '../../types';
import { View } from '../../components/Themed';
import Form from '../../components/form';
import * as Yup from 'yup';
import { StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import { useContext } from 'react';
import { useDispatch } from '../../store';
import { editVendor } from '../../slices/vendor';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { getVendorFields } from '../../utils/fields';
import {
  emailRegExp,
  phoneRegExp,
  websiteRegExp
} from '../../utils/validators';

export default function EditVendorScreen({
  navigation,
  route
}: RootStackScreenProps<'EditVendor'>) {
  const { t } = useTranslation();
  const { vendor } = route.params;
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const shape = {
    companyName: Yup.string().required(t('required_company_name')),
    rate: Yup.number(),
    phone: Yup.string().matches(phoneRegExp, t('invalid_phone')).nullable(),
    name: Yup.string().required(t('required_name')),
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
    showSnackBar(t('vendor_edit_failure'), 'error');

  return (
    <View style={styles.container}>
      <Form
        fields={getVendorFields(t)}
        validation={Yup.object().shape(shape)}
        navigation={navigation}
        submitText={t('save')}
        values={{
          ...vendor
        }}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          const formattedValues = values.rate
            ? {
                ...values,
                rate: Number(values.rate)
              }
            : values;
          return dispatch(editVendor(vendor.id, formattedValues))
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
