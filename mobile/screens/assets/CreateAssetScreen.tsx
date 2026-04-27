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
import { formatAssetValues, getAssetFields } from '../../utils/fields';
import { formatCustomFields } from '../../utils/formatters';
import { getErrorMessage } from '../../utils/api';
import useAuth from '../../hooks/useAuth';
import { addAsset, getAssetChildren } from '../../slices/asset';
import { getImageAndFiles } from '../../utils/overall';
import {
  IField,
  getCustomFieldsIFields,
  getCustomFieldsRequiredShape
} from '../../models/form';
import { CustomFieldEntityType } from '../../models/customField';

export default function CreateAssetScreen({
  navigation,
  route
}: RootStackScreenProps<'AddAsset'>) {
  const { t } = useTranslation();
  const { uploadFiles, getWOFieldsAndShapes } = useContext(
    CompanySettingsContext
  );
  const { getFilteredFields } = useAuth();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const { customFields } = useSelector((state) => state.customFields);

  const onCreationSuccess = () => {
    showSnackBar(t('asset_create_success'), 'success');
    navigation.goBack();
  };
  const onCreationFailure = (err) =>
    showSnackBar(getErrorMessage(err, t('asset_create_failure')), 'error');

  const defaultShape = {
    name: Yup.string().required(t('required_asset_name')),
    ...getCustomFieldsRequiredShape(
      customFields,
      CustomFieldEntityType.ASSET,
      t
    )
  };

  const getFieldsAndShapes = (): [Array<IField>, { [key: string]: any }] => {
    const fields = [
      ...getFilteredFields(getAssetFields(t)),
      ...getCustomFieldsIFields(customFields, CustomFieldEntityType.ASSET)
    ];
    return getWOFieldsAndShapes(fields, defaultShape);
  };

  return (
    <View style={styles.container}>
      <Form
        fields={getFieldsAndShapes()[0]}
        validation={Yup.object().shape(getFieldsAndShapes()[1])}
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
            try {
              await dispatch(addAsset(formattedValues));
              await onCreationSuccess();
              await dispatch(getAssetChildren(0, []));
            } catch (err) {
              onCreationFailure(err);
              throw err;
            }
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
