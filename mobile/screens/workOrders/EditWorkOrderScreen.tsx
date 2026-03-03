import { RootStackScreenProps } from '../../types';
import { View } from '../../components/Themed';
import Form from '../../components/form';
import * as Yup from 'yup';
import { StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import { IField } from '../../models/form';
import { useContext } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { getImageAndFiles } from '../../utils/overall';
import { useDispatch } from '../../store';
import { editWorkOrder } from '../../slices/workOrder';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { formatWorkOrderValues, getWorkOrderFields } from '../../utils/fields';
import { getWOBaseValues } from '../../utils/woBase';
import { patchTasks } from '../../slices/task';

export default function EditWorkOrderScreen({
  navigation,
  route
}: RootStackScreenProps<'EditWorkOrder'>) {
  const { t } = useTranslation();
  const { workOrder, tasks } = route.params;
  const { uploadFiles, getWOFieldsAndShapes } = useContext(
    CompanySettingsContext
  );
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const defaultShape: { [key: string]: any } = {
    title: Yup.string().required(t('required_wo_title'))
  };

  const onEditSuccess = () => {
    showSnackBar(t('changes_saved_success'), 'success');
    navigation.goBack();
  };
  const onEditFailure = (err) => showSnackBar(t('wo_update_failure'), 'error');
  const getFieldsAndShapes = (): [Array<IField>, { [key: string]: any }] => {
    return getWOFieldsAndShapes(getWorkOrderFields(t), defaultShape);
  };
  return (
    <View style={styles.container}>
      <Form
        fields={getFieldsAndShapes()[0]}
        validation={Yup.object().shape(getFieldsAndShapes()[1])}
        navigation={navigation}
        submitText={t('save')}
        values={{
          ...workOrder,
          tasks: tasks,
          ...getWOBaseValues(t, workOrder)
        }}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          let formattedValues = formatWorkOrderValues(values);
          try {
            //differentiate files from api and formattedValues
            const filesToUpload = formattedValues.files.filter(
              (file) => !file.id
            );
            const existingFiles = formattedValues.files.filter(
              (file) => file.id
            );
            const uploadedFiles = await uploadFiles(
              filesToUpload,
              formattedValues.image
            );
            const imageAndFiles = getImageAndFiles([
              ...existingFiles,
              ...uploadedFiles
            ]);
            formattedValues = {
              ...formattedValues,
              image: imageAndFiles.image,
              files: imageAndFiles.files
            };
            await dispatch(
              patchTasks(
                workOrder?.id,
                formattedValues.tasks.map((task) => {
                  return {
                    ...task.taskBase,
                    options: task.taskBase.options.map((option) => option.label)
                  };
                })
              )
            );
            await dispatch(editWorkOrder(workOrder?.id, formattedValues));
            onEditSuccess();
          } catch (err) {
            onEditFailure(err);
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
