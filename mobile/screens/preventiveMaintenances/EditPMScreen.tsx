import { StyleSheet } from 'react-native';
import * as Yup from 'yup';
import { useContext } from 'react';
import { useTranslation } from 'react-i18next';
import { RootStackScreenProps } from '../../types';
import { View } from '../../components/Themed';
import Form from '../../components/form';
import { IField } from '../../models/form';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { useDispatch } from '../../store';
import {
  editPreventiveMaintenance,
  getSinglePreventiveMaintenance,
  patchSchedule
} from '../../slices/preventiveMaintenance';
import {
  formatPreventiveMaintenanceValues,
  getPreventiveMaintenanceFields
} from '../../utils/fields';
import { getWOBaseValues } from '../../utils/woBase';
import { handleFileUpload } from '../../utils/overall';
import { getErrorMessage } from '../../utils/api';

export default function EditPMScreen({
  navigation,
  route
}: RootStackScreenProps<'EditPreventiveMaintenance'>) {
  const { preventiveMaintenance } = route.params;
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { uploadFiles, getWOFieldsAndShapes } = useContext(
    CompanySettingsContext
  );
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { schedule } = preventiveMaintenance;

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
          ...preventiveMaintenance,
          ...getWOBaseValues(t, preventiveMaintenance),
          startsOn: schedule?.startsOn ? new Date(schedule.startsOn) : null,
          endsOn: schedule?.endsOn ? new Date(schedule.endsOn) : null,
          frequency: schedule?.frequency,
          dueDateDelay: schedule?.dueDateDelay
        }}
        onChange={() => {}}
        onSubmit={async (values) => {
          let formattedValues = formatPreventiveMaintenanceValues(values);
          try {
            const imageAndFiles = await handleFileUpload(
              { files: formattedValues.files, image: formattedValues.image },
              uploadFiles
            );
            formattedValues = {
              ...formattedValues,
              image: imageAndFiles.image,
              files: imageAndFiles.files
            };
            await dispatch(
              editPreventiveMaintenance(
                preventiveMaintenance.id,
                formattedValues
              )
            );
            // The recurrence lives on a separate resource, so the two halves of
            // the form are saved separately.
            if (schedule) {
              await dispatch(
                patchSchedule(schedule.id, preventiveMaintenance.id, {
                  startsOn: formattedValues.startsOn,
                  endsOn: formattedValues.endsOn,
                  frequency: formattedValues.frequency,
                  dueDateDelay: formattedValues.dueDateDelay
                })
              );
            }
            await dispatch(
              getSinglePreventiveMaintenance(preventiveMaintenance.id)
            );
            showSnackBar(t('changes_saved_success'), 'success');
            navigation.goBack();
          } catch (err) {
            showSnackBar(getErrorMessage(err, t('pm_update_failure')), 'error');
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
