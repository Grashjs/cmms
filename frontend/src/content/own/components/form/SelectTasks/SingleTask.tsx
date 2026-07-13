import {
  Box,
  Button,
  CircularProgress,
  Collapse,
  Grid,
  IconButton,
  Link,
  MenuItem,
  Select,
  TextField,
  Tooltip,
  Typography,
  useTheme
} from '@mui/material';
import Field from '../Field';
import NoteTwoToneIcon from '@mui/icons-material/NoteTwoTone';
import AttachFileTwoToneIcon from '@mui/icons-material/AttachFileTwoTone';
import SpeedTwoToneIcon from '@mui/icons-material/SpeedTwoTone';
import ArrowDropDownCircleTwoToneIcon from '@mui/icons-material/ArrowDropDownCircleTwoTone';
import { Task, TaskOption, TaskType } from '../../../../../models/owns/tasks';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useMemo, useState } from 'react';
import debounce from 'lodash.debounce';
import { getAssetUrl, getUserUrl } from '../../../../../utils/urlPaths';
import useAuth from '../../../../../hooks/useAuth';
import { PermissionEntity } from '../../../../../models/owns/role';
import { PlanFeature } from '../../../../../models/owns/subscriptionPlan';

interface SingleTaskProps {
  task: Task;
  preview?: boolean;
  disabled?: boolean;
  handleChange?: (value: string | number, id: number) => void;
  handleSaveNotes?: (value: string, id: number) => Promise<void>;
  handleNoteChange?: (value: string, id: number) => void;
  handleSelectImages?: (id: number) => void;
  handleZoomImage?: (images: string[], image: string) => void;
  toggleNotes?: (id: number) => void;
  notes?: Map<number, boolean>;
}

export default function SingleTask({
  task,
  handleChange,
  handleNoteChange,
  handleSaveNotes,
  preview,
  toggleNotes,
  notes,
  handleSelectImages,
  handleZoomImage,
  disabled
}: SingleTaskProps) {
  const theme = useTheme();
  const { t }: { t: any } = useTranslation();
  const navigate = useNavigate();
  const [savingNotes, setSavingNotes] = useState<boolean>(false);
  const { user, hasCreatePermission, hasFeature } = useAuth();

  const changeHandler = (event) =>
    !preview && handleChange(event.target.value, task.id);

  const debouncedChangeHandler = useMemo(
    () => debounce(changeHandler, 1500),
    []
  );

  const subtaskOptions = [
    { label: t('OPEN'), value: 'OPEN' },
    { label: t('IN_PROGRESS'), value: 'IN_PROGRESS' },
    { label: t('ON_HOLD'), value: 'ON_HOLD' },
    { label: t('COMPLETE'), value: 'COMPLETE' }
  ];
  const inspectionOptions = [
    { label: t('PASS'), value: 'PASS' },
    { label: t('FLAG'), value: 'FLAG' },
    { label: t('FAIL'), value: 'FAIL' }
  ];

  const getOptions = (type: TaskType, options: TaskOption[]) => {
    switch (type) {
      case 'SUBTASK':
        return subtaskOptions;
      case 'INSPECTION':
        return inspectionOptions;
      case 'MULTIPLE':
        return options
          .map((option) => option.label)
          .map((option) => {
            return {
              label: option,
              value: option
            };
          });
      default:
        break;
    }
  };
  return (
    <Box
      key={task.id}
      sx={{
        mt: 1,
        p: 2,
        backgroundColor: theme.colors.alpha.black[5]
      }}
    >
      <Box display="flex" flexDirection="row" justifyContent="space-between">
        <Box>
          <Typography variant="h6" fontWeight="bold">
            {task.taskBase.label || `<${t('enter_task_name')}>`}
          </Typography>
          {['SUBTASK', 'INSPECTION', 'MULTIPLE'].includes(
            task.taskBase.taskType
          ) ? (
            <Select
              value={
                preview
                  ? getOptions(
                      task.taskBase.taskType,
                      task.taskBase.options
                    )?.[0]?.value
                  : task?.value
              }
              onChange={(event) =>
                !preview && handleChange(event.target.value, task.id)
              }
              sx={{ backgroundColor: 'white' }}
              disabled={
                (task.taskBase.user && task.taskBase.user.id !== user.id) ||
                disabled
              }
            >
              {getOptions(task.taskBase.taskType, task.taskBase.options).map(
                (option) => (
                  <MenuItem key={option.value} value={option.value}>
                    {option.label}
                  </MenuItem>
                )
              )}
            </Select>
          ) : (
            <Box sx={{ backgroundColor: 'white' }}>
              <TextField
                onChange={debouncedChangeHandler}
                defaultValue={task.value}
                label={t('value')}
                disabled={
                  (task.taskBase.user && task.taskBase.user.id !== user.id) ||
                  disabled
                }
                type={
                  task.taskBase.taskType === 'METER'
                    ? 'number'
                    : (task.taskBase.taskType as 'number' | 'text')
                }
              />
            </Box>
          )}
        </Box>
        <Box>
          <Tooltip arrow placement="top" title={t('see_details')}>
            <IconButton onClick={() => !preview && toggleNotes(task.id)}>
              <ArrowDropDownCircleTwoToneIcon />
            </IconButton>
          </Tooltip>
          {task.taskBase.taskType === 'METER' && (
            <IconButton
              onClick={() =>
                !preview && navigate(`/app/meters/${task.taskBase.meter.id}`)
              }
            >
              <SpeedTwoToneIcon color="primary" />
            </IconButton>
          )}
          <Tooltip arrow placement="top" title={t('add_notes')}>
            <IconButton onClick={() => !preview && toggleNotes(task.id)}>
              <NoteTwoToneIcon color="primary" />
            </IconButton>
          </Tooltip>
          <Tooltip
            arrow
            placement="top"
            title={t(
              hasCreatePermission(PermissionEntity.FILES) &&
                hasFeature(PlanFeature.FILE)
                ? 'Attach Images'
                : 'Upgrade to attach Images'
            )}
          >
            <span>
              <IconButton
                onClick={() => handleSelectImages(task.id)}
                disabled={
                  preview ||
                  disabled ||
                  !(
                    hasCreatePermission(PermissionEntity.FILES) &&
                    hasFeature(PlanFeature.FILE)
                  )
                }
              >
                <AttachFileTwoToneIcon color="primary" />
              </IconButton>
            </span>
          </Tooltip>
        </Box>
      </Box>
      {task.taskBase.asset && (
        <Box
          display="flex"
          flexDirection="row"
          justifyContent="space-between"
          sx={{ mt: 1 }}
        >
          <Typography variant="h6" fontWeight="bold">
            {t('concerned_asset')}
          </Typography>
          <Link variant="h6" href={getAssetUrl(task.taskBase.asset.id)}>
            {task.taskBase.asset.name}
          </Link>
        </Box>
      )}
      {task.taskBase.user && (
        <Box
          display="flex"
          flexDirection="row"
          justifyContent="space-between"
          sx={{ mt: 1 }}
        >
          <Typography variant="h6" fontWeight="bold">
            {t('assigned_to')}
          </Typography>
          {task.taskBase.user.id === user.id ? (
            <Typography variant="h6">{t('me')}</Typography>
          ) : (
            <Link variant="h6" href={getUserUrl(task.taskBase.user.id)}>
              {`${task.taskBase.user.firstName} ${task.taskBase.user.lastName}`}
            </Link>
          )}
        </Box>
      )}
      <Collapse sx={{ mt: 2 }} in={preview ? false : notes.get(task.id)}>
        <Box sx={{ p: 1, backgroundColor: 'white' }}>
          <Field
            multiple={true}
            onChange={(event) =>
              !preview && handleNoteChange(event.target.value, task.id)
            }
            value={task.notes}
            label={t('notes')}
            type={'text'}
          />
          <Button
            sx={{ mt: 1 }}
            variant="contained"
            startIcon={savingNotes ? <CircularProgress size="1rem" /> : null}
            disabled={savingNotes || disabled}
            onClick={() => {
              setSavingNotes(true);
              handleSaveNotes(task.notes, task.id).finally(() =>
                setSavingNotes(false)
              );
            }}
          >
            {t('save')}
          </Button>
        </Box>
        <Grid container spacing={1} sx={{ mt: 2 }}>
          {task.images.map((image) => (
            <Grid item key={image.id}>
              <img
                src={image.url}
                alt={'task'}
                onClick={() =>
                  handleZoomImage(
                    task.images.map((img) => img.url),
                    image.url
                  )
                }
                style={{ borderRadius: 5, height: 150, cursor: 'pointer' }}
              />
            </Grid>
          ))}
        </Grid>
      </Collapse>
    </Box>
  );
}
