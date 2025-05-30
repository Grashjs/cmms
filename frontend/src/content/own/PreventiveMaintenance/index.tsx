import { Helmet } from 'react-helmet-async';
import {
  Box,
  Button,
  Card,
  debounce,
  Dialog,
  DialogContent,
  DialogTitle,
  Drawer,
  Grid,
  Stack,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import * as React from 'react';
import { useContext, useEffect, useMemo, useState } from 'react';
import { TitleContext } from '../../../contexts/TitleContext';
import {
  addPreventiveMaintenance,
  clearSinglePM,
  deletePreventiveMaintenance,
  editPreventiveMaintenance,
  getPreventiveMaintenances,
  getSinglePreventiveMaintenance,
  patchSchedule
} from '../../../slices/preventiveMaintenance';
import { useDispatch, useSelector } from '../../../store';
import ConfirmDialog from '../components/ConfirmDialog';
import { GridEnrichedColDef } from '@mui/x-data-grid/models/colDef/gridColDef';
import CustomDataGrid, {
  CustomDatagridColumn
} from '../components/CustomDatagrid';
import {
  FilterField,
  SearchCriteria,
  SearchOperator,
  SortDirection
} from '../../../models/owns/page';
import { GridRenderCellParams, GridValueGetterParams } from '@mui/x-data-grid';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import Form from '../components/form';
import * as Yup from 'yup';
import { IField } from '../type';
import PMDetails from './PMDetails';
import { useNavigate, useParams } from 'react-router-dom';
import { isNumeric } from '../../../utils/validators';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import PriorityWrapper from '../components/PriorityWrapper';
import { formatSelect, formatSelectMultiple } from '../../../utils/formatters';
import useAuth from '../../../hooks/useAuth';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import { getWOBaseFields, getWOBaseValues } from '../../../utils/woBase';
import { PermissionEntity } from '../../../models/owns/role';
import PermissionErrorMessage from '../components/PermissionErrorMessage';
import NoRowsMessageWrapper from '../components/NoRowsMessageWrapper';
import {
  getImageAndFiles,
  getNextOccurence,
  onSearchQueryChange
} from '../../../utils/overall';
import { UserMiniDTO } from '../../../models/user';
import UserAvatars from '../components/UserAvatars';
import PreventiveMaintenance from '../../../models/owns/preventiveMaintenance';
import Category from '../../../models/owns/category';
import { LocationMiniDTO } from '../../../models/owns/location';
import { AssetMiniDTO } from '../../../models/owns/asset';
import { patchTasksOfPreventiveMaintenance } from '../../../slices/task';
import { useGridApiRef } from '@mui/x-data-grid-pro';
import useGridStatePersist from '../../../hooks/useGridStatePersist';
import EnumFilter from '../WorkOrders/Filters/EnumFilter';
import SignalCellularAltTwoToneIcon from '@mui/icons-material/SignalCellularAltTwoTone';
import SearchInput from '../components/SearchInput';
import WorkOrder from '../../../models/owns/workOrder';

function Files() {
  const { t }: { t: any } = useTranslation();
  const { setTitle } = useContext(TitleContext);
  const [openAddModal, setOpenAddModal] = useState<boolean>(false);
  const [openUpdateModal, setOpenUpdateModal] = useState<boolean>(false);
  const [openDrawer, setOpenDrawer] = useState<boolean>(false);
  const {
    companySettings,
    hasViewPermission,
    hasCreatePermission,
    getFilteredFields
  } = useAuth();
  const [currentPM, setCurrentPM] = useState<PreventiveMaintenance>();
  const { tasksByPreventiveMaintenance } = useSelector((state) => state.tasks);
  const tasks = tasksByPreventiveMaintenance[currentPM?.id] ?? [];
  const { uploadFiles, getWOFieldsAndShapes } = useContext(
    CompanySettingsContext
  );
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const { preventiveMaintenanceId } = useParams();
  const dispatch = useDispatch();
  const [openDelete, setOpenDelete] = useState<boolean>(false);
  const { preventiveMaintenances, loadingGet, singlePreventiveMaintenance } =
    useSelector((state) => state.preventiveMaintenances);
  const [openDrawerFromUrl, setOpenDrawerFromUrl] = useState<boolean>(false);
  const [criteria, setCriteria] = useState<SearchCriteria>({
    filterFields: [
      {
        field: 'priority',
        operation: 'in',
        values: ['NONE', 'LOW', 'MEDIUM', 'HIGH'],
        value: '',
        enumName: 'PRIORITY'
      }
    ],
    pageSize: 10,
    pageNum: 0,
    direction: 'DESC'
  });
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const navigate = useNavigate();

  useEffect(() => {
    setTitle(t('preventive_maintenance'));
  }, []);
  useEffect(() => {
    if (preventiveMaintenanceId && isNumeric(preventiveMaintenanceId)) {
      dispatch(getSinglePreventiveMaintenance(Number(preventiveMaintenanceId)));
    }
  }, [preventiveMaintenanceId]);

  useEffect(() => {
    if (hasViewPermission(PermissionEntity.PREVENTIVE_MAINTENANCES))
      dispatch(getPreventiveMaintenances(criteria));
  }, [criteria]);

  //see changes in ui on edit
  useEffect(() => {
    if (singlePreventiveMaintenance || preventiveMaintenances.content.length) {
      const currentInContent = preventiveMaintenances.content.find(
        (preventiveMaintenance) => preventiveMaintenance.id === currentPM?.id
      );
      const updatedPreventiveMaintenance =
        currentInContent ?? singlePreventiveMaintenance;
      if (updatedPreventiveMaintenance) {
        if (openDrawerFromUrl) {
          setCurrentPM(updatedPreventiveMaintenance);
        } else {
          handleOpenDrawer(updatedPreventiveMaintenance);
          setOpenDrawerFromUrl(true);
        }
      }
    }
    return () => {
      dispatch(clearSinglePM());
    };
  }, [singlePreventiveMaintenance, preventiveMaintenances]);

  const onPageSizeChange = (size: number) => {
    setCriteria({ ...criteria, pageSize: size });
  };
  const onPageChange = (number: number) => {
    setCriteria({ ...criteria, pageNum: number });
  };
  const handleOpenDrawer = (preventiveMaintenance: PreventiveMaintenance) => {
    setCurrentPM(preventiveMaintenance);
    window.history.replaceState(
      null,
      'PreventiveMaintenance details',
      `/app/preventive-maintenances/${preventiveMaintenance.id}`
    );
    setOpenDrawer(true);
  };

  const handleDelete = (id: number) => {
    handleCloseDetails();
    dispatch(deletePreventiveMaintenance(id))
      .then(onDeleteSuccess)
      .catch(onDeleteFailure);
    setOpenDelete(false);
  };
  const handleOpenUpdate = () => {
    setOpenUpdateModal(true);
  };
  const onCreationSuccess = () => {
    setOpenAddModal(false);
    showSnackBar(t('wo_schedule_success'), 'success');
  };
  const onCreationFailure = (err) =>
    showSnackBar(t('wo_schedule_failure'), 'error');
  const onEditSuccess = () => {
    setOpenUpdateModal(false);
    showSnackBar(t('changes_saved_success'), 'success');
  };
  const onEditFailure = (err) =>
    showSnackBar(t('wo_trigger_edit_failure'), 'error');
  const onDeleteSuccess = () => {
    showSnackBar(t('wo_trigger_delete_success'), 'success');
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('wo_trigger_delete_failure'), 'error');

  const handleOpenDetails = (id: number) => {
    const foundPreventiveMaintenance = preventiveMaintenances.content.find(
      (preventiveMaintenance) => preventiveMaintenance.id === id
    );
    if (foundPreventiveMaintenance) {
      handleOpenDrawer(foundPreventiveMaintenance);
    }
  };
  const handleCloseDetails = () => {
    window.history.replaceState(
      null,
      'Preventive',
      `/app/preventive-maintenances`
    );
    setOpenDrawer(false);
  };
  const onQueryChange = (event) => {
    onSearchQueryChange<PreventiveMaintenance>(event, criteria, setCriteria, [
      'title',
      'description',
      'name'
    ]);
  };
  const onFilterChange = (newFilters: FilterField[]) => {
    const newCriteria = { ...criteria };
    newCriteria.filterFields = newFilters;
    setCriteria(newCriteria);
  };
  const debouncedQueryChange = useMemo(() => debounce(onQueryChange, 1300), []);

  const formatValues = (values) => {
    const newValues = { ...values };
    newValues.primaryUser = formatSelect(newValues.primaryUser);
    newValues.location = formatSelect(newValues.location);
    newValues.team = formatSelect(newValues.team);
    newValues.asset = formatSelect(newValues.asset);
    newValues.assignedTo = formatSelectMultiple(newValues.assignedTo);
    newValues.priority = newValues.priority?.value;
    newValues.category = formatSelect(newValues.category);
    return newValues;
  };
  const columns: CustomDatagridColumn[] = [
    {
      field: 'customId',
      headerName: t('id'),
      description: t('id')
    },
    {
      field: 'name',
      headerName: t('name'),
      description: t('name'),
      width: 150,
      renderCell: (params: GridRenderCellParams<string>) => (
        <Box sx={{ fontWeight: 'bold' }}>{params.value}</Box>
      )
    },
    {
      field: 'title',
      headerName: t('wo_title'),
      description: t('wo_title'),
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
      field: 'next',
      headerName: t('next_wo'),
      description: t('next_wo'),
      width: 150,
      valueGetter: (
        params: GridValueGetterParams<null, PreventiveMaintenance>
      ) =>
        getFormattedDate(
          getNextOccurence(
            new Date(params.row.schedule?.startsOn),
            params.row.schedule.frequency
          ).toString()
        )
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
      width: 150,
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
    }
  ];
  const apiRef = useGridApiRef();
  useGridStatePersist(apiRef, columns, 'pm');

  // Mapping for column fields to API field names for sorting
  const fieldMapping: Record<string, string> = {
    customId: 'customId',
    name: 'name',
    title: 'title',
    priority: 'priority',
    description: 'description',
    next: 'schedule.startsOn',
    primaryUser: 'primaryUser.firstName',
    assignedTo: 'assignedTo',
    location: 'location.name',
    category: 'category.name',
    asset: 'asset.name'
  };

  const defaultFields: Array<IField> = [
    {
      name: 'triggerConfiguration',
      type: 'titleGroupField',
      label: t('trigger_configuration')
    },
    {
      name: 'name',
      type: 'text',
      label: t('trigger_name'),
      placeholder: t('enter_trigger_name'),
      required: true
    },
    {
      name: 'startsOn',
      type: 'date',
      label: t('starts_on'),
      required: true,
      midWidth: true
    },
    {
      name: 'endsOn',
      type: 'date',
      label: t('ends_on'),
      midWidth: true
    },
    {
      name: 'frequency',
      type: 'number',
      label: t('frequency_description'),
      required: true
    },
    {
      name: 'titleGroup',
      type: 'titleGroupField',
      label: 'wo_configuration'
    },
    ...getWOBaseFields(t, { delay: true }),
    {
      name: 'tasks',
      type: 'select',
      type2: 'task',
      label: t('tasks'),
      placeholder: t('select_tasks')
    }
  ];
  const defaultShape = {
    name: Yup.string().required(t('required_trigger_name')),
    title: Yup.string().required(t('required_wo_title')),
    frequency: Yup.number()
      .required(t('required_frequency'))
      .test(
        'test-frequency', // this is used internally by yup
        t('invalid_frequency'),
        (value) => value > 0
      )
  };
  const getFieldsAndShapes = (): [Array<IField>, { [key: string]: any }] => {
    return getWOFieldsAndShapes(defaultFields, defaultShape);
  };
  const renderAddModal = () => (
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
          {t('schedule_wo')}
        </Typography>
        <Typography variant="subtitle2">
          {t('schedule_wo_description')}
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
            submitText={t('add')}
            values={{ startsOn: null, endsOn: null, dueDate: null }}
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
                    dispatch(addPreventiveMaintenance(formattedValues))
                      .then(onCreationSuccess)
                      .catch(onCreationFailure)
                      .finally(resolve);
                  })
                  .catch((err) => {
                    onCreationFailure(err);
                    rej(err);
                  });
              });
            }}
          />
        </Box>
      </DialogContent>
    </Dialog>
  );
  const renderUpdateModal = () => (
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
          {t('edit_wo_trigger')}
        </Typography>
        <Typography variant="subtitle2">
          {t('edit_wo_trigger_description')}
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
              ...currentPM,
              ...getWOBaseValues(t, currentPM),
              startsOn: currentPM?.schedule.startsOn,
              endsOn: currentPM?.schedule.endsOn,
              frequency: Number(currentPM?.schedule.frequency),
              dueDateDelay: currentPM?.schedule.dueDateDelay,
              tasks
            }}
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              let formattedValues = formatValues(values);
              return new Promise<void>((resolve, rej) => {
                const files = formattedValues.files.find((file) => file.id)
                  ? []
                  : formattedValues.files;
                uploadFiles(files, formattedValues.image)
                  .then((files) => {
                    const imageAndFiles = getImageAndFiles(
                      files,
                      currentPM.image
                    );
                    formattedValues = {
                      ...formattedValues,
                      image: imageAndFiles.image,
                      files: [...currentPM.files, ...imageAndFiles.files]
                    };
                    dispatch(
                      patchTasksOfPreventiveMaintenance(
                        currentPM?.id,
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
                          editPreventiveMaintenance(
                            currentPM?.id,
                            formattedValues
                          )
                        )
                          .then(() => {
                            dispatch(
                              patchSchedule(
                                currentPM.schedule.id,
                                currentPM.id,
                                {
                                  ...currentPM.schedule,
                                  ...formattedValues
                                }
                              )
                            );
                          })
                          .then(onEditSuccess)
                          .catch(onEditFailure)
                          .finally(resolve)
                      )
                      .catch((err) => {
                        onEditFailure(err);
                        rej();
                      });
                  })
                  .catch((err) => {
                    onEditFailure(err);
                    rej(err);
                  });
              });
            }}
          />
        </Box>
      </DialogContent>
    </Dialog>
  );
  if (hasViewPermission(PermissionEntity.PREVENTIVE_MAINTENANCES))
    return (
      <>
        <Helmet>
          <title>{t('preventive_maintenance')}</title>
        </Helmet>
        {renderAddModal()}
        {renderUpdateModal()}
        <Grid
          container
          justifyContent="center"
          alignItems="stretch"
          spacing={1}
          paddingX={4}
        >
          {hasCreatePermission(PermissionEntity.PREVENTIVE_MAINTENANCES) && (
            <Grid
              item
              xs={12}
              display="flex"
              flexDirection="row"
              justifyContent="right"
              alignItems="center"
            >
              <Button
                startIcon={<AddTwoToneIcon />}
                sx={{ mx: 6, my: 1 }}
                variant="contained"
                onClick={() => setOpenAddModal(true)}
              >
                {t('create_trigger')}
              </Button>
            </Grid>
          )}
          <Grid item xs={12}>
            <Card
              sx={{
                p: 2,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'space-between'
              }}
            >
              <Stack
                sx={{ ml: 1, mb: 1 }}
                direction="row"
                spacing={1}
                justifyContent={'flex-start'}
                width={'95%'}
              >
                <EnumFilter
                  filterFields={criteria.filterFields}
                  onChange={onFilterChange}
                  completeOptions={['NONE', 'LOW', 'MEDIUM', 'HIGH']}
                  fieldName="priority"
                  icon={<SignalCellularAltTwoToneIcon />}
                />
                <SearchInput onChange={debouncedQueryChange} />
              </Stack>
              <Box sx={{ width: '95%' }}>
                <CustomDataGrid
                  apiRef={apiRef}
                  columns={columns}
                  loading={loadingGet}
                  pageSize={criteria.pageSize}
                  page={criteria.pageNum}
                  rows={preventiveMaintenances.content}
                  rowCount={preventiveMaintenances.totalElements}
                  pagination
                  paginationMode="server"
                  sortingMode="server"
                  onSortModelChange={(model) => {
                    if (model.length === 0) {
                      setCriteria((prevState) => ({
                        ...prevState,
                        sortField: undefined,
                        direction: undefined
                      }));
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
                  initialState={{
                    columns: {
                      columnVisibilityModel: {}
                    }
                  }}
                  onPageSizeChange={onPageSizeChange}
                  onPageChange={onPageChange}
                  rowsPerPageOptions={[10, 20, 50]}
                  onRowClick={({ id }) => handleOpenDetails(Number(id))}
                  components={{
                    NoRowsOverlay: () => (
                      <NoRowsMessageWrapper
                        message={t('noRows.pm.message')}
                        action={t('noRows.pm.action')}
                      />
                    )
                  }}
                />
              </Box>
            </Card>
          </Grid>
        </Grid>
        <Drawer
          anchor="right"
          open={openDrawer}
          onClose={handleCloseDetails}
          PaperProps={{
            sx: { width: '50%' }
          }}
        >
          <PMDetails
            onClose={handleCloseDetails}
            preventiveMaintenance={currentPM}
            handleOpenUpdate={handleOpenUpdate}
            handleOpenDelete={() => setOpenDelete(true)}
            tasks={tasks}
          />
        </Drawer>
        <ConfirmDialog
          open={openDelete}
          onCancel={() => {
            setOpenDelete(false);
            setOpenDrawer(true);
          }}
          onConfirm={() => handleDelete(currentPM?.id)}
          confirmText={t('to_delete')}
          question={t('confirm_delete_pm')}
        />
      </>
    );
  else return <PermissionErrorMessage message={'no_access_pm'} />;
}

export default Files;
