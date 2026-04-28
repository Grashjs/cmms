import { Task } from '../../models/tasks';
import { getTasks, patchTask } from '../../slices/task';
import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useState } from 'react';
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

export default function TasksScreen({
  navigation,
  route
}: RootStackScreenProps<'Tasks'>) {
  const { t }: { t: any } = useTranslation();
  const { tasksProps, workOrderId } = route.params;
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
  const [tasks, setTasks] = useState<Task[]>(tasksProps);
  const [cameraTaskId, setCameraTaskId] = useState<number | null>(null);
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);

  useEffect(() => setTasks(tasksProps), [tasksProps]);

  function handleChange(value: string | number, id: number) {
    const task = tasks.find((task) => task.id === id);
    dispatch(patchTask(workOrderId, id, { ...task, value }))
      .then(() => showSnackBar(t('task_update_success'), 'success'))
      .catch(() => showSnackBar(t('task_update_failure'), 'error'));

    const newTasks = tasks.map((task) => {
      if (task.id === id) {
        return { ...task, value };
      }
      return task;
    });
    setTasks(newTasks);
  }

  function handleNoteChange(value: string, id: number) {
    const newTasks = tasks.map((task) => {
      if (task.id === id) {
        return { ...task, notes: value };
      }
      return task;
    });
    setTasks(newTasks);
  }

  function toggleNotes(id: number) {
    const newNotes = new Map(notes);
    newNotes.set(id, !newNotes.get(id));
    setNotes(newNotes);
  }

  function handleSaveNotes(value: string, id: number) {
    const task = tasks.find((task) => task.id === id);
    return dispatch(patchTask(workOrderId, id, { ...task, notes: value })).then(
      () => {
        showSnackBar(t('notes_save_success'), 'success');
        toggleNotes(task.id);
      }
    );
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
    const files = [{ uri, name: fileName, type: mime.getType(fileName) || 'image/jpeg' }];
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
      console.warn('[TasksScreen] Picker result -> upload', JSON.stringify({ taskId, assets: result.assets.length }));
      return dispatch(addFiles(formatImages(result), 'IMAGE', taskId))
        .then(onImageUploadSuccess)
        .then(() => dispatch(getTasks(workOrderId)))
        .catch(onImageUploadFailure);
    }
  };
  return (
    <ScrollView style={styles.container}>
      <InAppCamera
        visible={cameraTaskId !== null}
        onCapture={handleInAppCapture}
        onClose={() => setCameraTaskId(null)}
      />
      {tasks.map((task) => (
        <SingleTask
          key={task.id}
          task={task}
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
