import {
  Box,
  Card,
  CardMedia,
  Dialog,
  DialogContent,
  DialogTitle,
  Drawer,
  Grid,
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
import ConfirmDialog from '../components/ConfirmDialog';
import { useDispatch, useSelector } from '../../../store';
import { ChangeEvent, useContext, useEffect, useState } from 'react';
import * as Yup from 'yup';
import Form from '../components/form';
import { IField } from '../type';
import SetType from '../../../models/owns/setType';
import SetDetails from './SetDetails';
import { useParams } from 'react-router-dom';
import { isNumeric } from '../../../utils/validators';
import {
  addMultiParts,
  deleteMultiParts,
  editMultiParts,
  getMultiParts
} from '../../../slices/multipart';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import NoRowsMessageWrapper from '../components/NoRowsMessageWrapper';
import { formatSelectMultiple } from '../../../utils/formatters';
import { useGridApiRef } from '@mui/x-data-grid-pro';
import useGridStatePersist from '../../../hooks/useGridStatePersist';

interface PropsType {
  setAction: (p: () => () => void) => void;
}

const Sets = ({ setAction }: PropsType) => {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const [currentTab, setCurrentTab] = useState<string>('list');
  const [openAddModal, setOpenAddModal] = useState<boolean>(false);
  const [openDelete, setOpenDelete] = useState<boolean>(false);
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const { multiParts, loadingGet } = useSelector((state) => state.multiParts);
  const [openUpdateModal, setOpenUpdateModal] = useState<boolean>(false);
  const [openDrawer, setOpenDrawer] = useState<boolean>(false);
  const [currentSet, setCurrentSet] = useState<SetType>();
  const { showSnackBar } = useContext(CustomSnackBarContext);

  const tabs = [
    { value: 'list', label: t('list_view') },
    { value: 'card', label: t('card_view') }
  ];
  const { setId } = useParams();
  const dispatch = useDispatch();

  const handleUpdate = () => {
    setOpenUpdateModal(true);
  };

  const handleDelete = (id: number) => {
    handleCloseDetails();
    dispatch(deleteMultiParts(id)).then(onDeleteSuccess).catch(onDeleteFailure);
    setOpenDelete(false);
  };
  const formatValues = (values) => {
    const newValues = { ...values };
    newValues.parts = formatSelectMultiple(values.parts);
    return newValues;
  };
  const onCreationSuccess = () => {
    setOpenAddModal(false);
    showSnackBar(t('set_create_success'), 'success');
  };
  const onCreationFailure = (err) =>
    showSnackBar(t('set_create_failure'), 'error');
  const onEditSuccess = () => {
    setOpenUpdateModal(false);
    showSnackBar(t('changes_saved_success'), 'success');
  };
  const onEditFailure = (err) => showSnackBar(t('set_edit_failure'), 'error');
  const onDeleteSuccess = () => {
    showSnackBar(t('set_delete_success'), 'success');
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('set_delete_failure'), 'error');

  useEffect(() => {
    dispatch(getMultiParts());
    const handleOpenModal = () => setOpenAddModal(true);
    setAction(() => handleOpenModal);
  }, []);

  const handleTabsChange = (_event: ChangeEvent<{}>, value: string): void => {
    setCurrentTab(value);
  };
  const columns: GridEnrichedColDef[] = [
    {
      field: 'name',
      headerName: t('name'),
      description: t('name'),
      flex: 1,
      renderCell: (params: GridRenderCellParams<string>) => (
        <Box sx={{ fontWeight: 'bold' }}>{params.value}</Box>
      )
    },
    {
      field: 'parts',
      headerName: t('parts'),
      description: t('parts'),
      flex: 1,
      valueGetter: (params: GridValueGetterParams<Part[]>) =>
        params.value.length
    },
    {
      field: 'cost',
      headerName: t('total_cost'),
      description: t('total_cost'),
      flex: 1,
      valueGetter: (params) =>
        params.row.parts.reduce((acc, part) => acc + part.cost, 0)
    },
    {
      field: 'createdAt',
      headerName: t('created_at'),
      description: t('created_at'),
      flex: 1,
      valueGetter: (params) => getFormattedDate(params.row.createdAt)
    }
  ];
  const apiRef = useGridApiRef();
  useGridStatePersist(apiRef, columns, 'multiPart');
  const fields: Array<IField> = [
    {
      name: 'name',
      type: 'text',
      label: t('name'),
      placeholder: t('Enter Part name'),
      required: true
    },
    {
      name: 'partsTitle',
      type: 'titleGroupField',
      label: t('parts')
    },
    {
      name: 'parts',
      type: 'select',
      type2: 'part',
      label: t('part'),
      placeholder: t('enter_part_name')
    }
  ];
  const shape = {
    name: Yup.string().required(t('required_set_name'))
  };

  useEffect(() => {
    if (multiParts.length && setId && isNumeric(setId)) {
      handleOpenDetails(Number(setId));
    }
  }, [multiParts]);

  const handleOpenDetails = (id: number) => {
    const foundSet = multiParts.find((set) => set.id === id);
    if (foundSet) {
      setCurrentSet(foundSet);
      window.history.replaceState(
        null,
        'Set details',
        `/app/inventory/sets/${id}`
      );
      setOpenDrawer(true);
    }
  };
  const handleCloseDetails = () => {
    window.history.replaceState(null, 'Sets', `/app/inventory/sets`);
    setOpenDrawer(false);
    setCurrentSet(null);
  };
  const fieldsToRender = (set: SetType) => [
    {
      label: t('parts'),
      value: set.parts.length
    },
    {
      label: t('total_cost'),
      value: set.parts.reduce((acc, part) => acc + part.cost, 0)
    }
  ];
  const BasicField = ({
    label,
    value
  }: {
    label: string | number;
    value: string | number;
  }) => {
    return value ? (
      <Grid item xs={12} key={label}>
        <Stack spacing={1} direction="row">
          <Typography variant="h6" sx={{ color: theme.colors.alpha.black[70] }}>
            {label}
          </Typography>
          <Typography variant="h6">{value}</Typography>
        </Stack>
      </Grid>
    ) : null;
  };
  const renderSetAddModal = () => (
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
          {t('add_set')}
        </Typography>
        <Typography variant="subtitle2">{t('add_set_description')}</Typography>
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
            submitText={t('create_set')}
            values={{}}
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              const formattedValues = formatValues(values);
              return dispatch(addMultiParts(formattedValues))
                .then(onCreationSuccess)
                .catch(onCreationFailure);
            }}
          />
        </Box>
      </DialogContent>
    </Dialog>
  );
  const renderSetUpdateModal = () => (
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
          {t('edit_set')}
        </Typography>
        <Typography variant="subtitle2">{t('edit_set_description')}</Typography>
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
            submitText={t('edit_set')}
            values={{
              ...currentSet,
              parts: currentSet?.parts?.map((part) => {
                return {
                  label: part.name,
                  value: part.id.toString()
                };
              })
            }}
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              const formattedValues = formatValues(values);
              return dispatch(editMultiParts(currentSet.id, formattedValues))
                .then(onEditSuccess)
                .catch(onEditFailure);
            }}
          />
        </Box>
      </DialogContent>
    </Dialog>
  );
  return (
    <Box sx={{ p: 2 }}>
      {renderSetAddModal()}
      {renderSetUpdateModal()}
      <Tabs
        sx={{ mb: 2 }}
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
      {currentTab === 'list' && (
        <Box sx={{ width: '95%' }}>
          <CustomDataGrid
            apiRef={apiRef}
            columns={columns}
            rows={multiParts}
            loading={loadingGet}
            components={{
              NoRowsOverlay: () => (
                <NoRowsMessageWrapper
                  message={t(
                    'Manage your inventory by combining inventory part items into a single item which can be a kit, bundle or package'
                  )}
                  action={t("Press the '+' button to create a Set")}
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
        </Box>
      )}
      {currentTab === 'card' && (
        <Grid item xs={12}>
          <Grid container spacing={2}>
            {multiParts.map((set) => (
              <Grid item xs={12} lg={3} key={set.id}>
                <Card
                  style={{ cursor: 'pointer' }}
                  onClick={() => handleOpenDetails(set.id)}
                >
                  <CardMedia
                    component="img"
                    height="280"
                    image={'/static/images/nopic.jpg'}
                    alt="..."
                  />
                  <Box sx={{ p: 2 }}>
                    <Typography variant="h4">{set.name}</Typography>
                    <Box sx={{ mt: 1 }}>
                      {fieldsToRender(set).map((field) => (
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
        <SetDetails
          set={currentSet}
          handleOpenUpdate={handleUpdate}
          handleOpenDelete={() => setOpenDelete(true)}
        />
      </Drawer>
      <ConfirmDialog
        open={openDelete}
        onCancel={() => {
          setOpenDelete(false);
          setOpenDrawer(true);
        }}
        onConfirm={() => handleDelete(currentSet?.id)}
        confirmText={t('to_delete')}
        question={t('confirm_delete_set')}
      />
    </Box>
  );
};

export default Sets;
