import {
  Box,
  Button,
  Chip,
  CircularProgress,
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
  Link,
  Stack
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
  addApiKey,
  deleteApiKey,
  getApiKeys,
  rotateApiKey
} from '../../../../slices/apiKey';
import { ApiKey, ApiKeyPostDTO } from '../../../../models/owns/apiKey';
import { Page, Pageable, SearchCriteria } from '../../../../models/owns/page';
import CustomDatagrid2, {
  CustomDatagridColumn2
} from '../../components/CustomDatagrid2';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import CloseIcon from '@mui/icons-material/Close';
import SyncTwoToneIcon from '@mui/icons-material/SyncTwoTone';
import ConfirmDialog from '../../components/ConfirmDialog';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { Formik, Form, Field } from 'formik';
import * as Yup from 'yup';
import { CompanySettingsContext } from '../../../../contexts/CompanySettingsContext';
import { onOpenApiDocs } from '../../../../utils/overall';
import DateTimePicker from '@mui/lab/DateTimePicker';

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

function ApiKeys() {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { apiKeys, loadingGet } = useSelector((state) => state.apiKeys);
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const [openCreateModal, setOpenCreateModal] = useState(false);
  const [loadingCreate, setLoadingCreate] = useState(false);
  const [openDeleteDialog, setOpenDeleteDialog] = useState(false);
  const [openRotateDialog, setOpenRotateDialog] = useState(false);
  const [currentApiKey, setCurrentApiKey] = useState<ApiKey | null>(null);
  const [pageable, setPageable] = useState<Pageable>({
    page: 0,
    size: 10,
    sort: ['lastUsed,asc']
  });
  const [createdApiKeyCode, setCreatedApiKeyCode] = useState<string | null>(
    null
  );
  const [showCode, setShowCode] = useState(false);
  const [loadingRotate, setLoadingRotate] = useState(false);
  const [rotatedApiKeyCode, setRotatedApiKeyCode] = useState<string | null>(
    null
  );
  const [showRotatedCode, setShowRotatedCode] = useState(false);

  const handleOpenCreateModal = () => {
    setOpenCreateModal(true);
    setCreatedApiKeyCode(null);
    setShowCode(false);
  };

  const handleCloseCreateModal = () => {
    setOpenCreateModal(false);
    setCreatedApiKeyCode(null);
    setShowCode(false);
  };

  const handleCreateApiKey = async (values: ApiKeyPostDTO) => {
    setLoadingCreate(true);
    try {
      const result = await dispatch(addApiKey(values));
      if (result) {
        setCreatedApiKeyCode(result.code || null);
        setShowCode(true);
        showSnackBar(t('api_key_created_success'), 'success');
      }
    } finally {
      setLoadingCreate(false);
    }
  };

  const handleOpenDeleteDialog = (apiKey: ApiKey) => {
    setCurrentApiKey(apiKey);
    setOpenDeleteDialog(true);
  };

  const handleCloseDeleteDialog = () => {
    setOpenDeleteDialog(false);
    setCurrentApiKey(null);
  };

  const handleDeleteApiKey = () => {
    if (currentApiKey) {
      dispatch(deleteApiKey(currentApiKey.id));
      showSnackBar(t('Deleted successfully'), 'success');
      handleCloseDeleteDialog();
    }
  };

  const handleOpenRotateDialog = (apiKey: ApiKey) => {
    setCurrentApiKey(apiKey);
    setOpenRotateDialog(true);
  };

  const handleCloseRotateDialog = () => {
    setOpenRotateDialog(false);
    setCurrentApiKey(null);
    setRotatedApiKeyCode(null);
    setShowRotatedCode(false);
  };

  const handleRotateApiKey = async () => {
    if (!currentApiKey) return;
    setLoadingRotate(true);
    try {
      const result: ApiKey = await dispatch(rotateApiKey(currentApiKey.id));
      if (result) {
        setRotatedApiKeyCode(result.code || null);
        setShowRotatedCode(true);
        showSnackBar(t('api_key_rotated_success'), 'success');
      }
    } finally {
      setLoadingRotate(false);
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    showSnackBar(t('api_key_code_copied'), 'success');
  };

  useEffect(() => {
    dispatch(getApiKeys({}, pageable));
  }, [pageable]);

  const getKeyStatus = (apiKey: ApiKey): 'active' | 'expired' | 'revoked' => {
    if (apiKey.revokedAt) return 'revoked';
    if (apiKey.expiresAt && new Date(apiKey.expiresAt) < new Date())
      return 'expired';
    return 'active';
  };

  const columns: CustomDatagridColumn2<ApiKey>[] = [
    {
      header: t('api_key_label'),
      accessorKey: 'label',
      cell: (info) => info.getValue() as string
    },
    {
      header: t('expiration'),
      accessorKey: 'expiresAt',
      cell: (info) => {
        const expiresAt = info.getValue() as string;
        return expiresAt ? getFormattedDate(expiresAt) : t('never');
      },
      size: 70
    },
    {
      header: t('status'),
      accessorKey: 'status',
      cell: (info) => {
        const status = getKeyStatus(info.row.original);
        if (status === 'revoked')
          return <Chip label={t('revoked')} color="error" size="small" />;
        if (status === 'expired')
          return <Chip label={t('expired')} color="warning" size="small" />;
        return <Chip label={t('active')} color="success" size="small" />;
      },
      size: 50
    },
    {
      header: t('last_used'),
      accessorKey: 'lastUsed',
      cell: (info) => {
        const lastUsed = info.getValue() as string;
        return lastUsed ? getFormattedDate(lastUsed) : t('never');
      }
    },
    {
      header: t('actions'),
      cell: (info) => {
        const apiKey = info.row.original;
        const status = getKeyStatus(apiKey);
        return (
          <Stack direction="row" spacing={0.5}>
            {status !== 'revoked' && (
              <IconButton
                color="primary"
                onClick={() => handleOpenRotateDialog(apiKey)}
                title={t('rotate')}
              >
                <SyncTwoToneIcon />
              </IconButton>
            )}
            <IconButton
              color="error"
              onClick={() => handleOpenDeleteDialog(apiKey)}
            >
              <DeleteTwoToneIcon />
            </IconButton>
          </Stack>
        );
      },
      size: 50
    }
  ];

  return (
    <Box>
      <Box display="flex" justifyContent="flex-end" mb={3}>
        <Stack direction={'row'} spacing={1} alignItems={'center'}>
          <Button onClick={onOpenApiDocs}>{t('open_api_docs')}</Button>
          <Button
            variant="contained"
            startIcon={<AddTwoToneIcon />}
            onClick={handleOpenCreateModal}
          >
            {t('create_api_key')}
          </Button>
        </Stack>
      </Box>

      <CustomDatagrid2
        columns={columns}
        data={apiKeys?.content || []}
        loading={loadingGet}
        notClickable
        pagination={{
          pageIndex: pageable.page,
          pageSize: pageable.size
        }}
        onPaginationChange={(pagination) => {
          setPageable({
            page: pagination.pageIndex,
            size: pagination.pageSize
          });
        }}
        // onSortingChange={}
        totalRows={apiKeys.totalElements}
      />

      {/* Create API Key Modal */}
      <DialogWrapper
        open={openCreateModal}
        maxWidth="sm"
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
            <Typography variant="h4">
              {createdApiKeyCode ? t('api_key_code') : t('create_api_key')}
            </Typography>
            <IconButton onClick={handleCloseCreateModal}>
              <CloseIcon />
            </IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          {!createdApiKeyCode ? (
            <Formik
              initialValues={{ label: '', expiresAt: null }}
              validationSchema={Yup.object({
                label: Yup.string().required(
                  t('api_key_label') + ' ' + t('required')
                )
              })}
              onSubmit={(values, { setFieldValue }) => {
                const payload = {
                  label: values.label,
                  ...(values.expiresAt && {
                    expiresAt: values.expiresAt.toISOString()
                  })
                };
                return handleCreateApiKey(payload);
              }}
            >
              {({
                errors,
                touched,
                values,
                handleChange,
                handleSubmit,
                setFieldValue
              }) => (
                <Form onSubmit={handleSubmit}>
                  <Box py={2}>
                    <TextField
                      name="label"
                      label={t('api_key_label')}
                      value={values.label}
                      onChange={handleChange}
                      error={touched.label && Boolean(errors.label)}
                      helperText={touched.label && errors.label}
                      fullWidth
                      autoFocus
                    />
                    <Box mt={2}>
                      <DateTimePicker
                        label={t('expiration_optional')}
                        value={values.expiresAt}
                        onChange={(value) => setFieldValue('expiresAt', value)}
                        renderInput={(params) => (
                          <TextField
                            {...params}
                            margin="normal"
                            variant="outlined"
                            fullWidth
                            name={'expiresAt'}
                          />
                        )}
                      />
                    </Box>
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
              )}
            </Formik>
          ) : (
            <Box py={3}>
              <Box
                display="flex"
                alignItems="center"
                justifyContent="space-between"
                p={2}
                sx={{
                  backgroundColor: 'action.hover',
                  borderRadius: 1,
                  border: '1px solid',
                  borderColor: 'divider'
                }}
              >
                <Typography
                  variant="body1"
                  fontFamily="monospace"
                  sx={{ wordBreak: 'break-all' }}
                >
                  {createdApiKeyCode}
                </Typography>
                <IconButton
                  onClick={() => copyToClipboard(createdApiKeyCode)}
                  color="primary"
                >
                  <ContentCopyIcon />
                </IconButton>
              </Box>
              <Box
                display="flex"
                alignItems="center"
                justifyContent="center"
                mt={3}
                p={2}
                sx={{
                  borderRadius: 1,
                  border: '1px solid',
                  borderColor: 'warning.main'
                }}
              >
                <Typography variant="body2" color="warning.dark" align="center">
                  {t('api_key_code_view_once')}
                </Typography>
              </Box>
              <Box mt={3} display="flex" justifyContent="flex-end">
                <Button variant="contained" onClick={handleCloseCreateModal}>
                  {t('close')}
                </Button>
              </Box>
            </Box>
          )}
        </DialogContent>
      </DialogWrapper>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={openDeleteDialog}
        onCancel={handleCloseDeleteDialog}
        onConfirm={handleDeleteApiKey}
        confirmText={t('delete')}
        question={t('delete_api_key_confirm')}
      />

      {/* Rotate Confirmation Dialog */}
      <DialogWrapper
        open={openRotateDialog}
        maxWidth="sm"
        fullWidth
        TransitionComponent={Transition}
        keepMounted
        onClose={handleCloseRotateDialog}
      >
        <DialogTitle>
          <Box
            display="flex"
            justifyContent="space-between"
            alignItems="center"
          >
            <Typography variant="h4">
              {showRotatedCode ? t('api_key_code') : t('rotate_api_key')}
            </Typography>
            <IconButton onClick={handleCloseRotateDialog}>
              <CloseIcon />
            </IconButton>
          </Box>
        </DialogTitle>
        <DialogContent>
          {!showRotatedCode ? (
            <Box py={2}>
              <Typography variant="body1" mb={3}>
                {t('rotate_api_key_confirm')}
              </Typography>
              <Box display="flex" justifyContent="flex-end">
                <Button
                  variant="text"
                  onClick={handleCloseRotateDialog}
                  sx={{ mr: 1 }}
                >
                  {t('cancel')}
                </Button>
                <Button
                  variant="contained"
                  onClick={handleRotateApiKey}
                  disabled={loadingRotate}
                >
                  {loadingRotate ? (
                    <CircularProgress size={24} color="inherit" />
                  ) : (
                    t('rotate')
                  )}
                </Button>
              </Box>
            </Box>
          ) : (
            <Box py={3}>
              <Box
                display="flex"
                alignItems="center"
                justifyContent="space-between"
                p={2}
                sx={{
                  backgroundColor: 'grey.100',
                  borderRadius: 1,
                  border: '1px solid',
                  borderColor: 'grey.300'
                }}
              >
                <Typography
                  variant="body1"
                  fontFamily="monospace"
                  sx={{ wordBreak: 'break-all' }}
                >
                  {rotatedApiKeyCode}
                </Typography>
                <IconButton
                  onClick={() => copyToClipboard(rotatedApiKeyCode)}
                  color="primary"
                >
                  <ContentCopyIcon />
                </IconButton>
              </Box>
              <Box
                display="flex"
                alignItems="center"
                justifyContent="center"
                mt={3}
                p={2}
                sx={{
                  borderRadius: 1,
                  border: '1px solid',
                  borderColor: 'warning.main'
                }}
              >
                <Typography variant="body2" color="warning.dark" align="center">
                  {t('api_key_code_view_once')}
                </Typography>
              </Box>
              <Box mt={3} display="flex" justifyContent="flex-end">
                <Button variant="contained" onClick={handleCloseRotateDialog}>
                  {t('close')}
                </Button>
              </Box>
            </Box>
          )}
        </DialogContent>
      </DialogWrapper>
    </Box>
  );
}

export default ApiKeys;
