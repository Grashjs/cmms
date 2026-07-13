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
import { editAsset } from '../../slices/asset';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { formatAssetValues, getAssetFields } from '../../utils/fields';
import useAuth from '../../hooks/useAuth';

export default function EditAssetScreen({
  navigation,
  route
}: RootStackScreenProps<'EditAsset'>) {
  const { t } = useTranslation();
  const { asset } = route.params;
  const { getFilteredFields } = useAuth();
  const { uploadFiles, getWOFieldsAndShapes } = useContext(
    CompanySettingsContext
  );
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const shape = {
    name: Yup.string().required(t('required_asset_name'))
  };

  const onEditSuccess = () => {
    showSnackBar(t('changes_saved_success'), 'success');
    navigation.goBack();
  };
  const onEditFailure = (err) =>
    showSnackBar(t('asset_update_failure'), 'error');

  return (
    <View style={styles.container}>
      <Form
        fields={getFilteredFields(getAssetFields(t))}
        validation={Yup.object().shape(shape)}
        navigation={navigation}
        submitText={t('save')}
        values={{
          ...asset,
          location: asset?.location
            ? {
                label: asset?.location.name,
                value: asset?.location.id
              }
            : null,
          category: asset?.category
            ? {
                label: asset.category.name,
                value: asset.category.id
              }
            : null,
          primaryUser: asset?.primaryUser
            ? {
                label: `${asset?.primaryUser.firstName} ${asset?.primaryUser.lastName}`,
                value: asset?.primaryUser.id
              }
            : null,
          assignedTo: asset?.assignedTo?.map((user) => {
            return {
              label: `${user.firstName} ${user.lastName}`,
              value: user.id
            };
          }),
          customers: asset?.customers?.map((customer) => {
            return {
              label: customer.name,
              value: customer.id
            };
          }),
          vendors: asset?.vendors?.map((vendor) => {
            return {
              label: vendor.companyName,
              value: vendor.id
            };
          }),
          teams: asset?.teams?.map((team) => {
            return {
              label: team.name,
              value: team.id
            };
          }),
          parts:
            asset?.parts?.map((part) => {
              return {
                label: part.name,
                value: part.id
              };
            }) ?? [],
          parentAsset: asset?.parentAsset
            ? {
                label: asset.parentAsset.name,
                value: asset.parentAsset.id
              }
            : null
        }}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          let formattedValues = formatAssetValues(values);
          const files = formattedValues.files.find((file) => file.id)
            ? []
            : formattedValues.files;
          return new Promise<void>((resolve, rej) => {
            uploadFiles(files, formattedValues.image)
              .then((files) => {
                const imageAndFiles = getImageAndFiles(files, asset.image);
                formattedValues = {
                  ...formattedValues,
                  image: imageAndFiles.image,
                  files: [...asset.files, ...imageAndFiles.files]
                };
                dispatch(editAsset(asset.id, formattedValues))
                  .then(onEditSuccess)
                  .catch(onEditFailure)
                  .finally(resolve);
              })
              .catch((err) => {
                onEditFailure(err);
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
