import { RootStackScreenProps } from '../../types';
import { View } from '../../components/Themed';
import Form from '../../components/form';
import * as Yup from 'yup';
import { StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import {
  IField,
  getCustomFieldsIFields,
  getCustomFieldsRequiredShape,
  getCustomFieldsValues
} from '../../models/form';
import { useContext, useEffect } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { getImageAndFiles, handleFileUpload } from '../../utils/overall';
import { useDispatch, useSelector } from '../../store';
import { editWorkOrder } from '../../slices/workOrder';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { formatWorkOrderValues, getWorkOrderFields } from '../../utils/fields';
import { formatCustomFields } from '../../utils/formatters';
import { getWOBaseValues } from '../../utils/woBase';
import { patchTasks } from '../../slices/task';
import { getCustomFields } from '../../slices/customField';
import { CustomFieldEntityType } from '../../models/customField';

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
  const { customFields } = useSelector((state) => state.customFields);

  const defaultShape: { [key: string]: any } = {
    title: Yup.string().required(t('required_wo_title')),
    ...getCustomFieldsRequiredShape(
      customFields,
      CustomFieldEntityType.WORK_ORDER,
      t
    )
  };

  const onEditSuccess = () => {
    showSnackBar(t('changes_saved_success'), 'success');
    navigation.goBack();
  };
  const onEditFailure = (err) => showSnackBar(t('wo_update_failure'), 'error');
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
        fields={getFieldsAndShapes()[0]}
        validation={Yup.object().shape(getFieldsAndShapes()[1])}
        navigation={navigation}
        submitText={t('save')}
        values={{
          ...workOrder,
          tasks: tasks,
          ...getWOBaseValues(t, workOrder),
          ...getCustomFieldsValues(workOrder)
        }}
        onChange={({ field, e }) => {}}
        onSubmit={async (values) => {
          let formattedValues = formatWorkOrderValues(values);
          formattedValues = formatCustomFields(formattedValues);
          try {
            const imageAndFiles = await handleFileUpload(
              {
                files: formattedValues.files,
                image: formattedValues.image
              },
              uploadFiles
            );
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
