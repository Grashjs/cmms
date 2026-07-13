import { Helmet } from 'react-helmet-async';
import {
  Box,
  Button,
  Card,
  CircularProgress,
  debounce,
  Dialog,
  DialogContent,
  DialogTitle,
  Divider,
  Drawer,
  Grid,
  IconButton,
  Menu,
  MenuItem,
  Stack,
  Tab,
  Tabs,
  Tooltip,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { IField } from '../type';
import WorkOrder from '../../../models/owns/workOrder';
import * as React from 'react';
import {
  ChangeEvent,
  useCallback,
  useContext,
  useEffect,
  useLayoutEffect,
  useMemo,
  useState
} from 'react';
import { TitleContext } from '../../../contexts/TitleContext';
import { GridEnrichedColDef } from '@mui/x-data-grid/models/colDef/gridColDef';
import CustomDataGrid, {
  CustomDatagridColumn
} from '../components/CustomDatagrid';
import {
  GridRenderCellParams,
  GridToolbar,
  GridValueGetterParams
} from '@mui/x-data-grid';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import Form from '../components/form';
import UserAvatars from '../components/UserAvatars';
import * as Yup from 'yup';
import { isNumeric } from '../../../utils/validators';
import { UserMiniDTO } from '../../../models/user';
import WorkOrderDetails from './Details/WorkOrderDetails';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { LocationMiniDTO } from '../../../models/owns/location';
import { AssetMiniDTO, assetStatuses } from '../../../models/owns/asset';
import { formatSelect, formatSelectMultiple } from '../../../utils/formatters';
import {
  addWorkOrder,
  clearSingleWorkOrder,
  deleteWorkOrder,
  editWorkOrder,
  getSingleWorkOrder,
  getWorkOrders
} from '../../../slices/workOrder';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import { useDispatch, useSelector } from '../../../store';
import PriorityWrapper from '../components/PriorityWrapper';
import { patchTasksOfWorkOrder } from '../../../slices/task';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import useAuth from '../../../hooks/useAuth';
import { getWOBaseValues } from '../../../utils/woBase';
import { PermissionEntity } from '../../../models/owns/role';
import PermissionErrorMessage from '../components/PermissionErrorMessage';
import ConfirmDialog from '../components/ConfirmDialog';
import NoRowsMessageWrapper from '../components/NoRowsMessageWrapper';
import { getImageAndFiles, onSearchQueryChange } from '../../../utils/overall';
import { getSingleLocation } from '../../../slices/location';
import { getSingleAsset } from '../../../slices/asset';
import Category from '../../../models/owns/category';
import File from '../../../models/owns/file';
import { dayDiff } from '../../../utils/dates';
import {
  FilterField,
  SearchCriteria,
  SortDirection
} from '../../../models/owns/page';
import WorkOrderCalendar from './Calendar';
import MoreVertTwoToneIcon from '@mui/icons-material/MoreVertTwoTone';
import { exportEntity } from '../../../slices/exports';
import FilterAltTwoToneIcon from '@mui/icons-material/FilterAltTwoTone';
import MoreFilters from './Filters/MoreFilters';
import EnumFilter from './Filters/EnumFilter';
import SignalCellularAltTwoToneIcon from '@mui/icons-material/SignalCellularAltTwoTone';
import CircleTwoToneIcon from '@mui/icons-material/CircleTwoTone';
import _ from 'lodash';
import SearchInput from '../components/SearchInput';
import { PlanFeature } from '../../../models/owns/subscriptionPlan';
import { getPreventiveMaintenanceUrl } from 'src/utils/urlPaths';
import { useGridApiRef } from '@mui/x-data-grid-pro';
import useGridStatePersist from '../../../hooks/useGridStatePersist';

function WorkOrders() {
  const { t }: { t: any } = useTranslation();
  const [currentTab, setCurrentTab] = useState<string>('list');
  const { workOrders, loadingGet, singleWorkOrder } = useSelector(
    (state) => state.workOrders
  );
  const { loadingExport } = useSelector((state) => state.exports);
  const [searchParams, setSearchParams] = useSearchParams();
  const locationParam = searchParams.get('location');
  const viewParam = searchParams.get('view');
  const assetParam = searchParams.get('asset');
  const dispatch = useDispatch();
  const {
    hasViewPermission,
    hasViewOtherPermission,
    hasCreatePermission,
    hasFeature,
    user
  } = useAuth();
  const uiConfiguration = user.uiConfiguration;
  const { uploadFiles, getWOFieldsAndShapes } = useContext(
    CompanySettingsContext
  );
  const { getFormattedDate, getUserNameById } = useContext(
    CompanySettingsContext
  );
  const tabs = [
    { value: 'list', label: t('list_view'), disabled: false },
    {
      value: 'calendar',
      label: t('calendar_view'),
      disabled: !hasViewPermission(PermissionEntity.WORK_ORDERS)
    },
    { value: 'column', label: t('column_view'), disabled: true }
  ];
  const handleTabsChange = (_event: ChangeEvent<{}>, value: string): void => {
    setCurrentTab(value);
  };
  const [openAddModal, setOpenAddModal] = useState<boolean>(false);
  const [openUpdateModal, setOpenUpdateModal] = useState<boolean>(false);
  const [openDrawer, setOpenDrawer] = useState<boolean>(false);
  const [openFilterDrawer, setOpenFilterDrawer] = useState<boolean>(false);
  const { setTitle } = useContext(TitleContext);
  const { workOrderId } = useParams();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [columnOrder, setColumnOrder] = useState([]);

  const [currentWorkOrder, setCurrentWorkOrder] = useState<WorkOrder>();
  const [openDelete, setOpenDelete] = useState<boolean>(false);
  const { tasksByWorkOrder } = useSelector((state) => state.tasks);
  const { locations } = useSelector((state) => state.locations);
  const { assetInfos } = useSelector((state) => state.assets);
  const [initialDueDate, setInitialDueDate] = useState<Date>(null);
  const locationParamObject = locations.find(
    (location) => location.id === Number(locationParam)
  );
  const assetParamObject = assetInfos[assetParam]?.asset;
  const tasks = tasksByWorkOrder[currentWorkOrder?.id] ?? [];
  const [openDrawerFromUrl, setOpenDrawerFromUrl] = useState<boolean>(false);
  const [openDrawerForSingleWO, setOpenDrawerForSingleWO] =
    useState<boolean>(false);
  const initialCriteria: SearchCriteria = {
    filterFields: [
      {
        field: 'priority',
        operation: 'in',
        values: ['NONE', 'LOW', 'MEDIUM', 'HIGH'],
        value: '',
        enumName: 'PRIORITY'
      },
      {
        field: 'status',
        operation: 'in',
        values: ['OPEN', 'IN_PROGRESS', 'ON_HOLD'],
        value: '',
        enumName: 'STATUS'
      },
      {
        field: 'archived',
        operation: 'eq',
        value: false
      }
    ],
    pageSize: 10,
    pageNum: 0,
    direction: 'DESC'
  };
  const [criteria, setCriteria] = useState<SearchCriteria>({
    ...initialCriteria,
    sortField: 'updatedAt',
    direction: 'DESC'
  });
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const openMenu = Boolean(anchorEl);
  const navigate = useNavigate();
  const handleOpenMenu = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };
  const handleCloseMenu = () => {
    setAnchorEl(null);
  };
  const handleDelete = (id: number) => {
    dispatch(deleteWorkOrder(id)).then(onDeleteSuccess).catch(onDeleteFailure);
    setOpenDelete(false);
  };
  const handleOpenUpdate = (id: number) => {
    // important if there were actions like edit
    if (currentWorkOrder.id !== id) {
      setCurrentWorkOrder(
        workOrders.content.find((workOrder) => workOrder.id === id)
      );
    }
    setOpenUpdateModal(true);
  };
  const handleOpenDelete = (id: number) => {
    if (currentWorkOrder.id !== id) {
      setCurrentWorkOrder(
        workOrders.content.find((workOrder) => workOrder.id === id)
      );
    }
    setOpenDelete(true);
    setOpenDrawer(false);
  };
  const handleOpenDrawer = (workOrder: WorkOrder) => {
    setCurrentWorkOrder(workOrder);
    window.history.replaceState(
      null,
      'WorkOrder details',
      `/app/work-orders/${workOrder.id}`
    );
    setOpenDrawer(true);
  };

  const handleOpenDetails = (id: number) => {
    const foundWorkOrder = workOrders.content.find(
      (workOrder) => workOrder.id === id
    );
    if (foundWorkOrder) {
      handleOpenDrawer(foundWorkOrder);
    } else {
      setOpenDrawerFromUrl(false);
      setOpenDrawerForSingleWO(true);
      dispatch(getSingleWorkOrder(id));
    }
  };
  const handleCloseDetails = () => {
    window.history.replaceState(null, 'WorkOrder', `/app/work-orders`);
    setOpenDrawer(false);
    setOpenDrawerForSingleWO(false);
  };
  const handleCloseFilterDrawer = () => setOpenFilterDrawer(false);
  useEffect(() => {
    setTitle(t('work_orders'));
  }, []);

  const onFilterChange = (newFilters: FilterField[]) => {
    const newCriteria = { ...criteria };
    newCriteria.filterFields = newFilters;
    setCriteria(newCriteria);
  };
  useEffect(() => {
    if (workOrderId && isNumeric(workOrderId)) {
      setOpenDrawerForSingleWO(true);
      dispatch(getSingleWorkOrder(Number(workOrderId)));
    }
  }, [workOrderId]);

  //see changes in ui on edit
  useEffect(() => {
    if (singleWorkOrder || workOrders.content.length) {
      const currentInContent = workOrders.content.find(
        (workOrder) => workOrder.id === currentWorkOrder?.id
      );
      const updatedWorkOrder = openDrawerForSingleWO
        ? singleWorkOrder ?? currentInContent
        : currentInContent;
      if (updatedWorkOrder) {
        if (openDrawerFromUrl) {
          setCurrentWorkOrder(updatedWorkOrder);
        } else {
          handleOpenDrawer(updatedWorkOrder);
          setOpenDrawerFromUrl(true);
        }
      }
    }
  }, [singleWorkOrder, workOrders.content]);

  useEffect(() => {
    if (locationParam || assetParam) {
      if (locationParam && isNumeric(locationParam)) {
        dispatch(getSingleLocation(Number(locationParam)));
      }
      if (assetParam && isNumeric(assetParam)) {
        dispatch(getSingleAsset(Number(assetParam)));
      }
    }
    if (viewParam === 'calendar') {
      setCurrentTab('calendar');
    }
  }, []);

  useEffect(() => {
    let shouldOpen1 = locationParam && locationParamObject;
    let shouldOpen2 = assetParam && assetParamObject;
    if (shouldOpen1 || shouldOpen2) {
      setOpenAddModal(true);
    }
  }, [locationParamObject, assetParamObject]);

  const formatValues = (values) => {
    const newValues = { ...values };
    newValues.assetStatus = newValues.assetStatus?.value ?? null;
    newValues.primaryUser = formatSelect(newValues.primaryUser);
    newValues.location = formatSelect(newValues.location);
    newValues.team = formatSelect(newValues.team);
    newValues.asset = formatSelect(newValues.asset);
    newValues.assignedTo = formatSelectMultiple(newValues.assignedTo);
    newValues.customers = formatSelectMultiple(newValues.customers);
    newValues.priority = newValues.priority ? newValues.priority.value : 'NONE';
    newValues.requiredSignature = Array.isArray(newValues.requiredSignature)
      ? newValues?.requiredSignature.includes('on')
      : newValues.requiredSignature;
    newValues.category = formatSelect(newValues.category);
    return newValues;
  };
  const onCreationSuccess = () => {
    setOpenAddModal(false);
    showSnackBar(t('wo_create_success'), 'success');
  };
  const onCreationFailure = (err) =>
    showSnackBar(t('wo_create_failure'), 'error');
  const onEditSuccess = () => {
    setOpenUpdateModal(false);
    showSnackBar(t('changes_saved_success'), 'success');
  };
  const onEditFailure = (err) => showSnackBar(t('wo_update_failure'), 'error');
  const onDeleteSuccess = () => {
    showSnackBar(t('wo_delete_success'), 'success');
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('wo_delete_failure'), 'error');

  const onPageSizeChange = (size: number) => {
    setCriteria({ ...criteria, pageSize: size });
  };
  const onPageChange = (number: number) => {
    setCriteria({ ...criteria, pageNum: number });
  };
  const onQueryChange = (event) => {
    onSearchQueryChange<WorkOrder>(event, criteria, setCriteria, [
      'title',
      'description',
      'feedback'
    ]);
  };
  const debouncedQueryChange = useMemo(() => debounce(onQueryChange, 1300), []);
  useEffect(() => {
    dispatch(getWorkOrders(criteria));
  }, [criteria]);

  const columns: CustomDatagridColumn[] = [
    {
      field: 'customId',
      headerName: t('id'),
      description: t('id')
    },
    {
      field: 'status',
      headerName: t('status'),
      description: t('status'),
      width: 150,
      renderCell: (params: GridRenderCellParams<string>) => (
        <Box display="flex" flexDirection="row" justifyContent="center">
          <CircleTwoToneIcon
            fontSize="small"
            color={
              params.value === 'IN_PROGRESS'
                ? 'success'
                : params.value === 'ON_HOLD'
                ? 'warning'
                : params.value === 'COMPLETE'
                ? 'info'
                : 'secondary'
            }
          />
          <Typography sx={{ ml: 1 }}>{t(params.value)}</Typography>
        </Box>
      )
    },
    {
      field: 'title',
      headerName: t('title'),
      description: t('title'),
      width: 150,
      renderCell: (params: GridRenderCellParams<string>) => (
        <Box sx={{ fontWeight: 'bold' }}>{params.value}</Box>
      )
    },

    {
      field: 'priority',
      headerName: t('priority'),
      description: t('priority'),
      width: 150,
      renderCell: (params: GridRenderCellParams<string>) => (
        <PriorityWrapper priority={params.value} />
      )
    },
    {
      field: 'description',
      headerName: t('description'),
      description: t('description'),
      width: 300
    },
    {
      field: 'primaryUser',
      headerName: t('worker'),
      description: t('worker'),
      width: 170,
      renderCell: (params: GridRenderCellParams<UserMiniDTO>) =>
        params.value ? <UserAvatars users={[params.value]} /> : null
    },
    {
      field: 'assignedTo',
      headerName: t('assigned_to'),
      description: t('assigned_to'),
      width: 170,
      renderCell: (params: GridRenderCellParams<UserMiniDTO[]>) => (
        <UserAvatars users={params.value} />
      )
    },
    {
      field: 'location',
      headerName: t('location_name'),
      description: t('location_name'),
      width: 150,
      valueGetter: (params: GridValueGetterParams<LocationMiniDTO>) =>
        params.value?.name,
      uiConfigKey: 'locations'
    },
    {
      field: 'locationAddress',
      headerName: t('location_address'),
      description: t('location_address'),
      width: 150,
      valueGetter: (params: GridValueGetterParams<null, WorkOrder>) =>
        params.row.location?.address,
      uiConfigKey: 'locations'
    },
    {
      field: 'category',
      headerName: t('category'),
      description: t('category'),
      width: 150,
      valueGetter: (params: GridValueGetterParams<Category>) =>
        params.value?.name
    },
    {
      field: 'asset',
      headerName: t('asset_name'),
      description: t('asset_name'),
      width: 150,
      valueGetter: (params: GridValueGetterParams<AssetMiniDTO>) =>
        params.value?.name
    },
    {
      field: 'daysSinceCreated',
      headerName: t('days_since_creation'),
      description: t('days_since_creation'),
      width: 150,
      valueGetter: (params: GridValueGetterParams<null, WorkOrder>) =>
        dayDiff(new Date(), new Date(params.row.createdAt))
    },
    {
      field: 'files',
      headerName: t('files'),
      description: t('files'),
      width: 150,
      valueGetter: (params: GridValueGetterParams<File[]>) =>
        params.value.length
    },
    {
      field: 'requestedBy',
      headerName: t('requested_by'),
      description: t('requested_by'),
      width: 150,
      valueGetter: (params: GridValueGetterParams<null, WorkOrder>) =>
        getUserNameById(params.row.parentRequest?.createdBy)
    },
    {
      field: 'completedOn',
      headerName: t('completed_on'),
      description: t('completed_on'),
      width: 150,
      valueGetter: (params: GridValueGetterParams<string>) =>
        getFormattedDate(params.value)
    },
    {
      field: 'updatedAt',
      headerName: t('updated_at'),
      description: t('updated_at'),
      width: 150,
      valueGetter: (params: GridValueGetterParams<string>) =>
        getFormattedDate(params.value)
    },
    {
      field: 'createdAt',
      headerName: t('created_at'),
      description: t('created_at'),
      width: 150,
      valueGetter: (params: GridValueGetterParams<string>) =>
        getFormattedDate(params.value)
    }
  ];
  // dataGrid state
  const apiRef = useGridApiRef();
  useGridStatePersist(apiRef, columns, 'workOrder');

  // Mapping for column fields to API field names for sorting
  const fieldMapping: Record<string, string> = {
    customId: 'customId',
    status: 'status',
    title: 'title',
    priority: 'priority',
    description: 'description',
    primaryUser: 'primaryUser.firstName',
    assignedTo: 'assignedTo',
    location: 'location.name',
    category: 'category.name',
    asset: 'asset.name',
    daysSinceCreated: 'createdAt',
    files: 'files',
    completedOn: 'completedOn',
    updatedAt: 'updatedAt',
    createdAt: 'createdAt'
  };

  const defaultFields: Array<IField> = [
    {
      name: 'title',
      type: 'text',
      label: t('title'),
      placeholder: t('wo.title_description'),
      required: true
    },
    {
      name: 'description',
      type: 'text',
      label: t('description'),
      placeholder: t('description'),
      multiple: true
    },
    {
      name: 'image',
      type: 'file',
      fileType: 'image',
      label: t('image')
    },
    {
      name: 'dueDate',
      type: 'date',
      label: t('due_date')
    },
    {
      name: 'estimatedStartDate',
      type: 'date',
      label: t('estimated_start_date')
    },
    {
      name: 'estimatedDuration',
      type: 'number',
      label: t('estimated_duration'),
      placeholder: t('hours')
    },
    {
      name: 'priority',
      type: 'select',
      label: t('priority'),
      type2: 'priority'
    },
    {
      name: 'category',
      type: 'select',
      label: t('category'),
      type2: 'category',
      category: 'work-order-categories'
    },
    {
      name: 'primaryUser',
      type: 'select',
      label: t('primary_worker'),
      type2: 'user'
    },
    {
      name: 'assignedTo',
      type: 'select',
      label: t('additional_workers'),
      type2: 'user',
      multiple: true
    },
    {
      name: 'customers',
      type: 'select',
      label: t('customers'),
      type2: 'customer',
      multiple: true
    },
    {
      name: 'team',
      type: 'select',
      type2: 'team',
      label: t('team'),
      placeholder: t('select_team')
    },
    {
      name: 'location',
      type: 'select',
      type2: 'location',
      label: t('location'),
      placeholder: t('select_location')
    },
    {
      name: 'asset',
      type: 'select',
      type2: 'asset',
      label: t('asset'),
      placeholder: t('select_asset'),
      relatedFields: [{ field: 'location' }]
    },
    {
      name: 'assetStatus',
      type: 'select',
      label: t('asset_status'),
      placeholder: t('select_asset_status'),
      items: assetStatuses.map((assetStatus) => ({
        label: t(assetStatus.status),
        value: assetStatus.status
      }))
    },
    {
      name: 'tasks',
      type: 'select',
      type2: 'task',
      label: t('tasks'),
      placeholder: t('select_tasks')
    },
    {
      name: 'files',
      type: 'file',
      multiple: true,
      label: t('files'),
      fileType: 'file'
    },
    {
      name: 'requiredSignature',
      type: 'switch',
      label: t('requires_signature')
    }
  ];
  const defaultShape: { [key: string]: any } = {
    title: Yup.string().required(t('required_wo_title'))
  };
  const getFieldsAndShapes = (): [Array<IField>, { [key: string]: any }] => {
    return getWOFieldsAndShapes(defaultFields, defaultShape);
  };
  const renderWorkOrderAddModal = () => (
    <Dialog
      fullWidth
      maxWidth="md"
      open={openAddModal}
      onClose={() => setOpenAddModal(false)}
    >
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {t('add_wo')}
        </Typography>
        <Typography variant="subtitle2">{t('add_wo_description')}</Typography>
      </DialogTitle>
      <DialogContent
        dividers
        sx={{
          p: 3
        }}
      >
        <Box>
          <Form
            fields={getFieldsAndShapes()[0]}
            validation={Yup.object().shape(getFieldsAndShapes()[1])}
            submitText={t('add')}
            values={{
              requiredSignature: false,
              dueDate: initialDueDate,
              asset: assetParamObject
                ? { label: assetParamObject.name, value: assetParamObject.id }
                : null,
              location: locationParamObject
                ? {
                    label: locationParamObject.name,
                    value: locationParamObject.id
                  }
                : null
            }}
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              let formattedValues = formatValues(values);
              return new Promise<void>((resolve, rej) => {
                uploadFiles(formattedValues.files, formattedValues.image)
                  .then((files) => {
                    const imageAndFiles = getImageAndFiles(files);
                    formattedValues = {
                      ...formattedValues,
                      image: imageAndFiles.image,
                      files: imageAndFiles.files
                    };
                    dispatch(addWorkOrder(formattedValues))
                      .then(() => {
                        onCreationSuccess();
                        resolve();
                      })
                      .catch((err) => {
                        onCreationFailure(err);
                        rej();
                      });
                  })
                  .catch((err) => {
                    onCreationFailure(err);
                    rej();
                  });
              });
            }}
          />
        </Box>
      </DialogContent>
    </Dialog>
  );
  const renderWorkOrderUpdateModal = () => (
    <Dialog
      fullWidth
      maxWidth="md"
      open={openUpdateModal}
      onClose={() => setOpenUpdateModal(false)}
    >
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {t('Edit Work Order')}
        </Typography>
        <Typography variant="subtitle2">
          {t('Fill in the fields below to update the Work Order')}
        </Typography>
      </DialogTitle>
      <DialogContent
        dividers
        sx={{
          p: 3
        }}
      >
        <Box>
          <Form
            fields={getFieldsAndShapes()[0]}
            validation={Yup.object().shape(getFieldsAndShapes()[1])}
            submitText={t('save')}
            values={{
              ...currentWorkOrder,
              tasks,
              ...getWOBaseValues(t, currentWorkOrder)
            }}
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              let formattedValues = formatValues(values);
              return new Promise<void>((resolve, rej) => {
                //differentiate files from api and formattedValues
                const files = formattedValues.files.find((file) => file.id)
                  ? []
                  : formattedValues.files;
                uploadFiles(files, formattedValues.image)
                  .then((files) => {
                    const imageAndFiles = getImageAndFiles(
                      files,
                      currentWorkOrder.image
                    );
                    formattedValues = {
                      ...formattedValues,
                      image: imageAndFiles.image,
                      files: [...currentWorkOrder.files, ...imageAndFiles.files]
                    };
                    dispatch(
                      //TODO editTask
                      patchTasksOfWorkOrder(
                        currentWorkOrder?.id,
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
                        dispatch(
                          editWorkOrder(currentWorkOrder?.id, formattedValues)
                        )
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
        </Box>
      </DialogContent>
    </Dialog>
  );
  const renderMenu = () => (
    <Menu
      id="basic-menu"
      anchorEl={anchorEl}
      open={openMenu}
      onClose={handleCloseMenu}
      MenuListProps={{
        'aria-labelledby': 'basic-button'
      }}
    >
      {hasViewOtherPermission(PermissionEntity.WORK_ORDERS) && (
        <MenuItem
          disabled={loadingExport['work-orders']}
          onClick={() => {
            dispatch(exportEntity('work-orders')).then((url: string) => {
              window.open(url);
            });
          }}
        >
          <Stack spacing={2} direction="row">
            {loadingExport['work-orders'] && <CircularProgress size="1rem" />}
            <Typography>{t('to_export')}</Typography>
          </Stack>
        </MenuItem>
      )}
      {hasViewPermission(PermissionEntity.SETTINGS) && (
        <MenuItem
          onClick={() => navigate('/app/imports/work-orders')}
          disabled={!hasFeature(PlanFeature.IMPORT_CSV)}
        >
          {t('to_import')}
        </MenuItem>
      )}
    </Menu>
  );
  return (
    <>
      <Helmet>
        <title>{t('work_orders')}</title>
      </Helmet>
      <Grid
        container
        justifyContent="center"
        alignItems="stretch"
        spacing={1}
        paddingX={4}
      >
        <Grid
          item
          xs={12}
          display="flex"
          flexDirection="row"
          justifyContent="space-between"
          alignItems="center"
        >
          <Tabs
            onChange={handleTabsChange}
            value={currentTab}
            variant="scrollable"
            scrollButtons="auto"
            textColor="primary"
            indicatorColor="primary"
          >
            {tabs.map((tab) =>
              tab.disabled ? (
                <Tooltip title={t('Coming Soon')} placement="top">
                  <span>
                    <Tab
                      key={tab.value}
                      label={tab.label}
                      value={tab.value}
                      disabled={tab.disabled}
                    />
                  </span>
                </Tooltip>
              ) : (
                <Tab key={tab.value} label={tab.label} value={tab.value} />
              )
            )}
          </Tabs>
          <Stack direction={'row'} alignItems="center" spacing={1}>
            <IconButton onClick={handleOpenMenu} color="primary">
              <MoreVertTwoToneIcon />
            </IconButton>
            {hasCreatePermission(PermissionEntity.WORK_ORDERS) && (
              <Button
                onClick={() => setOpenAddModal(true)}
                startIcon={<AddTwoToneIcon />}
                sx={{ mx: 6, my: 1 }}
                variant="contained"
              >
                {t('work_order')}
              </Button>
            )}
          </Stack>
        </Grid>
        <Grid item xs={12}>
          <Card
            sx={{
              p: 2,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center'
            }}
          >
            {currentTab !== 'calendar' && (
              <Stack
                sx={{ ml: 1 }}
                direction="row"
                spacing={1}
                justifyContent={'flex-start'}
                width={'95%'}
              >
                <Button
                  onClick={() => setOpenFilterDrawer(true)}
                  sx={{
                    '& .MuiButton-startIcon': { margin: '0px' },
                    minWidth: 0
                  }}
                  variant={
                    _.isEqual(
                      criteria.filterFields,
                      initialCriteria.filterFields
                    )
                      ? 'outlined'
                      : 'contained'
                  }
                  startIcon={<FilterAltTwoToneIcon />}
                />
                <EnumFilter
                  filterFields={criteria.filterFields}
                  onChange={onFilterChange}
                  completeOptions={['NONE', 'LOW', 'MEDIUM', 'HIGH']}
                  fieldName="priority"
                  icon={<SignalCellularAltTwoToneIcon />}
                />
                <EnumFilter
                  filterFields={criteria.filterFields}
                  onChange={onFilterChange}
                  completeOptions={[
                    'OPEN',
                    'IN_PROGRESS',
                    'ON_HOLD',
                    'COMPLETE'
                  ]}
                  fieldName="status"
                  icon={<CircleTwoToneIcon />}
                />
                <SearchInput onChange={debouncedQueryChange} />
              </Stack>
            )}
            <Divider sx={{ mt: 1 }} />
            <Box sx={{ width: '95%' }}>
              {currentTab === 'list' ? (
                <CustomDataGrid
                  apiRef={apiRef}
                  pageSize={criteria.pageSize}
                  page={criteria.pageNum}
                  columns={columns}
                  rows={workOrders.content}
                  rowCount={workOrders.totalElements}
                  loading={loadingGet}
                  pagination
                  disableColumnFilter
                  paginationMode="server"
                  sortingMode="server"
                  initialState={{
                    columns: {
                      columnVisibilityModel: {}
                    }
                  }}
                  onSortModelChange={(model) => {
                    if (model.length === 0) {
                      setCriteria({
                        ...criteria,
                        sortField: undefined,
                        direction: undefined
                      });
                      return;
                    }

                    const field = model[0].field;
                    const mappedField = fieldMapping[field];

                    // Only proceed if we have a mapping for this field
                    if (!mappedField) return;

                    setCriteria({
                      ...criteria,
                      sortField: mappedField,
                      direction: (model[0].sort?.toUpperCase() ||
                        'ASC') as SortDirection
                    });
                  }}
                  onPageSizeChange={onPageSizeChange}
                  onPageChange={onPageChange}
                  rowsPerPageOptions={[10, 20, 50]}
                  components={{
                    NoRowsOverlay: () => (
                      <NoRowsMessageWrapper
                        message={t('noRows.wo.message')}
                        action={t('noRows.wo.action')}
                      />
                    )
                  }}
                  onRowClick={(params) => handleOpenDetails(Number(params.id))}
                />
              ) : (
                <WorkOrderCalendar
                  handleAddWorkOrder={(date: Date) => {
                    setInitialDueDate(date);
                    setOpenAddModal(true);
                  }}
                  handleOpenDetails={(id, type) => {
                    if (type === 'WORK_ORDER') handleOpenDetails(id);
                    else navigate(getPreventiveMaintenanceUrl(id));
                  }}
                />
              )}
            </Box>
          </Card>
        </Grid>
      </Grid>
      {renderWorkOrderAddModal()}
      {renderWorkOrderUpdateModal()}
      <Drawer
        anchor="right"
        open={openDrawer}
        onClose={handleCloseDetails}
        PaperProps={{
          sx: { width: '50%' }
        }}
      >
        <WorkOrderDetails
          workOrder={currentWorkOrder}
          onEdit={handleOpenUpdate}
          tasks={tasks}
          onDelete={handleOpenDelete}
        />
      </Drawer>
      <Drawer
        anchor="left"
        open={openFilterDrawer}
        onClose={handleCloseFilterDrawer}
        PaperProps={{
          sx: { width: '30%' }
        }}
      >
        <MoreFilters
          filterFields={criteria.filterFields}
          onFilterChange={onFilterChange}
          onClose={handleCloseFilterDrawer}
        />
      </Drawer>
      <ConfirmDialog
        open={openDelete}
        onCancel={() => {
          setOpenDelete(false);
          setOpenDrawer(true);
        }}
        onConfirm={() => handleDelete(currentWorkOrder?.id)}
        confirmText={t('to_delete')}
        question={t('confirm_delete_wo')}
      />
      {renderMenu()}
    </>
  );
}

export default WorkOrders;
