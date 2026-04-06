import {
  Box,
  Button,
  Checkbox,
  Chip,
  CircularProgress,
  Dialog,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  FormHelperText,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  OutlinedInput,
  Select,
  Slide,
  Stack,
  styled,
  TextField,
  Tooltip,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import {
  forwardRef,
  ReactElement,
  Ref,
  useContext,
  useEffect,
  useState
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
  EVENT_ASKS_ASSET_STATUSES,
  EVENT_ASKS_PART_FIELDS,
  EVENT_ASKS_SERIALIZE,
  EVENT_ASKS_WO_CATEGORIES,
  EVENT_ASKS_WO_FIELDS,
  EVENT_ASKS_WO_STATUSES,
  EVENT_ASKS_WR_APPROVED,
  PartField,
  WebhookEndpointPostDTO,
  WebhookEndpointShowDTO,
  WebhookEvent,
  WOField,
  WEBHOOK_EVENTS,
  WO_FIELDS,
  PART_FIELDS,
  getWebhookEventLabelKey
} from '../../../../models/owns/webhookEndpoint';
import CustomDatagrid2, {
  CustomDatagridColumn2
} from '../../components/CustomDatagrid2';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import CloseIcon from '@mui/icons-material/Close';
import ConfirmDialog from '../../components/ConfirmDialog';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { Form, Formik } from 'formik';
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
  const [loadingCreate, setLoadingCreate] = useState(false);
  const [openDeleteDialog, setOpenDeleteDialog] = useState(false);
  const [currentEndpoint, setCurrentEndpoint] =
    useState<WebhookEndpointShowDTO | null>(null);
  const [rotatingSecretId, setRotatingSecretId] = useState<number | null>(null);

  const handleOpenCreateModal = () => setOpenCreateModal(true);
  const handleCloseCreateModal = () => setOpenCreateModal(false);

  const handleCreateEndpoint = async (values: WebhookEndpointPostDTO) => {
    setLoadingCreate(true);
    try {
      const result = await dispatch(addWebhookEndpoint(values));
      if (result) {
        showSnackBar(t('webhook_endpoint_created_success'), 'success');
        handleCloseCreateModal();
      }
    } finally {
      setLoadingCreate(false);
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

  const getEventLabel = (event: WebhookEvent) =>
    t(getWebhookEventLabelKey(event));

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
      cell: (info) => getEventLabel(info.getValue() as WebhookEvent)
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
              approved: null,
              workOrderCategories: [] as { id: number; name: string }[],
              woFields: [] as WOField[],
              partFields: [] as PartField[],
              serialize: false
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
              const showAssetStatuses =
                selectedEvent &&
                EVENT_ASKS_ASSET_STATUSES.includes(selectedEvent);
              const showWoStatuses =
                selectedEvent && EVENT_ASKS_WO_STATUSES.includes(selectedEvent);
              const showWrApproved =
                selectedEvent && EVENT_ASKS_WR_APPROVED.includes(selectedEvent);
              const showWoCategories =
                selectedEvent &&
                EVENT_ASKS_WO_CATEGORIES.includes(selectedEvent);
              const showWoFields =
                selectedEvent && EVENT_ASKS_WO_FIELDS.includes(selectedEvent);
              const showPartFields =
                selectedEvent && EVENT_ASKS_PART_FIELDS.includes(selectedEvent);
              const showSerialize =
                selectedEvent && EVENT_ASKS_SERIALIZE.includes(selectedEvent);

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
                            {WEBHOOK_EVENTS.map((event) => (
                              <MenuItem key={event} value={event}>
                                {getEventLabel(event)}
                              </MenuItem>
                            ))}
                          </Select>
                        </FormControl>
                      </Grid>

                      {/* Asset Statuses */}
                      {showAssetStatuses && (
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

                      {/* Work Order Statuses */}
                      {showWoStatuses && (
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

                      {/* Work Request Approved Checkbox */}
                      {showWrApproved && (
                        <Grid item xs={12}>
                          <FormControlLabel
                            control={
                              <Checkbox
                                checked={values.approved}
                                onChange={(e) =>
                                  setFieldValue('approved', e.target.checked)
                                }
                              />
                            }
                            label={t(
                              'webhook_endpoint_work_request_approved_only'
                            )}
                          />
                          <FormHelperText>
                            {t(
                              'webhook_endpoint_work_request_approved_only_desc'
                            )}
                          </FormHelperText>
                        </Grid>
                      )}

                      {/* Work Order Categories */}
                      {showWoCategories && (
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
                                const selectedIds = e.target.value as number[];
                                const selectedCategories = categoryList.filter(
                                  (c: Category) => selectedIds.includes(c.id)
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
                                      <Chip key={id} label={cat?.name || id} />
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
                                      label={t(
                                        WO_FIELDS.find((f) => f.value === value)
                                          ?.labelKey || value
                                      )}
                                    />
                                  ))}
                                </Box>
                              )}
                            >
                              {WO_FIELDS.map((field) => (
                                <MenuItem key={field.value} value={field.value}>
                                  {t(field.labelKey)}
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
                                      label={t(
                                        PART_FIELDS.find(
                                          (f) => f.value === value
                                        )?.labelKey || value
                                      )}
                                    />
                                  ))}
                                </Box>
                              )}
                            >
                              {PART_FIELDS.map((field) => (
                                <MenuItem key={field.value} value={field.value}>
                                  {t(field.labelKey)}
                                </MenuItem>
                              ))}
                            </Select>
                          </FormControl>
                        </Grid>
                      )}

                      {/* Serialize Checkbox */}
                      {showSerialize && (
                        <Grid item xs={12}>
                          <FormControlLabel
                            control={
                              <Checkbox
                                checked={values.serialize}
                                onChange={(e) =>
                                  setFieldValue('serialize', e.target.checked)
                                }
                              />
                            }
                            label={t('webhook_endpoint_serialize')}
                          />
                          <FormHelperText>
                            {t('webhook_endpoint_serialize_description')}
                          </FormHelperText>
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
                      <Button
                        type="submit"
                        variant="contained"
                        disabled={loadingCreate}
                      >
                        {loadingCreate ? (
                          <CircularProgress size={24} color="inherit" />
                        ) : (
                          t('create')
                        )}
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
