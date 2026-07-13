import { RootStackScreenProps } from '../../types';
import { View } from '../../components/Themed';
import Form from '../../components/form';
import * as Yup from 'yup';
import { StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { useDispatch } from '../../store';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { formatPartValues, getPartFields } from '../../utils/fields';
import useAuth from '../../hooks/useAuth';
import { addPart } from '../../slices/part';
import { getImageAndFiles } from '../../utils/overall';

export default function CreatePartScreen({
  navigation,
  route
}: RootStackScreenProps<'AddPart'>) {
  const { t } = useTranslation();
  const { uploadFiles } = useContext(CompanySettingsContext);
  const { getFilteredFields } = useAuth();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const onCreationSuccess = () => {
    showSnackBar(t('part_create_success'), 'success');
    navigation.goBack();
  };
  const onCreationFailure = (err) =>
    showSnackBar(t('part_create_failure'), 'error');

  const shape = {
    name: Yup.string().required(t('required_part_name'))
  };

  return (
    <View style={styles.container}>
      <Form
        fields={getFilteredFields(getPartFields(t))}
        validation={Yup.object().shape(shape)}
        navigation={navigation}
        submitText={t('create_part')}
        values={{}}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          let formattedValues = formatPartValues(values);
          return new Promise<void>((resolve, rej) => {
            uploadFiles(formattedValues.files, formattedValues.image)
              .then((files) => {
                const imageAndFiles = getImageAndFiles(files);
                formattedValues = {
                  ...formattedValues,
                  image: imageAndFiles.image,
                  files: imageAndFiles.files
                };
                dispatch(addPart(formattedValues))
                  .then(onCreationSuccess)
                  .catch(onCreationFailure)
                  .finally(resolve);
              })
              .catch((err) => {
                onCreationFailure(err);
                rej(err);
              });
          });
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
