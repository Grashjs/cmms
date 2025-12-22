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
  Drawer,
  Grid,
  IconButton,
  Menu,
  MenuItem,
  Stack,
  styled,
  Typography,
  useTheme
} from '@mui/material';
import {
  addMeter,
  clearSingleMeter,
  deleteMeter,
  editMeter,
  getMeters,
  getSingleMeter
} from '../../../slices/meter';
import { useDispatch, useSelector } from '../../../store';
import ConfirmDialog from '../components/ConfirmDialog';
import { useTranslation } from 'react-i18next';
import * as React from 'react';
import { useContext, useEffect, useMemo, useState } from 'react';
import { TitleContext } from '../../../contexts/TitleContext';
import { GridEnrichedColDef } from '@mui/x-data-grid/models/colDef/gridColDef';
import CustomDataGrid, {
  CustomDatagridColumn
} from '../components/CustomDatagrid';
import { SearchCriteria, SortDirection } from '../../../models/owns/page';
import {
  GridRenderCellParams,
  GridToolbar,
  GridValueGetterParams
} from '@mui/x-data-grid';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import Meter from '../../../models/owns/meter';
import Form from '../components/form';
import * as Yup from 'yup';
import { IField } from '../type';
import MeterDetails from './MeterDetails';
import { useNavigate, useParams } from 'react-router-dom';
import { isNumeric } from '../../../utils/validators';
import { formatSelect, formatSelectMultiple } from '../../../utils/formatters';
import { CustomSnackBarContext } from 'src/contexts/CustomSnackBarContext';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import useAuth from '../../../hooks/useAuth';
import { PermissionEntity } from '../../../models/owns/role';
import PermissionErrorMessage from '../components/PermissionErrorMessage';
import FeatureErrorMessage from '../components/FeatureErrorMessage';
import { PlanFeature } from '../../../models/owns/subscriptionPlan';
import NoRowsMessageWrapper from '../components/NoRowsMessageWrapper';
import { exportEntity } from '../../../slices/exports';
import MoreVertTwoToneIcon from '@mui/icons-material/MoreVertTwoTone';
import { canAddReading, onSearchQueryChange } from '../../../utils/overall';
import SearchInput from '../components/SearchInput';
import { useGridApiRef } from '@mui/x-data-grid-pro';
import useGridStatePersist from '../../../hooks/useGridStatePersist';

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

function Meters() {
  const { t }: { t: any } = useTranslation();
  const { setTitle } = useContext(TitleContext);
  const [openAddModal, setOpenAddModal] = useState<boolean>(false);
  const [openUpdateModal, setOpenUpdateModal] = useState<boolean>(false);
  const [openDrawer, setOpenDrawer] = useState<boolean>(false);
  const [currentMeter, setCurrentMeter] = useState<Meter>();
  const { meterId } = useParams();
  const {
    hasViewPermission,
    hasCreatePermission,
    hasViewOtherPermission,
    getFilteredFields,
    hasFeature
  } = useAuth();
  const dispatch = useDispatch();
  const theme = useTheme();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { getFormattedDate, uploadFiles, getUserNameById } = useContext(
    CompanySettingsContext
  );
  const [openDelete, setOpenDelete] = useState<boolean>(false);
  const { meters, loadingGet, singleMeter } = useSelector(
    (state) => state.meters
  );
  const [openDrawerFromUrl, setOpenDrawerFromUrl] = useState<boolean>(false);
  const [criteria, setCriteria] = useState<SearchCriteria>({
    filterFields: [],
    pageSize: 10,
    pageNum: 0,
    direction: 'DESC'
  });
  const { loadingExport } = useSelector((state) => state.exports);
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const openMenu = Boolean(anchorEl);
  const navigate = useNavigate();

  const onQueryChange = (event) => {
    onSearchQueryChange<Meter>(event, criteria, setCriteria, ['name', 'unit']);
  };
  const debouncedQueryChange = useMemo(() => debounce(onQueryChange, 1300), []);

  const handleOpenMenu = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };
  const handleCloseMenu = () => {
    setAnchorEl(null);
  };
  useEffect(() => {
    setTitle(t('meters'));
  }, []);
  useEffect(() => {
    if (meterId && isNumeric(meterId)) {
      dispatch(getSingleMeter(Number(meterId)));
    }
  }, [meterId]);

  useEffect(() => {
    if (hasViewPermission(PermissionEntity.METERS))
      dispatch(getMeters(criteria));
  }, [criteria]);

  const onNewReading = () => {
    dispatch(getMeters(criteria));
  };
  //see changes in ui on edit
  useEffect(() => {
    if (singleMeter || meters.content.length) {
      const currentInContent = meters.content.find(
        (meter) => meter.id === currentMeter?.id
      );
      const updatedMeter = currentInContent ?? singleMeter;
      if (updatedMeter) {
        if (openDrawerFromUrl) {
          setCurrentMeter(updatedMeter);
        } else {
          handleOpenDrawer(updatedMeter);
          setOpenDrawerFromUrl(true);
        }
      }
    }
    return () => {
      dispatch(clearSingleMeter());
    };
  }, [singleMeter, meters]);

  const onPageSizeChange = (size: number) => {
    setCriteria({ ...criteria, pageSize: size });
  };
  const onPageChange = (number: number) => {
    setCriteria({ ...criteria, pageNum: number });
  };

  const formatValues = (values) => {
    const newValues = { ...values };
    newValues.users = formatSelectMultiple(newValues.users);
    //values.teams = formatSelectMultiple(values.teams);
    newValues.meterCategory = formatSelect(newValues.category);
    newValues.location = formatSelect(newValues.location);
    newValues.asset = formatSelect(newValues.asset);
    newValues.updateFrequency = Number(newValues.updateFrequency);
    return newValues;
  };
  const handleDelete = (id: number) => {
    handleCloseDetails();
    dispatch(deleteMeter(id)).then(onDeleteSuccess).catch(onDeleteFailure);
    setOpenDelete(false);
  };
  const handleOpenUpdate = () => {
    setOpenUpdateModal(true);
  };
  const handleOpenDrawer = (meter: Meter) => {
    setCurrentMeter(meter);
    window.history.replaceState(
      null,
      'Meter details',
      `/app/meters/${meter.id}`
    );
    setOpenDrawer(true);
  };
  const handleOpenDetails = (id: number) => {
    const foundMeter = meters.content.find((meter) => meter.id === id);
    if (foundMeter) {
      handleOpenDrawer(foundMeter);
    }
  };
  const handleCloseDetails = () => {
    window.history.replaceState(null, 'Meter', `/app/meters`);
    setOpenDrawer(false);
  };
  const onCreationSuccess = () => {
    setOpenAddModal(false);
    showSnackBar(t('meter_create_success'), 'success');
  };
  const onCreationFailure = (err) =>
    showSnackBar(t('meter_create_failure'), 'error');
  const onEditSuccess = () => {
    setOpenUpdateModal(false);
    showSnackBar(t('changes_saved_success'), 'success');
  };
  const onEditFailure = (err) => showSnackBar(t('meter_edit_failure'), 'error');
  const onDeleteSuccess = () => {
    showSnackBar(t('meter_delete_success'), 'success');
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('meter_delete_failure'), 'error');
  const columns: CustomDatagridColumn[] = [
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
      field: 'nextReading',
      headerName: t('next_reading_due'),
      description: t('next_reading_due'),
      width: 150,
      renderCell: (params: GridRenderCellParams<string, Meter>) =>
        canAddReading(params.row) ? (
          <LabelWrapper
            sx={{
              background: theme.colors.error.main,
              color: theme.palette.getContrastText(theme.colors.info.dark)
            }}
          >
            {t('past_due')}
          </LabelWrapper>
        ) : (
          <Typography>{getFormattedDate(params.value)}</Typography>
        )
    },
    {
      field: 'unit',
      headerName: t('unit_of_measurement'),
      description: t('unit_of_measurement'),
      width: 150
    },
    {
      field: 'lastReading',
      headerName: t('last_reading'),
      description: t('last_reading'),
      width: 150,
      valueGetter: (params: GridValueGetterParams<string>) =>
        getFormattedDate(params.value)
    },
    {
      field: 'location',
      headerName: t('location'),
      description: t('location'),
      width: 150,
      valueGetter: (params) => params.row.location?.name,
      uiConfigKey: 'locations'
    },
    {
      field: 'asset',
      headerName: t('asset'),
      description: t('asset'),
      width: 150,
      valueGetter: (params) => params.row.asset.name
    },
    {
      field: 'createdBy',
      headerName: t('created_by'),
      description: t('created_by'),
      width: 150,
      valueGetter: (params) => getUserNameById(params.value)
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
  const apiRef = useGridApiRef();
  useGridStatePersist(apiRef, columns, 'meter');
  const fields: Array<IField> = [
    {
      name: 'name',
      type: 'text',
      label: t('name'),
      placeholder: t('enter_meter_name'),
      required: true
    },
    {
      name: 'unit',
      type: 'text',
      label: t('unit'),
      placeholder: t('unit'),
      required: true
    },
    {
      name: 'updateFrequency',
      type: 'number',
      label: t('update_frequency'),
      placeholder: t('update_frequency_in_days'),
      required: true
    },
    {
      name: 'category',
      type: 'select',
      type2: 'category',
      category: 'meter-categories',
      label: t('category'),
      placeholder: t('category')
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
      label: t('location')
    },
    {
      name: 'asset',
      type: 'select',
      type2: 'asset',
      label: t('asset'),
      relatedFields: [{ field: 'location' }],
      required: true
    },
    {
      name: 'users',
      type: 'select',
      type2: 'user',
      label: t('workers'),
      multiple: true
    }
  ];
  const shape = {
    name: Yup.string().required(t('required_meter_name')),
    unit: Yup.string().required(t('required_meter_unit')),
    updateFrequency: Yup.number().required(
      t('required_meter_update_frequency')
    ),
    asset: Yup.object().required(t('required_asset')).nullable()
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
          {t('add_meter')}
        </Typography>
        <Typography variant="subtitle2">
          {t('add_meter_description')}
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
            fields={getFilteredFields(fields)}
            validation={Yup.object().shape(shape)}
            submitText={t('add')}
            values={{}}
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              let formattedValues = formatValues(values);
              return new Promise<void>((resolve, rej) => {
                uploadFiles([], values.image)
                  .then((files) => {
                    formattedValues = {
                      ...formattedValues,
                      image: files.length ? { id: files[0].id } : null
                    };
                    dispatch(addMeter(formattedValues))
                      .then(onCreationSuccess)
                      .catch(onCreationFailure)
                      .finally(resolve);
                  })
                  .catch((err) => {
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
          {t('edit_meter')}
        </Typography>
        <Typography variant="subtitle2">
          {t('edit_meter_description')}
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
            fields={fields}
            validation={Yup.object().shape(shape)}
            submitText={t('save')}
            values={{
              ...currentMeter,
              users: currentMeter?.users.map((worker) => {
                return {
                  label: `${worker?.firstName} ${worker.lastName}`,
                  value: worker.id
                };
              }),
              location: currentMeter?.location
                ? {
                    label: currentMeter?.location.name,
                    value: currentMeter?.location.id
                  }
                : null,
              asset: {
                label: currentMeter?.asset.name,
                value: currentMeter?.asset.id
              },
              category: currentMeter?.meterCategory
                ? {
                    label: currentMeter?.meterCategory.name,
                    value: currentMeter?.meterCategory.id
                  }
                : null
            }}
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              let formattedValues = formatValues(values);
              return new Promise<void>((resolve, rej) => {
                uploadFiles([], values.image)
                  .then((files) => {
                    formattedValues = {
                      ...formattedValues,
                      image: files.length
                        ? { id: files[0].id }
                        : currentMeter.image
                    };
                    dispatch(editMeter(currentMeter.id, formattedValues))
                      .then(onEditSuccess)
                      .catch(onEditFailure)
                      .finally(resolve);
                  })
                  .catch((err) => {
                    rej(err);
                    onEditFailure(err);
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
      {hasViewOtherPermission(PermissionEntity.METERS) && (
        <MenuItem
          disabled={loadingExport['meters']}
          onClick={() => {
            dispatch(exportEntity('meters')).then((url: string) => {
              window.open(url);
            });
          }}
        >
          <Stack spacing={2} direction="row">
            {loadingExport['meters'] && <CircularProgress size="1rem" />}
            <Typography>{t('to_export')}</Typography>
          </Stack>
        </MenuItem>
      )}
      {hasViewPermission(PermissionEntity.SETTINGS) && (
        <MenuItem
          onClick={() => navigate('/app/imports/meters')}
          disabled={!hasFeature(PlanFeature.IMPORT_CSV)}
        >
          {t('to_import')}
        </MenuItem>
      )}
    </Menu>
  );
  if (hasFeature(PlanFeature.METER)) {
    if (hasViewPermission(PermissionEntity.METERS))
      return (
        <>
          <Helmet>
            <title>{t('meters')}</title>
          </Helmet>
          {renderAddModal()}
          {renderUpdateModal()}
          {renderMenu()}
          <Box justifyContent="center" alignItems="stretch" paddingX={4}>
            <Stack
              my={1}
              direction="row"
              justifyContent="space-between"
              alignItems="center"
            >
              <SearchInput onChange={debouncedQueryChange} />
              <Stack direction={'row'} alignItems="center" spacing={1}>
                <IconButton onClick={handleOpenMenu} color="primary">
                  <MoreVertTwoToneIcon />
                </IconButton>
                {hasCreatePermission(PermissionEntity.METERS) && (
                  <Button
                    startIcon={<AddTwoToneIcon />}
                    variant="contained"
                    onClick={() => setOpenAddModal(true)}
                  >
                    {t('meter')}
                  </Button>
                )}
              </Stack>
            </Stack>
            <Card
              sx={{
                py: 2,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'space-between'
              }}
            >
              <Box sx={{ width: '95%' }}>
                <CustomDataGrid
                  apiRef={apiRef}
                  columns={columns}
                  loading={loadingGet}
                  pageSize={criteria.pageSize}
                  page={criteria.pageNum}
                  rows={meters.content}
                  rowCount={meters.totalElements}
                  pagination
                  paginationMode="server"
                  sortingMode="server"
                  onPageSizeChange={onPageSizeChange}
                  onPageChange={onPageChange}
                  rowsPerPageOptions={[10, 20, 50]}
                  onSortModelChange={(model) => {
                    if (model.length === 0) {
                      setCriteria({
                        ...criteria,
                        sortField: undefined,
                        direction: undefined
                      });
                      return;
                    }

                    const fieldMapping = {
                      name: 'name',
                      unit: 'unit',
                      asset: 'asset.name',
                      location: 'location.name',
                      customId: 'customId',
                      createdAt: 'createdAt',
                      createdBy: 'createdBy'
                    };

                    const field = model[0].field;
                    const mappedField = fieldMapping[field];

                    if (!mappedField) return;

                    setCriteria({
                      ...criteria,
                      sortField: mappedField,
                      direction: (model[0].sort?.toUpperCase() ||
                        'ASC') as SortDirection
                    });
                  }}
                  onRowClick={({ id }) => handleOpenDetails(Number(id))}
                  components={{
                    NoRowsOverlay: () => (
                      <NoRowsMessageWrapper
                        message={t('noRows.meter.message')}
                        action={t('')}
                      />
                    )
                  }}
                  initialState={{
                    columns: {
                      columnVisibilityModel: {}
                    }
                  }}
                />
              </Box>
            </Card>
          </Box>
          <Drawer
            anchor="right"
            open={openDrawer}
            onClose={handleCloseDetails}
            PaperProps={{
              sx: { width: '50%' }
            }}
          >
            <MeterDetails
              meter={currentMeter}
              handleOpenUpdate={handleOpenUpdate}
              handleOpenDelete={() => setOpenDelete(true)}
              onNewReading={onNewReading}
            />
          </Drawer>
          <ConfirmDialog
            open={openDelete}
            onCancel={() => {
              setOpenDelete(false);
              setOpenDrawer(true);
            }}
            onConfirm={() => handleDelete(currentMeter?.id)}
            confirmText={t('to_delete')}
            question={t('confirm_delete_meter')}
          />
        </>
      );
    else return <PermissionErrorMessage message={'no_access_meters'} />;
  } else return <FeatureErrorMessage message={'upgrade_create_meter'} />;
}

export default Meters;
