import {
  Box,
  Button,
  Card,
  CircularProgress,
  Grid,
  IconButton,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import SettingsLayout from '../SettingsLayout';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import { useContext, useEffect, useState } from 'react';
import { useDispatch, useSelector } from '../../../../store';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import ConfirmDialog from '../../components/ConfirmDialog';
import useAuth from '../../../../hooks/useAuth';
import { PlanFeature } from '../../../../models/owns/subscriptionPlan';
import {
  addWorkflow,
  deleteWorkflow,
  editWorkflow,
  getWorkflows
} from '../../../../slices/workflow';
import {
  mainConditions,
  partActions,
  partConditions,
  purchaseOrderActions,
  purchaseOrderConditions,
  requestActions,
  requestConditions,
  taskActions,
  taskConditions,
  WFMainCondition,
  Workflow,
  WorkflowAction,
  WorkflowActionType,
  WorkflowCondition,
  WorkflowConditionType,
  workOrderActions,
  workOrderConditions
} from '../../../../models/owns/workflow';
import { getVendorsMini } from '../../../../slices/vendor';
import { getUsersMini } from '../../../../slices/user';
import { getLocationsMini } from '../../../../slices/location';
import { getCategories } from '../../../../slices/category';
import { getAssetsMini } from '../../../../slices/asset';
import { getTeamsMini } from '../../../../slices/team';
import { AssetMiniDTO } from '../../../../models/owns/asset';
import { LocationMiniDTO } from '../../../../models/owns/location';
import { PartMiniDTO } from '../../../../models/owns/part';
import { getPartsMini } from '../../../../slices/part';
import { UserMiniDTO } from '../../../../models/user';
import { VendorMiniDTO } from '../../../../models/owns/vendor';
import { TeamMiniDTO } from '../../../../models/owns/team';
import Category from '../../../../models/owns/category';
import DateTimePicker from '@mui/lab/DateTimePicker';
import { LocalizationProvider } from '@mui/x-date-pickers-pro';
import { AdapterDayjs } from '@mui/x-date-pickers-pro/AdapterDayjs';
import { DateRangePicker } from '@mui/x-date-pickers-pro/DateRangePicker';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';

interface UICondition {
  type: WorkflowConditionType;
  value: string | number;
  values?: (string | number)[];
}
interface UIAction {
  type: WorkflowActionType;
  value: string | number;
  values?: (string | number)[];
}
type FieldType = 'simple' | 'text' | 'number' | 'select' | 'date' | 'dateRange';

interface Field<T, C> {
  type: FieldType;
  items?: T[];
  accessor?: keyof C;
  accessors?: (keyof C)[];
  formatter?: (value: T) => { label: string; value: string | number };
  apiFormatter?: (value: string | number) => string | number | { id: number };
  onOpen?: () => void;
}
function Workflows() {
  const { t }: { t: any } = useTranslation();
  const [view, setView] = useState<'list' | 'create' | 'update'>('list');
  const [currentMainCondition, setCurrentMainCondition] =
    useState<WFMainCondition>(mainConditions[0]);

  const [currentConditions, setCurrentConditions] = useState<UICondition[]>([]);
  const [saving, setSaving] = useState<boolean>(false);
  const [currentTitle, setCurrentTitle] = useState<string>('');
  const [currentWorkflowId, setCurrentWorkflowId] = useState<number>();
  // @ts-ignore
  const mainConfig: Record<
    WFMainCondition,
    {
      conditions: WorkflowConditionType[];
      actions: WorkflowActionType[];
    }
  > = {
    WORK_ORDER_CREATED: {
      conditions: workOrderConditions,
      actions: workOrderActions
    },
    WORK_ORDER_CLOSED: {
      conditions: workOrderConditions,
      actions: workOrderActions
    },
    WORK_ORDER_ARCHIVED: {
      conditions: workOrderConditions,
      actions: workOrderActions
    },
    REQUEST_CREATED: {
      conditions: requestConditions,
      actions: requestActions
    },
    REQUEST_APPROVED: {
      conditions: requestConditions,
      actions: requestActions
    },
    REQUEST_REJECTED: {
      conditions: workOrderConditions,
      actions: requestActions
    },
    PURCHASE_ORDER_CREATED: {
      conditions: purchaseOrderConditions,
      actions: purchaseOrderActions
    },
    PURCHASE_ORDER_UPDATED: {
      conditions: purchaseOrderConditions,
      actions: purchaseOrderActions
    },
    TASK_UPDATED: {
      conditions: taskConditions,
      actions: taskActions
    },
    PART_UPDATED: {
      conditions: partConditions,
      actions: partActions
    }
  } as const;
  const [currentAction, setCurrentAction] = useState<UIAction>({
    type: mainConfig[mainConditions[0]].actions[0],
    value: null
  });
  const [openDelete, setOpenDelete] = useState<boolean>(false);
  const { hasFeature } = useAuth();
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { workflows } = useSelector((state) => state.workflows);
  const { vendorsMini } = useSelector((state) => state.vendors);
  const { locationsMini } = useSelector((state) => state.locations);
  const { categories } = useSelector((state) => state.categories);
  const { usersMini } = useSelector((state) => state.users);
  const { assetsMini } = useSelector((state) => state.assets);
  const { teamsMini } = useSelector((state) => state.teams);
  const { partsMini } = useSelector((state) => state.parts);
  const fetchVendors = async () => {
    if (!vendorsMini.length) dispatch(getVendorsMini());
  };
  const fetchParts = async () => {
    if (!partsMini.length) dispatch(getPartsMini());
  };
  const fetchUsers = async () => {
    if (!usersMini.length) dispatch(getUsersMini());
  };
  const fetchLocations = async () => {
    if (!locationsMini.length) dispatch(getLocationsMini());
  };
  const fetchCategories = async (category: string) => {
    if (!categories[category]) dispatch(getCategories(category));
  };
  const fetchAssets = async () => {
    if (!assetsMini.length) dispatch(getAssetsMini());
  };
  const fetchTeams = async () => {
    if (!teamsMini.length) dispatch(getTeamsMini());
  };
  const objectFormatter = (value) => ({ id: Number(value) });
  const conditionsConfig: Record<
    WorkflowConditionType,
    Field<
      | AssetMiniDTO
      | LocationMiniDTO
      | PartMiniDTO
      | string
      | UserMiniDTO
      | VendorMiniDTO
      | TeamMiniDTO
      | Category,
      WorkflowCondition
    >
  > = {
    ASSET_IS: {
      type: 'select',
      items: assetsMini,
      accessor: 'asset',
      onOpen: fetchAssets,
      formatter: (asset: AssetMiniDTO) => ({
        label: asset.name,
        value: asset.id
      }),
      apiFormatter: objectFormatter
    },
    CATEGORY_IS: {
      type: 'select',
      accessor: currentMainCondition.startsWith('WORK_ORDER')
        ? 'workOrderCategory'
        : 'purchaseOrderCategory',
      items:
        categories[
          currentMainCondition.startsWith('WORK_ORDER')
            ? 'work-order-categories'
            : 'purchase-order-categories'
        ] ?? [],
      onOpen: () =>
        fetchCategories(
          currentMainCondition.startsWith('WORK_ORDER')
            ? 'work-order-categories'
            : 'purchase-order-categories'
        ),
      formatter: (category: Category) => ({
        label: category.name,
        value: category.id
      }),
      apiFormatter: objectFormatter
    },
    CREATED_AT_BETWEEN: {
      type: 'dateRange',
      accessors: ['startDate', 'endDate']
    },
    DUE_DATE_AFTER: { type: 'date', accessor: 'endDate' },
    DUE_DATE_BETWEEN: {
      type: 'dateRange',
      accessors: ['startDate', 'endDate']
    },
    LOCATION_IS: {
      type: 'select',
      items: locationsMini,
      accessor: 'location',
      onOpen: fetchLocations,
      formatter: (location: LocationMiniDTO) => ({
        label: location.name,
        value: location.id
      }),
      apiFormatter: objectFormatter
    },
    NAME_CONTAINS: { type: 'text', accessor: 'label' },
    NAME_IS: { type: 'text', accessor: 'label' },
    NUMBER_VALUE_INFERIOR: { type: 'number', accessor: 'numberValue' },
    NUMBER_VALUE_SUPERIOR: { type: 'number', accessor: 'numberValue' },
    PART_IS: {
      type: 'select',
      items: partsMini,
      accessor: 'part',
      onOpen: fetchParts,
      formatter: (part: PartMiniDTO) => ({ label: part.name, value: part.id }),
      apiFormatter: objectFormatter
    },
    PRIORITY_IS: {
      type: 'select',
      items: ['NONE', 'LOW', 'MEDIUM', 'HIGH'],
      accessor: 'priority',
      formatter: (priority: string) => ({ label: t(priority), value: priority })
    },
    QUANTITY_INFERIOR: { type: 'number', accessor: 'numberValue' },
    STATUS_IS: {
      type: 'select',
      items: ['OPEN', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETE'],
      accessor: 'workOrderStatus',
      formatter: (status: string) => ({ label: t(status), value: status })
    },
    TEAM_IS: {
      type: 'select',
      items: teamsMini,
      accessor: 'team',
      onOpen: fetchTeams,
      formatter: (team: TeamMiniDTO) => ({ label: team.name, value: team.id }),
      apiFormatter: objectFormatter
    },
    TITLE_CONTAINS: { type: 'text', accessor: 'value' },
    USER_IS: {
      type: 'select',
      items: usersMini,
      accessor: 'user',
      onOpen: fetchUsers,
      formatter: (user: UserMiniDTO) => ({
        label: `${user.firstName} ${user.lastName}`,
        value: user.id
      }),
      apiFormatter: objectFormatter
    },
    VALUE_CONTAINS: { type: 'text', accessor: 'value' },
    VALUE_IS: { type: 'text', accessor: 'value' },
    VENDOR_IS: {
      type: 'select',
      items: vendorsMini,
      accessor: 'vendor',
      onOpen: fetchVendors,
      formatter: (vendor: VendorMiniDTO) => ({
        label: vendor.companyName,
        value: vendor.id
      }),
      apiFormatter: objectFormatter
    }
  };
  const actionsConfig: Record<
    WorkflowActionType,
    Field<
      | AssetMiniDTO
      | LocationMiniDTO
      | string
      | UserMiniDTO
      | VendorMiniDTO
      | TeamMiniDTO
      | Category,
      WorkflowAction
    >
  > = {
    //TODO
    ADD_CHECKLIST: { type: 'simple' },
    APPROVE: { type: 'simple' },
    ASSIGN_ASSET: {
      type: 'select',
      items: assetsMini,
      accessor: 'asset',
      onOpen: fetchAssets,
      formatter: (asset: AssetMiniDTO) => ({
        label: asset.name,
        value: asset.id
      }),
      apiFormatter: objectFormatter
    },
    ASSIGN_CATEGORY: {
      type: 'select',
      accessor:
        currentMainCondition.startsWith('WORK_ORDER') ||
        currentMainCondition.startsWith('REQUEST')
          ? 'workOrderCategory'
          : 'purchaseOrderCategory',
      items:
        categories[
          currentMainCondition.startsWith('WORK_ORDER') ||
          currentMainCondition.startsWith('REQUEST')
            ? 'work-order-categories'
            : 'purchase-order-categories'
        ] ?? [],
      onOpen: () =>
        fetchCategories(
          currentMainCondition.startsWith('WORK_ORDER') ||
            currentMainCondition.startsWith('REQUEST')
            ? 'work-order-categories'
            : 'purchase-order-categories'
        ),
      formatter: (category: Category) => ({
        label: category.name,
        value: category.id
      }),
      apiFormatter: objectFormatter
    },
    ASSIGN_LOCATION: {
      type: 'select',
      items: locationsMini,
      accessor: 'location',
      onOpen: fetchLocations,
      formatter: (location: LocationMiniDTO) => ({
        label: location.name,
        value: location.id
      }),
      apiFormatter: objectFormatter
    },
    ASSIGN_PRIORITY: {
      type: 'select',
      items: ['NONE', 'LOW', 'MEDIUM', 'HIGH'],
      accessor: 'priority',
      formatter: (priority: string) => ({ label: t(priority), value: priority })
    },
    ASSIGN_TEAM: {
      type: 'select',
      items: teamsMini,
      accessor: 'team',
      onOpen: fetchTeams,
      formatter: (team: TeamMiniDTO) => ({ label: team.name, value: team.id }),
      apiFormatter: objectFormatter
    },
    ASSIGN_USER: {
      type: 'select',
      items: usersMini,
      accessor: 'user',
      onOpen: fetchUsers,
      formatter: (user: UserMiniDTO) => ({
        label: `${user.firstName} ${user.lastName}`,
        value: user.id
      }),
      apiFormatter: objectFormatter
    },
    ASSIGN_VENDOR: {
      type: 'select',
      items: vendorsMini,
      accessor: 'vendor',
      onOpen: fetchVendors,
      formatter: (vendor: VendorMiniDTO) => ({
        label: vendor.companyName,
        value: vendor.id
      }),
      apiFormatter: objectFormatter
    },
    CREATE_PURCHASE_ORDER: { type: 'number', accessor: 'numberValue' },
    CREATE_REQUEST: { type: 'simple' },
    CREATE_WORK_ORDER: { type: 'simple' },
    REJECT: { type: 'simple' },
    SEND_REMINDER_EMAIL: { type: 'simple' },
    SET_ASSET_STATUS: {
      type: 'select',
      items: ['OPERATIONAL', 'DOWN'],
      accessor: 'assetStatus',
      formatter: (assetStatus: string) => ({
        label: t(assetStatus.toLowerCase()),
        value: assetStatus
      })
    }
  };

  useEffect(() => {
    dispatch(getWorkflows());
  }, []);
  const onDeleteSuccess = () => {
    showSnackBar(t('workflow_delete_success'), 'success');
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('workflow_delete_failure'), 'error');

  const handleDelete = (id: number) => {
    dispatch(deleteWorkflow(id)).then(onDeleteSuccess).catch(onDeleteFailure);
    setOpenDelete(false);
  };
  const handleConditionTypeChange = (
    value: WorkflowConditionType,
    index: number
  ) => {
    const newConditions = [...currentConditions];
    newConditions[index].type = value;
    setCurrentConditions(newConditions);
  };
  const handleConditionValueChange = (
    value: string | number,
    index: number
  ) => {
    const newConditions = [...currentConditions];
    newConditions[index].value = value;
    setCurrentConditions(newConditions);
  };
  const handleConditionValuesChange = (values: string[], index: number) => {
    const newConditions = [...currentConditions];
    newConditions[index].values = values;
    setCurrentConditions(newConditions);
  };
  const onNewCondition = () => {
    setCurrentConditions((conditions) => [
      ...conditions,
      {
        type: mainConfig[currentMainCondition].conditions[0],
        value: null
      }
    ]);
  };
  const handleActionTypeChange = (value: WorkflowActionType) => {
    setCurrentAction((action) => ({
      type: value,
      value: null
    }));
  };
  const handleActionValueChange = (value: string | number) => {
    setCurrentAction((action) => ({
      ...action,
      value
    }));
  };
  const handleActionValuesChange = (values: string[]) => {
    setCurrentAction((action) => ({
      ...action,
      values
    }));
  };
  const onWorkflowCreationSuccess = () => {
    showSnackBar(t('workflow_creation_success'), 'success');
    setView('list');
    //TODO reset state
  };
  const onWorkflowEditSuccess = () => {
    showSnackBar(t('workflow_edit_success'), 'success');
    setView('list');
    //TODO reset state
  };
  const onEdit = (workflow: Workflow) => {
    fetchCategories('work-order-categories');
    fetchCategories('purchase-order-categories');
    fetchVendors();
    fetchLocations();
    fetchParts();
    fetchAssets();
    fetchTeams();
    const getTypeFromCondition = (
      condition: WorkflowCondition
    ): WorkflowConditionType => {
      if (condition.workOrderCondition) return condition.workOrderCondition;
      if (condition.requestCondition) return condition.requestCondition;
      if (condition.purchaseOrderCondition)
        return condition.purchaseOrderCondition;
      if (condition.partCondition) return condition.partCondition;
      if (condition.taskCondition) return condition.taskCondition;
    };
    const getTypeFromAction = (action: WorkflowAction): WorkflowActionType => {
      if (action.workOrderAction) return action.workOrderAction;
      if (action.requestAction) return action.requestAction;
      if (action.purchaseOrderAction) return action.purchaseOrderAction;
      if (action.partAction) return action.partAction;
      if (action.taskAction) return action.taskAction;
    };
    const getValueFromCondition = (
      condition: WorkflowCondition
    ): string | number => {
      const basicKeys: (keyof WorkflowCondition)[] = [
        'priority',
        'workOrderStatus',
        'purchaseOrderStatus',
        'label',
        'value',
        'numberValue'
      ];
      const objectKeys: (keyof WorkflowCondition)[] = [
        'asset',
        'location',
        'user',
        'team',
        'workOrderCategory',
        'purchaseOrderCategory',
        'checklist',
        'vendor',
        'part'
      ];
      let result;
      for (let key of basicKeys) {
        if (condition[key]) {
          result = condition[key];
          break;
        }
      }
      if (result) return result;
      for (let key of objectKeys) {
        if (condition[key]) {
          //@ts-ignore
          result = condition[key].id;
          break;
        }
      }
      if (result) return result;
    };
    const getValuesFromCondition = (
      condition: WorkflowCondition
    ): (string | number)[] => {
      let result = [];
      if (condition.startDate && condition.endDate) {
        result = [condition.startDate, condition.endDate];
      }
      return result;
    };
    const getValueFromAction = (action: WorkflowAction): string | number => {
      const basicKeys: (keyof WorkflowAction)[] = [
        'priority',
        'value',
        'numberValue',
        'assetStatus'
      ];
      const objectKeys: (keyof WorkflowAction)[] = [
        'asset',
        'location',
        'user',
        'team',
        'workOrderCategory',
        'purchaseOrderCategory',
        'checklist',
        'vendor'
      ];
      let result;
      for (let key of basicKeys) {
        if (action[key]) {
          result = action[key];
          break;
        }
      }
      if (result) return result;
      for (let key of objectKeys) {
        if (action[key]) {
          //@ts-ignore
          result = action[key].id;
          break;
        }
      }
      if (result) return result;
    };
    setCurrentTitle(workflow.title);
    setCurrentMainCondition(workflow.mainCondition);
    setCurrentConditions(
      workflow.secondaryConditions.map((condition) => {
        return {
          type: getTypeFromCondition(condition),
          value: getValueFromCondition(condition),
          values: getValuesFromCondition(condition)
        };
      })
    );
    setCurrentAction({
      type: getTypeFromAction(workflow.action),
      value: getValueFromAction(workflow.action),
      values: []
    });
    setCurrentWorkflowId(workflow.id);
    setView('update');
  };
  const checkFieldTypeValue = <
    T extends { values?: (string | number)[]; value: string | number }
  >(
    fieldType: FieldType,
    field: T
  ): boolean => {
    switch (fieldType) {
      case 'dateRange':
        return (
          field.values?.length == 2 && field.values.every((value) => !!value)
        );
      case 'simple':
        return true;
      default:
        return !!field.value;
    }
  };
  const onSave = () => {
    setSaving(true);
    if (
      currentConditions.every((condition) =>
        checkFieldTypeValue(conditionsConfig[condition.type].type, condition)
      )
    ) {
      if (
        checkFieldTypeValue(
          actionsConfig[currentAction.type].type,
          currentAction
        )
      ) {
        const getTypeAccessor = () => {
          if (currentMainCondition.startsWith('WORK_ORDER')) return 'workOrder';
          if (currentMainCondition.startsWith('REQUEST')) return 'request';
          if (currentMainCondition.startsWith('PURCHASE_ORDER'))
            return 'purchaseOrder';
          if (currentMainCondition.startsWith('PART')) return 'part';
          if (currentMainCondition.startsWith('TASK')) return 'task';
        };
        const actionConfig = actionsConfig[currentAction.type];
        const workflow = {
          title: currentTitle,
          mainCondition: currentMainCondition,
          secondaryConditions: currentConditions.map((condition) => {
            const config = conditionsConfig[condition.type];
            const formattedValue = config.apiFormatter
              ? config.apiFormatter(condition.value)
              : condition.value ?? condition.values;
            const result = {
              [`${getTypeAccessor()}Condition`]: condition.type,
              [config.accessor]: formattedValue
            };
            config.accessors?.forEach((accessor, index) => {
              result[accessor] = condition.values[index];
            });
            return result;
          }),
          action: {
            [`${getTypeAccessor()}Action`]: currentAction.type,
            [actionConfig.accessor]: actionConfig.apiFormatter
              ? actionConfig.apiFormatter(currentAction.value)
              : currentAction.value ?? currentAction.values
          }
        };
        dispatch(
          view === 'create'
            ? addWorkflow(workflow)
            : editWorkflow(currentWorkflowId, workflow)
        )
          .then(
            view === 'create'
              ? onWorkflowCreationSuccess
              : onWorkflowEditSuccess
          )
          .catch(() =>
            showSnackBar(
              t(
                view === 'create'
                  ? 'workflow_creation_failure'
                  : 'workflow_edit_failure'
              ),
              'error'
            )
          )
          .finally(() => {
            setSaving(false);
          });
      } else {
        setSaving(false);
        showSnackBar(t('action_value_missing'), 'error');
      }
    } else {
      setSaving(false);
      showSnackBar(t('condition_value_missing'), 'error');
    }
  };
  const onRemoveCondition = (index: number) => {
    const newConditions = [...currentConditions];
    newConditions.splice(index, 1);
    setCurrentConditions(newConditions);
  };
  const renderSingleCondition = (condition: UICondition, index: number) => {
    const config = conditionsConfig[condition.type];
    return (
      <Stack direction={'row'} spacing={1} alignItems={'flex-start'}>
        <Box>
          <Select
            value={condition.type}
            onChange={(event) =>
              handleConditionTypeChange(
                event.target.value as WorkflowConditionType,
                index
              )
            }
          >
            {mainConfig[currentMainCondition].conditions.map(
              (condition, index) => (
                <MenuItem key={index} value={condition}>
                  {t(condition)}
                </MenuItem>
              )
            )}
          </Select>
          <Box sx={{ mt: 1 }}>
            {['text', 'number'].includes(config.type) ? (
              <TextField
                value={condition.value}
                onChange={(event) =>
                  handleConditionValueChange(event.target.value, index)
                }
                type={config.type}
              />
            ) : config.type === 'select' ? (
              <Select
                value={condition.value}
                onChange={(event) =>
                  handleConditionValueChange(event.target.value, index)
                }
                onOpen={config.onOpen}
              >
                {config.items.map((item, index) => (
                  <MenuItem key={index} value={config.formatter(item).value}>
                    {config.formatter(item).label}
                  </MenuItem>
                ))}
              </Select>
            ) : config.type === 'date' ? (
              <DateTimePicker
                value={condition.value}
                onChange={(newValue) =>
                  handleConditionValueChange(newValue, index)
                }
                renderInput={(params) => (
                  <TextField
                    fullWidth
                    placeholder={t('select_date')}
                    required={true}
                    {...params}
                  />
                )}
              />
            ) : config.type === 'dateRange' ? (
              <LocalizationProvider
                localeText={{ start: t('start'), end: t('end') }}
                dateAdapter={AdapterDayjs}
              >
                <DateRangePicker
                  value={
                    condition.values?.length > 1
                      ? [condition.values[0], condition.values[1]]
                      : [null, null]
                  }
                  onChange={(newValues) => {
                    handleConditionValuesChange(newValues as string[], index);
                  }}
                  renderInput={(startProps, endProps) => (
                    <>
                      <TextField {...startProps} />
                      <Box sx={{ mx: 2 }}> {t('to')} </Box>
                      <TextField {...endProps} />
                    </>
                  )}
                />
              </LocalizationProvider>
            ) : null}
          </Box>
        </Box>
        <IconButton onClick={() => onRemoveCondition(index)}>
          <DeleteTwoToneIcon color={'error'} />
        </IconButton>
      </Stack>
    );
  };
  const renderActionField = (action: UIAction) => {
    const config = actionsConfig[action.type];
    return (
      <Box>
        <Box sx={{ mt: 1 }}>
          {['text', 'number'].includes(config.type) ? (
            <TextField
              value={action.value}
              onChange={(event) => handleActionValueChange(event.target.value)}
              type={config.type}
            />
          ) : config.type === 'select' ? (
            <Select
              value={action.value}
              onChange={(event) => handleActionValueChange(event.target.value)}
              onOpen={config.onOpen}
            >
              {config.items.map((item, index) => (
                <MenuItem key={index} value={config.formatter(item).value}>
                  {config.formatter(item).label}
                </MenuItem>
              ))}
            </Select>
          ) : config.type === 'date' ? (
            <DateTimePicker
              value={action.value}
              onChange={(newValue) => handleActionValueChange(newValue)}
              renderInput={(params) => (
                <TextField
                  fullWidth
                  placeholder={t('select_date')}
                  required={true}
                  {...params}
                />
              )}
            />
          ) : config.type === 'dateRange' ? (
            <LocalizationProvider
              localeText={{ start: t('start'), end: t('end') }}
              dateAdapter={AdapterDayjs}
            >
              <DateRangePicker
                value={
                  action.values?.length > 1
                    ? [action.values[0], action.values[1]]
                    : [null, null]
                }
                onChange={(newValues) => {
                  handleActionValuesChange(newValues as string[]);
                }}
                renderInput={(startProps, endProps) => (
                  <>
                    <TextField {...startProps} />
                    <Box sx={{ mx: 2 }}> {t('to')} </Box>
                    <TextField {...endProps} />
                  </>
                )}
              />
            </LocalizationProvider>
          ) : null}
        </Box>
      </Box>
    );
  };
  return (
    <Box
      sx={{
        p: 4
      }}
    >
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <Typography variant="h6">{t('workflow_description')}</Typography>
        </Grid>
        <Grid item xs={12}>
          {view === 'list' && (
            <Grid container spacing={2}>
              <Grid item xs={4}>
                <Button
                  sx={{
                    mb: 2
                  }}
                  variant="contained"
                  onClick={() => setView('create')}
                  disabled={
                    !hasFeature(PlanFeature.WORKFLOW) && workflows.length > 0
                  }
                  startIcon={<AddTwoToneIcon fontSize="small" />}
                >
                  {t('create_workflow')}
                </Button>
              </Grid>
              <Grid item xs={12}>
                {workflows.map((workflow) => (
                  <Card
                    sx={{ p: 2, mt: 1 }}
                    style={{
                      cursor: workflow.enabled ? 'pointer' : 'not-allowed'
                    }}
                    key={workflow.id}
                    onClick={() => {
                      if (workflow.enabled) onEdit(workflow);
                    }}
                  >
                    <Stack direction="row" justifyContent="space-between">
                      <Typography variant="h4">{workflow.title}</Typography>
                      {workflow.enabled || (
                        <Typography variant="h6">{t('disabled')}</Typography>
                      )}
                      <Button
                        onClick={(event) => {
                          event.stopPropagation();
                          setCurrentWorkflowId(workflow.id);
                          setOpenDelete(true);
                        }}
                        color="error"
                      >
                        {t('to_delete')}
                      </Button>
                    </Stack>
                  </Card>
                ))}
              </Grid>
            </Grid>
          )}
          {['create', 'update'].includes(view) && (
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <Typography variant="h6">{t('title')}</Typography>
                <TextField
                  value={currentTitle}
                  onChange={(event) => setCurrentTitle(event.target.value)}
                  placeholder={t('title')}
                />
              </Grid>
              <Grid item xs={4}>
                <Typography variant="h6">{t('if')}</Typography>
                <Select
                  value={currentMainCondition}
                  onChange={(event) => {
                    setCurrentMainCondition(
                      event.target.value as WFMainCondition
                    );
                    setCurrentConditions([]);
                  }}
                >
                  {mainConditions.map((mainCondition, index) => (
                    <MenuItem key={index} value={mainCondition}>
                      {t(mainCondition)}
                    </MenuItem>
                  ))}
                </Select>
              </Grid>
              <Grid item xs={4}>
                <Typography variant="h6">{t('and_optional')}</Typography>
                <Stack direction="column" spacing={2}>
                  {currentConditions.map((condition, index) =>
                    renderSingleCondition(condition, index)
                  )}
                  <Button
                    onClick={onNewCondition}
                    color="success"
                    variant="outlined"
                  >
                    {t('add_condition')}
                  </Button>
                </Stack>
              </Grid>
              <Grid item xs={4}>
                <Typography variant="h6">{t('then')}</Typography>
                <Select
                  value={currentAction?.type}
                  onChange={(event) =>
                    handleActionTypeChange(
                      event.target.value as WorkflowActionType
                    )
                  }
                >
                  {mainConfig[currentMainCondition].actions.map(
                    (action, index) => (
                      <MenuItem key={index} value={action}>
                        {t(action)}
                      </MenuItem>
                    )
                  )}
                </Select>
                {renderActionField(currentAction)}
              </Grid>
              <Grid item xs={12}>
                <Stack direction="row" spacing={1}>
                  <Button variant="outlined" onClick={() => setView('list')}>
                    {t('cancel')}
                  </Button>
                  <Button
                    disabled={saving}
                    startIcon={saving ? <CircularProgress size="1rem" /> : null}
                    onClick={onSave}
                    variant="contained"
                  >
                    {t('save')}
                  </Button>
                </Stack>
              </Grid>
            </Grid>
          )}
        </Grid>

        <ConfirmDialog
          open={openDelete}
          onCancel={() => {
            setOpenDelete(false);
          }}
          onConfirm={() => handleDelete(currentWorkflowId)}
          confirmText={t('to_delete')}
          question={t('confirm_delete_workflow')}
        />
      </Grid>
    </Box>
  );
}

export default Workflows;
