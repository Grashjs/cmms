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
          return new Promise<void>((resolve, rej) => {
            //differentiate files from api and formattedValues
            const files = formattedValues.files.find((file) => file.id)
              ? []
              : formattedValues.files;
            uploadFiles(files, formattedValues.image)
              .then((files) => {
                const imageAndFiles = getImageAndFiles(files, workOrder.image);
                formattedValues = {
                  ...formattedValues,
                  image: imageAndFiles.image,
                  files: [...workOrder.files, ...imageAndFiles.files]
                };
                dispatch(
                  //TODO editTask
                  patchTasks(
                    workOrder?.id,
                    formattedValues.tasks.map((task) => {
                      return {
                        ...task.taskBase,
                        options: task.taskBase.options.map(
                          (option) => option.label
                        )
                      };
                    })
                  )
                )
                  .then(() =>
                    dispatch(editWorkOrder(workOrder?.id, formattedValues))
                      .then(onEditSuccess)
                      .then(() => resolve())
                      .catch((err) => {
                        onEditFailure(err);
                        rej();
                      })
                  )
                  .catch((err) => {
                    onEditFailure(err);
                    rej();
                  });
              })
              .catch((err) => {
                onEditFailure(err);
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
