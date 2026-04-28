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
import { getImageAndFiles } from '../../utils/overall';
import useAuth from '../../hooks/useAuth';
import { addLocation, getLocationChildren } from '../../slices/location';
import { getErrorMessage } from '../../utils/api';
import { formatLocationValues, getLocationFields } from '../../utils/fields';
import { formatCustomFields } from '../../utils/formatters';
import {
  IField,
  getCustomFieldsIFields,
  getCustomFieldsRequiredShape
} from '../../models/form';
import { CustomFieldEntityType } from '../../models/customField';

export default function CreateLocationScreen({
  navigation,
  route
}: RootStackScreenProps<'AddLocation'>) {
  const { t } = useTranslation();
  const { uploadFiles, getWOFieldsAndShapes } = useContext(
    CompanySettingsContext
  );
  const { getFilteredFields } = useAuth();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const { customFields } = useSelector((state) => state.customFields);

  const onCreationSuccess = () => {
    showSnackBar(t('location_create_success'), 'success');
    navigation.goBack();
  };
  const onCreationFailure = (err) =>
    showSnackBar(getErrorMessage(err, t('location_create_failure')), 'error');

  const defaultShape = {
    name: Yup.string().required(t('required_location_name')),
    ...getCustomFieldsRequiredShape(
      customFields,
      CustomFieldEntityType.LOCATION,
      t
    )
  };

  const getFieldsAndShapes = (): [Array<IField>, { [key: string]: any }] => {
    const fields = [
      ...getFilteredFields(getLocationFields(t)),
      ...getCustomFieldsIFields(customFields, CustomFieldEntityType.LOCATION)
    ];
    return getWOFieldsAndShapes(fields, defaultShape);
  };

  return (
    <View style={styles.container}>
      <Form
        fields={getFieldsAndShapes()[0]}
        validation={Yup.object().shape(getFieldsAndShapes()[1])}
        navigation={navigation}
        submitText={t('create_location')}
        values={{}}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          let formattedValues = formatLocationValues(values);
          formattedValues = formatCustomFields(formattedValues);
          try {
            const uploadedFiles = await uploadFiles(
              formattedValues.files,
              formattedValues.image
            );
            const imageAndFiles = getImageAndFiles(uploadedFiles);
            formattedValues = {
              ...formattedValues,
              image: imageAndFiles.image,
              files: imageAndFiles.files
            };
            await dispatch(addLocation(formattedValues));
            onCreationSuccess();
            dispatch(getLocationChildren(0, []));
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
