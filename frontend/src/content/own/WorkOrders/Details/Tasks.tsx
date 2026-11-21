import {
  Card,
  CardContent,
  CardHeader,
  Dialog,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  Typography,
  Alert,
  Box,
  Button
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import TaskAltTwoToneIcon from '@mui/icons-material/TaskAltTwoTone';
import { useContext, useEffect, useMemo, useRef, useState } from 'react';
import SingleTask from '../../components/form/SelectTasks/SingleTask';
import { Task } from '../../../../models/owns/tasks';
import { getTasksByWorkOrder, patchTask } from '../../../../slices/task';
import { useDispatch } from '../../../../store';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import Form from '../../components/form';
import * as Yup from 'yup';
import { addFiles } from '../../../../slices/file';
import { IField } from '../../type';
import api from '../../../../utils/api';
import { acknowledgeTaskDraft, projectTaskDraft, TaskDraft, TaskFields, TaskSavePausedError, TaskSaveQueue } from '../../../../utils/taskSaveQueue';

interface TasksProps {
  tasksProps: Task[];
  workOrderId: number;
  disabled: boolean;
  handleZoomImage: (images: string[], image: string) => void;
}

type TaskSaveIssue = { server?: Task; reviewing?: boolean };

export default function Tasks(props: TasksProps) {
  return <TaskEditor key={props.workOrderId} {...props} />;
}

function TaskEditor({
  tasksProps,
  workOrderId,
  handleZoomImage,
  disabled
}: TasksProps) {
  const { t }: { t: any } = useTranslation();
  const [openSelectImages, setOpenSelectImages] = useState<boolean>(false);
  const initialNotes = new Map();
  tasksProps.forEach((task) => {
    if (task.notes || task.images.length) {
      initialNotes.set(task.id, true);
    }
  });
  const [notes, setNotes] = useState<Map<number, boolean>>(initialNotes);
  const [tasks, setTasks] = useState<Task[]>(tasksProps);
  const [currentTask, setCurrentTask] = useState<Task>();
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);

  const canonical = useRef(new Map(tasksProps.map((task) => [task.id, task])));
  const drafts = useRef(new Map<number, TaskDraft>());
  const nextRevision = useRef(0);
  const submittedRevisions = useRef(new Map<number, Partial<Record<keyof TaskDraft, number>>>());
  const issuesRef = useRef(new Map<number, TaskSaveIssue>());
  const [issues, setIssues] = useState(new Map<number, TaskSaveIssue>());
  const saves = useMemo(() => new TaskSaveQueue((id, patch) =>
    dispatch(patchTask(workOrderId, id, patch)) as Promise<Task>), [dispatch, workOrderId]);

  function renderDrafts() {
    setTasks(Array.from(canonical.current.values()).map((task) => projectTaskDraft(task, drafts.current.get(task.id))));
  }

  function ingest(task: Task) {
    const previous = canonical.current.get(task.id);
    if (previous?.version === undefined || task.version === undefined || task.version >= previous.version) {
      canonical.current.set(task.id, task);
      // A server refresh must not rebase an input that has not been submitted yet.
      if (!drafts.current.has(task.id)) saves.observe(task);
    }
  }

  useEffect(() => {
    tasksProps.forEach(ingest);
    for (const id of canonical.current.keys()) {
      if (!tasksProps.some((task) => task.id === id) && !drafts.current.has(id)) canonical.current.delete(id);
    }
    renderDrafts();
  }, [tasksProps, saves]);

  function remember(id: number, field: keyof TaskDraft, value: string | number): number {
    if (!drafts.current.has(id)) saves.observe(canonical.current.get(id));
    const revision = ++nextRevision.current;
    drafts.current.set(id, { ...drafts.current.get(id), [field]: { revision, value } });
    renderDrafts();
    return revision;
  }

  function setTaskIssue(id: number, issue?: TaskSaveIssue) {
    if (issue) issuesRef.current.set(id, issue);
    else issuesRef.current.delete(id);
    setIssues(new Map(issuesRef.current));
  }

  async function recordFailure(id: number, error: Error) {
    if (error instanceof TaskSavePausedError || issuesRef.current.has(id)) return;
    const issue = {};
    setTaskIssue(id, issue);
    try {
      // Canonical refresh never replaces a local input, including after an unrelated notes save.
      const server = await api.get<Task>(`tasks/${id}`);
      if (issuesRef.current.get(id) !== issue) return;
      ingest(server);
      setTaskIssue(id, { server: canonical.current.get(id) });
    } catch { /* Retain the local draft and paused saves until the server can be checked. */ }
    renderDrafts();
  }

  async function saveFields(id: number, fields: TaskFields, revisions: Partial<Record<keyof TaskDraft, number>>) {
    submittedRevisions.current.set(id, { ...submittedRevisions.current.get(id), ...revisions });
    try {
      const saved = await saves.enqueue(id, fields);
      ingest(saved);
      let draft = drafts.current.get(id) || {};
      (Object.keys(revisions) as (keyof TaskDraft)[]).forEach((field) => {
        draft = acknowledgeTaskDraft(draft, field, revisions[field]);
      });
      if (Object.keys(draft).length) drafts.current.set(id, draft); else drafts.current.delete(id);
      renderDrafts();
      return true;
    } catch (error) {
      await recordFailure(id, error as Error);
      return false;
    }
  }

  function handleChange(value: string | number, id: number, inputRevision?: number) {
    if (inputRevision !== undefined && (drafts.current.get(id)?.value?.revision !== inputRevision
      || (submittedRevisions.current.get(id)?.value || 0) >= inputRevision)) return;
    const revision = inputRevision ?? remember(id, 'value', value);
    void saveFields(id, { value }, { value: revision }).then((saved) => {
      if (saved) showSnackBar(t('task_update_success'), 'success');
    });
  }

  function handleNoteChange(value: string, id: number) {
    remember(id, 'notes', value);
  }

  function toggleNotes(id: number) {
    setNotes((current) => new Map(current).set(id, !current.get(id)));
  }

  function handleSaveNotes(value: string, id: number) {
    const revision = remember(id, 'notes', value);
    return saveFields(id, { notes: value }, { notes: revision }).then(
      (saved) => {
        if (!saved) return;
        showSnackBar(t('notes_save_success'), 'success');
        if (!drafts.current.get(id)?.notes) setNotes((current) => new Map(current).set(id, false));
      }
    );
  }

  async function reviewTask(id: number, discard: boolean) {
    const issue = issuesRef.current.get(id);
    if (!issue || issue.reviewing) return;
    const draft = drafts.current.get(id);
    const reviewing = { ...issue, reviewing: true };
    setTaskIssue(id, reviewing);
    try {
      const fetched = await api.get<Task>(`tasks/${id}`);
      if (issuesRef.current.get(id) !== reviewing) return;
      ingest(fetched);
      const server = canonical.current.get(id);
      if (drafts.current.get(id) !== draft
        || (!discard && (!issue.server || issue.server.version !== server.version))) {
        setTaskIssue(id, { server });
        renderDrafts();
        return; // The input or server changed during review; ask again before replacing either.
      }
      saves.resume(server);
      setTaskIssue(id);
      if (discard) {
        drafts.current.delete(id);
        renderDrafts();
      } else if (draft) {
        const fields: TaskFields = {};
        const revisions: Partial<Record<keyof TaskDraft, number>> = {};
        if (draft.value) { fields.value = draft.value.value; revisions.value = draft.value.revision; }
        if (draft.notes) { fields.notes = draft.notes.value; revisions.notes = draft.notes.revision; }
        await saveFields(id, fields, revisions);
      }
    } catch {
      if (issuesRef.current.get(id) === reviewing) setTaskIssue(id, issue);
      showSnackBar(t('task_update_failure'), 'error');
    }
  }

  function handleSelectImages(id: number) {
    setCurrentTask(tasks.find((task) => task.id === id));
    setOpenSelectImages(true);
  }

  const onImageUploadSuccess = () => {
    setOpenSelectImages(false);
    showSnackBar(t('images_add_task_success'), 'success');
  };
  const onImageUploadFailure = (err) =>
    showSnackBar(t('images_add_task_failure'), 'error');

  const fields: Array<IField> = [
    {
      name: 'images',
      type: 'file',
      label: t('images'),
      fileType: 'image',
      multiple: true
    }
  ];
  const shape = {
    images: Yup.array().required(t('required_images'))
  };
  const renderSelectImages = () => {
    return (
      <Dialog
        fullWidth
        maxWidth="sm"
        open={openSelectImages}
        onClose={() => setOpenSelectImages(false)}
      >
        <DialogTitle
          sx={{
            p: 3
          }}
        >
          <Typography variant="h4" gutterBottom>
            {t('add_images')}
          </Typography>
        </DialogTitle>
        <DialogContent
          dividers
          sx={{
            p: 3
          }}
        >
          <Form
            fields={fields}
            validation={Yup.object().shape(shape)}
            submitText={t('add')}
            values={{}}
            onChange={({ field, e }) => {
            }}
            onSubmit={async (values) => {
              return dispatch(addFiles(values.images, 'IMAGE', currentTask.id))
                .then(onImageUploadSuccess)
                .then(() =>
                  dispatch(getTasksByWorkOrder(workOrderId)).then(() => {
                    const newNotes = new Map(notes);
                    newNotes.set(currentTask.id, true);
                    setNotes(newNotes);
                  })
                )
                .catch(onImageUploadFailure);
            }}
          />
        </DialogContent>
      </Dialog>
    );
  };
  return (
    <Card>
      <CardHeader title={t('tasks')} avatar={<TaskAltTwoToneIcon />} />
      <Divider />
      <CardContent>
        <FormControl fullWidth>
          {tasks.map((task) => (
            <Box key={task.id}>
            <SingleTask
              disabled={disabled}
              task={task}
              handleChange={handleChange}
              onValueInput={(value, id) => remember(id, 'value', value)}
              handleNoteChange={handleNoteChange}
              handleSaveNotes={handleSaveNotes}
              toggleNotes={toggleNotes}
              handleSelectImages={handleSelectImages}
              handleZoomImage={handleZoomImage}
              notes={notes}
            />
              {issues.has(task.id) && (
                <Alert severity="warning" sx={{ mb: 2 }}>
                  {t('task_save_review')}
                  {issues.get(task.id).server && (
                    <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', my: 1 }}>
                      {t('task_server_value')}: {issues.get(task.id).server.value ?? '—'}
                      {'\n'}{t('notes')}: {issues.get(task.id).server.notes || '—'}
                    </Typography>
                  )}
                  <Button disabled={issues.get(task.id).reviewing} onClick={() => void reviewTask(task.id, false)}>{t('task_review_submit')}</Button>
                  <Button disabled={issues.get(task.id).reviewing} onClick={() => void reviewTask(task.id, true)}>{t('task_review_discard')}</Button>
                </Alert>
              )}
            </Box>
          ))}
        </FormControl>
      </CardContent>
      {renderSelectImages()}
    </Card>
  );
}
