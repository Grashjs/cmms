import { RootStackScreenProps } from '../../types';
import { View } from '../../components/Themed';
import Form from '../../components/form';
import * as Yup from 'yup';
import { StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { useDispatch, useSelector } from '../../store';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { formatMeterValues, getMeterFields } from '../../utils/fields';
import { formatCustomFields } from '../../utils/formatters';
import useAuth from '../../hooks/useAuth';
import { addMeter } from '../../slices/meter';
import { getErrorMessage } from '../../utils/api';
import { getImageAndFiles } from '../../utils/overall';
import {
  IField,
  getCustomFieldsIFields,
  getCustomFieldsRequiredShape
} from '../../models/form';
import { CustomFieldEntityType } from '../../models/customField';

export default function CreateMeterScreen({
  navigation,
  route
}: RootStackScreenProps<'AddMeter'>) {
  const { t } = useTranslation();
  const { uploadFiles, getWOFieldsAndShapes } = useContext(
    CompanySettingsContext
  );
  const { getFilteredFields } = useAuth();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const { customFields } = useSelector((state) => state.customFields);

  const onCreationSuccess = () => {
    showSnackBar(t('meter_create_success'), 'success');
    navigation.goBack();
  };
  const onCreationFailure = (err) =>
    showSnackBar(getErrorMessage(err, t('meter_create_failure')), 'error');

  const defaultShape = {
    name: Yup.string().required(t('required_meter_name')),
    unit: Yup.string().required(t('required_meter_unit')),
    updateFrequency: Yup.number().required(
      t('required_meter_update_frequency')
    ),
    asset: Yup.object().required(t('required_asset')).nullable(),
    ...getCustomFieldsRequiredShape(
      customFields,
      CustomFieldEntityType.METER,
      t
    )
  };

  const getFieldsAndShapes = (): [Array<IField>, { [key: string]: any }] => {
    const fields = [
      ...getFilteredFields(getMeterFields(t)),
      ...getCustomFieldsIFields(customFields, CustomFieldEntityType.METER)
    ];
    return getWOFieldsAndShapes(fields, defaultShape);
  };

  return (
    <View style={styles.container}>
      <Form
        fields={getFieldsAndShapes()[0]}
        validation={Yup.object().shape(getFieldsAndShapes()[1])}
        navigation={navigation}
        submitText={t('add_meter')}
        values={{}}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          let formattedValues = formatMeterValues(values);
          formattedValues = formatCustomFields(formattedValues);
          try {
            const uploadedFiles = await uploadFiles([], values.image);
            const imageAndFiles = getImageAndFiles(uploadedFiles);
            formattedValues = {
              ...formattedValues,
              image: imageAndFiles.image
            };
            await dispatch(addMeter(formattedValues));
            onCreationSuccess();
          } catch (err) {
            onCreationFailure(err);
            throw err;
          }
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
