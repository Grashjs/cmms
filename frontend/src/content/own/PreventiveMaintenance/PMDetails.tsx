import {
  Box,
  Divider,
  Grid,
  IconButton,
  Link,
  Typography,
  useTheme
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import PreventiveMaintenance from '../../../models/owns/preventiveMaintenance';
import { getPriorityLabel } from '../../../utils/formatters';
import { useDispatch } from '../../../store';
import { editPreventiveMaintenance } from '../../../slices/preventiveMaintenance';
import { useContext, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  getAssetUrl,
  getLocationUrl,
  getTeamUrl,
  getUserUrl
} from '../../../utils/urlPaths';
import useAuth from '../../../hooks/useAuth';
import { PermissionEntity } from '../../../models/owns/role';
import ImageViewer from 'react-simple-image-viewer';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import FilesList from '../components/FilesList';
import { getTasksByPreventiveMaintenance } from '../../../slices/task';
import Tasks from '../WorkOrders/Details/Tasks';
import { Task } from '../../../models/owns/tasks';
import { getScheduleDescription } from '../../../utils/dates';
import { supportedLanguages } from '../../../i18n/i18n';
import i18n from 'i18next';

interface RequestDetailsProps {
  preventiveMaintenance: PreventiveMaintenance;
  handleOpenUpdate: () => void;
  handleOpenDelete: () => void;
  onClose: () => void;
  tasks: Task[];
}

export default function PMDetails({
  preventiveMaintenance,
  handleOpenUpdate,
  handleOpenDelete,
  onClose,
  tasks
}: RequestDetailsProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const theme = useTheme();
  const getLanguage = i18n.language;
  const { hasEditPermission, hasDeletePermission, hasViewPermission } =
    useAuth();
  const navigate = useNavigate();
  const { getFormattedDate, getUserNameById } = useContext(
    CompanySettingsContext
  );
  const [isImageViewerOpen, setIsImageViewerOpen] = useState<boolean>(false);

  useEffect(() => {
    dispatch(getTasksByPreventiveMaintenance(preventiveMaintenance.id));
  }, []);

  const BasicField = ({
    label,
    value,
    isPriority
  }: {
    label: string | number;
    value: string | number;
    isPriority?: boolean;
  }) => {
    return value ? (
      <Grid item xs={12} lg={6}>
        <Typography variant="h6" sx={{ color: theme.colors.alpha.black[70] }}>
          {label}
        </Typography>
        <Typography variant="h6">
          {isPriority ? getPriorityLabel(value.toString(), t) : value}
        </Typography>
      </Grid>
    ) : null;
  };
  const fieldsToRender = (
    preventiveMaintenance: PreventiveMaintenance
  ): { label: string; value: string | number }[] => [
    {
      label: t('title'),
      value: preventiveMaintenance.title
    },
    {
      label: t('description'),
      value: preventiveMaintenance.description
    },
    {
      label: t('priority'),
      value: preventiveMaintenance.priority
    },
    {
      label: t('due_date_delay'),
      value:
        preventiveMaintenance.schedule.dueDateDelay === null
          ? null
          : t('days_count', {
              days: preventiveMaintenance.schedule.dueDateDelay
            })
    },
    {
      label: t('estimated_start_date'),
      value: getFormattedDate(preventiveMaintenance.estimatedStartDate)
    },
    {
      label: t('estimated_duration'),
      value: preventiveMaintenance.estimatedDuration
    },
    {
      label: t('category'),
      value: preventiveMaintenance?.category?.name
    }
  ];
  return (
    <Grid
      container
      justifyContent="center"
      alignItems="stretch"
      spacing={2}
      padding={4}
    >
      <Grid
        item
        xs={12}
        display="flex"
        flexDirection="row"
        justifyContent="space-between"
      >
        <Box>
          <Typography variant="h2">{preventiveMaintenance?.name}</Typography>
          {preventiveMaintenance?.schedule.disabled && (
            <Typography variant="h5">{t('paused')}</Typography>
          )}
        </Box>
        <Box>
          {hasEditPermission(
            PermissionEntity.PREVENTIVE_MAINTENANCES,
            preventiveMaintenance
          ) && (
            <IconButton style={{ marginRight: 10 }} onClick={handleOpenUpdate}>
              <EditTwoToneIcon color="primary" />
            </IconButton>
          )}
          {hasDeletePermission(
            PermissionEntity.PREVENTIVE_MAINTENANCES,
            preventiveMaintenance
          ) && (
            <IconButton onClick={handleOpenDelete}>
              <DeleteTwoToneIcon color="error" />
            </IconButton>
          )}
        </Box>
      </Grid>
      <Divider />
      <Grid item xs={12}>
        <Box>
          <Typography sx={{ mt: 2, mb: 1 }} variant="h4">
            {t('trigger_details')}
          </Typography>
          <Grid container spacing={2}>
            {preventiveMaintenance.schedule?.recurrenceBasedOn && (
              <Grid item xs={12} lg={6}>
                <Typography
                  variant="h6"
                  sx={{ color: theme.colors.alpha.black[70] }}
                >
                  {t('based_on')}
                </Typography>
                <Typography variant="h6">
                  {preventiveMaintenance.schedule.recurrenceBasedOn ===
                  'SCHEDULED_DATE'
                    ? t('scheduled_date')
                    : t('completed_on')}
                </Typography>
              </Grid>
            )}
            {preventiveMaintenance.schedule?.startsOn && (
              <Grid item xs={12} lg={6}>
                <Typography
                  variant="h6"
                  sx={{ color: theme.colors.alpha.black[70] }}
                >
                  {t('starts_on')}
                </Typography>
                <Typography variant="h6">
                  {getFormattedDate(preventiveMaintenance.schedule.startsOn)}
                </Typography>
              </Grid>
            )}
            {preventiveMaintenance.schedule?.endsOn && (
              <Grid item xs={12} lg={6}>
                <Typography
                  variant="h6"
                  sx={{ color: theme.colors.alpha.black[70] }}
                >
                  {t('ends_on')}
                </Typography>
                <Typography variant="h6">
                  {getFormattedDate(preventiveMaintenance.schedule.endsOn)}
                </Typography>
              </Grid>
            )}
            {preventiveMaintenance.schedule.frequency && (
              <Grid item xs={12} lg={6}>
                <Typography
                  variant="h6"
                  sx={{ color: theme.colors.alpha.black[70] }}
                >
                  {t('frequency')}
                </Typography>
                <Typography variant="h6">
                  {getScheduleDescription(
                    preventiveMaintenance.schedule,
                    supportedLanguages.find(({ code }) => code === getLanguage)
                      .dateLocale,
                    t
                  )}
                </Typography>
              </Grid>
            )}
            {preventiveMaintenance?.createdBy && (
              <Grid item xs={12} lg={6}>
                <Typography
                  variant="h6"
                  sx={{ color: theme.colors.alpha.black[70] }}
                >
                  {t('created_by')}
                </Typography>
                <Link
                  variant="h6"
                  href={getUserUrl(preventiveMaintenance.createdBy)}
                >
                  {getUserNameById(preventiveMaintenance.createdBy)}
                </Link>
              </Grid>
            )}
          </Grid>
        </Box>
        <Box>
          <Typography sx={{ mt: 2, mb: 1 }} variant="h4">
            {t('wo_details')}
          </Typography>
          <Grid container spacing={2}>
            <>
              {preventiveMaintenance.image && (
                <Grid
                  item
                  xs={12}
                  lg={12}
                  display="flex"
                  justifyContent="center"
                >
                  <img
                    src={preventiveMaintenance.image.url}
                    style={{ borderRadius: 5, height: 250, cursor: 'pointer' }}
                    onClick={() => setIsImageViewerOpen(true)}
                  />
                </Grid>
              )}
              {fieldsToRender(preventiveMaintenance).map((field) => (
                <BasicField
                  key={field.label}
                  label={field.label}
                  value={field.value}
                  isPriority={field.label === t('priority')}
                />
              ))}
              {preventiveMaintenance?.asset && (
                <Grid item xs={12} lg={6}>
                  <Typography
                    variant="h6"
                    sx={{ color: theme.colors.alpha.black[70] }}
                  >
                    {t('asset')}
                  </Typography>
                  <Link
                    variant="h6"
                    href={getAssetUrl(preventiveMaintenance.asset.id)}
                  >
                    {preventiveMaintenance.asset.name}
                  </Link>
                </Grid>
              )}
              {preventiveMaintenance?.location && (
                <Grid item xs={12} lg={6}>
                  <Typography
                    variant="h6"
                    sx={{ color: theme.colors.alpha.black[70] }}
                  >
                    {t('location')}
                  </Typography>
                  <Link
                    variant="h6"
                    href={getLocationUrl(preventiveMaintenance.location.id)}
                  >
                    {preventiveMaintenance.location.name}
                  </Link>
                </Grid>
              )}
              {preventiveMaintenance?.primaryUser && (
                <Grid item xs={12} lg={6}>
                  <Typography
                    variant="h6"
                    sx={{ color: theme.colors.alpha.black[70] }}
                  >
                    {t('assigned_to')}
                  </Typography>
                  <Link
                    variant="h6"
                    href={getUserUrl(preventiveMaintenance.primaryUser.id)}
                  >
                    {`${preventiveMaintenance.primaryUser.firstName} ${preventiveMaintenance.primaryUser.lastName}`}
                  </Link>
                </Grid>
              )}
              {preventiveMaintenance?.team && (
                <Grid item xs={12} lg={6}>
                  <Typography
                    variant="h6"
                    sx={{ color: theme.colors.alpha.black[70] }}
                  >
                    {t('team')}
                  </Typography>
                  <Link
                    variant="h6"
                    href={getTeamUrl(preventiveMaintenance.team.id)}
                  >
                    {preventiveMaintenance.team.name}
                  </Link>
                </Grid>
              )}
              {!!preventiveMaintenance.files.length && (
                <Grid item xs={12} lg={12}>
                  <Typography
                    variant="h6"
                    sx={{ color: theme.colors.alpha.black[70] }}
                  >
                    {t('files')}
                  </Typography>
                  <FilesList
                    confirmMessage={t('confirm_delete_file')}
                    removeDisabled={
                      !hasEditPermission(
                        PermissionEntity.PREVENTIVE_MAINTENANCES,
                        preventiveMaintenance
                      )
                    }
                    files={preventiveMaintenance.files}
                    onRemove={(id: number) => {
                      dispatch(
                        editPreventiveMaintenance(preventiveMaintenance.id, {
                          ...preventiveMaintenance,
                          files: preventiveMaintenance.files.filter(
                            (f) => f.id !== id
                          )
                        })
                      );
                    }}
                  />
                </Grid>
              )}
              {!!tasks.length && (
                <Grid item xs={12} lg={12}>
                  <Tasks
                    tasksProps={tasks}
                    workOrderId={null}
                    handleZoomImage={null}
                    disabled={true}
                  />
                </Grid>
              )}
            </>
          </Grid>
        </Box>
      </Grid>
      {isImageViewerOpen && (
        <div style={{ zIndex: 100 }}>
          <ImageViewer
            src={[preventiveMaintenance.image.url]}
            currentIndex={0}
            onClose={() => setIsImageViewerOpen(false)}
            disableScroll={true}
            backgroundStyle={{
              backgroundColor: 'rgba(0,0,0,0.9)'
            }}
            closeOnClickOutside={true}
          />
        </div>
      )}
    </Grid>
  );
}
