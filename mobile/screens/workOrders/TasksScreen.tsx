import { Task } from '../../models/tasks';
import { getTasks, patchTask } from '../../slices/task';
import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useRef, useState } from 'react';
import { useDispatch } from '../../store';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import { ScrollView, StyleSheet } from 'react-native';
import SingleTask from '../../components/SingleTask';
import { RootStackScreenProps } from '../../types';
import { addFiles } from '../../slices/file';
import * as ImagePicker from 'expo-image-picker';
import mime from 'mime';
import { formatImages } from '../../utils/overall';
import ImageView from 'react-native-image-viewing';
import { SheetManager } from 'react-native-actions-sheet';
import { openLibraryWithPermission } from '../../utils/mediaPermissions';
import InAppCamera from '../../components/InAppCamera';
import { taskPatch } from '../../offline/runtime';
import { useOffline } from '../../offline/OfflineProvider';
import { useOfflineWorkOrder } from '../../offline/hooks';
import { OfflineTaskValue } from '../../offline/components/OfflineTaskValue';
import {
  OfflineWorkPanel,
  OfflineStatus
} from '../../offline/components/OfflineWorkPanel';
import { Text } from 'react-native-paper';
import { useSelector } from '../../store';
import useAuth from '../../hooks/useAuth';

const EMPTY_TASKS: Task[] = [];
type TaskInput = { revision: number; value: string | number };
type TaskInputs = { value?: TaskInput; notes?: TaskInput };

export default function TasksScreen({
  navigation,
  route
}: RootStackScreenProps<'Tasks'>) {
  const { t }: { t: any } = useTranslation();
  const { user: offlineUser } = useAuth();
  const offlineUserId = offlineUser?.id;
  const { tasksProps = EMPTY_TASKS, workOrderId } = route.params;
  const offline = useOffline();
  const prepared = useOfflineWorkOrder(workOrderId);
  const isPrepared =
    offline.enabled && offline.prepared.some((item) => item.id === workOrderId);
  const connectedTasks = useSelector(
    (state) => state.tasks.tasksByWorkOrder[workOrderId]
  );
  // Connected form inputs are separate from both Redux replies and SQLite projections.
  const [inputs, setInputs] = useState<Record<number, TaskInputs>>({});
  const latestInputs = useRef(inputs);
  const inputRevision = useRef(0);
  const [savingTasks, setSavingTasks] = useState<Record<number, boolean>>({});
  const [isImageViewerOpen, setIsImageViewerOpen] = useState<boolean>(false);
  const [currentImage, setCurrentImage] = useState<string>();
  const [currentImages, setCurrentImages] = useState<string[]>([]);
  const initialNotes = new Map();
  tasksProps.forEach((task) => {
    if (task.notes) {
      initialNotes.set(task.id, true);
    }
  });
  const [notes, setNotes] = useState<Map<number, boolean>>(initialNotes);
  const serverTasks = isPrepared
    ? prepared?.tasks ?? EMPTY_TASKS
    : connectedTasks ?? tasksProps;
  const tasks = serverTasks.map((task) => ({
    ...task,
    ...(inputs[task.id]?.value ? { value: inputs[task.id].value.value } : {}),
    ...(inputs[task.id]?.notes
      ? { notes: String(inputs[task.id].notes.value) }
      : {})
  }));
  const [cameraTaskId, setCameraTaskId] = useState<number | null>(null);
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);

  useEffect(() => {
    if (
      offline.initialized &&
      !isPrepared &&
      !connectedTasks &&
      (!offline.enabled || offline.networkAvailable)
    ) {
      void dispatch(getTasks(workOrderId)).catch(offline.reportError);
    }
  }, [offline.initialized, isPrepared, workOrderId, offline.networkAvailable]);

  function captureInput(
    field: keyof TaskInputs,
    value: string | number,
    id: number
  ) {
    const input = { value, revision: ++inputRevision.current };
    const next = {
      ...latestInputs.current,
      [id]: { ...latestInputs.current[id], [field]: input }
    };
    latestInputs.current = next;
    setInputs(next);
    return input.revision;
  }

  function acknowledgeInput(
    field: keyof TaskInputs,
    id: number,
    revision: number
  ) {
    if (latestInputs.current[id]?.[field]?.revision !== revision) return false;
    const fields = { ...latestInputs.current[id] };
    delete fields[field];
    const next = { ...latestInputs.current, [id]: fields };
    if (!Object.keys(fields).length) delete next[id];
    latestInputs.current = next;
    setInputs(next);
    return true;
  }

  function handleChange(value: string | number, id: number) {
    const task = serverTasks.find((task) => task.id === id);
    const revision = captureInput('value', value, id);
    setSavingTasks((current) => ({ ...current, [id]: true }));
    dispatch(
      patchTask(workOrderId, id, taskPatch('value', value, task.version))
    )
      .then(() => {
        acknowledgeInput('value', id, revision);
        showSnackBar(t('task_update_success'), 'success');
      })
      .catch(() => showSnackBar(t('task_update_failure'), 'error'))
      .finally(() =>
        setSavingTasks((current) => ({ ...current, [id]: false }))
      );
  }

  function handleNoteChange(value: string, id: number) {
    captureInput('notes', value, id);
  }

  function toggleNotes(id: number) {
    setNotes((current) => new Map(current).set(id, !current.get(id)));
  }

  function handleSaveNotes(value: string, id: number) {
    const task = serverTasks.find((task) => task.id === id);
    const input = latestInputs.current[id]?.notes;
    const revision =
      input?.value === value
        ? input.revision
        : captureInput('notes', value, id);
    return dispatch(
      patchTask(workOrderId, id, taskPatch('notes', value, task.version))
    )
      .then(() => {
        showSnackBar(t('notes_save_success'), 'success');
        if (acknowledgeInput('notes', id, revision)) {
          setNotes((current) => new Map(current).set(id, false));
        }
      })
      .catch(() => showSnackBar(t('task_update_failure'), 'error'));
  }

  const onImageUploadSuccess = () => {
    showSnackBar(t('images_add_task_success'), 'success');
  };
  const onImageUploadFailure = (err) =>
    showSnackBar(t('images_add_task_failure'), 'error');
  const handleZoomImage = (images: string[], image: string) => {
    setCurrentImage(image);
    setCurrentImages(images);
    setIsImageViewerOpen(true);
  };

  const uploadImage = async (taskId: number) => {
    console.warn('[TasksScreen] Tap -> library', JSON.stringify({ taskId }));
    const result = await openLibraryWithPermission('TasksScreen', {
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsMultipleSelection: true,
      quality: 1
    });

    if (!result || result.canceled) {
      console.warn('[TasksScreen] Library picker canceled or unavailable');
      return;
    }

    await onImagePicked(result, taskId);
  };
  const takePhoto = (taskId: number) => {
    setCameraTaskId(taskId);
  };
  const handleInAppCapture = async (uri: string) => {
    if (cameraTaskId === null) return;
    const taskId = cameraTaskId;
    setCameraTaskId(null);
    const fileName = uri.split('/').pop() || 'photo.jpg';
    const files = [
      { uri, name: fileName, type: mime.getType(fileName) || 'image/jpeg' }
    ];
    return dispatch(addFiles(files, 'IMAGE', taskId))
      .then(onImageUploadSuccess)
      .then(() => dispatch(getTasks(workOrderId)))
      .catch(onImageUploadFailure);
  };
  const onImagePicked = async (
    result: ImagePicker.ImagePickerResult,
    taskId: number
  ) => {
    if (!result.canceled) {
      console.warn(
        '[TasksScreen] Picker result -> upload',
        JSON.stringify({ taskId, assets: result.assets.length })
      );
      return dispatch(addFiles(formatImages(result), 'IMAGE', taskId))
        .then(onImageUploadSuccess)
        .then(() => dispatch(getTasks(workOrderId)))
        .catch(onImageUploadFailure);
    }
  };
  if (!offline.initialized) return <Text>{t('offline_preparing')}</Text>;
  if (isPrepared && (!prepared?.available || !offline.readable))
    return (
      <ScrollView style={styles.container}>
        <OfflineStatus />
        <Text>{t('offline_unavailable')}</Text>
        <OfflineWorkPanel workOrderId={workOrderId} />
      </ScrollView>
    );
  return (
    <ScrollView style={styles.container}>
      {isPrepared && <OfflineStatus />}
      <InAppCamera
        visible={cameraTaskId !== null}
        onCapture={handleInAppCapture}
        onClose={() => setCameraTaskId(null)}
      />
      {tasks.map((task) => (
        <SingleTask
          key={task.id}
          task={task}
          saving={!!savingTasks[task.id]}
          connectedDisabled={isPrepared && !offline.networkAvailable}
          valueEditor={
            isPrepared && task.taskBase.taskType === 'TEXT' ? (
              <OfflineTaskValue
                workOrderId={workOrderId}
                taskId={task.id}
                editable={
                  !task.taskBase.user || task.taskBase.user.id === offlineUserId
                }
              />
            ) : undefined
          }
          handleChange={handleChange}
          handleNoteChange={handleNoteChange}
          handleSaveNotes={handleSaveNotes}
          toggleNotes={toggleNotes}
          handleSelectImages={(taskId) => {
            SheetManager.show('upload-file-sheet', {
              payload: {
                onPickImage: () => uploadImage(taskId),
                onTakePhoto: () => takePhoto(taskId)
              }
            });
          }}
          handleZoomImage={handleZoomImage}
          notes={notes}
        />
      ))}
      {isPrepared && <OfflineWorkPanel workOrderId={workOrderId} />}
      <ImageView
        images={currentImages.map((uri) => ({ uri }))}
        imageIndex={currentImages.findIndex((img) => img === currentImage)}
        visible={isImageViewerOpen}
        onRequestClose={() => setIsImageViewerOpen(false)}
      />
    </ScrollView>
  );
}
const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingHorizontal: 20,
    backgroundColor: 'white'
  }
});
