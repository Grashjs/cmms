import {
  Box,
  Button,
  CircularProgress,
  debounce,
  Divider,
  Grid,
  IconButton,
  Link,
  List,
  ListItem,
  ListItemText,
  ListSubheader,
  Menu,
  MenuItem,
  Select,
  Stack,
  styled,
  Tab,
  Tabs,
  TextField,
  Typography,
  useTheme
} from '@mui/material';
import WorkOrder from '../../../../models/owns/workOrder';
import {
  ChangeEvent,
  Fragment,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react';
import { useTranslation } from 'react-i18next';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import AddTimeModal from './AddTimeModal';
import AddCostModal from './AddCostModal';
import Tasks from './Tasks';
import LinkTwoToneIcon from '@mui/icons-material/LinkTwoTone';
import ArchiveTwoToneIcon from '@mui/icons-material/ArchiveTwoTone';
import PictureAsPdfTwoToneIcon from '@mui/icons-material/PictureAsPdfTwoTone';
import PriorityWrapper from '../../components/PriorityWrapper';
import TimerTwoToneIcon from '@mui/icons-material/TimerTwoTone';
import {
  changeWorkOrderStatus,
  editWorkOrder,
  getPDFReport,
  removeFileFromWorkOrder
} from '../../../../slices/workOrder';
import { useDispatch, useSelector } from '../../../../store';
import MoreVertTwoToneIcon from '@mui/icons-material/MoreVertTwoTone';
import SelectParts from '../../components/form/SelectParts';
import ImageViewer from 'react-simple-image-viewer';
import {
  editPartQuantity,
  editWOPartQuantities,
  getPartQuantitiesByWorkOrder
} from '../../../../slices/partQuantity';
import Labor from '../../../../models/owns/labor';
import {
  controlTimer,
  deleteLabor,
  editLabor,
  getLabors
} from '../../../../slices/labor';
import {
  durationToHours,
  getHoursAndMinutesAndSeconds
} from '../../../../utils/formatters';
import {
  deleteAdditionalCost,
  getAdditionalCosts
} from '../../../../slices/additionalCost';
import { getTasksByWorkOrder } from '../../../../slices/task';
import { Task } from '../../../../models/owns/tasks';
import { getWorkOrderHistories } from '../../../../slices/workOrderHistory';
import LinkModal from './LinkModal';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { deleteRelation, getRelations } from '../../../../slices/relation';
import Relation, { relationTypes } from '../../../../models/owns/relation';
import { CompanySettingsContext } from '../../../../contexts/CompanySettingsContext';
import {
  getAssetUrl,
  getPreventiveMaintenanceUrl,
  getUserUrl
} from '../../../../utils/urlPaths';
import CompleteWOModal from './CompleteWOModal';
import useAuth from '../../../../hooks/useAuth';
import { PermissionEntity } from '../../../../models/owns/role';
import { getSingleUserMini } from '../../../../slices/user';
import FilesList from '../../components/FilesList';
import { PlanFeature } from '../../../../models/owns/subscriptionPlan';
import PartQuantitiesList from '../../components/PartQuantitiesList';
import AddFileModal from './AddFileModal';

const LabelWrapper = styled(Box)(
  ({ theme }) => `
    font-size: ${theme.typography.pxToRem(10)};
    font-weight: bold;
    text-transform: uppercase;
    border-radius: ${theme.general.borderRadiusSm};
    padding: ${theme.spacing(0.9, 1.5, 0.7)};
    line-height: 1;
  `
);

interface WorkOrderDetailsProps {
  workOrder: WorkOrder;
  onEdit: (id: number) => void;
  onDelete: (id: number) => void;
  tasks: Task[];
}

export default function WorkOrderDetails(props: WorkOrderDetailsProps) {
  const { workOrder, onEdit, tasks, onDelete } = props;
  const theme = useTheme();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { getFormattedDate, getUserNameById, getFormattedCurrency } =
    useContext(CompanySettingsContext);
  const { t }: { t: any } = useTranslation();
  const { user, hasEditPermission, hasDeletePermission } = useAuth();

  const [openAddTimeModal, setOpenAddTimeModal] = useState<boolean>(false);
  const [openAddFileModal, setOpenAddFileModal] = useState<boolean>(false);
  const [openAddCostModal, setOpenAddCostModal] = useState<boolean>(false);
  const [openLinkModal, setOpenLinkModal] = useState<boolean>(false);
  const [openCompleteModal, setOpenCompleteModal] = useState<boolean>(false);
  const [currentTab, setCurrentTab] = useState<string>('details');
  const [changingStatus, setChangingStatus] = useState<boolean>(false);
  const { partQuantitiesByWorkOrder, loadingPartQuantities } = useSelector(
    (state) => state.partQuantities
  );
  const partQuantities = partQuantitiesByWorkOrder[workOrder.id] ?? [];
  const [controllingTime, setControllingTime] = useState<boolean>(false);
  const { timesByWorkOrder, loadingLabors } = useSelector(
    (state) => state.labors
  );
  const { workOrderHistories } = useSelector(
    (state) => state.workOrderHistories
  );
  const { relationsByWorkOrder, loadingRelations } = useSelector(
    (state) => state.relations
  );
  const currentWorkOrderHistories = workOrderHistories[workOrder.id] ?? [];
  const currentWorkOrderRelations = relationsByWorkOrder[workOrder.id] ?? [];
  const labors = timesByWorkOrder[workOrder.id] ?? [];
  const primaryTime = labors.find(
    (labor) => labor.logged && labor.assignedTo.id === user.id
  );
  const runningTimer = primaryTime?.status === 'RUNNING';
  const { costsByWorkOrder, loadingCosts } = useSelector(
    (state) => state.additionalCosts
  );
  const additionalCosts = costsByWorkOrder[workOrder.id] ?? [];
  const dispatch = useDispatch();
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const openMenu = Boolean(anchorEl);
  const { companySettings, hasFeature } = useAuth();
  const { workOrderConfiguration, generalPreferences } = companySettings;
  const [generatingReport, setGeneratingReport] = useState<boolean>(false);
  const [openEditPrimaryTime, setOpenEditPrimaryTime] =
    useState<boolean>(false);
  const [primaryTimeHours, setPrimaryTimeHours] = useState<number>();
  const [primaryTimeMinutes, setPrimaryTimeMinutes] = useState<number>();
  const [savingPrimaryTime, setSavingPrimaryTime] = useState<boolean>(false);
  useEffect(() => {
    [workOrder.createdBy, workOrder.parentRequest?.createdBy].forEach(
      (createdBy) => {
        if (!usersMini.find((user) => user.id === createdBy) && createdBy) {
          dispatch(getSingleUserMini(createdBy));
        }
      }
    );
  }, []);

  const { usersMini } = useSelector((state) => state.users);
  const [isImageViewerOpen, setIsImageViewerOpen] = useState<boolean>(false);
  const [currentImage, setCurrentImage] = useState<string>();
  const [currentImages, setCurrentImages] = useState<string[]>();

  const handleOpenMenu = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };
  const handleCloseMenu = () => {
    setAnchorEl(null);
  };
  const onArchiveSuccess = () => {
    showSnackBar(t('wo_archive_success'), 'success');
  };
  const onArchiveFailure = (err) =>
    showSnackBar(t('wo_archive_failure'), 'error');
  const onArchive = () => {
    handleCloseMenu();
    if (window.confirm(t('wo_archive_confirm') + workOrder.title + ' ?')) {
      dispatch(editWorkOrder(workOrder?.id, { ...workOrder, archived: true }))
        .then(onArchiveSuccess)
        .catch(onArchiveFailure);
    }
  };
  const onGenerateReport = () => {
    setGeneratingReport(true);
    dispatch(getPDFReport(workOrder.id))
      .then((url: string) => {
        handleCloseMenu();
        window.open(url);
      })
      .finally(() => setGeneratingReport(false));
  };
  useEffect(() => {
    dispatch(getPartQuantitiesByWorkOrder(workOrder.id));
    dispatch(getLabors(workOrder.id));
    dispatch(getAdditionalCosts(workOrder.id));
    dispatch(getTasksByWorkOrder(workOrder.id));
    dispatch(getRelations(workOrder.id));
  }, []);
  useEffect(() => {
    const [hours, minutes] = getHoursAndMinutesAndSeconds(
      primaryTime?.duration
    );
    setPrimaryTimeHours(hours);
    setPrimaryTimeMinutes(minutes);
  }, [primaryTime]);

  const setImageState = (images: string[], image: string) => {
    setCurrentImage(image);
    setCurrentImages(images);
    setIsImageViewerOpen(true);
  };
  const canComplete = (): boolean => {
    let error;
    const fieldsToTest = [
      {
        name: 'completeFiles',
        condition: !workOrder.files.length,
        message: 'required_files_on_completion'
      },
      {
        name: 'completeTasks',
        condition: tasks.some((task) => !task.value),
        message: 'required_tasks_on_completion'
      },
      {
        name: 'completeTime',
        condition: labors
          .filter((labor) => labor.logged)
          .some((labor) => !labor.duration),
        message: 'required_labor_on_completion'
      },
      {
        name: 'completeParts',
        condition: !partQuantities.length,
        message: 'required_part_on_completion'
      },
      {
        name: 'completeCost',
        condition: !additionalCosts.length,
        message: 'required_cost_on_completion'
      }
    ];
    fieldsToTest.every((field) => {
      const fieldConfig =
        workOrderConfiguration.workOrderFieldConfigurations.find(
          (woFC) => woFC.fieldName === field.name
        );
      if (fieldConfig.fieldType === 'REQUIRED' && field.condition) {
        showSnackBar(t(field.message), 'error');
        error = true;
        return false;
      }
      return true;
    });

    return !error;
  };
  const isParent = (relation: Relation): boolean => {
    return relation.parent.id === workOrder.id;
  };
  const onPartQuantityChange = (value: string, partQuantity) => {
    dispatch(
      editPartQuantity(workOrder.id, partQuantity.id, Number(value), false)
    )
      .then(() => showSnackBar(t('quantity_change_success'), 'success'))
      .catch((err) => showSnackBar(t('quantity_change_failure'), 'error'));
  };
  const debouncedPartQuantityChange = useMemo(
    () => debounce(onPartQuantityChange, 1500),
    []
  );
  const onCompleteWO = (
    signatureId: number | undefined,
    feedback: string | undefined
  ) => {
    setChangingStatus(true);
    return dispatch(
      changeWorkOrderStatus(workOrder?.id, {
        status: 'COMPLETE',
        feedback: feedback ?? null,
        signature: signatureId ? { id: signatureId } : null
      })
    )
      .then(() => dispatch(getLabors(workOrder?.id)))
      .finally(() => setChangingStatus(false));
  };
  const groupRelations = (
    relations: Relation[]
  ): { [key: string]: { id: number; workOrder: WorkOrder }[] } => {
    const result = {};
    relationTypes.forEach((relationType) => {
      result[relationType] = [];
    });
    relations.forEach((relation) => {
      switch (relation.relationType) {
        case 'BLOCKS':
          if (isParent(relation)) {
            result['BLOCKS'].push({
              id: relation.id,
              workOrder: relation.child
            });
          } else
            result['BLOCKED_BY'].push({
              id: relation.id,
              workOrder: relation.parent
            });
          break;
        case 'DUPLICATE_OF':
          if (isParent(relation)) {
            result['DUPLICATE_OF'].push({
              id: relation.id,
              workOrder: relation.child
            });
          } else
            result['DUPLICATED_BY'].push({
              id: relation.id,
              workOrder: relation.parent
            });
          break;
        case 'RELATED_TO':
          result['RELATED_TO'].push({
            id: relation.id,
            workOrder: isParent(relation) ? relation.child : relation.parent
          });
          break;
        case 'SPLIT_FROM':
          if (isParent(relation)) {
            result['SPLIT_FROM'].push({
              id: relation.id,
              workOrder: relation.child
            });
          } else
            result['SPLIT_TO'].push({
              id: relation.id,
              workOrder: relation.parent
            });
          break;
        default:
          break;
      }
    });

    return result;
  };
  const getLaborCost = (labor: Labor): number => {
    const [hours, minutes] = getHoursAndMinutesAndSeconds(labor.duration);
    return Number((labor.hourlyRate * (hours + minutes / 60)).toFixed(2));
  };
  const workOrderStatuses = ['OPEN', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETE'];
  const tabs = [
    { value: 'details', label: t('details') },
    { value: 'updates', label: t('updates') }
  ];

  const getPath = (resource, id) => {
    switch (resource) {
      case 'asset':
        return getAssetUrl(id);
      case 'team':
        return `/app/people-teams/teams/${id}`;
      default:
        return `/app/${resource}s/${id}`;
    }
  };
  const onSavePrimaryTime = () => {
    setSavingPrimaryTime(true);
    const duration = primaryTimeHours * 3600 + primaryTimeMinutes * 60;
    dispatch(
      editLabor(primaryTime.id, workOrder.id, {
        ...primaryTime,
        duration
      })
    )
      .then(() => {
        setOpenEditPrimaryTime(false);
      })
      .finally(() => setSavingPrimaryTime(false));
  };
  const BasicField = ({
    label,
    value,
    id,
    type
  }: {
    label: string | number;
    value: string | number;
    type?: string;
    id?: number;
  }) => {
    if (value && (!type || (type && id))) {
      return (
        <Grid item xs={12} lg={6}>
          <Typography variant="h6" sx={{ color: theme.colors.alpha.black[70] }}>
            {label}
          </Typography>
          {type ? (
            <Link href={getPath(type, id)} variant="h6" fontWeight="bold">
              {value}
            </Link>
          ) : (
            <Typography variant="h6">{value}</Typography>
          )}
        </Grid>
      );
    } else return null;
  };
  const handleTabsChange = (_event: ChangeEvent<{}>, value: string): void => {
    setCurrentTab(value);
    if (value === 'updates' && !currentWorkOrderHistories.length)
      dispatch(getWorkOrderHistories(workOrder.id));
  };
  const detailsFieldsToRender = (
    workOrder: WorkOrder
  ): {
    label: string;
    value: string | number;
    type?: 'location' | 'asset' | 'team';
    id?: number;
  }[] => [
    {
      label: t('id'),
      value: workOrder.customId
    },
    {
      label: t('due_date'),
      value: getFormattedDate(workOrder.dueDate)
    },
    {
      label: t('estimated_start_date'),
      value: getFormattedDate(workOrder.estimatedStartDate)
    },
    {
      label: t('estimated_duration'),
      value: !!workOrder.estimatedDuration
        ? t('estimated_hours_in_text', { hours: workOrder.estimatedDuration })
        : null
    },
    {
      label: t('category'),
      value: workOrder.category?.name
    },
    {
      label: t('location'),
      value: workOrder.location?.name,
      type: 'location',
      id: workOrder.location?.id
    },
    {
      label: t('asset'),
      value: workOrder.asset?.name,
      type: 'asset',
      id: workOrder.asset?.id
    },
    {
      label: t('team'),
      value: workOrder.team?.name,
      type: 'team',
      id: workOrder.team?.id
    },
    {
      label: t('created_at'),
      value: getFormattedDate(workOrder.createdAt)
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
          <Box sx={{ mb: 2 }}>
            {workOrder?.priority !== 'NONE' && (
              <PriorityWrapper priority={workOrder?.priority} withSuffix />
            )}
          </Box>
          <Typography variant="h2">{workOrder?.title}</Typography>
          <Typography variant="h6">{workOrder?.description}</Typography>
        </Box>
        <Box>
          {hasEditPermission(PermissionEntity.WORK_ORDERS, workOrder) && (
            <IconButton style={{ marginRight: 10 }} onClick={handleOpenMenu}>
              <MoreVertTwoToneIcon />
            </IconButton>
          )}
          {hasEditPermission(PermissionEntity.WORK_ORDERS, workOrder) && (
            <IconButton
              onClick={() => onEdit(workOrder.id)}
              style={{ marginRight: 10 }}
            >
              <EditTwoToneIcon color="primary" />
            </IconButton>
          )}
          {hasDeletePermission(PermissionEntity.WORK_ORDERS, workOrder) && (
            <IconButton>
              <DeleteTwoToneIcon
                color="error"
                onClick={() => onDelete(workOrder.id)}
              />
            </IconButton>
          )}
        </Box>
      </Grid>
      <Divider />
      <Grid item xs={12}>
        <Tabs
          onChange={handleTabsChange}
          value={currentTab}
          variant="scrollable"
          scrollButtons="auto"
          textColor="primary"
          indicatorColor="primary"
        >
          {tabs.map((tab) => (
            <Tab key={tab.value} label={tab.label} value={tab.value} />
          ))}
        </Tabs>
      </Grid>
      <Grid item xs={12}>
        {currentTab === 'details' && (
          <Box>
            <Grid container spacing={2}>
              <Grid
                item
                xs={12}
                display="flex"
                flexDirection="row"
                justifyContent="space-between"
                alignItems="center"
              >
                <Box>
                  {changingStatus ? (
                    <CircularProgress />
                  ) : (
                    <Select
                      onChange={(event) => {
                        if (event.target.value === 'COMPLETE') {
                          if (canComplete()) {
                            if (
                              generalPreferences.askFeedBackOnWOClosed ||
                              workOrder.requiredSignature
                            ) {
                              let error;
                              if (workOrder.requiredSignature) {
                                if (!hasFeature(PlanFeature.SIGNATURE)) {
                                  error =
                                    'Signature on Work Order completion is not available in your current subscription plan.';
                                }
                              }
                              if (error) {
                                showSnackBar(t(error), 'error');
                              } else {
                                setOpenCompleteModal(true);
                                return;
                              }
                            }
                          } else return;
                        }
                        setChangingStatus(true);
                        dispatch(
                          changeWorkOrderStatus(workOrder?.id, {
                            status: event.target.value
                          })
                        ).finally(() => setChangingStatus(false));
                      }}
                      disabled={
                        !hasEditPermission(
                          PermissionEntity.WORK_ORDERS,
                          workOrder
                        )
                      }
                      value={workOrder.status}
                      sx={
                        workOrder.status === 'OPEN'
                          ? {}
                          : {
                              backgroundColor:
                                workOrder.status === 'IN_PROGRESS'
                                  ? theme.colors.success.main
                                  : workOrder.status === 'ON_HOLD'
                                  ? theme.colors.warning.main
                                  : theme.colors.alpha.black[30],
                              color: 'white',
                              fontWeight: 'bold',
                              '.MuiSvgIcon-root ': {
                                fill: 'white !important'
                              }
                            }
                      }
                    >
                      {workOrderStatuses.map((workOrderStatus, index) => (
                        <MenuItem key={index} value={workOrderStatus}>
                          {t(workOrderStatus)}
                        </MenuItem>
                      ))}
                    </Select>
                  )}
                </Box>
                <Box>
                  <Button
                    startIcon={
                      controllingTime ? (
                        <CircularProgress size="1rem" />
                      ) : (
                        <TimerTwoToneIcon />
                      )
                    }
                    disabled={
                      controllingTime ||
                      !hasEditPermission(
                        PermissionEntity.WORK_ORDERS,
                        workOrder
                      )
                    }
                    onClick={() => {
                      setControllingTime(true);
                      dispatch(
                        controlTimer(!runningTimer, workOrder.id)
                      ).finally(() => setControllingTime(false));
                    }}
                    variant={runningTimer ? 'contained' : 'outlined'}
                  >
                    {runningTimer
                      ? t('timer_running')
                      : t('run_timer') +
                        ' - ' +
                        durationToHours(primaryTime?.duration)}
                  </Button>
                </Box>
              </Grid>
              {workOrder.image && (
                <Grid
                  item
                  xs={12}
                  lg={12}
                  display="flex"
                  justifyContent="center"
                >
                  <img
                    src={workOrder.image.url}
                    style={{ borderRadius: 5, height: 250, cursor: 'pointer' }}
                    onClick={() => {
                      setImageState([workOrder.image.url], workOrder.image.url);
                    }}
                  />
                </Grid>
              )}
              {detailsFieldsToRender(workOrder).map((field, index) => (
                <BasicField
                  key={index}
                  label={field.label}
                  value={field.value}
                  type={field.type}
                  id={field.id}
                />
              ))}
              {workOrder.primaryUser && (
                <Grid item xs={12} lg={6}>
                  <Typography
                    variant="h6"
                    sx={{ color: theme.colors.alpha.black[70] }}
                  >
                    {t('primary_worker')}
                  </Typography>
                  <Link
                    variant="h6"
                    href={getUserUrl(workOrder.primaryUser.id)}
                  >
                    {getUserNameById(workOrder.primaryUser.id)}
                  </Link>
                </Grid>
              )}
              {(workOrder.parentRequest || workOrder.createdBy) && (
                <Grid item xs={12} lg={6}>
                  <Typography
                    variant="h6"
                    sx={{ color: theme.colors.alpha.black[70] }}
                  >
                    {workOrder.parentRequest
                      ? t('approved_by')
                      : t('created_by')}
                  </Typography>
                  <Link variant="h6" href={getUserUrl(workOrder.createdBy)}>
                    {getUserNameById(workOrder.createdBy)}
                  </Link>
                </Grid>
              )}
              {workOrder.parentPreventiveMaintenance && (
                <Grid item xs={12} lg={6}>
                  <Typography
                    variant="h6"
                    sx={{ color: theme.colors.alpha.black[70] }}
                  >
                    {t('preventive_maintenance')}
                  </Typography>
                  <Link
                    variant="h6"
                    href={getPreventiveMaintenanceUrl(
                      workOrder.parentPreventiveMaintenance.id
                    )}
                  >
                    {workOrder.parentPreventiveMaintenance.name}
                  </Link>
                </Grid>
              )}
              {workOrder.status === 'COMPLETE' && (
                <>
                  {workOrder.completedBy && (
                    <Grid item xs={12} lg={6}>
                      <Typography
                        variant="h6"
                        sx={{ color: theme.colors.alpha.black[70] }}
                      >
                        {t('completed_by')}
                      </Typography>
                      <Link
                        variant="h6"
                        href={getUserUrl(workOrder.completedBy.id)}
                      >
                        {`${workOrder.completedBy.firstName} ${workOrder.completedBy.lastName}`}
                      </Link>
                    </Grid>
                  )}
                  <Grid item xs={12} lg={6}>
                    <Typography
                      variant="h6"
                      sx={{ color: theme.colors.alpha.black[70] }}
                    >
                      {t('completed_on')}
                    </Typography>
                    <Typography variant="h6">
                      {getFormattedDate(workOrder.completedOn)}
                    </Typography>
                  </Grid>
                  {workOrder.feedback && (
                    <Grid item xs={12} lg={6}>
                      <Typography
                        variant="h6"
                        sx={{ color: theme.colors.alpha.black[70] }}
                      >
                        {t('feedback')}
                      </Typography>
                      <Typography variant="h6">{workOrder.feedback}</Typography>
                    </Grid>
                  )}
                  {workOrder.signature && (
                    <Grid item xs={12} lg={6}>
                      <Typography
                        variant="h6"
                        sx={{ color: theme.colors.alpha.black[70] }}
                      >
                        {t('signature')}
                      </Typography>
                      <img
                        src={workOrder.signature.url}
                        style={{
                          borderRadius: 5,
                          height: 100,
                          cursor: 'pointer'
                        }}
                        onClick={() => {
                          setImageState(
                            [workOrder.signature.url],
                            workOrder.signature.url
                          );
                        }}
                      />
                    </Grid>
                  )}
                </>
              )}
              {workOrder.parentRequest && (
                <Grid item xs={12} lg={6}>
                  <Typography
                    variant="h6"
                    sx={{ color: theme.colors.alpha.black[70] }}
                  >
                    {t('requested_by')}
                  </Typography>
                  <Link
                    variant="h6"
                    href={getUserUrl(workOrder.parentRequest.createdBy)}
                  >
                    {getUserNameById(workOrder.parentRequest.createdBy)}
                  </Link>
                </Grid>
              )}
              {primaryTime && (
                <Grid item xs={12} lg={6}>
                  <Typography
                    variant="h6"
                    sx={{ color: theme.colors.alpha.black[70] }}
                  >
                    {t('time')}
                  </Typography>
                  {openEditPrimaryTime ? (
                    <Stack direction="row" spacing={1} alignItems="center">
                      <TextField
                        value={primaryTimeHours}
                        type="number"
                        onChange={(event) =>
                          setPrimaryTimeHours(Number(event.target.value))
                        }
                      />
                      <Typography variant="h6">h</Typography>
                      <TextField
                        value={primaryTimeMinutes}
                        type="number"
                        InputProps={{ inputProps: { min: 0, max: 59 } }}
                        onChange={(event) =>
                          setPrimaryTimeMinutes(Number(event.target.value))
                        }
                      />
                      <Typography variant="h6">m</Typography>
                      <Button
                        startIcon={
                          savingPrimaryTime ? (
                            <CircularProgress size="1rem" />
                          ) : null
                        }
                        disabled={savingPrimaryTime}
                        variant="contained"
                        onClick={onSavePrimaryTime}
                      >
                        {t('save')}
                      </Button>
                    </Stack>
                  ) : (
                    <Typography
                      variant="h6"
                      style={{ cursor: 'pointer' }}
                      onClick={() => setOpenEditPrimaryTime(true)}
                      color="primary"
                    >
                      {durationToHours(primaryTime?.duration)}
                    </Typography>
                  )}
                </Grid>
              )}
              {!!workOrder.assignedTo.length && (
                <Grid item xs={12} lg={6}>
                  <Typography
                    variant="h6"
                    sx={{ color: theme.colors.alpha.black[70] }}
                  >
                    {t('assigned_to')}
                  </Typography>
                  {workOrder.assignedTo.map((user, index) => (
                    <Box key={user.id}>
                      <Link
                        href={getUserUrl(user.id)}
                        variant="h6"
                        fontWeight="bold"
                      >{`${user.firstName} ${user.lastName}`}</Link>
                    </Box>
                  ))}
                </Grid>
              )}
              {!!workOrder.customers.length && (
                <Grid item xs={12} lg={6}>
                  <Typography
                    variant="h6"
                    sx={{ color: theme.colors.alpha.black[70] }}
                  >
                    {t('customers')}
                  </Typography>
                  {workOrder.customers.map((customer, index) => (
                    <Box key={customer.id}>
                      <Link
                        href={`/app/vendors-customers/customers/${customer.id}`}
                        variant="h6"
                        fontWeight="bold"
                      >
                        {customer.name}
                      </Link>
                    </Box>
                  ))}
                </Grid>
              )}
            </Grid>
            {!!tasks.length && (
              <Box>
                <Divider sx={{ mt: 2 }} />
                <Tasks
                  tasksProps={tasks}
                  workOrderId={workOrder?.id}
                  handleZoomImage={setImageState}
                  disabled={
                    !hasEditPermission(PermissionEntity.WORK_ORDERS, workOrder)
                  }
                />
              </Box>
            )}
            <Box>
              <Divider sx={{ mt: 2 }} />
              <Typography sx={{ mt: 2, mb: 1 }} variant="h3">
                {t('labors')}
              </Typography>
              {loadingLabors[workOrder.id] ? (
                <Stack width={'100%'} alignItems="center">
                  <CircularProgress />
                </Stack>
              ) : !labors.filter((labor) => !labor.logged).length ? (
                <Typography sx={{ color: theme.colors.alpha.black[70] }}>
                  {t('no_labor')}
                </Typography>
              ) : (
                <List>
                  {labors
                    .filter((labor) => !labor.logged)
                    .map((labor) => (
                      <ListItem
                        key={labor.id}
                        secondaryAction={
                          <Box
                            sx={{
                              display: 'flex',
                              flexDirection: 'row',
                              alignItems: 'center',
                              justifyContent: 'flex-end'
                            }}
                          >
                            <Typography variant="h6">
                              {getFormattedCurrency(getLaborCost(labor))}
                            </Typography>
                            {hasEditPermission(
                              PermissionEntity.WORK_ORDERS,
                              workOrder
                            ) && (
                              <IconButton
                                sx={{ ml: 1 }}
                                onClick={() =>
                                  dispatch(deleteLabor(workOrder.id, labor.id))
                                }
                              >
                                <DeleteTwoToneIcon
                                  fontSize="small"
                                  color="error"
                                />
                              </IconButton>
                            )}
                          </Box>
                        }
                      >
                        <ListItemText
                          primary={
                            <>
                              {labor.assignedTo ? (
                                <Link
                                  href={getUserUrl(labor.assignedTo.id)}
                                  variant="h6"
                                >
                                  {`${labor.assignedTo.firstName} ${labor.assignedTo.lastName}`}
                                </Link>
                              ) : (
                                <Typography>{t('not_assigned')}</Typography>
                              )}
                            </>
                          }
                          secondary={`${
                            getHoursAndMinutesAndSeconds(labor.duration)[0]
                          }h ${
                            getHoursAndMinutesAndSeconds(labor.duration)[1]
                          }m`}
                        />
                      </ListItem>
                    ))}
                  <ListItem
                    secondaryAction={
                      <Box>
                        <Typography variant="h6" fontWeight="bold">
                          {getFormattedCurrency(
                            labors
                              .filter((labor) => !labor.logged)
                              .reduce(
                                (acc, labor) =>
                                  labor.includeToTotalTime
                                    ? acc + getLaborCost(labor)
                                    : acc,
                                0
                              )
                          )}
                        </Typography>
                      </Box>
                    }
                  >
                    <ListItemText
                      primary={
                        <Typography variant="h6" fontWeight="bold">
                          Total
                        </Typography>
                      }
                    />
                  </ListItem>
                </List>
              )}
              {hasEditPermission(PermissionEntity.WORK_ORDERS, workOrder) && (
                <Button
                  onClick={() => setOpenAddTimeModal(true)}
                  variant="outlined"
                  sx={{ mt: 1 }}
                >
                  {t('add_time')}
                </Button>
              )}
            </Box>
            <Box>
              <Divider sx={{ mt: 2 }} />
              <Typography sx={{ mt: 2, mb: 1 }} variant="h3">
                {t('additional_costs')}
              </Typography>
              {loadingCosts[workOrder.id] ? (
                <Stack width={'100%'} alignItems={'center'}>
                  <CircularProgress />
                </Stack>
              ) : (
                <Fragment>
                  {!additionalCosts.length ? (
                    <Typography sx={{ color: theme.colors.alpha.black[70] }}>
                      {t('no_additional_cost')}
                    </Typography>
                  ) : (
                    <List>
                      {additionalCosts.map((additionalCost) => (
                        <ListItem
                          key={additionalCost.id}
                          secondaryAction={
                            <Box
                              sx={{
                                display: 'flex',
                                flexDirection: 'row',
                                alignItems: 'center',
                                justifyContent: 'flex-end'
                              }}
                            >
                              <Typography variant="h6">
                                {getFormattedCurrency(additionalCost.cost)}
                              </Typography>
                              {hasEditPermission(
                                PermissionEntity.WORK_ORDERS,
                                workOrder
                              ) && (
                                <IconButton
                                  sx={{ ml: 1 }}
                                  onClick={() =>
                                    dispatch(
                                      deleteAdditionalCost(
                                        workOrder.id,
                                        additionalCost.id
                                      )
                                    )
                                  }
                                >
                                  <DeleteTwoToneIcon
                                    fontSize="small"
                                    color="error"
                                  />
                                </IconButton>
                              )}
                            </Box>
                          }
                        >
                          <ListItemText
                            primary={
                              <Typography variant="h6">
                                {additionalCost.description}
                              </Typography>
                            }
                            secondary={getFormattedDate(
                              additionalCost.createdAt
                            )}
                          />
                        </ListItem>
                      ))}
                      <ListItem
                        secondaryAction={
                          <Typography variant="h6" fontWeight="bold">
                            {getFormattedCurrency(
                              additionalCosts.reduce(
                                (acc, additionalCost) =>
                                  additionalCost.includeToTotalCost
                                    ? acc + additionalCost.cost
                                    : acc,
                                0
                              )
                            )}
                          </Typography>
                        }
                      >
                        <ListItemText
                          primary={
                            <Typography variant="h6" fontWeight="bold">
                              Total
                            </Typography>
                          }
                        />
                      </ListItem>
                    </List>
                  )}
                </Fragment>
              )}
              {hasEditPermission(PermissionEntity.WORK_ORDERS, workOrder) && (
                <Button
                  onClick={() => setOpenAddCostModal(true)}
                  variant="outlined"
                  sx={{ mt: 1 }}
                >
                  {t('add_additional_cost')}
                </Button>
              )}
            </Box>
            <Box>
              <Divider sx={{ mt: 2 }} />
              <Typography sx={{ mt: 2, mb: 1 }} variant="h3">
                {t('parts')}
              </Typography>
              {loadingPartQuantities[workOrder.id] ? (
                <Stack width={'100%'} alignItems={'center'}>
                  <CircularProgress />
                </Stack>
              ) : (
                <Fragment>
                  <PartQuantitiesList
                    partQuantities={partQuantities}
                    disabled={
                      !hasEditPermission(
                        PermissionEntity.WORK_ORDERS,
                        workOrder
                      )
                    }
                    onChange={debouncedPartQuantityChange}
                  />
                  {hasEditPermission(
                    PermissionEntity.WORK_ORDERS,
                    workOrder
                  ) && (
                    <SelectParts
                      selected={partQuantities.map(
                        (partQuantity) => partQuantity.part.id
                      )}
                      onChange={(selectedParts) => {
                        dispatch(
                          editWOPartQuantities(
                            workOrder.id,
                            selectedParts.map((part) => part.id)
                          )
                        ).catch((error) =>
                          showSnackBar(t('not_enough_part'), 'error')
                        );
                      }}
                    />
                  )}
                </Fragment>
              )}
            </Box>
            <Box>
              <Divider sx={{ mt: 2 }} />
              <Typography sx={{ mt: 2, mb: 1 }} variant="h3">
                {t('links')}
              </Typography>
              {loadingRelations[workOrder.id] ? (
                <Stack width={'100%'} alignItems={'center'}>
                  <CircularProgress />
                </Stack>
              ) : (
                <Fragment>
                  <List>
                    {Object.entries(
                      groupRelations(currentWorkOrderRelations)
                    ).map(
                      ([relationType, relations]) =>
                        !!relations.length && (
                          <Box key={relationType}>
                            <ListSubheader
                              sx={{ fontWeight: 'bold', fontSize: 20 }}
                            >
                              {t(relationType)}
                            </ListSubheader>
                            {relations.map((relation) => (
                              <ListItem
                                key={relation.id}
                                secondaryAction={
                                  <Box
                                    sx={{
                                      display: 'flex',
                                      flexDirection: 'row',
                                      alignItems: 'center',
                                      justifyContent: 'flex-end'
                                    }}
                                  >
                                    <IconButton
                                      sx={{ ml: 1 }}
                                      onClick={() =>
                                        dispatch(
                                          deleteRelation(
                                            workOrder.id,
                                            relation.id
                                          )
                                        )
                                      }
                                    >
                                      <DeleteTwoToneIcon
                                        fontSize="small"
                                        color="error"
                                      />
                                    </IconButton>
                                  </Box>
                                }
                              >
                                <ListItemText
                                  primary={
                                    <Stack direction={'row'} spacing={2}>
                                      <Typography variant="h6">
                                        {relation.workOrder.title}
                                      </Typography>
                                      <LabelWrapper
                                        sx={{
                                          background:
                                            relation.workOrder.status ===
                                            'IN_PROGRESS'
                                              ? theme.colors.success.main
                                              : relation.workOrder.status ===
                                                'ON_HOLD'
                                              ? theme.colors.warning.main
                                              : relation.workOrder.status ===
                                                'COMPLETE'
                                              ? theme.colors.primary.main
                                              : theme.colors.alpha.black[30],
                                          color: 'white'
                                        }}
                                      >
                                        {t(relation.workOrder.status)}
                                      </LabelWrapper>
                                    </Stack>
                                  }
                                  secondary={getFormattedDate(
                                    relation.workOrder.createdAt
                                  )}
                                />
                              </ListItem>
                            ))}
                          </Box>
                        )
                    )}
                  </List>
                  {hasEditPermission(
                    PermissionEntity.WORK_ORDERS,
                    workOrder
                  ) && (
                    <Button
                      onClick={() => setOpenLinkModal(true)}
                      variant="outlined"
                      sx={{ mt: 1 }}
                    >
                      {t('link_wo')}
                    </Button>
                  )}
                </Fragment>
              )}
            </Box>
            <Box>
              <Divider sx={{ mt: 2 }} />
              <Typography sx={{ mt: 2, mb: 1 }} variant="h3">
                {t('files')}
              </Typography>
              {!!workOrder.files.length ? (
                <FilesList
                  confirmMessage={t('confirm_delete_file_wo')}
                  removeDisabled={
                    !hasEditPermission(PermissionEntity.WORK_ORDERS, workOrder)
                  }
                  files={workOrder.files}
                  onRemove={(id: number) => {
                    dispatch(removeFileFromWorkOrder(workOrder.id, id));
                  }}
                />
              ) : (
                <Typography sx={{ color: theme.colors.alpha.black[70] }}>
                  {t('no_file_linked_to_wo')}
                </Typography>
              )}
              {hasEditPermission(PermissionEntity.WORK_ORDERS, workOrder) && (
                <Button
                  onClick={() => setOpenAddFileModal(true)}
                  variant="outlined"
                  sx={{ mt: 1 }}
                >
                  {t('add_file')}
                </Button>
              )}
            </Box>
          </Box>
        )}
        {currentTab == 'updates' && (
          <List>
            {[...currentWorkOrderHistories]
              .reverse()
              .map((workOrderHistory) => (
                <ListItem
                  key={workOrderHistory.id}
                  secondaryAction={getFormattedDate(workOrderHistory.createdAt)}
                >
                  <ListItemText
                    primary={`${workOrderHistory.user.firstName} ${workOrderHistory.user.lastName}`}
                    secondary={workOrderHistory.name}
                  />
                </ListItem>
              ))}
          </List>
        )}
      </Grid>
      <AddTimeModal
        open={openAddTimeModal}
        onClose={() => setOpenAddTimeModal(false)}
        workOrderId={workOrder.id}
      />
      <AddFileModal
        open={openAddFileModal}
        onClose={() => setOpenAddFileModal(false)}
        workOrderId={workOrder.id}
      />
      <AddCostModal
        open={openAddCostModal}
        onClose={() => setOpenAddCostModal(false)}
        workOrderId={workOrder.id}
      />
      <LinkModal
        open={openLinkModal}
        onClose={() => setOpenLinkModal(false)}
        workOrderId={workOrder.id}
      />
      {isImageViewerOpen && (
        <div style={{ zIndex: 100 }}>
          <ImageViewer
            src={currentImages}
            currentIndex={currentImages.findIndex(
              (image) => image === currentImage
            )}
            onClose={() => setIsImageViewerOpen(false)}
            disableScroll={true}
            backgroundStyle={{
              backgroundColor: 'rgba(0,0,0,0.9)'
            }}
            closeOnClickOutside={true}
          />
        </div>
      )}
      <CompleteWOModal
        open={openCompleteModal}
        onClose={() => setOpenCompleteModal(false)}
        fieldsConfig={{
          feedback: generalPreferences.askFeedBackOnWOClosed,
          signature: workOrder.requiredSignature
        }}
        onComplete={onCompleteWO}
      />
      <Menu anchorEl={anchorEl} open={openMenu} onClose={handleCloseMenu}>
        <MenuItem
          onClick={() => {
            setOpenLinkModal(true);
            handleCloseMenu();
          }}
        >
          <Stack spacing={2} direction="row">
            <LinkTwoToneIcon />
            <Typography variant="h6">{t('link')}</Typography>
          </Stack>
        </MenuItem>
        <MenuItem disabled={generatingReport} onClick={onGenerateReport}>
          <Stack spacing={2} direction="row">
            {generatingReport ? (
              <CircularProgress size="1rem" />
            ) : (
              <PictureAsPdfTwoToneIcon />
            )}
            <Typography variant="h6">{t('pdf_report')}</Typography>
          </Stack>
        </MenuItem>
        <MenuItem onClick={onArchive}>
          <Stack spacing={2} direction="row">
            <ArchiveTwoToneIcon />
            <Typography variant="h6">{t('archive')}</Typography>
          </Stack>
        </MenuItem>
      </Menu>
    </Grid>
  );
}
