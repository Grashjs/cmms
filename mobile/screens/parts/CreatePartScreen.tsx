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
import { formatPartValues, getPartFields } from '../../utils/fields';
import { formatCustomFields } from '../../utils/formatters';
import useAuth from '../../hooks/useAuth';
import { addPart } from '../../slices/part';
import { getImageAndFiles } from '../../utils/overall';
import { getErrorMessage } from '../../utils/api';
import {
  IField,
  getCustomFieldsIFields,
  getCustomFieldsRequiredShape
} from '../../models/form';
import { CustomFieldEntityType } from '../../models/customField';

export default function CreatePartScreen({
  navigation,
  route
}: RootStackScreenProps<'AddPart'>) {
  const { t } = useTranslation();
  const { uploadFiles, getWOFieldsAndShapes } = useContext(
    CompanySettingsContext
  );
  const { getFilteredFields } = useAuth();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const { customFields } = useSelector((state) => state.customFields);

  const onCreationSuccess = () => {
    showSnackBar(t('part_create_success'), 'success');
    navigation.goBack();
  };
  const onCreationFailure = (err) =>
    showSnackBar(getErrorMessage(err, t('part_create_failure')), 'error');

  const defaultShape = {
    name: Yup.string().required(t('required_part_name')),
    ...getCustomFieldsRequiredShape(customFields, CustomFieldEntityType.PART, t)
  };

  const getFieldsAndShapes = (): [Array<IField>, { [key: string]: any }] => {
    const fields = [
      ...getFilteredFields(getPartFields(t)),
      ...getCustomFieldsIFields(customFields, CustomFieldEntityType.PART)
    ];
    return getWOFieldsAndShapes(fields, defaultShape);
  };

  return (
    <View style={styles.container}>
      <Form
        fields={getFieldsAndShapes()[0]}
        validation={Yup.object().shape(getFieldsAndShapes()[1])}
        navigation={navigation}
        submitText={t('create_part')}
        values={{}}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          let formattedValues = formatPartValues(values);
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
            await dispatch(addPart(formattedValues));
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
