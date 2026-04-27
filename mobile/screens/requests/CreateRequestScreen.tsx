import { RootStackScreenProps } from '../../types';
import { View } from '../../components/Themed';
import Form from '../../components/form';
import * as Yup from 'yup';
import { StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { getImageAndFiles } from '../../utils/overall';
import { useDispatch, useSelector } from '../../store';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { formatPartValues, formatRequestValues } from '../../utils/fields';
import { formatCustomFields } from '../../utils/formatters';
import useAuth from '../../hooks/useAuth';
import { addRequest } from '../../slices/request';
import {
  IField,
  getCustomFieldsIFields,
  getCustomFieldsRequiredShape
} from '../../models/form';
import { CustomFieldEntityType } from '../../models/customField';

export default function CreateRequestScreen({
  navigation,
  route
}: RootStackScreenProps<'AddRequest'>) {
  const { t } = useTranslation();
  const { uploadFiles, getRequestFieldsAndShapes } = useContext(
    CompanySettingsContext
  );
  const { companySettings } = useAuth();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const { customFields } = useSelector((state) => state.customFields);

  const onCreationSuccess = () => {
    showSnackBar(t('request_create_success'), 'success');
    navigation.goBack();
  };
  const onCreationFailure = (err) =>
    showSnackBar(t('request_create_failure'), 'error');

  const getFieldsAndShapes = (): [Array<IField>, { [key: string]: any }] => {
    const baseShape = getRequestFieldsAndShapes()[1];
    const customShape = getCustomFieldsRequiredShape(
      customFields,
      CustomFieldEntityType.WORK_ORDER,
      t
    );
    const customFieldsList = getCustomFieldsIFields(
      customFields,
      CustomFieldEntityType.WORK_ORDER
    );
    return [
      [...getRequestFieldsAndShapes()[0], ...customFieldsList],
      { ...baseShape, ...customShape }
    ];
  };

  return (
    <View style={styles.container}>
      <Form
        fields={getFieldsAndShapes()[0]}
        validation={Yup.object().shape(getFieldsAndShapes()[1])}
        navigation={navigation}
        submitText={t('save')}
        values={{ dueDate: null }}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          try {
            let formattedValues = formatRequestValues(values);
            formattedValues = formatCustomFields(formattedValues);
            const uploadedFiles = await uploadFiles(
              formattedValues.files,
              formattedValues.image
            );
            const imageAndFiles = getImageAndFiles(uploadedFiles);
            if (values.audioDescription) {
              const audioFiles = await uploadFiles(
                [values.audioDescription],
                []
              );
              const audioImageAndFiles = getImageAndFiles(audioFiles);
              formattedValues.audioDescription = audioImageAndFiles.files[0];
            }
            formattedValues = {
              ...formattedValues,
              image: imageAndFiles.image,
              files: imageAndFiles.files
            };
            await dispatch(addRequest(formattedValues));
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
