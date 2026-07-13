import { RootStackScreenProps } from '../../types';
import { View } from '../../components/Themed';
import Form from '../../components/form';
import * as Yup from 'yup';
import { StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import { IField } from '../../models/form';
import { useContext, useState } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { getImageAndFiles } from '../../utils/overall';
import { useDispatch } from '../../store';
import { addWorkOrder } from '../../slices/workOrder';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { formatWorkOrderValues, getWorkOrderFields } from '../../utils/fields';
import { assetStatuses } from '../../models/asset';
import { useTheme } from 'react-native-paper';

export default function CreateWorkOrderScreen({
  navigation,
  route
}: RootStackScreenProps<'AddWorkOrder'>) {
  const { t } = useTranslation();
  const [initialDueDate, setInitialDueDate] = useState<Date>(null);
  const theme = useTheme();
  const { uploadFiles, getWOFieldsAndShapes } = useContext(
    CompanySettingsContext
  );
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const defaultShape: { [key: string]: any } = {
    title: Yup.string().required(t('required_wo_title'))
  };

  const onCreationSuccess = () => {
    showSnackBar(t('wo_create_success'), 'success');
    navigation.goBack();
  };
  const onCreationFailure = (err) =>
    showSnackBar(t('wo_create_failure'), 'error');
  const getFieldsAndShapes = (): [Array<IField>, { [key: string]: any }] => {
    return getWOFieldsAndShapes(getWorkOrderFields(t), defaultShape);
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
          dueDate: initialDueDate,
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
            : null
        }}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          let formattedValues = formatWorkOrderValues(values);
          return new Promise<void>((resolve, rej) => {
            uploadFiles(formattedValues.files, formattedValues.image)
              .then((files) => {
                const imageAndFiles = getImageAndFiles(files);
                formattedValues = {
                  ...formattedValues,
                  image: imageAndFiles.image,
                  files: imageAndFiles.files
                };
                dispatch(addWorkOrder(formattedValues))
                  .then(() => {
                    onCreationSuccess();
                    resolve();
                  })
                  .catch((err) => {
                    onCreationFailure(err);
                    rej();
                  });
              })
              .catch((err) => {
                onCreationFailure(err);
                rej();
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
