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
import {
  editWorkOrder,
  workOrderAttachmentChanges
} from '../../slices/workOrder';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { formatWorkOrderValues, getWorkOrderFields } from '../../utils/fields';
import { formatCustomFields } from '../../utils/formatters';
import { getWOBaseValues } from '../../utils/woBase';
import { patchTasks } from '../../slices/task';
import { getCustomFields } from '../../slices/customField';
import { CustomFieldEntityType } from '../../models/customField';
import { useOffline } from '../../offline/OfflineProvider';
import { useOfflineWorkOrder } from '../../offline/hooks';
import { OfflineAttachmentEditor } from '../../offline/components/OfflineAttachmentEditor';
import {
  OfflineStatus,
  OfflineWorkPanel
} from '../../offline/components/OfflineWorkPanel';
import { Text } from 'react-native-paper';
import useAuth from '../../hooks/useAuth';
import { PermissionEntity } from '../../models/role';
import { PlanFeature } from '../../models/subscriptionPlan';
import { OfflineError } from '../../offline/types';

export default function EditWorkOrderScreen({
  navigation,
  route
}: RootStackScreenProps<'EditWorkOrder'>) {
  const { t } = useTranslation();
  const { workOrder, tasks } = route.params;
  const offline = useOffline();
  const prepared = useOfflineWorkOrder(workOrder.id);
  const isPrepared =
    offline.enabled &&
    offline.prepared.some((item) => item.id === workOrder.id);
  const { hasCreatePermission, hasFeature } = useAuth();
  const attachmentsEditable =
    !!prepared?.canEdit &&
    hasCreatePermission(PermissionEntity.FILES) &&
    hasFeature(PlanFeature.FILE);
  const connectedAvailable = !offline.enabled || offline.networkAvailable;
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
  const [fields, shape] = getFieldsAndShapes();
  if (isPrepared && (!offline.readable || !prepared?.available))
    return (
      <View>
        <OfflineStatus />
        <Text>{t('offline_unavailable')}</Text>
        <OfflineWorkPanel workOrderId={workOrder.id} />
      </View>
    );
  return (
    <View style={styles.container}>
      {isPrepared && !connectedAvailable && (
        <Text>{t('offline_other_fields_read_only')}</Text>
      )}
      <Form
        fields={fields}
        validation={Yup.object().shape(shape)}
        navigation={navigation}
        preserveFileIds
        submitText={t(isPrepared ? 'offline_save_other_changes' : 'save')}
        readOnly={isPrepared && !connectedAvailable}
        submitDisabled={isPrepared && !connectedAvailable}
        fieldOverrides={
          isPrepared
            ? {
                image: (
                  <OfflineAttachmentEditor
                    workOrderId={workOrder.id}
                    image
                    title={t('image')}
                    files={
                      prepared.workOrder.image ? [prepared.workOrder.image] : []
                    }
                    showSubmit={!fields.some((field) => field.name === 'files')}
                    editable={attachmentsEditable}
                    onRemoveServer={
                      prepared.canEdit
                        ? (fileId) =>
                            offline.removeServerAttachment(
                              workOrder.id,
                              fileId,
                              true
                            )
                        : undefined
                    }
                  />
                ),
                files: (
                  <OfflineAttachmentEditor
                    workOrderId={workOrder.id}
                    title={t('files')}
                    files={prepared.workOrder.files}
                    editable={attachmentsEditable}
                    onRemoveServer={
                      prepared.canEdit
                        ? (fileId) =>
                            offline.removeServerAttachment(
                              workOrder.id,
                              fileId,
                              false
                            )
                        : undefined
                    }
                  />
                )
              }
            : undefined
        }
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
            if (!connectedAvailable)
              throw new OfflineError('NETWORK_UNAVAILABLE');
            if (isPrepared) {
              delete formattedValues.image;
              delete formattedValues.files;
            } else {
              const imageAndFiles = await handleFileUpload(
                {
                  files: formattedValues.files.filter((file) => !file.id),
                  image: formattedValues.image
                },
                uploadFiles
              );
              formattedValues = workOrderAttachmentChanges(
                {
                  ...formattedValues,
                  image: imageAndFiles.image,
                  files: [
                    ...formattedValues.files
                      .filter((file) => file.id)
                      .map((file) => ({ id: file.id })),
                    ...imageAndFiles.files
                  ]
                },
                workOrder
              );
            }
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
      {isPrepared && <OfflineWorkPanel workOrderId={workOrder.id} />}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
