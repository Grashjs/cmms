import {
  Box,
  Button,
  Dialog,
  Grid,
  Slide,
  styled,
  Typography,
  TextField,
  DialogContent,
  DialogTitle,
  IconButton,
  InputAdornment,
  FormHelperText,
  MenuItem,
  Chip,
  FormControl,
  InputLabel,
  Select,
  OutlinedInput,
  Stack,
  Tooltip,
  CircularProgress
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import {
  forwardRef,
  ReactElement,
  Ref,
  useState,
  useContext,
  useEffect
} from 'react';
import { TransitionProps } from '@mui/material/transitions';
import { useDispatch, useSelector } from '../../../../store';
import {
  addWebhookEndpoint,
  deleteWebhookEndpoint,
  getWebhookEndpoints,
  rotateSecret
} from '../../../../slices/webhookEndpoint';
import {
  WebhookEndpointShowDTO,
  WebhookEndpointPostDTO,
  WebhookEvent,
  WOField,
  PartField,
  CHANGE_EVENTS,
  EVENT_REQUIRES_STATUS_FILTER
} from '../../../../models/owns/webhookEndpoint';
import CustomDatagrid2, {
  CustomDatagridColumn2
} from '../../components/CustomDatagrid2';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import CloseIcon from '@mui/icons-material/Close';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import ConfirmDialog from '../../components/ConfirmDialog';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { Formik, Form, Field } from 'formik';
import * as Yup from 'yup';
import { CompanySettingsContext } from '../../../../contexts/CompanySettingsContext';
import { getCategories } from '../../../../slices/category';
import Category from '../../../../models/owns/category';
import { assetStatuses } from '../../../../models/owns/asset';
import { RotateLeft } from '@mui/icons-material';

const DialogWrapper = styled(Dialog)(
  () => `
        .MuiDialog-paper {
          overflow: visible;
        }
  `
);

const Transition = forwardRef(function Transition(
  props: TransitionProps & { children: ReactElement<any, any> },
  ref: Ref<unknown>
) {
  return <Slide direction="down" ref={ref} {...props} />;
});

const WEBHOOK_EVENT_LABELS: Record<WebhookEvent, string> = {
  ASSET_STATUS_CHANGE: 'WEBHOOK_ASSET_STATUS_CHANGE',
  METER_TRIGGER_STATUS_CHANGE: 'WEBHOOK_METER_TRIGGER_STATUS_CHANGE',
  NEW_ASSET: 'WEBHOOK_NEW_ASSET',
  NEW_CATEGORY_ON_WORK_ORDER: 'WEBHOOK_NEW_CATEGORY_ON_WORK_ORDER',
  NEW_COMMENT_ON_WORK_ORDER: 'WEBHOOK_NEW_COMMENT_ON_WORK_ORDER',
  NEW_LOCATION: 'WEBHOOK_NEW_LOCATION',
  NEW_PART: 'WEBHOOK_NEW_PART',
  NEW_PURCHASE_ORDER: 'WEBHOOK_NEW_PURCHASE_ORDER',
  NEW_VENDOR: 'WEBHOOK_NEW_VENDOR',
  NEW_WORK_ORDER: 'WEBHOOK_NEW_WORK_ORDER',
  NEW_REQUEST: 'WEBHOOK_NEW_REQUEST',
  PART_CHANGE: 'WEBHOOK_PART_CHANGE',
  PART_DELETE: 'WEBHOOK_PART_DELETE',
  PART_QUANTITY_CHANGED: 'WEBHOOK_PART_QUANTITY_CHANGED',
  PURCHASE_ORDER_CHANGE: 'WEBHOOK_PURCHASE_ORDER_CHANGE',
  PURCHASE_ORDER_STATUS_CHANGE: 'WEBHOOK_PURCHASE_ORDER_STATUS_CHANGE',
  WORK_ORDER_CHANGE: 'WEBHOOK_WORK_ORDER_CHANGE',
  WORK_ORDER_DELETE: 'WEBHOOK_WORK_ORDER_DELETE',
  WORK_ORDER_OVERDUE: 'WEBHOOK_WORK_ORDER_OVERDUE',
  WORK_ORDER_STATUS_CHANGE: 'WEBHOOK_WORK_ORDER_STATUS_CHANGE',
  WORK_REQUEST_STATUS_CHANGE: 'WEBHOOK_WORK_REQUEST_STATUS_CHANGE'
};

const WO_FIELD_LABELS: Record<WOField, string> = {
  ASSET: 'asset',
  ASSIGNEES: 'assigned_to',
  CATEGORY: 'category',
  DESCRIPTION: 'description',
  DUE_DATE: 'due_date',
  ESTIMATED_DURATION: 'estimated_duration',
  LOCATION: 'location',
  PARTS: 'parts',
  PRIORITY: 'priority',
  TITLE: 'title',
  TEAM: 'team',
  CUSTOMERS: 'customers'
};

const PART_FIELD_LABELS: Record<PartField, string> = {
  NAME: 'name',
  COST: 'cost',
  ASSIGNED_TO: 'assigned_to',
  BARCODE: 'barcode',
  DESCRIPTION: 'description',
  CATEGORY: 'category',
  QUANTITY: 'quantity',
  AREA: 'area',
  ADDITIONAL_INFOS: 'additional_information',
  NON_STOCK: 'non_stock',
  CUSTOMERS: 'customers',
  VENDORS: 'vendors',
  MIN_QUANTITY: 'min_quantity',
  TEAMS: 'teams',
  ASSETS: 'assets',
  MULTI_PARTS: 'multi_parts',
  UNIT: 'unit'
};

function Webhooks() {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { webhookEndpoints, loadingGet } = useSelector(
    (state) => state.webhookEndpoints
  );
  const { getUserNameById, getFormattedDate } = useContext(
    CompanySettingsContext
  );
  const { categories } = useSelector((state) => state.categories);
  const categoryList = categories['work-order-categories'] || [];

  const [openCreateModal, setOpenCreateModal] = useState(false);
  const [openDeleteDialog, setOpenDeleteDialog] = useState(false);
  const [currentEndpoint, setCurrentEndpoint] =
    useState<WebhookEndpointShowDTO | null>(null);
  const [rotatingSecretId, setRotatingSecretId] = useState<number | null>(null);

  const handleOpenCreateModal = () => {
    setOpenCreateModal(true);
  };

  const handleCloseCreateModal = () => {
    setOpenCreateModal(false);
  };

  const handleCreateEndpoint = async (values: WebhookEndpointPostDTO) => {
    const result = await dispatch(addWebhookEndpoint(values));
    if (result) {
      showSnackBar(t('webhook_endpoint_created_success'), 'success');
      handleCloseCreateModal();
    }
  };

  const handleOpenDeleteDialog = (endpoint: WebhookEndpointShowDTO) => {
    setCurrentEndpoint(endpoint);
    setOpenDeleteDialog(true);
  };

  const handleCloseDeleteDialog = () => {
    setOpenDeleteDialog(false);
    setCurrentEndpoint(null);
  };

  const handleDeleteEndpoint = () => {
    if (currentEndpoint) {
      dispatch(deleteWebhookEndpoint(currentEndpoint.id));
      showSnackBar(t('webhook_endpoint_deleted_success'), 'success');
      handleCloseDeleteDialog();
    }
  };

  const handleRotateSecret = async (id: number) => {
    setRotatingSecretId(id);
    const newSecret = await dispatch(rotateSecret(id));
    setRotatingSecretId(null);
    if (newSecret) {
      showSnackBar(t('webhook_endpoint_secret_rotated'), 'success');
    }
  };

  const copySecret = (secret: string) => {
    navigator.clipboard.writeText(secret);
    showSnackBar(t('webhook_endpoint_secret_copied'), 'success');
  };

  useEffect(() => {
    dispatch(getWebhookEndpoints());
  }, []);

  useEffect(() => {
    dispatch(getCategories('work-order-categories'));
  }, []);

  const allWoFields: WOField[] = Object.keys(WO_FIELD_LABELS) as WOField[];
  const allPartFields: PartField[] = Object.keys(
    PART_FIELD_LABELS
  ) as PartField[];

  const columns: CustomDatagridColumn2<WebhookEndpointShowDTO>[] = [
    {
      header: t('id'),
      accessorKey: 'id',
      cell: (info) => info.getValue() as number,
      size: 70
    },
    {
      header: t('webhook_endpoint_url'),
      accessorKey: 'url',
      cell: (info) => (
        <Typography
          variant="body2"
          sx={{
            maxWidth: 250,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap'
          }}
          title={info.getValue() as string}
        >
          {info.getValue() as string}
        </Typography>
      )
    },
    {
      header: t('user'),
      accessorKey: 'createdBy',
      cell: (info) => {
        const createdById = info.getValue() as number;
        const name = getUserNameById(createdById);
        return name || '-';
      }
    },
    {
      header: t('webhook_endpoint_type'),
      accessorKey: 'event',
      cell: (info) => {
        const event = info.getValue() as WebhookEvent;
        return event ? t(WEBHOOK_EVENT_LABELS[event]) : '-';
      }
    },
    {
      header: t('webhook_endpoint_last_triggered'),
      accessorKey: 'lastTriggeredAt',
      cell: (info) => {
        const val = info.getValue() as string | null;
        return val
          ? getFormattedDate(val)
          : t('webhook_endpoint_never_triggered');
      },
      size: 160
    },
    {
      header: t('webhook_endpoint_secret'),
      accessorKey: 'secret',
      cell: (info) => {
        const secret = info.getValue() as string;
        const row = info.row.original;
        const isRotating = rotatingSecretId === row.id;

        return (
          <Stack direction="row" spacing={0.5} alignItems="center">
            <IconButton
              size="small"
              onClick={(e) => {
                e.stopPropagation();
                copySecret(secret);
              }}
            >
              <ContentCopyIcon fontSize="small" />
            </IconButton>
            <Tooltip title={t('webhook_endpoint_rotate_secret')}>
              <IconButton
                size="small"
                onClick={(e) => {
                  e.stopPropagation();
                  handleRotateSecret(row.id);
                }}
                disabled={isRotating}
              >
                {isRotating ? <CircularProgress size={18} /> : <RotateLeft />}
              </IconButton>
            </Tooltip>
          </Stack>
        );
      },
      size: 170
    },
    {
      header: t('actions'),
      cell: (info) => (
        <IconButton
          color="error"
          onClick={() => handleOpenDeleteDialog(info.row.original)}
        >
          <DeleteTwoToneIcon />
        </IconButton>
      ),
      size: 80
    }
  ];

  return (
    <Box>
      <Box display="flex" justifyContent="flex-end" mb={3}>
        <Button
          variant="contained"
          startIcon={<AddTwoToneIcon />}
          onClick={handleOpenCreateModal}
        >
          {t('create_webhook_endpoint')}
        </Button>
      </Box>

      <CustomDatagrid2
        columns={columns}
        data={webhookEndpoints || []}
        loading={loadingGet}
        notClickable
        hidePagination
      />

      {/* Create Webhook Endpoint Modal */}
      <DialogWrapper
        open={openCreateModal}
        maxWidth="md"
        fullWidth
        TransitionComponent={Transition}
        keepMounted
        onClose={handleCloseCreateModal}
      >
        <DialogTitle>
          <Box
            display="flex"
            justifyContent="space-between"
            alignItems="center"
          >
            <Typography variant="h4">{t('create_webhook_endpoint')}</Typography>
            <IconButton onClick={handleCloseCreateModal}>
              <CloseIcon />
            </IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          <Formik
            initialValues={{
              url: '',
              event: '' as WebhookEvent | '',
              assetStatuses: [] as string[],
              workOrderStatuses: [] as string[],
              workOrderCategories: [] as { id: number; name: string }[],
              woFields: [] as WOField[],
              partFields: [] as PartField[]
            }}
            validationSchema={Yup.object({
              url: Yup.string()
                .matches(/^https?:\/\//, t('invalid_url'))
                .required(t('webhook_endpoint_url') + ' ' + t('required'))
            })}
            onSubmit={handleCreateEndpoint}
          >
            {({
              errors,
              touched,
              values,
              handleChange,
              handleSubmit,
              setFieldValue
            }) => {
              const selectedEvent = values.event as WebhookEvent | null;
              const showStatusFilter =
                selectedEvent && EVENT_REQUIRES_STATUS_FILTER[selectedEvent];
              const showWoFields =
                selectedEvent &&
                CHANGE_EVENTS.includes(selectedEvent as WebhookEvent);
              const showPartFields =
                selectedEvent &&
                [
                  'NEW_PART',
                  'PART_CHANGE',
                  'PART_DELETE',
                  'PART_QUANTITY_CHANGED'
                ].includes(selectedEvent);

              const isChangeEvent = CHANGE_EVENTS.includes(
                selectedEvent as WebhookEvent
              );

              return (
                <Form onSubmit={handleSubmit}>
                  <Box py={2}>
                    <Grid container spacing={3}>
                      {/* URL */}
                      <Grid item xs={12}>
                        <TextField
                          name="url"
                          label={t('webhook_endpoint_url')}
                          placeholder={t('webhook_endpoint_url_placeholder')}
                          value={values.url}
                          onChange={handleChange}
                          error={touched.url && Boolean(errors.url)}
                          helperText={touched.url && errors.url}
                          fullWidth
                        />
                      </Grid>

                      {/* Event */}
                      <Grid item xs={12}>
                        <FormControl fullWidth>
                          <InputLabel>{t('webhook_endpoint_event')}</InputLabel>
                          <Select
                            name="event"
                            value={values.event}
                            onChange={(e) =>
                              setFieldValue('event', e.target.value)
                            }
                            input={
                              <OutlinedInput
                                label={t('webhook_endpoint_event')}
                              />
                            }
                          >
                            {(
                              Object.keys(
                                WEBHOOK_EVENT_LABELS
                              ) as WebhookEvent[]
                            ).map((event) => (
                              <MenuItem key={event} value={event}>
                                {t(WEBHOOK_EVENT_LABELS[event])}
                              </MenuItem>
                            ))}
                          </Select>
                        </FormControl>
                      </Grid>

                      {/* Asset Statuses (conditionally shown) */}
                      {showStatusFilter &&
                        selectedEvent === 'ASSET_STATUS_CHANGE' && (
                          <Grid item xs={12}>
                            <FormControl fullWidth>
                              <InputLabel>
                                {t('webhook_endpoint_asset_statuses')}
                              </InputLabel>
                              <Select
                                multiple
                                value={values.assetStatuses}
                                onChange={(e) =>
                                  setFieldValue('assetStatuses', e.target.value)
                                }
                                input={
                                  <OutlinedInput
                                    label={t('webhook_endpoint_asset_statuses')}
                                  />
                                }
                                renderValue={(selected) => (
                                  <Box
                                    sx={{
                                      display: 'flex',
                                      flexWrap: 'wrap',
                                      gap: 0.5
                                    }}
                                  >
                                    {(selected as string[]).map((value) => (
                                      <Chip key={value} label={t(value)} />
                                    ))}
                                  </Box>
                                )}
                              >
                                {assetStatuses.map((s) => (
                                  <MenuItem key={s.status} value={s.status}>
                                    {t(s.status)}
                                  </MenuItem>
                                ))}
                              </Select>
                            </FormControl>
                          </Grid>
                        )}

                      {/* Work Order Statuses (conditionally shown) */}
                      {showStatusFilter &&
                        (selectedEvent === 'WORK_ORDER_STATUS_CHANGE' ||
                          selectedEvent === 'WORK_ORDER_CHANGE') && (
                          <Grid item xs={12}>
                            <FormControl fullWidth>
                              <InputLabel>
                                {t('webhook_endpoint_wo_statuses')}
                              </InputLabel>
                              <Select
                                multiple
                                value={values.workOrderStatuses}
                                onChange={(e) =>
                                  setFieldValue(
                                    'workOrderStatuses',
                                    e.target.value
                                  )
                                }
                                input={
                                  <OutlinedInput
                                    label={t('webhook_endpoint_wo_statuses')}
                                  />
                                }
                                renderValue={(selected) => (
                                  <Box
                                    sx={{
                                      display: 'flex',
                                      flexWrap: 'wrap',
                                      gap: 0.5
                                    }}
                                  >
                                    {(selected as string[]).map((value) => (
                                      <Chip key={value} label={t(value)} />
                                    ))}
                                  </Box>
                                )}
                              >
                                {[
                                  'OPEN',
                                  'IN_PROGRESS',
                                  'ON_HOLD',
                                  'COMPLETE'
                                ].map((s) => (
                                  <MenuItem key={s} value={s}>
                                    {t(s)}
                                  </MenuItem>
                                ))}
                              </Select>
                            </FormControl>
                          </Grid>
                        )}

                      {/* Work Order Categories (conditionally shown) */}
                      {isChangeEvent &&
                        (selectedEvent === 'NEW_WORK_ORDER' ||
                          selectedEvent === 'WORK_ORDER_CHANGE') && (
                          <Grid item xs={12}>
                            <FormControl fullWidth>
                              <InputLabel>
                                {t('webhook_endpoint_wo_categories')}
                              </InputLabel>
                              <Select
                                multiple
                                value={values.workOrderCategories.map(
                                  (c) => c.id
                                )}
                                onChange={(e) => {
                                  const selectedIds = e.target
                                    .value as number[];
                                  const selectedCategories =
                                    categoryList.filter((c: Category) =>
                                      selectedIds.includes(c.id)
                                    );
                                  setFieldValue(
                                    'workOrderCategories',
                                    selectedCategories
                                  );
                                }}
                                input={
                                  <OutlinedInput
                                    label={t('webhook_endpoint_wo_categories')}
                                  />
                                }
                                renderValue={(selected) => (
                                  <Box
                                    sx={{
                                      display: 'flex',
                                      flexWrap: 'wrap',
                                      gap: 0.5
                                    }}
                                  >
                                    {(selected as number[]).map((id) => {
                                      const cat = categoryList.find(
                                        (c: Category) => c.id === id
                                      );
                                      return (
                                        <Chip
                                          key={id}
                                          label={cat?.name || id}
                                        />
                                      );
                                    })}
                                  </Box>
                                )}
                              >
                                {categoryList.map((cat: Category) => (
                                  <MenuItem key={cat.id} value={cat.id}>
                                    {cat.name}
                                  </MenuItem>
                                ))}
                              </Select>
                            </FormControl>
                          </Grid>
                        )}

                      {/* WO Fields */}
                      {showWoFields && (
                        <Grid item xs={12}>
                          <FormControl fullWidth>
                            <InputLabel>
                              {t('webhook_endpoint_wo_fields')}
                            </InputLabel>
                            <Select
                              multiple
                              value={values.woFields}
                              onChange={(e) =>
                                setFieldValue('woFields', e.target.value)
                              }
                              input={
                                <OutlinedInput
                                  label={t('webhook_endpoint_wo_fields')}
                                />
                              }
                              renderValue={(selected) => (
                                <Box
                                  sx={{
                                    display: 'flex',
                                    flexWrap: 'wrap',
                                    gap: 0.5
                                  }}
                                >
                                  {(selected as WOField[]).map((value) => (
                                    <Chip
                                      key={value}
                                      label={t(WO_FIELD_LABELS[value])}
                                    />
                                  ))}
                                </Box>
                              )}
                            >
                              {allWoFields.map((field) => (
                                <MenuItem key={field} value={field}>
                                  {t(WO_FIELD_LABELS[field])}
                                </MenuItem>
                              ))}
                            </Select>
                          </FormControl>
                        </Grid>
                      )}

                      {/* Part Fields */}
                      {showPartFields && (
                        <Grid item xs={12}>
                          <FormControl fullWidth>
                            <InputLabel>
                              {t('webhook_endpoint_part_fields')}
                            </InputLabel>
                            <Select
                              multiple
                              value={values.partFields}
                              onChange={(e) =>
                                setFieldValue('partFields', e.target.value)
                              }
                              input={
                                <OutlinedInput
                                  label={t('webhook_endpoint_part_fields')}
                                />
                              }
                              renderValue={(selected) => (
                                <Box
                                  sx={{
                                    display: 'flex',
                                    flexWrap: 'wrap',
                                    gap: 0.5
                                  }}
                                >
                                  {(selected as PartField[]).map((value) => (
                                    <Chip
                                      key={value}
                                      label={t(PART_FIELD_LABELS[value])}
                                    />
                                  ))}
                                </Box>
                              )}
                            >
                              {allPartFields.map((field) => (
                                <MenuItem key={field} value={field}>
                                  {t(PART_FIELD_LABELS[field])}
                                </MenuItem>
                              ))}
                            </Select>
                          </FormControl>
                        </Grid>
                      )}
                    </Grid>

                    <Box mt={3} display="flex" justifyContent="flex-end">
                      <Button
                        variant="text"
                        onClick={handleCloseCreateModal}
                        sx={{ mr: 1 }}
                      >
                        {t('cancel')}
                      </Button>
                      <Button type="submit" variant="contained">
                        {t('create')}
                      </Button>
                    </Box>
                  </Box>
                </Form>
              );
            }}
          </Formik>
        </DialogContent>
      </DialogWrapper>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={openDeleteDialog}
        onCancel={handleCloseDeleteDialog}
        onConfirm={handleDeleteEndpoint}
        confirmText={t('delete')}
        question={t('delete_webhook_endpoint_confirm')}
      />
    </Box>
  );
}

export default Webhooks;
