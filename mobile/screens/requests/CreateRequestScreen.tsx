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
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { formatPartValues, formatRequestValues } from '../../utils/fields';
import useAuth from '../../hooks/useAuth';
import { addRequest } from '../../slices/request';

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
  const onCreationSuccess = () => {
    showSnackBar(t('request_create_success'), 'success');
    navigation.goBack();
  };
  const onCreationFailure = (err) =>
    showSnackBar(t('request_create_failure'), 'error');

  return (
    <View style={styles.container}>
      <Form
        fields={getRequestFieldsAndShapes()[0]}
        validation={Yup.object().shape(getRequestFieldsAndShapes()[1])}
        navigation={navigation}
        submitText={t('save')}
        values={{ dueDate: null }}
        onChange={({ field, e }) => {
        }}
        onSubmit={async (values) => {
          try {
            let formattedValues = formatRequestValues(values);
            const files = await uploadFiles(formattedValues.files, formattedValues.image);
            const imageAndFiles = getImageAndFiles(files);
            if (values.audioDescription) {
              const audioFiles = await uploadFiles([values.audioDescription], []);
              const imageAndFiles = getImageAndFiles(audioFiles);
              formattedValues.audioDescription = imageAndFiles.files[0];
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
