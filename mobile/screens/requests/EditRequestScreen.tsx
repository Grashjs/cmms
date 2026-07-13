import { RootStackScreenProps } from '../../types';
import { View } from '../../components/Themed';
import Form from '../../components/form';
import * as Yup from 'yup';
import { StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { getImageAndFiles } from '../../utils/overall';
import { useDispatch } from '../../store';
import { editRequest } from '../../slices/request';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { formatRequestValues } from '../../utils/fields';
import { getWOBaseValues } from '../../utils/woBase';

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

  const onEditSuccess = () => {
    showSnackBar(t('changes_saved_success'), 'success');
    navigation.goBack();
  };
  const onEditFailure = (err) =>
    showSnackBar(t('request_edit_failure'), 'error');

  return (
    <View style={styles.container}>
      <Form
        fields={getRequestFieldsAndShapes()[0]}
        validation={Yup.object().shape(getRequestFieldsAndShapes()[1])}
        navigation={navigation}
        submitText={t('save')}
        values={{
          ...request,
          ...getWOBaseValues(t, request)
        }}
        onChange={({ field, e }) => {
        }}
        onSubmit={async (values) => {
          try {
            let formattedValues = formatRequestValues(values);
            const filesToUpload = formattedValues.files.some((file) => file.id) ? [] : formattedValues.files;

            const uploadedFiles = await uploadFiles(filesToUpload, formattedValues.image);
            const imageAndFiles = getImageAndFiles(uploadedFiles, request.image);
            if (values.audioDescription) {
              const audioFiles = await uploadFiles([values.audioDescription], []);
              const imageAndFiles = getImageAndFiles(audioFiles);
              formattedValues.audioDescription = imageAndFiles.files[0];
            }
            formattedValues = {
              ...formattedValues,
              image: imageAndFiles.image,
              files: [...request.files, ...imageAndFiles.files]
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
