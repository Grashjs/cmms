import {
  Box,
  Card,
  CardMedia,
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
  Tab,
  Tabs,
  Typography,
  useTheme
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import CustomDataGrid from '../components/CustomDatagrid';
import {
  GridRenderCellParams,
  GridToolbar,
  GridValueGetterParams
} from '@mui/x-data-grid';
import { GridEnrichedColDef } from '@mui/x-data-grid/models/colDef/gridColDef';
import Part from '../../../models/owns/part';
import {
  addPart,
  clearSinglePart,
  deletePart,
  editPart,
  getParts,
  getSinglePart
} from '../../../slices/part';
import ConfirmDialog from '../components/ConfirmDialog';
import { useDispatch, useSelector } from '../../../store';
import * as React from 'react';
import { ChangeEvent, useContext, useEffect, useMemo, useState } from 'react';
import * as Yup from 'yup';
import Form from '../components/form';
import { IField } from '../type';
import PartDetails from './PartDetails';
import { useNavigate, useParams } from 'react-router-dom';
import { isNumeric } from '../../../utils/validators';
import {
  formatSelect,
  formatSelectMultiple,
  getFormattedCostPerUnit
} from '../../../utils/formatters';
import { UserMiniDTO } from '../../../models/user';
import UserAvatars from '../components/UserAvatars';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import useAuth from '../../../hooks/useAuth';
import NoRowsMessageWrapper from '../components/NoRowsMessageWrapper';
import { getImageAndFiles, onSearchQueryChange } from '../../../utils/overall';
import { SearchCriteria, SortDirection } from '../../../models/owns/page';
import { exportEntity } from '../../../slices/exports';
import MoreVertTwoToneIcon from '@mui/icons-material/MoreVertTwoTone';
import { PermissionEntity } from '../../../models/owns/role';
import SearchInput from '../components/SearchInput';
import { PlanFeature } from '../../../models/owns/subscriptionPlan';
import { useGridApiRef } from '@mui/x-data-grid-pro';
import useGridStatePersist from '../../../hooks/useGridStatePersist';
import { CategoryMiniDTO } from '../../../models/owns/category';

interface PropsType {
  setAction: (p: () => () => void) => void;
}

export const getFormattedQuantityWithUnit = (
  quantity: number,
  unit: string
) => {
  return unit ? quantity + ' ' + unit : quantity;
};
const Parts = ({ setAction }: PropsType) => {
  const { t }: { t: any } = useTranslation();
  const [currentTab, setCurrentTab] = useState<string>('list');
  const [openUpdateModal, setOpenUpdateModal] = useState<boolean>(false);
  const [openDrawer, setOpenDrawer] = useState<boolean>(false);
  const { getFormattedDate, uploadFiles, getFormattedCurrency } = useContext(
    CompanySettingsContext
  );
  const { parts, loadingGet, singlePart } = useSelector((state) => state.parts);
  const [openDrawerFromUrl, setOpenDrawerFromUrl] = useState<boolean>(false);
  const [criteria, setCriteria] = useState<SearchCriteria>({
    filterFields: [],
    pageSize: 10,
    pageNum: 0,
    direction: 'DESC'
  });
  const [openDelete, setOpenDelete] = useState<boolean>(false);
  const [openAddModal, setOpenAddModal] = useState<boolean>(false);
  const [currentPart, setCurrentPart] = useState<Part>();
  const {
    getFilteredFields,
    hasViewPermission,
    hasViewOtherPermission,
    hasFeature
  } = useAuth();
  const { partId } = useParams();
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { loadingExport } = useSelector((state) => state.exports);
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const openMenu = Boolean(anchorEl);
  const navigate = useNavigate();
  const onQueryChange = (event) => {
    onSearchQueryChange<Part>(event, criteria, setCriteria, [
      'name',
      'description',
      'additionalInfos'
    ]);
  };
  const debouncedQueryChange = useMemo(() => debounce(onQueryChange, 1300), []);

  const handleOpenMenu = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };
  const handleCloseMenu = () => {
    setAnchorEl(null);
  };

  const handleOpenUpdate = () => {
    setOpenUpdateModal(true);
  };

  const handleDelete = (id: number) => {
    handleCloseDetails();
    dispatch(deletePart(id)).then(onDeleteSuccess).catch(onDeleteFailure);
    setOpenDelete(false);
  };
  const tabs = [
    { value: 'list', label: t('list_view') },
    { value: 'card', label: t('card_view') }
  ];
  const theme = useTheme();
  const onCreationSuccess = () => {
    setOpenAddModal(false);
    showSnackBar(t('part_create_success'), 'success');
  };
  const onCreationFailure = (err) =>
    showSnackBar(t('part_create_failure'), 'error');
  const onEditSuccess = () => {
    setOpenUpdateModal(false);
    showSnackBar(t('changes_saved_success'), 'success');
  };
  const onEditFailure = (err) => showSnackBar(t('part_edit_failure'), 'error');
  const onDeleteSuccess = () => {
    showSnackBar(t('part_delete_success'), 'success');
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('part_delete_failure'), 'error');
  useEffect(() => {
    const handleOpenModal = () => setOpenAddModal(true);
    setAction(() => handleOpenModal);
  }, []);

  const handleOpenDrawer = (part: Part) => {
    setCurrentPart(part);
    window.history.replaceState(
      null,
      'Part details',
      `/app/inventory/parts/${part.id}`
    );
    setOpenDrawer(true);
  };
  useEffect(() => {
    if (partId && isNumeric(partId)) {
      dispatch(getSinglePart(Number(partId)));
    }
  }, [partId]);

  useEffect(() => {
    dispatch(getParts(criteria));
  }, [criteria]);

  //see changes in ui on edit
  useEffect(() => {
    if (singlePart || parts.content.length) {
      const currentInContent = parts.content.find(
        (part) => part.id === currentPart?.id
      );
      const updatedPart = currentInContent ?? singlePart;
      if (updatedPart) {
        if (openDrawerFromUrl) {
          setCurrentPart(updatedPart);
        } else {
          handleOpenDrawer(updatedPart);
          setOpenDrawerFromUrl(true);
        }
      }
    }
    return () => {
      dispatch(clearSinglePart());
    };
  }, [singlePart, parts]);

  const onPageSizeChange = (size: number) => {
    setCriteria({ ...criteria, pageSize: size });
  };
  const onPageChange = (number: number) => {
    setCriteria({ ...criteria, pageNum: number });
  };

  const handleTabsChange = (_event: ChangeEvent<{}>, value: string): void => {
    setCurrentTab(value);
  };
  const handleOpenDetails = (id: number) => {
    const foundPart = parts.content.find((part) => part.id === id);
    if (foundPart) {
      handleOpenDrawer(foundPart);
    }
  };
  const handleCloseDetails = () => {
    window.history.replaceState(null, 'Part', `/app/inventory/parts`);
    setOpenDrawer(false);
  };
  const formatValues = (values) => {
    const newValues = { ...values };
    newValues.assignedTo = formatSelectMultiple(newValues.assignedTo);
    newValues.teams = formatSelectMultiple(newValues.teams);
    newValues.customers = formatSelectMultiple(newValues.customers);
    newValues.vendors = formatSelectMultiple(newValues.vendors);
    newValues.category = formatSelect(newValues.category);
    // values.image = formatSelect(values.image);
    // values.files = formatSelect(values.files);
    return newValues;
  };
  const columns: GridEnrichedColDef<Part>[] = [
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
      field: 'cost',
      headerName: t('cost'),
      description: t('cost'),
      valueGetter: (params) =>
        getFormattedCostPerUnit(
          params.value,
          params.row.unit,
          getFormattedCurrency
        ),
      width: 150
    },
    {
      field: 'quantity',
      headerName: t('quantity'),
      description: t('quantity'),
      width: 150,
      renderCell: (params: GridRenderCellParams<number, Part>) => (
        <Box sx={params.value < params.row.minQuantity ? { color: 'red' } : {}}>
          {getFormattedQuantityWithUnit(params.value, params.row.unit)}{' '}
          {params.value < params.row.minQuantity && t('(Running Low !)')}
        </Box>
      )
    },
    {
      field: 'barcode',
      headerName: t('barcode'),
      description: t('barcode'),
      width: 150
    },
    {
      field: 'area',
      headerName: t('area'),
      description: t('area'),
      width: 150
    },
    {
      field: 'category',
      headerName: t('category'),
      description: t('category'),
      valueGetter: (params: GridValueGetterParams<CategoryMiniDTO>) =>
        params.value?.name,
      width: 150
    },
    {
      field: 'description',
      headerName: t('description'),
      description: t('description'),
      width: 300
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
      field: 'createdAt',
      headerName: t('created_at'),
      description: t('created_at'),
      width: 150,
      valueGetter: (params: GridValueGetterParams<string>) =>
        getFormattedDate(params.value)
    },
    {
      field: 'openWorkOrders',
      headerName: t('open_wo'),
      description: t('open_wo'),
      width: 150
    }
  ];
  const fields: Array<IField> = [
    {
      name: 'name',
      type: 'text',
      label: t('name'),
      placeholder: t('enter_part_name'),
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
      name: 'category',
      type: 'select',
      type2: 'category',
      category: 'part-categories',
      label: t('category'),
      placeholder: t('enter_part_category')
    },
    {
      name: 'cost',
      type: 'number',
      label: t('cost'),
      placeholder: t('enter_part_cost')
    },
    {
      name: 'quantity',
      type: 'number',
      label: t('quantity'),
      placeholder: t('enter_part_quantity')
    },
    {
      name: 'unit',
      type: 'text',
      label: t('unit'),
      placeholder: t('unit_of_measurement')
    },
    {
      name: 'minQuantity',
      type: 'number',
      label: t('minimum_quantity'),
      placeholder: t('enter_part_minimum_quantity')
    },
    {
      name: 'nonStock',
      type: 'checkbox',
      label: t('non_stock')
    },
    {
      name: 'barcode',
      type: 'text',
      label: t('barcode'),
      placeholder: t('enter_part_barcode')
    },
    {
      name: 'area',
      type: 'text',
      label: t('area'),
      placeholder: t('enter_part_area')
    },
    {
      name: 'additionalInfos',
      type: 'text',
      label: t('additional_part_details'),
      placeholder: t('additional_part_details'),
      multiple: true
    },
    {
      name: 'assignedTo',
      type: 'select',
      type2: 'user',
      multiple: true,
      label: t('workers'),
      placeholder: 'Select Workers'
    },
    {
      name: 'teams',
      type: 'select',
      type2: 'team',
      multiple: true,
      label: t('teams'),
      placeholder: 'Select Teams'
    },
    {
      name: 'vendors',
      type: 'select',
      type2: 'vendor',
      multiple: true,
      label: t('vendors'),
      placeholder: 'Select Vendors'
    },
    {
      name: 'customers',
      type: 'select',
      type2: 'customer',
      multiple: true,
      label: t('customers'),
      placeholder: 'Select Customers'
    },
    {
      name: 'image',
      type: 'file',
      label: t('image'),
      fileType: 'image'
    },
    {
      name: 'files',
      type: 'file',
      multiple: true,
      label: t('files')
    }
  ];
  const apiRef = useGridApiRef();
  useGridStatePersist(apiRef, columns, 'part');
  const shape = {
    name: Yup.string().required(t('required_part_name'))
  };
  const renderPartAddModal = () => (
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
          {t('add_part')}
        </Typography>
        <Typography variant="subtitle2">{t('add_part_description')}</Typography>
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
            submitText={t('create_part')}
            values={{}}
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
                    dispatch(addPart(formattedValues))
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
  const BasicField = ({
    label,
    value
  }: {
    label: string | number;
    value: string | number;
  }) => {
    return value ? (
      <Grid item xs={12}>
        <Stack spacing={1} direction="row">
          <Typography variant="h6" sx={{ color: theme.colors.alpha.black[70] }}>
            {label}
          </Typography>
          <Typography variant="h6">{value}</Typography>
        </Stack>
      </Grid>
    ) : null;
  };
  const fieldsToRender = (part: Part) => [
    {
      label: t('id'),
      value: part.id
    },
    {
      label: t('category'),
      value: part.category?.name
    },
    {
      label: t('quantity'),
      value: getFormattedQuantityWithUnit(part.quantity, part.unit)
    },
    {
      label: t('cost'),
      value: getFormattedCurrency(part.cost)
    },
    {
      label: t('barcode'),
      value: part.barcode
    },
    {
      label: t('created_at'),
      value: getFormattedDate(part.createdAt)
    }
  ];
  const renderPartUpdateModal = () => (
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
          {t('update_part')}
        </Typography>
        <Typography variant="subtitle2">
          {t('update_part_description')}
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
            submitText={t('save')}
            values={{
              ...currentPart,
              assignedTo: currentPart?.assignedTo.map((user) => {
                return {
                  label: `${user.firstName} ${user.lastName}`,
                  value: user.id.toString()
                };
              }),
              teams: currentPart?.teams.map((team) => {
                return {
                  label: team.name,
                  value: team.id.toString()
                };
              }),
              vendors: currentPart?.vendors.map((vendor) => {
                return {
                  label: vendor.companyName,
                  value: vendor.id.toString()
                };
              }),
              customers: currentPart?.customers.map((customer) => {
                return {
                  label: customer.name,
                  value: customer.id.toString()
                };
              })
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
                      currentPart.image
                    );
                    formattedValues = {
                      ...formattedValues,
                      image: imageAndFiles.image,
                      files: [...currentPart.files, ...imageAndFiles.files]
                    };
                    dispatch(editPart(currentPart.id, formattedValues))
                      .then(onEditSuccess)
                      .catch(onEditFailure)
                      .finally(resolve);
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
      {hasViewOtherPermission(PermissionEntity.PARTS_AND_MULTIPARTS) && (
        <MenuItem
          disabled={loadingExport['parts']}
          onClick={() => {
            dispatch(exportEntity('parts')).then((url: string) => {
              window.open(url);
            });
          }}
        >
          <Stack spacing={2} direction="row">
            {loadingExport['parts'] && <CircularProgress size="1rem" />}
            <Typography>{t('to_export')}</Typography>
          </Stack>
        </MenuItem>
      )}
      {hasViewPermission(PermissionEntity.SETTINGS) && (
        <MenuItem
          onClick={() => navigate('/app/imports/parts')}
          disabled={!hasFeature(PlanFeature.IMPORT_CSV)}
        >
          {t('to_import')}
        </MenuItem>
      )}
    </Menu>
  );
  return (
    <Box sx={{ p: 2 }}>
      {renderPartAddModal()}
      {renderPartUpdateModal()}
      {renderMenu()}
      <Stack
        mb={1}
        direction="row"
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
          {tabs.map((tab) => (
            <Tab key={tab.value} label={tab.label} value={tab.value} />
          ))}
        </Tabs>
        <SearchInput onChange={debouncedQueryChange} />
        <IconButton onClick={handleOpenMenu} color="primary">
          <MoreVertTwoToneIcon />
        </IconButton>
      </Stack>
      {currentTab === 'list' && (
        <CustomDataGrid
          apiRef={apiRef}
          columns={columns}
          pageSize={criteria.pageSize}
          page={criteria.pageNum}
          rows={parts.content}
          rowCount={parts.totalElements}
          pagination
          paginationMode="server"
          sortingMode="server"
          onPageSizeChange={onPageSizeChange}
          onPageChange={onPageChange}
          rowsPerPageOptions={[10, 20, 50]}
          loading={loadingGet}
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
              cost: 'cost',
              category: 'category',
              quantity: 'quantity',
              description: 'description',
              minQuantity: 'minQuantity',
              customId: 'customId'
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
          components={{
            NoRowsOverlay: () => (
              <NoRowsMessageWrapper
                message={t('noRows.part.message')}
                action={t('noRows.part.action')}
              />
            )
          }}
          onRowClick={(params) => {
            handleOpenDetails(Number(params.id));
          }}
          initialState={{
            columns: {
              columnVisibilityModel: {}
            }
          }}
        />
      )}
      {currentTab === 'card' && (
        <Grid item xs={12}>
          <Grid container spacing={2}>
            {parts.content.map((part) => (
              <Grid item xs={12} lg={3} key={part.id}>
                <Card
                  style={{ cursor: 'pointer' }}
                  onClick={() => handleOpenDetails(part.id)}
                >
                  <CardMedia
                    component="img"
                    height="280"
                    image={
                      part.image ? part.image.url : '/static/images/nopic.jpg'
                    }
                    alt={part.name}
                  />
                  <Box sx={{ p: 2 }}>
                    <Typography variant="h4">{part.name}</Typography>
                    <Box sx={{ mt: 1 }}>
                      {fieldsToRender(part).map((field) => (
                        <BasicField
                          key={field.label}
                          label={field.label}
                          value={field.value}
                        />
                      ))}
                    </Box>
                  </Box>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Grid>
      )}
      <Drawer
        anchor="right"
        open={openDrawer}
        onClose={handleCloseDetails}
        PaperProps={{
          sx: { width: '50%' }
        }}
      >
        <PartDetails
          part={currentPart}
          handleOpenUpdate={handleOpenUpdate}
          handleOpenDelete={() => setOpenDelete(true)}
        />
      </Drawer>
      <ConfirmDialog
        open={openDelete}
        onCancel={() => {
          setOpenDelete(false);
          setOpenDrawer(true);
        }}
        onConfirm={() => handleDelete(currentPart?.id)}
        confirmText={t('to_delete')}
        question={t('confirm_delete_part')}
      />
    </Box>
  );
};

export default Parts;
