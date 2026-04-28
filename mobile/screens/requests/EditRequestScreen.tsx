import { RootStackScreenProps } from '../../types';
import { View } from '../../components/Themed';
import Form from '../../components/form';
import * as Yup from 'yup';
import { StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { getImageAndFiles, handleFileUpload } from '../../utils/overall';
import { useDispatch, useSelector } from '../../store';
import { editRequest } from '../../slices/request';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { formatRequestValues } from '../../utils/fields';
import { formatCustomFields } from '../../utils/formatters';
import { getWOBaseValues } from '../../utils/woBase';
import {
  IField,
  getCustomFieldsIFields,
  getCustomFieldsRequiredShape,
  getCustomFieldsValues
} from '../../models/form';
import { CustomFieldEntityType } from '../../models/customField';

export default function EditRequestScreen({
  navigation,
  route
}: RootStackScreenProps<'EditRequest'>) {
  const { t } = useTranslation();
  const { request } = route.params;
  const { uploadFiles, getRequestFieldsAndShapes } = useContext(
    CompanySettingsContext
  );

  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const { customFields } = useSelector((state) => state.customFields);

  const onEditSuccess = () => {
    showSnackBar(t('changes_saved_success'), 'success');
    navigation.goBack();
  };
  const onEditFailure = (err) =>
    showSnackBar(t('request_edit_failure'), 'error');

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
        values={{
          ...request,
          ...getWOBaseValues(t, request),
          ...getCustomFieldsValues(request)
        }}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          try {
            let formattedValues = formatRequestValues(values);
            formattedValues = formatCustomFields(formattedValues);
            const imageAndFiles = await handleFileUpload(
              {
                files: formattedValues.files,
                image: formattedValues.image
              },
              uploadFiles
            );
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

            await dispatch(editRequest(request?.id, formattedValues));
            onEditSuccess();
          } catch (err) {
            onEditFailure(err);
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
