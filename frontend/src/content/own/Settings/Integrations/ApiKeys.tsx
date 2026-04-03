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
import { addApiKey, deleteApiKey, getApiKeys } from '../../../../slices/apiKey';
import { ApiKey, ApiKeyPostDTO } from '../../../../models/owns/apiKey';
import { Page, Pageable, SearchCriteria } from '../../../../models/owns/page';
import CustomDatagrid2, {
  CustomDatagridColumn2
} from '../../components/CustomDatagrid2';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import CloseIcon from '@mui/icons-material/Close';
import ConfirmDialog from '../../components/ConfirmDialog';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { Formik, Form, Field } from 'formik';
import * as Yup from 'yup';
import { CompanySettingsContext } from '../../../../contexts/CompanySettingsContext';
import { onOpenApiDocs } from '../../../../utils/overall';

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
  const [openDeleteDialog, setOpenDeleteDialog] = useState(false);
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
    const result = await dispatch(addApiKey(values));
    if (result) {
      setCreatedApiKeyCode(result.code || null);
      setShowCode(true);
      showSnackBar(t('api_key_created_success'), 'success');
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

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    showSnackBar(t('api_key_code_copied'), 'success');
  };

  useEffect(() => {
    dispatch(getApiKeys({}, pageable));
  }, [pageable]);

  const columns: CustomDatagridColumn2<ApiKey>[] = [
    {
      header: t('api_key_label'),
      accessorKey: 'label',
      cell: (info) => info.getValue() as string
    },
    {
      header: t('user'),
      accessorKey: 'user',
      cell: (info) => {
        const user = info.getValue() as { firstName: string; lastName: string };
        return user ? `${user.firstName} ${user.lastName}` : '-';
      }
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
      cell: (info) => (
        <IconButton
          color="error"
          onClick={() => handleOpenDeleteDialog(info.row.original)}
        >
          <DeleteTwoToneIcon />
        </IconButton>
      ),
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
              initialValues={{ label: '' }}
              validationSchema={Yup.object({
                label: Yup.string().required(
                  t('api_key_label') + ' ' + t('required')
                )
              })}
              onSubmit={handleCreateApiKey}
            >
              {({ errors, touched, values, handleChange, handleSubmit }) => (
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
    </Box>
  );
}

export default ApiKeys;
