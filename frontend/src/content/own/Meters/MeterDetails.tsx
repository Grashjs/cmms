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
  useTheme,
  CircularProgress
} from '@mui/material';
import {
  ChangeEvent,
  useContext,
  useEffect,
  useState,
  useCallback
} from 'react';
import { useTranslation } from 'react-i18next';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import Meter from '../../../models/owns/meter';
import * as Yup from 'yup';
import Form from '../components/form';
import { getCustomFieldValuesForDetails, IField } from '../type';
import { useDispatch, useSelector } from '../../../store';
import {
  createReading,
  getReadings,
  getHistogramData
} from '../../../slices/reading';
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
import BasicField from '../components/BasicField';
import DateRangePicker from '../components/form/DateRangePicker';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer
} from 'recharts';
import { format } from 'date-fns';
import { subDays } from 'date-fns';
import useDateLocale from '../../../hooks/useDateLocale';

interface MeterDetailsProps {
  meter: Meter;
  handleOpenUpdate: () => void;
  handleOpenDelete: () => void;
  onCopy: (meter: Meter) => void;
  onNewReading: () => void;
}
export default function MeterDetails(props: MeterDetailsProps) {
  const { meter, handleOpenUpdate, handleOpenDelete, onCopy, onNewReading } =
    props;
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { hasEditPermission, hasDeletePermission, hasCreatePermission } =
    useAuth();
  const [currentTab, setCurrentTab] = useState<string>('details');
  const dateLocale = useDateLocale();
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const theme = useTheme();
  const { readingsByMeter, histogramData, loadingHistogram } = useSelector(
    (state) => state.readings
  );
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
  const [dateRange, setDateRange] = useState<[Date, Date]>([
    subDays(new Date(), 7),
    new Date()
  ]);
  const [historyFetched, setHistoryFetched] = useState(false);

  const currentMeterTriggers = metersTriggers[meter?.id] ?? [];
  const currentMeterReadings = readingsByMeter[meter?.id] ?? [];
  const tabs = [
    { value: 'details', label: t('details') },
    { value: 'history', label: t('history') }
  ];

  useEffect(() => {
    dispatch(getWorkOrderMeterTriggers(meter.id));
  }, [meter.id]);

  useEffect(() => {
    dispatch(
      getHistogramData(
        meter.id,
        dateRange[0].toISOString(),
        dateRange[1].toISOString()
      )
    );
  }, [meter.id, dateRange]);

  const handleTabsChange = (_event: ChangeEvent<{}>, value: string): void => {
    setCurrentTab(value);
    if (value === 'history' && !historyFetched) {
      setHistoryFetched(true);
      dispatch(getReadings(meter.id));
    }
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
    },
    ...getCustomFieldValuesForDetails(meter.customFieldValues, getFormattedDate)
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
          {hasCreatePermission(PermissionEntity.METERS) && (
            <IconButton
              style={{ marginRight: 10 }}
              onClick={() => onCopy(meter)}
            >
              <ContentCopyIcon color="primary" />
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
            <Box sx={{ mb: 2 }}>
              <DateRangePicker
                value={dateRange}
                onChange={(range) => setDateRange(range)}
              />
            </Box>
            {loadingHistogram ? (
              <Box display="flex" justifyContent="center" py={4}>
                <CircularProgress />
              </Box>
            ) : histogramData.length > 0 ? (
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={histogramData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis
                    dataKey="date"
                    tickFormatter={(d) =>
                      format(new Date(d), 'MMM dd', { locale: dateLocale })
                    }
                  />
                  <YAxis />
                  <Tooltip
                    labelFormatter={(d) =>
                      format(new Date(d), 'MMM dd, yyyy', {
                        locale: dateLocale
                      })
                    }
                  />
                  <Bar dataKey="value" fill={theme.palette.primary.main} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <Typography
                variant="body1"
                color="textSecondary"
                textAlign="center"
                py={4}
              >
                {t('no_data')}
              </Typography>
            )}
            <Box sx={{ mt: 2 }}>
              {canAddReading(meter) &&
              hasEditPermission(PermissionEntity.METERS, meter) ? (
                <Form
                  fields={fields}
                  validation={Yup.object().shape(shape)}
                  submitText={t('add_reading')}
                  values={{ value: 0 }}
                  onSubmit={async (values) => {
                    return dispatch(createReading(meter.id, values))
                      .then(onNewReading)
                      .then(() =>
                        dispatch(
                          getHistogramData(
                            meter.id,
                            dateRange[0].toISOString(),
                            new Date().toISOString()
                          )
                        )
                      );
                  }}
                />
              ) : null}
            </Box>
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
                <BasicField key={field.label} {...field} />
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
        {currentTab === 'history' &&
          (historyFetched ? (
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
          ) : null)}
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
