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
import CustomDatagrid2, {
  CustomDatagridColumn2
} from '../components/CustomDatagrid2';
import { SearchCriteria, SortDirection } from '../../../models/owns/page';
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
import {
  canAddReading,
  getImageAndFiles,
  onSearchQueryChange
} from '../../../utils/overall';
import SearchInput from '../components/SearchInput';
import { createColumnHelper } from '@tanstack/react-table';
import useTableState from '../../../hooks/useTableState';
import { getErrorMessage } from '../../../utils/api';
import SplitButton from '../components/SplitButton';

const LabelWrapper = styled(Box)(
  ({ theme }) => `
    font-size: ${theme.typography.pxToRem(10)};
    font-weight: bold;
    text-transform: uppercase;
    border-radius: ${theme.general.borderRadiusSm};
    padding: ${theme.spacing(0.9, 1.5, 0.7)};
    line-height: 1;
    width: fit-content;
  `
);

const fieldMapping: Record<string, string> = {
  name: 'name',
  unit: 'unit',
  asset: 'asset.name',
  location: 'location.name',
  customId: 'customId',
  createdAt: 'createdAt',
  createdBy: 'createdBy'
};

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

  // Use the table state hook for TanStack Table
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
    prefix: 'meters',
    initialSorting: [],
    initialPagination: {
      pageSize: criteria.pageSize,
      pageIndex: criteria.pageNum
    },
    setCriteria,
    fieldMapping
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
    showSnackBar(getErrorMessage(err, t('meter_create_failure')), 'error');
  const onEditSuccess = () => {
    setOpenUpdateModal(false);
    showSnackBar(t('changes_saved_success'), 'success');
  };
  const onEditFailure = (err) =>
    showSnackBar(getErrorMessage(err, t('meter_edit_failure')), 'error');
  const onDeleteSuccess = () => {
    showSnackBar(t('meter_delete_success'), 'success');
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('meter_delete_failure'), 'error');

  const columnHelper = createColumnHelper<Meter>();

  const columns: CustomDatagridColumn2<Meter>[] = [
    columnHelper.accessor('name', {
      id: 'name',
      header: () => t('name'),
      cell: (info) => <Box sx={{ fontWeight: 'bold' }}>{info.getValue()}</Box>,
      size: 150
    }),
    columnHelper.accessor('nextReading', {
      id: 'nextReading',
      header: () => t('next_reading_due'),
      cell: (info) =>
        canAddReading(info.row.original) ? (
          <LabelWrapper
            sx={{
              background: theme.colors.error.main,
              color: theme.palette.getContrastText(theme.colors.info.dark)
            }}
          >
            {t('past_due')}
          </LabelWrapper>
        ) : (
          <Typography>{getFormattedDate(info.getValue())}</Typography>
        ),
      size: 150
    }),
    columnHelper.accessor('unit', {
      id: 'unit',
      header: () => t('unit_of_measurement'),
      cell: (info) => info.getValue(),
      size: 150
    }),
    columnHelper.accessor('lastReading', {
      id: 'lastReading',
      header: () => t('last_reading'),
      cell: (info) => getFormattedDate(info.getValue()),
      size: 150
    }),
    columnHelper.accessor((row) => row.location?.name, {
      id: 'location',
      header: () => t('location'),
      cell: (info) => info.getValue() || '',
      size: 150,
      meta: {
        uiConfigKey: 'locations'
      }
    }),
    columnHelper.accessor((row) => row.asset?.name, {
      id: 'asset',
      header: () => t('asset'),
      cell: (info) => info.getValue() || '',
      size: 150
    }),
    columnHelper.accessor('createdBy', {
      id: 'createdBy',
      header: () => t('created_by'),
      cell: (info) => getUserNameById(info.getValue()),
      size: 150
    }),
    columnHelper.accessor('createdAt', {
      id: 'createdAt',
      header: () => t('created_at'),
      cell: (info) => getFormattedDate(info.getValue()),
      size: 150
    })
  ];
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
              try {
                const uploadedFiles = await uploadFiles([], values.image);
                formattedValues = {
                  ...formattedValues,
                  image: uploadedFiles.length
                    ? { id: uploadedFiles[0].id }
                    : null
                };
                await dispatch(addMeter(formattedValues));
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
              try {
                const uploadedFiles = await uploadFiles([], values.image);
                formattedValues = {
                  ...formattedValues,
                  image: values.image
                    ? uploadedFiles.length
                      ? { id: uploadedFiles[0].id }
                      : null
                    : null
                };
                await dispatch(editMeter(currentMeter.id, formattedValues));
                onEditSuccess();
              } catch (err) {
                onEditFailure(err);
                throw err;
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
                  <SplitButton
                    onMainClick={() => setOpenAddModal(true)}
                    startIcon={<AddTwoToneIcon />}
                    label={t('meter')}
                    menuItems={
                      hasViewPermission(PermissionEntity.SETTINGS) &&
                      hasFeature(PlanFeature.IMPORT_CSV)
                        ? [
                            {
                              label: t('to_import'),
                              onClick: () => navigate('/app/imports/meters')
                            }
                          ]
                        : []
                    }
                  />
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
                <CustomDatagrid2
                  columns={columns}
                  data={meters.content}
                  loading={loadingGet}
                  pagination={pagination}
                  onPaginationChange={setPagination}
                  totalRows={meters.totalElements}
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
                  noRowsMessage={t('noRows.meter.message')}
                  noRowsAction={t('')}
                  enableColumnReordering
                  enableColumnResizing
                  pinnedColumns={pinnedColumns}
                  onPinnedColumnsChange={setPinnedColumns}
                />
              </Box>
            </Card>
          </Box>
          <Drawer
            anchor="right"
            open={openDrawer}
            onClose={handleCloseDetails}
            PaperProps={{
              sx: { width: { xs: '90%', sm: '70%', md: '50%' } }
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
