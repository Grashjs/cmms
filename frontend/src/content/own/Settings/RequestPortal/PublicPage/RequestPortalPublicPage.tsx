import {
  Box,
  Container,
  Paper,
  Stack,
  Typography,
  CircularProgress,
  Alert,
  Button,
  TextField,
  alpha,
  useTheme,
  Divider,
  Grid,
  Avatar
} from '@mui/material';
import { useEffect, useMemo, useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import FileUpload from '../../../components/FileUpload';
import PersonOutlineIcon from '@mui/icons-material/PersonOutline';
import {
  clearSingleRequestPortal,
  getRequestPortalPublic
} from '../../../../../slices/requestPortal';
import {
  AssetLocationClause,
  buildDefaultConfigs,
  PreviewFieldConfig
} from 'src/content/own/components/form/RequestPortalPreview';
import RequestPortalPreview from '../../../components/form/RequestPortalPreview';
import { LocationMiniDTO } from '../../../../../models/owns/location';
import { AssetMiniDTO } from '../../../../../models/owns/asset';
import { useDispatch, useSelector } from '../../../../../store';
import BuildOutlinedIcon from '@mui/icons-material/BuildOutlined';
import { Business } from '@mui/icons-material';
import BusinessTwoToneIcon from '@mui/icons-material/BusinessTwoTone';
import { useSnackbar } from 'notistack';
import { submitPublicRequest } from '../../../../../slices/request';
import api from '../../../../../utils/api';
import { uploadToRequestPortal } from '../../../../../slices/file';

interface FormValues {
  title: string;
  description?: string;
  contact?: string;
  location?: LocationMiniDTO | null;
  asset?: AssetMiniDTO | null;
  images?: File[];
  files?: File[];
}

export default function RequestPortalPublicPage() {
  const { t } = useTranslation();
  const theme = useTheme();
  const { uuid } = useParams<{ uuid: string }>();
  const { enqueueSnackbar } = useSnackbar();

  const dispatch = useDispatch();
  const { singleRequestPortal: portal, loadingGet } = useSelector(
    (state) => state.requestPortals
  );

  const [fieldConfigs, setFieldConfigs] = useState<PreviewFieldConfig[]>([]);
  const [formValues, setFormValues] = useState<FormValues>({
    title: '',
    description: '',
    contact: '',
    location: null,
    asset: null,
    images: [],
    files: []
  });
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (uuid) {
      dispatch(getRequestPortalPublic(uuid));
    }
    return () => {
      dispatch(clearSingleRequestPortal());
    };
  }, [uuid, dispatch]);

  useEffect(() => {
    if (portal?.fields) {
      const configs = buildDefaultConfigs(portal.fields);
      setFieldConfigs(configs);
    }
  }, [portal]);

  const handleLocationSelect = useCallback(
    (index: number, location: LocationMiniDTO | null) => {
      setFormValues((prev) => ({ ...prev, location }));
    },
    []
  );

  const handleAssetSelect = useCallback(
    (index: number, asset: AssetMiniDTO | null) => {
      setFormValues((prev) => ({ ...prev, asset }));
    },
    []
  );

  const handleTitleChange = useCallback((value: string) => {
    setFormValues((prev) => ({ ...prev, title: value }));
  }, []);
  const handleDescriptionChange = useCallback((value: string) => {
    setFormValues((prev) => ({ ...prev, description: value }));
  }, []);

  const handleContactChange = useCallback((value: string) => {
    setFormValues((prev) => ({ ...prev, contact: value }));
  }, []);

  const handleImagesChange = useCallback((files: File[]) => {
    setFormValues((prev) => ({ ...prev, images: files }));
  }, []);

  const handleFilesChange = useCallback((files: File[]) => {
    setFormValues((prev) => ({ ...prev, files: files }));
  }, []);

  const validateForm = useCallback(() => {
    const errors: Record<string, string> = {};
    // Check required fields from fieldConfigs
    fieldConfigs.forEach((config) => {
      if (!config.enabled || !config.required) return;

      switch (config.type) {
        case 'TITLE':
          if (!formValues.title.trim()) {
            errors.title = t('required_title');
          }
          break;
        case 'DESCRIPTION':
          if (!formValues.description?.trim()) {
            errors.description = t('required_description');
          }
          break;
        case 'CONTACT':
          if (!formValues.contact?.trim()) {
            errors.contact = t('required_contact');
          }
          break;
        case 'LOCATION':
          if (!formValues.location) {
            errors.location = t('required_location');
          }
          break;
        case 'ASSET':
          if (!formValues.asset) {
            errors.asset = t('required_asset');
          }
          break;
      }
    });

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  }, [fieldConfigs, formValues, t]);

  const handleSubmit = useCallback(async () => {
    if (!validateForm()) {
      return;
    }

    setSubmitting(true);
    try {
      // Upload images and files
      let imageIds: number[] = [];
      let fileIds: number[] = [];

      if (formValues.images && formValues.images.length > 0) {
        imageIds = (await dispatch(
          uploadToRequestPortal(uuid, formValues.images, 'IMAGE')
        )) as number[];
      }

      if (formValues.files && formValues.files.length > 0) {
        fileIds = (await dispatch(
          uploadToRequestPortal(uuid, formValues.files, 'OTHER')
        )) as number[];
      }

      // Submit the request
      await dispatch(
        submitPublicRequest(uuid!, {
          title: formValues.title,
          description: formValues.description,
          contact: formValues.contact,
          location: formValues.location || null,
          asset: formValues.asset || null,
          image: imageIds.length ? { id: imageIds[0] } : null,
          files: fileIds.map((fileId) => ({ id: fileId }))
        })
      );

      setSubmitted(true);
    } catch (error: any) {
      enqueueSnackbar(error.message || t('request_submit_failure'), {
        variant: 'error'
      });
    } finally {
      setSubmitting(false);
    }
  }, [validateForm, formValues, uuid, dispatch, t]);

  if (loadingGet) {
    return (
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          minHeight: '100vh'
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

  if (!portal) {
    return (
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          minHeight: '100vh'
        }}
      >
        <Alert severity="error">{t('portal_not_found')}</Alert>
      </Box>
    );
  }

  if (submitted) {
    return (
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          minHeight: '100vh'
        }}
      >
        <Alert severity="success">{t('request_submitted_success')}</Alert>
      </Box>
    );
  }

  return (
    <Box
      sx={{
        minHeight: '100vh',
        bgcolor: 'background.default'
      }}
    >
      <Container maxWidth="lg" sx={{ py: 4 }}>
        <Typography
          variant="h4"
          component="h1"
          sx={{
            fontWeight: 700,
            mb: 2,
            color: 'text.primary'
          }}
        >
          {t('portail_de_demandes')} - {portal.companyName}
        </Typography>
        <Grid container spacing={4}>
          {/* Left Panel - Company Logo, Title and Welcome Message */}
          <Grid item xs={12} md={6}>
            <Avatar
              sx={{
                width: 140,
                height: 140,
                mt: 10
              }}
              src={portal.companyLogo}
            >
              <BusinessTwoToneIcon sx={{ fontSize: 70 }} />
            </Avatar>
            <Typography mt={1} fontSize={32} fontWeight={'bold'}>
              {portal.title}
            </Typography>
          </Grid>

          {/* Right Panel - Request Form Preview */}
          <Grid item xs={12} md={6}>
            <Paper
              sx={{
                p: 4,
                bgcolor: 'background.paper'
              }}
            >
              <RequestPortalPreview
                title={portal.title}
                welcomeMessage={portal.welcomeMessage}
                fieldConfigs={fieldConfigs}
                preview={false}
                onLocationSelect={handleLocationSelect}
                onAssetSelect={handleAssetSelect}
                onDescriptionChange={handleDescriptionChange}
                onTitleChange={handleTitleChange}
                onContactChange={handleContactChange}
                onImagesChange={handleImagesChange}
                onFilesChange={handleFilesChange}
                onSubmit={handleSubmit}
                submitting={submitting}
                errors={formErrors}
                portalUUID={uuid}
              />
            </Paper>
          </Grid>
        </Grid>
      </Container>
    </Box>
  );
}
