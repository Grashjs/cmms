import { RootStackScreenProps } from '../../types';
import { View } from '../../components/Themed';
import Form from '../../components/form';
import * as Yup from 'yup';
import { Alert, StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import {
  getCustomFieldsIFields,
  getCustomFieldsRequiredShape,
  IField
} from '../../models/form';
import { useContext, useEffect, useState } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { getImageAndFiles } from '../../utils/overall';
import { useDispatch, useSelector } from '../../store';
import { addWorkOrder } from '../../slices/workOrder';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { formatWorkOrderValues, getWorkOrderFields } from '../../utils/fields';
import { formatCustomFields } from '../../utils/formatters';
import { assetStatuses } from '../../models/asset';
import { useAppTheme } from '../../custom-theme';
import { getErrorMessage } from '../../utils/api';
import { CustomFieldEntityType } from '../../models/customField';

export default function CreateWorkOrderScreen({
  navigation,
  route
}: RootStackScreenProps<'AddWorkOrder'>) {
  const { t } = useTranslation();
  const [isFormDirty, setIsFormDirty] = useState(false);
  const theme = useAppTheme();
  const { uploadFiles, getWOFieldsAndShapes } = useContext(
    CompanySettingsContext
  );
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const { customFields } = useSelector((state) => state.customFields);

  useEffect(() => {
    return navigation.addListener('beforeRemove', (e) => {
      if (!isFormDirty) {
        return;
      }
      e.preventDefault();
      Alert.alert(t('discard_changes'), t('discard_changes_question'), [
        { text: t('cancel'), style: 'cancel' },
        {
          text: t('discard_changes'),
          style: 'destructive',
          onPress: () => navigation.dispatch(e.data.action)
        }
      ]);
    });
  }, [navigation, isFormDirty]);

  const defaultShape: { [key: string]: any } = {
    title: Yup.string().required(t('required_wo_title')),
    ...getCustomFieldsRequiredShape(
      customFields,
      CustomFieldEntityType.WORK_ORDER,
      t
    )
  };

  const onCreationSuccess = () => {
    showSnackBar(t('wo_create_success'), 'success');
    navigation.goBack();
  };
  const onCreationFailure = (err) =>
    showSnackBar(getErrorMessage(err, t('wo_create_failure')), 'error');
  const getFieldsAndShapes = (): [Array<IField>, { [key: string]: any }] => {
    const fields = [
      ...getWorkOrderFields(t),
      ...getCustomFieldsIFields(customFields, CustomFieldEntityType.WORK_ORDER)
    ];
    return getWOFieldsAndShapes(fields, defaultShape);
  };
  return (
    <View style={styles.container}>
      <Form
        fields={[
          ...getFieldsAndShapes()[0],
          {
            name: 'assetStatus',
            type: 'select',
            label: t('asset_status'),
            placeholder: t('select_asset_status'),
            items: assetStatuses.map((assetStatus) => ({
              label: t(assetStatus.status),
              value: assetStatus.status,
              color: assetStatus.color(theme)
            }))
          }
        ]}
        validation={Yup.object().shape(getFieldsAndShapes()[1])}
        navigation={navigation}
        submitText={t('save')}
        values={{
          requiredSignature: false,
          dueDate: null,
          location: route.params?.location
            ? {
                label: route.params.location.name,
                value: route.params.location.id.toString()
              }
            : null,
          asset: route.params?.asset
            ? {
                label: route.params.asset.name,
                value: route.params.asset.id.toString()
              }
            : null,
          estimatedDuration: 1
        }}
        onChange={() => setIsFormDirty(true)}
        onSubmit={async (values) => {
          setIsFormDirty(false);
          let formattedValues = formatWorkOrderValues(values);
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
            await dispatch(addWorkOrder(formattedValues));
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
