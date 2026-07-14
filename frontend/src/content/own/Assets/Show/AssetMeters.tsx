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
import { GridRowParams, GridToolbar } from '@mui/x-data-grid';
import { GridEnrichedColDef } from '@mui/x-data-grid/models/colDef/gridColDef';
import { AssetDTO } from '../../../../models/owns/asset';
import { useDispatch, useSelector } from '../../../../store';
import { getMetersByAsset } from '../../../../slices/meter';
import { useContext, useEffect, useState } from 'react';
import Meter from '../../../../models/owns/meter';
import { createReading, getReadings } from '../../../../slices/reading';
import { CompanySettingsContext } from '../../../../contexts/CompanySettingsContext';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import Form from '../../components/form';
import * as Yup from 'yup';
import { IField } from '../../type';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { canAddReading } from '../../../../utils/overall';
import useAuth from '../../../../hooks/useAuth';
import { PermissionEntity } from '../../../../models/owns/role';

interface PropsType {
  asset: AssetDTO;
}

const AssetMeters = ({ asset }: PropsType) => {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { metersByAsset, loadingGet: loadingMeters } = useSelector(
    (state) => state.meters
  );
  const { readingsByMeter, loadingGet } = useSelector(
    (state) => state.readings
  );
  const meters = metersByAsset[asset?.id] ?? [];
  const [selectedMeter, setSelectedMeter] = useState<Meter>();
  const { hasEditPermission } = useAuth();
  const { getFormattedDate, getUserNameById } = useContext(
    CompanySettingsContext
  );
  const [openReadingModal, setOpenReadingModal] = useState<boolean>(false);
  const readings = readingsByMeter[selectedMeter?.id] ?? [];
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [canAddReadingInternal, setCanAddReadingInternal] =
    useState<boolean>(true);
  useEffect(() => {
    if (asset) dispatch(getMetersByAsset(asset?.id));
  }, [asset?.id]);

  useEffect(() => {
    if (meters.length) {
      setSelectedMeter(meters[0]);
    }
  }, [meters]);

  useEffect(() => {
    if (!readingsByMeter[selectedMeter?.id] && selectedMeter) {
      dispatch(getReadings(selectedMeter?.id));
    }
  }, [selectedMeter]);

  const onCreationSuccess = () => {
    setOpenReadingModal(false);
    showSnackBar(t('reading_create_success'), 'success');
  };
  const onCreationFailure = (err) =>
    showSnackBar(t('reading_create_failure'), 'error');

  const columns: GridEnrichedColDef[] = [
    {
      field: 'value',
      headerName: t('reading'),
      description: t('reading'),
      width: 150,
      valueGetter: (params) => `${params.value} ${selectedMeter.unit}`
    },
    {
      field: 'createdAt',
      headerName: t('date'),
      description: t('date'),
      width: 150,
      valueGetter: (params) => getFormattedDate(params.value)
    },
    {
      field: 'createdBy',
      headerName: t('added_by'),
      description: t('added_by'),
      width: 150,
      valueGetter: (params) => getUserNameById(params.value)
    }
  ];
  const fields: Array<IField> = [
    {
      name: 'value',
      type: 'number',
      label: t('reading'),
      placeholder: t('enter_meter_value'),
      required: true
    }
  ];
  const renderReadingModal = () => (
    <Dialog
      fullWidth
      maxWidth="md"
      open={openReadingModal}
      onClose={() => setOpenReadingModal(false)}
    >
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {t('add_reading')}
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
            validation={Yup.object().shape({})}
            submitText={t('add')}
            values={{}}
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              return dispatch(createReading(selectedMeter.id, values))
                .then(onCreationSuccess)
                .then(() => setCanAddReadingInternal(false))
                .catch(onCreationFailure);
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
            <Box sx={{ height: 600, width: '95%' }}>
              {!!meters.length && (
                <Stack direction="row" justifyContent="space-between">
                  <Stack
                    direction="row"
                    spacing={1}
                    sx={{ my: 2, alignItems: 'center' }}
                  >
                    <Typography variant="h5">{t('select_meter')}</Typography>
                    <Select
                      value={selectedMeter?.id ?? ''}
                      onChange={(event) => {
                        setSelectedMeter(
                          meters.find(
                            (meter) => meter.id === Number(event.target.value)
                          )
                        );
                      }}
                    >
                      <MenuItem value={''}>{t('select_meter')}</MenuItem>
                      {meters.map((meter) => (
                        <MenuItem key={meter.id} value={meter.id}>
                          {meter.name}
                        </MenuItem>
                      ))}
                    </Select>
                  </Stack>

                  {hasEditPermission(PermissionEntity.ASSETS, asset) && (
                    <Button
                      startIcon={<AddTwoToneIcon />}
                      variant="contained"
                      sx={{ my: 3 }}
                      disabled={
                        !(canAddReading(selectedMeter) && canAddReadingInternal)
                      }
                      onClick={() => setOpenReadingModal(true)}
                    >
                      {t('add_reading')}
                    </Button>
                  )}
                </Stack>
              )}
              <CustomDataGrid
                columns={columns}
                rows={readings}
                components={{
                  Toolbar: GridToolbar
                }}
                loading={loadingMeters || loadingGet}
                onRowClick={(params: GridRowParams<Meter>) => null}
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
      {renderReadingModal()}
    </Box>
  );
};

export default AssetMeters;
