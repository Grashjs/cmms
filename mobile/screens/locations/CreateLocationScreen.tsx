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
import { formatLocationValues, getLocationFields } from '../../utils/fields';
import useAuth from '../../hooks/useAuth';
import { addLocation, getLocationChildren } from '../../slices/location';

export default function CreateLocationScreen({
  navigation,
  route
}: RootStackScreenProps<'AddLocation'>) {
  const { t } = useTranslation();
  const { uploadFiles } = useContext(CompanySettingsContext);
  const { getFilteredFields } = useAuth();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const onCreationSuccess = () => {
    showSnackBar(t('location_create_success'), 'success');
    navigation.goBack();
  };
  const onCreationFailure = (err) =>
    showSnackBar(t('location_create_failure'), 'error');

  const shape = {
    name: Yup.string().required(t('required_location_name'))
  };

  return (
    <View style={styles.container}>
      <Form
        fields={getFilteredFields(getLocationFields(t))}
        validation={Yup.object().shape(shape)}
        navigation={navigation}
        submitText={t('create_location')}
        values={{}}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          let formattedValues = formatLocationValues(values);
          return new Promise<void>((resolve, rej) => {
            uploadFiles(formattedValues.files, formattedValues.image)
              .then((files) => {
                formattedValues = {
                  ...formattedValues,
                  image: files.length ? { id: files[0].id } : null,
                  files: files.map((file) => {
                    return { id: file.id };
                  })
                };
                dispatch(addLocation(formattedValues))
                  .then(onCreationSuccess)
                  .then(() => {
                    dispatch(getLocationChildren(0, []));
                  })
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
