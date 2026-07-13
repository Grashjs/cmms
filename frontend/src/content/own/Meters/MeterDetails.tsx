import {
  Box,
  Button,
  Divider,
  Grid,
  IconButton,
  List,
  ListItem,
  ListItemText,
  Stack,
  Tab,
  Tabs,
  Typography,
  useTheme
} from '@mui/material';
import { ChangeEvent, useContext, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import Meter from '../../../models/owns/meter';
import * as Yup from 'yup';
import Form from '../components/form';
import { IField } from '../type';
import { useDispatch, useSelector } from '../../../store';
import { createReading, getReadings } from '../../../slices/reading';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import {
  deleteWorkOrderMeterTrigger,
  getWorkOrderMeterTriggers
} from '../../../slices/workOrderMeterTrigger';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import AddTriggerModal from './AddTriggerModal';
import EditTriggerModal from './EditTriggerModal';
import WorkOrderMeterTrigger from '../../../models/owns/workOrderMeterTrigger';
import useAuth from '../../../hooks/useAuth';
import { PermissionEntity } from '../../../models/owns/role';
import ImageViewer from 'react-simple-image-viewer';
import { canAddReading } from '../../../utils/overall';

interface MeterDetailsProps {
  meter: Meter;
  handleOpenUpdate: () => void;
  handleOpenDelete: () => void;
  onNewReading: () => void;
}
export default function MeterDetails(props: MeterDetailsProps) {
  const { meter, handleOpenUpdate, handleOpenDelete, onNewReading } = props;
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { hasEditPermission, hasDeletePermission } = useAuth();
  const [currentTab, setCurrentTab] = useState<string>('details');
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const theme = useTheme();
  const { readingsByMeter } = useSelector((state) => state.readings);
  const { metersTriggers } = useSelector(
    (state) => state.workOrderMeterTriggers
  );
  const [openAddTriggerModal, setOpenAddTriggerModal] =
    useState<boolean>(false);
  const [openEditTriggerModal, setOpenEditTriggerModal] =
    useState<boolean>(false);
  const [currentWorkOrderMeterTrigger, setCurrentWorkOrderMeterTrigger] =
    useState<WorkOrderMeterTrigger>();
  const [isImageViewerOpen, setIsImageViewerOpen] = useState<boolean>(false);

  const currentMeterTriggers = metersTriggers[meter?.id] ?? [];
  const currentMeterReadings = readingsByMeter[meter?.id] ?? [];
  const tabs = [
    { value: 'details', label: t('details') },
    { value: 'history', label: t('history') }
  ];

  useEffect(() => {
    dispatch(getWorkOrderMeterTriggers(meter.id));
    dispatch(getReadings(meter.id));
  }, [meter.id]);

  const handleTabsChange = (_event: ChangeEvent<{}>, value: string): void => {
    setCurrentTab(value);
  };
  const BasicField = ({
    label,
    value
  }: {
    label: string | number;
    value: string | number;
  }) => {
    return value ? (
      <Grid item xs={12} lg={6}>
        <Typography variant="h6" sx={{ color: theme.colors.alpha.black[70] }}>
          {label}
        </Typography>
        <Typography variant="h6">{value}</Typography>
      </Grid>
    ) : null;
  };
  const fieldsToRender = (meter: Meter): { label: string; value: any }[] => [
    {
      label: t('location_name'),
      value: meter.location?.name
    },
    {
      label: t('asset_name'),
      value: meter.asset.name
    },
    {
      label: t('reading_frequency'),
      value: t('every_frequency_days', { frequency: meter.updateFrequency })
    },
    {
      label: t('category'),
      value: meter.meterCategory?.name
    },
    {
      label: t('assigned_to'),
      value: meter.users.reduce(
        (acc, user, index) =>
          acc + `${index !== 0 ? ',' : ''} ${user.firstName} ${user.lastName}`,
        ''
      )
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
  const shape = {
    value: Yup.number().required(t('required_reading_value'))
  };
  return (
    <Grid
      container
      justifyContent="center"
      alignItems="stretch"
      spacing={2}
      padding={4}
    >
      <Grid
        item
        xs={12}
        display="flex"
        flexDirection="row"
        justifyContent="space-between"
      >
        <Box>
          <Typography variant="h2">{meter?.name}</Typography>
        </Box>
        <Box>
          {hasEditPermission(PermissionEntity.METERS, meter) && (
            <IconButton onClick={handleOpenUpdate} style={{ marginRight: 10 }}>
              <EditTwoToneIcon color="primary" />
            </IconButton>
          )}
          {hasDeletePermission(PermissionEntity.METERS, meter) && (
            <IconButton onClick={handleOpenDelete}>
              <DeleteTwoToneIcon color="error" />
            </IconButton>
          )}
        </Box>
      </Grid>
      <Divider />
      <Grid item xs={12}>
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
      </Grid>
      <Grid item xs={12}>
        {currentTab === 'details' && (
          <Box>
            {canAddReading(meter) &&
            hasEditPermission(PermissionEntity.METERS, meter) ? (
              <Form
                fields={fields}
                validation={Yup.object().shape(shape)}
                submitText={t('add_reading')}
                values={{ value: 0 }}
                onSubmit={async (values) => {
                  return dispatch(createReading(meter.id, values)).then(
                    onNewReading
                  );
                }}
              />
            ) : (
              !!currentMeterReadings.length && (
                <Box>
                  <Typography variant="h4">{t('last_reading')}</Typography>
                  <Typography>{`${
                    [...currentMeterReadings].reverse()[0].value
                  } ${meter.unit}`}</Typography>
                </Box>
              )
            )}
            {meter.image && (
              <Grid
                item
                xs={12}
                lg={12}
                display="flex"
                justifyContent="center"
                sx={{ mt: 2 }}
              >
                <img
                  src={meter.image.url}
                  style={{ borderRadius: 5, height: 250, cursor: 'pointer' }}
                  onClick={() => setIsImageViewerOpen(true)}
                />
              </Grid>
            )}
            <Typography sx={{ mt: 2, mb: 1 }} variant="h4">
              {t('meter_details')}
            </Typography>
            <Grid container spacing={2}>
              {fieldsToRender(meter).map((field) => (
                <BasicField
                  key={field.label}
                  label={field.label}
                  value={field.value}
                />
              ))}
            </Grid>
            <Typography sx={{ mt: 2, mb: 1 }} variant="h4">
              {t('wo_triggers')}
            </Typography>
            <Grid container spacing={1}>
              <Grid item xs={12} lg={12}>
                <List>
                  {currentMeterTriggers.map((trigger) => (
                    <ListItem
                      key={trigger.id}
                      secondaryAction={
                        <Stack spacing={1} direction="row">
                          <IconButton
                            onClick={() => {
                              setCurrentWorkOrderMeterTrigger(
                                currentMeterTriggers.find(
                                  (t) => t.id === trigger.id
                                )
                              );
                              setOpenEditTriggerModal(true);
                            }}
                          >
                            <EditTwoToneIcon />
                          </IconButton>
                          <IconButton
                            onClick={() => {
                              dispatch(
                                deleteWorkOrderMeterTrigger(
                                  meter.id,
                                  trigger.id
                                )
                              );
                            }}
                          >
                            <DeleteTwoToneIcon color="error" />
                          </IconButton>
                        </Stack>
                      }
                    >
                      <ListItemText
                        primary={trigger.name}
                        secondary={`${
                          trigger.triggerCondition === 'MORE_THAN'
                            ? t('greater_than')
                            : t('lower_than')
                        } ${trigger.value} ${meter.unit}`}
                      />
                    </ListItem>
                  ))}
                </List>
                {hasEditPermission(PermissionEntity.METERS, meter) && (
                  <Button
                    startIcon={<AddTwoToneIcon />}
                    sx={{ my: 1 }}
                    variant="outlined"
                    onClick={() => setOpenAddTriggerModal(true)}
                  >
                    {t('add_trigger')}
                  </Button>
                )}
              </Grid>
            </Grid>
          </Box>
        )}
        {currentTab === 'history' && (
          <List>
            {[...currentMeterReadings].reverse().map((reading) => (
              <ListItem key={reading.id} divider>
                <ListItemText
                  primary={`${reading.value} ${meter.unit}`}
                  secondary={getFormattedDate(reading.createdAt)}
                />
              </ListItem>
            ))}
          </List>
        )}
      </Grid>
      <AddTriggerModal
        open={openAddTriggerModal}
        onClose={() => setOpenAddTriggerModal(false)}
        meter={meter}
      />
      <EditTriggerModal
        open={openEditTriggerModal}
        onClose={() => setOpenEditTriggerModal(false)}
        meter={meter}
        workOrderMeterTrigger={currentWorkOrderMeterTrigger}
      />
      {isImageViewerOpen && (
        <div style={{ zIndex: 100 }}>
          <ImageViewer
            src={[meter.image.url]}
            currentIndex={0}
            onClose={() => setIsImageViewerOpen(false)}
            disableScroll={true}
            backgroundStyle={{
              backgroundColor: 'rgba(0,0,0,0.9)'
            }}
            closeOnClickOutside={true}
          />
        </div>
      )}
    </Grid>
  );
}
