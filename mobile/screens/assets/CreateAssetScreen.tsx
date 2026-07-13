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
import { formatAssetValues, getAssetFields } from '../../utils/fields';
import useAuth from '../../hooks/useAuth';
import { addAsset, getAssetChildren } from '../../slices/asset';

export default function CreateAssetScreen({
  navigation,
  route
}: RootStackScreenProps<'AddAsset'>) {
  const { t } = useTranslation();
  const { uploadFiles } = useContext(CompanySettingsContext);
  const { getFilteredFields } = useAuth();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const onCreationSuccess = () => {
    showSnackBar(t('asset_create_success'), 'success');
    navigation.goBack();
  };
  const onCreationFailure = (err) =>
    showSnackBar(t('asset_create_failure'), 'error');

  const shape = {
    name: Yup.string().required(t('required_asset_name'))
  };

  return (
    <View style={styles.container}>
      <Form
        fields={getFilteredFields(getAssetFields(t))}
        validation={Yup.object().shape(shape)}
        navigation={navigation}
        submitText={t('create_asset')}
        values={{
          inServiceDate: null,
          warrantyExpirationDate: null,
          location: route.params?.location
            ? {
                label: route.params.location.name,
                value: route.params.location.id.toString()
              }
            : null,
          parentAsset: route.params?.parentAsset
            ? {
                label: route.params.parentAsset.name,
                value: route.params.parentAsset.id.toString()
              }
            : null,
          nfcId: route.params?.nfcId ?? null,
          barCode: route.params?.barCode ?? null
        }}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          let formattedValues = formatAssetValues(values);
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
                dispatch(addAsset(formattedValues))
                  .then(onCreationSuccess)
                  .then(() => {
                    dispatch(getAssetChildren(0, []));
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
