import { Task } from '../../models/tasks';
import { getTasks, patchTask } from '../../slices/task';
import { useTranslation } from 'react-i18next';
import { useCallback, useContext, useEffect, useRef, useState } from 'react';
import { useDispatch } from '../../store';
import { CustomSnackBarContext } from '../../contexts/CustomSnackBarContext';
import {
  Keyboard,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  StyleSheet
} from 'react-native';
import { useHeaderHeight } from '@react-navigation/elements';
import SingleTask from '../../components/SingleTask';
import useAuth from '../../hooks/useAuth';
import { PermissionEntity } from '../../models/role';
import { PlanFeature } from '../../models/subscriptionPlan';
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
import { OfflineStatus } from '../../offline/components/OfflineWorkPanel';
import { Button, Text } from 'react-native-paper';
import { useSelector } from '../../store';

import { OfflineAttachmentEditor } from '../../offline/components/OfflineAttachmentEditor';
import type { IFile } from '../../models/file';
import { sessionGeneration } from '../../offline/sessionBridge';
import { OfflineFileItem } from '../../offline/components/OfflineFileItem';
import { usePendingWork } from '../../offline/hooks';

const EMPTY_TASKS: Task[] = [];
type TaskInput = {
  revision: number;
  value: string | number;
  committed?: boolean;
};
type TaskInputs = { value?: TaskInput; notes?: TaskInput };

export default function TasksScreen({
  navigation,
  route
}: RootStackScreenProps<'Tasks'>) {
  const { t }: { t: any } = useTranslation();
  const headerHeight = useHeaderHeight();
  const scrollView = useRef<ScrollView>(null);
  const focusedInput = useRef<number | null>(null);
  const keyboardVisible = useRef(false);
  const scrollToFocusedInput = useCallback(() => {
    if (!keyboardVisible.current || focusedInput.current === null) return;
    scrollView.current?.scrollResponderScrollNativeHandleToKeyboard(
      focusedInput.current,
      24,
      true
    );
  }, []);
  useEffect(() => {
    const shown = Keyboard.addListener('keyboardDidShow', () => {
      keyboardVisible.current = true;
      requestAnimationFrame(scrollToFocusedInput);
    });
    const hidden = Keyboard.addListener('keyboardDidHide', () => {
      keyboardVisible.current = false;
    });
    return () => {
      shown.remove();
      hidden.remove();
    };
  }, [scrollToFocusedInput]);
  const { hasCreatePermission, hasFeature } = useAuth();
  const canAddFiles = hasCreatePermission(PermissionEntity.FILES) && hasFeature(PlanFeature.FILE);

  const { tasksProps = EMPTY_TASKS, workOrderId } = route.params;
  const offline = useOffline();
  const prepared = useOfflineWorkOrder(workOrderId);
  const isPrepared =
    offline.enabled && offline.prepared.some((item) => item.id === workOrderId);
  const pending = usePendingWork(workOrderId, isPrepared);
  const [failedImages, setFailedImages] = useState<Record<number, IFile[]>>({});
  const scope = workOrderId + ':' + sessionGeneration() + ':' + isPrepared;
  const owner = useRef(scope);
  owner.current = scope;
  const cameraScope = useRef('');
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
  useEffect(() => {
    owner.current = scope;
    setFailedImages({});
    setCameraTaskId(null);
    latestInputs.current = {};
    setInputs({});
    return () => {
      owner.current = '';
    };
  }, [scope]);
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

  function finishInput(field: keyof TaskInputs, id: number, revision: number) {
    if (!isPrepared) return acknowledgeInput(field, id, revision);
    const input = latestInputs.current[id]?.[field];
    if (input?.revision !== revision) return false;
    const next = {
      ...latestInputs.current,
      [id]: {
        ...latestInputs.current[id],
        [field]: { ...input, committed: true }
      }
    };
    latestInputs.current = next;
    setInputs(next);
    return true;
  }

  // SQLite observers can notify after the write promise resolves. Keep the
  // visible input until its value is actually present in the projection.
  useEffect(() => {
    if (!isPrepared) return;
    for (const task of serverTasks) {
      for (const field of ['value', 'notes'] as const) {
        const input = inputs[task.id]?.[field];
        if (
          input?.committed &&
          String(task[field] ?? '') === String(input.value)
        )
          acknowledgeInput(field, task.id, input.revision);
      }
    }
  }, [isPrepared, serverTasks, inputs]);

  function handleChange(value: string | number, id: number) {
    const task = serverTasks.find((task) => task.id === id);
    const revision = captureInput('value', value, id);
    setSavingTasks((current) => ({ ...current, [id]: true }));
    return (isPrepared
      ? offline.commitTaskField(
          workOrderId,
          id,
          'value',
          String(value),
          task.version
        )
      : dispatch(
          patchTask(workOrderId, id, taskPatch('value', value, task.version))
        )
    )
      .then(() => {
        const latest = finishInput('value', id, revision);
        if (!isPrepared || latest)
          showSnackBar(t(isPrepared ? 'offline_queued' : 'task_update_success'), 'success');
        return isPrepared && latest;
      })
      .catch((error) => {
        isPrepared
          ? offline.reportError(error)
          : showSnackBar(t('task_update_failure'), 'error');
        return false;
      })
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
    return (
      isPrepared
        ? offline.commitTaskField(workOrderId, id, 'notes', value, task.version)
        : dispatch(
            patchTask(workOrderId, id, taskPatch('notes', value, task.version))
          )
    )
      .then(() => {
        if (!isPrepared) showSnackBar(t('notes_save_success'), 'success');
        if (finishInput('notes', id, revision)) {
          setNotes((current) => new Map(current).set(id, false));
        }
      })
      .catch((error) =>
        isPrepared
          ? offline.reportError(error)
          : showSnackBar(t('task_update_failure'), 'error')
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
    cameraScope.current = scope;
    setCameraTaskId(taskId);
  };
  const handleInAppCapture = async (uri: string) => {
    if (cameraTaskId === null || cameraScope.current !== scope) return;
    const taskId = cameraTaskId;
    setCameraTaskId(null);
    const fileName = uri.split('/').pop() || 'photo.jpg';
    const files = [
      { uri, name: fileName, type: mime.getType(fileName) || 'image/jpeg' }
    ];
    await addImages(taskId, files);
  };
  const addImages = async (taskId: number, files: IFile[]) => {
    if (owner.current !== scope) return;
    if (isPrepared) {
      try {
        const review = await offline.repository.getAttachmentDraft(
          workOrderId,
          taskId
        );
        if (owner.current !== scope) return;
        await offline.commitAttachments(
          workOrderId,
          { kind: 'TASK', taskId },
          files,
          null,
          review?.revision ?? null
        );
        if (owner.current === scope)
          setFailedImages((previous) => ({ ...previous, [taskId]: [] }));
      } catch (error) {
        if (owner.current === scope) {
          setFailedImages((previous) => ({ ...previous, [taskId]: files }));
          offline.reportError(error);
        }
      }
      return;
    }
    return dispatch(addFiles(files, 'IMAGE', taskId))
      .then(onImageUploadSuccess)
      .then(() => dispatch(getTasks(workOrderId)))
      .catch(onImageUploadFailure);
  };
  const onImagePicked = async (
    result: ImagePicker.ImagePickerResult,
    taskId: number
  ) => {
    if (!result.canceled) await addImages(taskId, formatImages(result));
  };
  if (!offline.initialized) return <Text>{t('offline_preparing')}</Text>;
  if (isPrepared && (!prepared?.available || !offline.readable))
    return (
      <ScrollView style={styles.container}>
        <OfflineStatus />
        <Text>{t('offline_unavailable')}</Text>
      </ScrollView>
    );
  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
      keyboardVerticalOffset={Platform.OS === 'ios' ? headerHeight : 0}
    >
      <ScrollView
        ref={scrollView}
        style={styles.container}
        contentContainerStyle={{ paddingBottom: 32 }}
        keyboardShouldPersistTaps="handled"
        onLayout={() => requestAnimationFrame(scrollToFocusedInput)}
      >
        {isPrepared && <OfflineStatus />}
        <InAppCamera
          visible={cameraTaskId !== null}
          onCapture={handleInAppCapture}
          onClose={() => setCameraTaskId(null)}
        />
        {tasks.map((task) => (
          <SingleTask
            key={scope + ':' + task.id}
            task={task}
            saving={!isPrepared && !!savingTasks[task.id]}
            imagesEditor={
              isPrepared ? (
                <>
                  <OfflineAttachmentEditor
                    workOrderId={workOrderId}
                    taskId={task.id}
                    files={task.images}
                    title={t('images')}
                    sharedTaskPicker
                    editable={!!prepared?.canEdit && canAddFiles}
                  />
                  {pending
                    .filter(
                      (item) =>
                        item.operationType === 'ADD_TASK_IMAGES' &&
                        item.taskId === task.id &&
                        item.targetAvailable
                    )
                    .flatMap((item) => item.fileIds ?? [])
                    .map((localId) => (
                      <OfflineFileItem
                        key={localId}
                        workOrderId={workOrderId}
                        localId={localId}
                        compact
                      />
                    ))}
                  {!!failedImages[task.id]?.length && (
                    <Button
                      disabled={!offline.writable || !prepared?.canEdit || !canAddFiles}
                      onPress={() =>
                        void addImages(task.id, failedImages[task.id])
                      }
                    >
                      {t('retry')}
                    </Button>
                  )}
                </>
              ) : undefined
            }
            connectedDisabled={
              isPrepared && (!prepared?.canEdit || !offline.writable)
            }
            handleChange={handleChange}
            onInputFocus={(target) => {
              focusedInput.current = target;
              scrollToFocusedInput();
            }}
            localCommitOnSave={isPrepared}
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
    </KeyboardAvoidingView>
  );
}
const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingHorizontal: 20,
    backgroundColor: 'white'
  }
});
