import { StyleSheet } from 'react-native';
import * as Yup from 'yup';
import { useContext, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { RootStackScreenProps } from '../../types';
import { View } from '../../components/Themed';
import Form from '../../components/form';
import { IField } from '../../models/form';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { useDispatch } from '../../store';
import { addPreventiveMaintenance } from '../../slices/preventiveMaintenance';
import {
  formatPreventiveMaintenanceValues,
  getPreventiveMaintenanceFields
} from '../../utils/fields';
import { getImageAndFiles } from '../../utils/overall';
import { getErrorMessage } from '../../utils/api';
import useUnsavedChanges from '../../hooks/useUnsavedChanges';

export default function CreatePMScreen({
  navigation
}: RootStackScreenProps<'AddPreventiveMaintenance'>) {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const [isFormDirty, setIsFormDirty] = useState(false);
  const { uploadFiles, getWOFieldsAndShapes } = useContext(
    CompanySettingsContext
  );
  const { showSnackBar } = useContext(CustomSnackBarContext);

  useUnsavedChanges(navigation, isFormDirty);

  const defaultShape: { [key: string]: any } = {
    name: Yup.string().required(t('required_pm_name')),
    title: Yup.string().required(t('required_wo_title')),
    startsOn: Yup.date().required(t('required_starts_on')),
    frequency: Yup.number()
      .min(1, t('invalid_frequency'))
      .required(t('required_frequency'))
  };

  const getFieldsAndShapes = (): [Array<IField>, { [key: string]: any }] =>
    getWOFieldsAndShapes(getPreventiveMaintenanceFields(t), defaultShape);

  return (
    <View style={styles.container}>
      <Form
        fields={getFieldsAndShapes()[0]}
        validation={Yup.object().shape(getFieldsAndShapes()[1])}
        navigation={navigation}
        submitText={t('save')}
        values={{
          startsOn: new Date(),
          frequency: 7,
          dueDateDelay: 1,
          estimatedDuration: 1
        }}
        onChange={() => setIsFormDirty(true)}
        onSubmit={async (values) => {
          setIsFormDirty(false);
          let formattedValues = formatPreventiveMaintenanceValues(values);
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
            await dispatch(addPreventiveMaintenance(formattedValues));
            showSnackBar(t('pm_create_success'), 'success');
            navigation.goBack();
          } catch (err) {
            showSnackBar(getErrorMessage(err, t('pm_create_failure')), 'error');
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
