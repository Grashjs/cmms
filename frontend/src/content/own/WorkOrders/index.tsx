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
import {
  getCustomFieldsIFields,
  getCustomFieldsRequiredShape,
  IField
} from '../type';
import WorkOrder from '../../../models/owns/workOrder';
import * as React from 'react';
import { ChangeEvent, useContext, useEffect, useMemo, useState } from 'react';
import { TitleContext } from '../../../contexts/TitleContext';
import CustomDatagrid2, {
  CustomDatagridColumn2
} from '../components/CustomDatagrid2';
import { createColumnHelper } from '@tanstack/react-table';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import Form from '../components/form';
import UserAvatars from '../components/UserAvatars';
import * as Yup from 'yup';
import { isNumeric } from '../../../utils/validators';
import WorkOrderDetails from './Details/WorkOrderDetails';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { formatSelect, formatSelectMultiple, formatCustomFields } from '../../../utils/formatters';
import {
  addWorkOrder,
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
import ConfirmDialog from '../components/ConfirmDialog';
import {
  fireGa4Event,
  getImageAndFiles,
  handleFileUpload,
  onSearchQueryChange
} from '../../../utils/overall';
import { getSingleLocation } from '../../../slices/location';
import { getSingleAsset } from '../../../slices/asset';
import { dayDiff } from '../../../utils/dates';
import { FilterField, SearchCriteria } from '../../../models/owns/page';
import WorkOrderCalendar from './Calendar';
import MoreVertTwoToneIcon from '@mui/icons-material/MoreVertTwoTone';
import FilterAltTwoToneIcon from '@mui/icons-material/FilterAltTwoTone';
import MoreFilters from './Filters/MoreFilters';
import EnumFilter from './Filters/EnumFilter';
import SignalCellularAltTwoToneIcon from '@mui/icons-material/SignalCellularAltTwoTone';
import CircleTwoToneIcon from '@mui/icons-material/CircleTwoTone';
import _ from 'lodash';
import SearchInput from '../components/SearchInput';
import { PlanFeature } from '../../../models/owns/subscriptionPlan';
import { getPreventiveMaintenanceUrl } from 'src/utils/urlPaths';
import { getErrorMessage } from '../../../utils/api';
import SplitButton from '../components/SplitButton';
import useTableState from '../../../hooks/useTableState';
import { assetStatuses } from '../../../models/owns/asset';
import { useExport } from '../../../hooks/useExport';
import { getCustomFields } from '../../../slices/customField';
import { CustomFieldEntityType } from '../../../models/owns/customField';

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
  createdAt: 'createdAt',
  dueDate: 'dueDate'
};
function WorkOrders() {
  const { t }: { t: any } = useTranslation();
  const [currentTab, setCurrentTab] = useState<string>('list');
  const { workOrders, loadingGet, singleWorkOrder } = useSelector(
    (state) => state.workOrders
  );
  const { exportEntity, loadingExport } = useExport();
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

  // Use the table state hook for TanStack Table
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
  const {
    sorting,
    setSorting,
    pagination,
    setPagination,
    columnOrder,
    setColumnOrder,
    columnSizing,
    setColumnSizing,
    columnVisibility,
    setColumnVisibility,
    pinnedColumns,
    setPinnedColumns
  } = useTableState({
    prefix: 'workOrder',
    initialSorting: [{ id: 'updatedAt', desc: true }],
    initialPagination: {
      pageSize: initialCriteria.pageSize,
      pageIndex: initialCriteria.pageNum
    },
    setCriteria,
    fieldMapping
  });
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const openMenu = Boolean(anchorEl);
  const navigate = useNavigate();
  const { customFields } = useSelector((state) => state.customFields);
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
    const newParam = searchParams.get('new');

    if (newParam === 'true') {
      setOpenAddModal(true);

      const newParams = new URLSearchParams(searchParams);
      newParams.delete('new');

      setSearchParams(newParams);
    }
  }, [searchParams]);

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
    return formatCustomFields(newValues);
  };
  const onCreationSuccess = () => {
    setOpenAddModal(false);
    showSnackBar(t('wo_create_success'), 'success');
  };
  const onCreationFailure = (err) =>
    showSnackBar(getErrorMessage(err, t('wo_create_failure')), 'error');
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

  const onQueryChange = (event) => {
    onSearchQueryChange<WorkOrder>(event, criteria, setCriteria, [
      'title',
      'description',
      'feedback',
      'customId'
    ]);
  };
  const debouncedQueryChange = useMemo(() => debounce(onQueryChange, 1300), []);
  useEffect(() => {
    dispatch(getWorkOrders(criteria));
  }, [criteria]);

  useEffect(() => {
    if ((openAddModal || openUpdateModal) && !customFields.length) {
      dispatch(getCustomFields());
    }
  }, [openAddModal, openUpdateModal]);

  const columnHelper = createColumnHelper<WorkOrder>();

  const columns: CustomDatagridColumn2<WorkOrder>[] = [
    columnHelper.accessor('customId', {
      id: 'customId',
      header: () => t('id'),
      cell: (info) => info.getValue(),
      size: 80
    }),
    columnHelper.accessor('status', {
      id: 'status',
      header: () => t('status'),
      cell: (info) => (
        <Box display="flex" flexDirection="row">
          <CircleTwoToneIcon
            fontSize="small"
            color={
              info.getValue() === 'IN_PROGRESS'
                ? 'success'
                : info.getValue() === 'ON_HOLD'
                ? 'warning'
                : info.getValue() === 'COMPLETE'
                ? 'info'
                : 'secondary'
            }
          />
          <Typography sx={{ ml: 1 }}>{t(info.getValue())}</Typography>
        </Box>
      ),
      size: 150
    }),
    columnHelper.accessor('title', {
      id: 'title',
      header: () => t('title'),
      cell: (info) => <Box sx={{ fontWeight: 'bold' }}>{info.getValue()}</Box>,
      size: 150
    }),
    columnHelper.accessor('priority', {
      id: 'priority',
      header: () => t('priority'),
      cell: (info) => <PriorityWrapper priority={info.getValue()} />,
      size: 120
    }),
    columnHelper.accessor('description', {
      id: 'description',
      header: () => t('description'),
      cell: (info) => info.getValue() || '',
      size: 300
    }),
    columnHelper.accessor('primaryUser', {
      id: 'primaryUser',
      header: () => t('worker'),
      cell: (info) =>
        info.getValue() ? <UserAvatars users={[info.getValue()]} /> : null,
      size: 170
    }),
    columnHelper.accessor('assignedTo', {
      id: 'assignedTo',
      header: () => t('assigned_to'),
      cell: (info) => <UserAvatars users={info.getValue()} />,
      size: 170
    }),
    columnHelper.accessor((row) => row.location?.name, {
      id: 'location',
      header: () => t('location_name'),
      cell: (info) => info.getValue() || '',
      meta: {
        uiConfigKey: 'locations'
      },
      size: 150
    }),
    columnHelper.accessor((row) => row.location?.address, {
      id: 'locationAddress',
      header: () => t('location_address'),
      cell: (info) => info.getValue() || '',
      meta: {
        uiConfigKey: 'locations'
      },
      size: 150
    }),
    columnHelper.accessor((row) => row.category?.name, {
      id: 'category',
      header: () => t('category'),
      cell: (info) => info.getValue() || '',
      size: 150
    }),
    columnHelper.accessor((row) => row.asset?.name, {
      id: 'asset',
      header: () => t('asset_name'),
      cell: (info) => info.getValue() || '',
      size: 150
    }),
    columnHelper.accessor('dueDate', {
      id: 'dueDate',
      header: () => t('due_date'),
      cell: (info) => getFormattedDate(info.getValue()),
      size: 150
    }),
    columnHelper.accessor(
      (row) => dayDiff(new Date(), new Date(row.createdAt)),
      {
        id: 'daysSinceCreated',
        header: () => t('days_since_creation'),
        cell: (info) => info.getValue(),
        size: 150
      }
    ),
    columnHelper.accessor('files', {
      id: 'files',
      header: () => t('files'),
      cell: (info) => info.getValue()?.length ?? 0,
      size: 80
    }),
    columnHelper.accessor(
      (row) => getUserNameById(row.parentRequest?.createdBy),
      {
        id: 'requestedBy',
        header: () => t('requested_by'),
        cell: (info) => info.getValue() || '',
        size: 150
      }
    ),
    columnHelper.accessor('completedOn', {
      id: 'completedOn',
      header: () => t('completed_on'),
      cell: (info) => getFormattedDate(info.getValue()),
      size: 140
    }),
    columnHelper.accessor('updatedAt', {
      id: 'updatedAt',
      header: () => t('updated_at'),
      cell: (info) => getFormattedDate(info.getValue()),
      size: 140
    }),
    columnHelper.accessor('createdAt', {
      id: 'createdAt',
      header: () => t('created_at'),
      cell: (info) => getFormattedDate(info.getValue()),
      size: 140
    })
  ];

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
    },
    ...getCustomFieldsIFields(customFields, CustomFieldEntityType.WORK_ORDER)
  ];
  const defaultShape: { [key: string]: any } = {
    title: Yup.string().required(t('required_wo_title')),
    ...getCustomFieldsRequiredShape(
      customFields,
      CustomFieldEntityType.WORK_ORDER,
      t
    )
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
              if (workOrders.totalElements === 0)
                fireGa4Event('first_wo_creation');
              let formattedValues = formatValues(values);
              try {
                const uploadedFiles = await uploadFiles(
                  formattedValues.files,
                  formattedValues.image
                );

                const imageAndFiles = getImageAndFiles(uploadedFiles);
                formattedValues = {
                  ...formattedValues,
                  image: imageAndFiles.image,
                  files: imageAndFiles.files
                };

                await dispatch(addWorkOrder(formattedValues));
                onCreationSuccess();
              } catch (err) {
                onCreationFailure(err);
                throw err;
              }
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

              try {
                const imageAndFiles = await handleFileUpload(
                  {
                    files: formattedValues.files,
                    image: formattedValues.image
                  },
                  uploadFiles
                );

                formattedValues = {
                  ...formattedValues,
                  image: imageAndFiles.image,
                  files: imageAndFiles.files
                };

                await dispatch(
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
                );

                await dispatch(
                  editWorkOrder(currentWorkOrder?.id, formattedValues)
                );

                await onEditSuccess();
              } catch (err) {
                onEditFailure(err);
                throw err; // Re-throw to maintain the rejection behavior
              }
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
          onClick={async () => {
            try {
              await exportEntity('work-orders');
            } catch (error) {
              showSnackBar(t('Export failed'), 'error');
            }
            handleCloseMenu();
          }}
        >
          <Stack spacing={2} direction="row">
            {loadingExport['work-orders'] && <CircularProgress size="1rem" />}
            <Typography>{t('to_export')}</Typography>
          </Stack>
        </MenuItem>
      )}
    </Menu>
  );
  return (
    <>
      <Helmet>
        <title>{t('work_orders')}</title>
      </Helmet>
      <Box justifyContent="center" alignItems="stretch" paddingX={4}>
        <Box
          my={1}
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
              <SplitButton
                onMainClick={() => setOpenAddModal(true)}
                startIcon={<AddTwoToneIcon />}
                sx={{ mx: 6, my: 1 }}
                label={t('work_order')}
                menuItems={
                  hasViewPermission(PermissionEntity.SETTINGS) &&
                  hasFeature(PlanFeature.IMPORT_CSV)
                    ? [
                        {
                          label: t('to_import'),
                          onClick: () => navigate('/app/imports/work-orders')
                        }
                      ]
                    : []
                }
              />
            )}
          </Stack>
        </Box>
        <Card
          sx={{
            py: 2,
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
                  _.isEqual(criteria.filterFields, initialCriteria.filterFields)
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
                completeOptions={['OPEN', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETE']}
                fieldName="status"
                icon={<CircleTwoToneIcon />}
              />
              <SearchInput onChange={debouncedQueryChange} />
            </Stack>
          )}
          <Divider sx={{ mt: 1 }} />
          <Box sx={{ width: '95%' }}>
            {currentTab === 'list' ? (
              <CustomDatagrid2
                columns={columns}
                data={workOrders.content}
                loading={loadingGet}
                pagination={pagination}
                onPaginationChange={setPagination}
                totalRows={workOrders.totalElements}
                pageSizeOptions={[10, 20, 50]}
                sorting={sorting}
                onSortingChange={setSorting}
                columnOrder={columnOrder}
                onColumnOrderChange={setColumnOrder}
                columnSizing={columnSizing}
                onColumnSizingChange={setColumnSizing}
                columnVisibility={columnVisibility}
                onColumnVisibilityChange={setColumnVisibility}
                onRowClick={(row) => handleOpenDetails(row.id)}
                noRowsMessage={t('noRows.wo.message')}
                noRowsAction={t('noRows.wo.action')}
                enableColumnReordering
                enableColumnResizing
                pinnedColumns={pinnedColumns}
                onPinnedColumnsChange={setPinnedColumns}
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
      </Box>
      {renderWorkOrderAddModal()}
      {renderWorkOrderUpdateModal()}
      <Drawer
        anchor="right"
        open={openDrawer}
        onClose={handleCloseDetails}
        PaperProps={{
          sx: { width: { xs: '90%', sm: '70%', md: '50%' } }
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
