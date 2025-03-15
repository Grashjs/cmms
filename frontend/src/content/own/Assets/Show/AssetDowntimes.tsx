import {
  Box,
  Button,
  Card,
  Dialog,
  DialogContent,
  DialogTitle,
  Grid,
  MenuItem,
  Select,
  Stack,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import CustomDataGrid from '../../components/CustomDatagrid';
import {
  GridActionsCellItem,
  GridRowParams,
  GridToolbar,
  GridValueGetterParams
} from '@mui/x-data-grid';
import { GridEnrichedColDef } from '@mui/x-data-grid/models/colDef/gridColDef';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import {
  AssetDTO,
  AssetStatus,
  assetStatuses
} from '../../../../models/owns/asset';
import { useDispatch, useSelector } from '../../../../store';
import { editAsset } from '../../../../slices/asset';
import useAuth from '../../../../hooks/useAuth';
import { PermissionEntity } from '../../../../models/owns/role';
import {
  createAssetDowntime,
  deleteAssetDowntime,
  getAssetDowntimes
} from '../../../../slices/assetDowntime';
import { useContext, useEffect, useState } from 'react';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import Form from '../../components/form';
import * as Yup from 'yup';
import { IField } from '../../type';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import {
  getHMSString,
  getHoursAndMinutesAndSeconds
} from '../../../../utils/formatters';
import { CompanySettingsContext } from '../../../../contexts/CompanySettingsContext';
import AssetDowntime from '../../../../models/owns/assetDowntime';

interface PropsType {
  asset: AssetDTO;
}

const AssetDowntimes = ({ asset }: PropsType) => {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { assetDowntimesByAsset } = useSelector((state) => state.downtimes);
  const downtimes = assetDowntimesByAsset[asset?.id] ?? [];
  const { hasEditPermission, hasDeletePermission } = useAuth();
  const [openAddModal, setOpenAddModal] = useState<boolean>(false);
  const [openEditModal, setOpenEditModal] = useState<boolean>(false);
  const [currentDowntime, setCurrentDowntime] = useState<AssetDowntime>();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { getFormattedDate } = useContext(CompanySettingsContext);

  useEffect(() => {
    if (asset) dispatch(getAssetDowntimes(asset.id));
  }, [asset]);
  const handleDelete = (id: number) => {
    if (window.confirm(t('confirm_delete_asset_downtime'))) {
      dispatch(deleteAssetDowntime(asset.id, id));
    }
  };
  const onCreationSuccess = () => {
    setOpenAddModal(false);
    showSnackBar(t('create_downtime_success'), 'success');
  };
  const onCreationFailure = (err) =>
    showSnackBar(t('create_downtime_failure'), 'error');
  const onEditSuccess = () => {
    setOpenEditModal(false);
    showSnackBar(t('edit_downtime_success'), 'success');
  };
  const onEditFailure = (err) =>
    showSnackBar(t("The Downtime couldn't be edited"), 'error');

  const handleEdit = (id: number) => {
    setCurrentDowntime(downtimes.find((downtime) => downtime.id === id));
    setOpenEditModal(true);
  };
  const verifyValues = (values): boolean => {
    const seconds = values.hours * 60 * 60 + values.minutes * 60;
    let startsOn = new Date(values.startsOn);
    startsOn.setSeconds(startsOn.getSeconds() + seconds);
    if (startsOn > new Date()) {
      showSnackBar(
        'The end of the downtime should not be in the future',
        'error'
      );
      return false;
    }
    return true;
  };
  const columns: GridEnrichedColDef[] = [
    {
      field: 'duration',
      headerName: t('duration'),
      description: t('duration'),
      width: 150,
      valueGetter: (params: GridValueGetterParams<number>) =>
        getHMSString(params.value)
    },
    {
      field: 'startsOn',
      headerName: t('started_on'),
      description: t('started_on'),
      width: 150,
      valueGetter: (params: GridValueGetterParams<string>) =>
        getFormattedDate(params.value)
    },
    {
      field: 'actions',
      type: 'actions',
      headerName: t('actions'),
      description: t('actions'),
      getActions: (params: GridRowParams) => {
        let actions = [
          <GridActionsCellItem
            key="edit"
            icon={<EditTwoToneIcon fontSize="small" color="primary" />}
            onClick={() => handleEdit(Number(params.id))}
            label={t('edit_downtime')}
          />,
          <GridActionsCellItem
            key="delete"
            icon={<DeleteTwoToneIcon fontSize="small" color="error" />}
            onClick={() => handleDelete(Number(params.id))}
            label={t('remove_downtime')}
          />
        ];
        if (!hasEditPermission(PermissionEntity.ASSETS, asset)) actions = [];
        return actions;
      }
    }
  ];
  const fields: Array<IField> = [
    { name: 'startsOn', type: 'date', label: t('started_on') },
    {
      name: 'hours',
      type: 'number',
      label: t('hours'),
      placeholder: t('hours'),
      required: true,
      midWidth: true
    },
    {
      name: 'minutes',
      type: 'number',
      label: t('minutes'),
      placeholder: t('minutes'),
      required: true,
      midWidth: true
    }
  ];
  const shape = {
    startsOn: Yup.string().required(t('required_startsOn')),
    hours: Yup.number().required(t('required_hours')),
    minutes: Yup.number().required(t('required_minutes'))
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
          {t('add_downtime')}
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
            submitText={t('add')}
            values={{}}
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              if (verifyValues(values))
                return dispatch(
                  createAssetDowntime(asset.id, {
                    ...values,
                    duration: values.hours * 60 * 60 + values.minutes * 60
                  })
                )
                  .then(onCreationSuccess)
                  .catch(onCreationFailure);
            }}
          />
        </Box>
      </DialogContent>
    </Dialog>
  );
  const renderEditModal = () => (
    <Dialog
      fullWidth
      maxWidth="md"
      open={openEditModal}
      onClose={() => setOpenEditModal(false)}
    >
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {t('Edit Downtime')}
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
              ...currentDowntime,
              hours: currentDowntime
                ? getHoursAndMinutesAndSeconds(currentDowntime.duration)[0]
                : 0,
              minutes: currentDowntime
                ? getHoursAndMinutesAndSeconds(currentDowntime.duration)[1]
                : 0
            }}
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              if (verifyValues(values))
                return dispatch(
                  createAssetDowntime(asset.id, {
                    ...values,
                    duration: values.hours * 60 * 60 + values.minutes * 60
                  })
                )
                  .then(onEditSuccess)
                  .catch(onEditFailure);
            }}
          />
        </Box>
      </DialogContent>
    </Dialog>
  );
  return (
    <Box sx={{ px: 4 }}>
      <Grid container spacing={2}>
        <Grid item xs={12}>
          <Card sx={{ p: 2 }}>
            <Box sx={{ height: 550, width: '95%' }}>
              <Stack direction="row" justifyContent="space-between" py={3}>
                <Select
                  value={asset?.status ?? 'OPERATIONAL'}
                  onChange={(event) => {
                    dispatch(
                      editAsset(asset.id, {
                        ...asset,
                        status: event.target.value as AssetStatus
                      })
                    );
                  }}
                >
                  {assetStatuses.map((assetStatusConfig) => (
                    <MenuItem
                      key={assetStatusConfig.status}
                      value={assetStatusConfig.status}
                    >
                      {t(assetStatusConfig.status)}
                    </MenuItem>
                  ))}
                </Select>
                {hasEditPermission(PermissionEntity.ASSETS, asset) && (
                  <Button
                    startIcon={<AddTwoToneIcon />}
                    variant="contained"
                    onClick={() => setOpenAddModal(true)}
                  >
                    {t('add_downtime')}
                  </Button>
                )}
              </Stack>
              <CustomDataGrid
                columns={columns}
                rows={downtimes.filter((downtime) => downtime.duration)}
                components={{
                  Toolbar: GridToolbar
                }}
                onRowClick={(params) => null}
                initialState={{
                  columns: {
                    columnVisibilityModel: {}
                  }
                }}
              />
            </Box>
          </Card>
        </Grid>
      </Grid>
      {renderAddModal()}
      {renderEditModal()}
    </Box>
  );
};

export default AssetDowntimes;
