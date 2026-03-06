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
  Avatar,
  AppBar,
  Toolbar,
  IconButton,
  Select,
  MenuItem,
  FormControl,
  useMediaQuery
} from '@mui/material';
import { useEffect, useMemo, useState, useCallback, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import i18n from 'i18next';
import ReCAPTCHA from 'react-google-recaptcha';
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
import LanguageIcon from '@mui/icons-material/Language';
import { useSnackbar } from 'notistack';
import { submitPublicRequest } from '../../../../../slices/request';
import api from '../../../../../utils/api';
import { uploadToRequestPortal } from '../../../../../slices/file';
import { supportedLanguages } from '../../../../../i18n/i18n';
import { useBrand } from '../../../../../hooks/useBrand';
import { recaptchaSiteKey } from '../../../../../config';
import { Helmet } from 'react-helmet-async';

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
  const brandConfig = useBrand();

  const dispatch = useDispatch();
  const { singleRequestPortal: portal, loadingGet } = useSelector(
    (state) => state.requestPortals
  );

  const recaptchaRef = useRef<ReCAPTCHA>(null);
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
  const isUnderMd = useMediaQuery(theme.breakpoints.down('md'));
  const [recaptchaToken, setRecaptchaToken] = useState<string | null>(null);

  useEffect(() => {
    if (uuid) {
      dispatch(getRequestPortalPublic(uuid));
    }
    return () => {
      dispatch(clearSingleRequestPortal());
    };
  }, [uuid, dispatch]);

  useEffect(() => {
    if (portal) {
      const configs = buildDefaultConfigs(portal.fields);
      setFieldConfigs(configs);
      i18n.changeLanguage(portal.companyLanguage.toLowerCase());
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
          if (!config.location && !formValues.location) {
            errors.location = t('required_location');
          }
          break;
        case 'ASSET':
          if (!config.asset && !formValues.asset) {
            errors.asset = t('required_asset');
          }
          break;
        case 'IMAGE':
          if (!formValues.images || formValues.images.length === 0) {
            errors.image = t('required_image');
          }
          break;
        case 'FILES':
          if (!formValues.files || formValues.files.length === 0) {
            errors.files = t('required_files');
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

    // If reCAPTCHA is configured, execute it
    if (recaptchaSiteKey?.trim() && recaptchaRef.current) {
      try {
        const token = await recaptchaRef.current.executeAsync();
        if (!token) {
          enqueueSnackbar(t('recaptcha_failed'), { variant: 'error' });
          return;
        }
        setRecaptchaToken(token);
      } catch (error) {
        enqueueSnackbar(t('recaptcha_failed'), { variant: 'error' });
        return;
      }
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
        submitPublicRequest(
          uuid!,
          {
            title: formValues.title,
            description: formValues.description,
            contact: formValues.contact,
            location: formValues.location || null,
            asset: formValues.asset || null,
            image: imageIds.length ? { id: imageIds[0] } : null,
            files: fileIds.map((fileId) => ({ id: fileId }))
          },
          recaptchaToken || ''
        )
      );

      setSubmitted(true);
    } catch (error: any) {
      enqueueSnackbar(error.message || t('request_submit_failure'), {
        variant: 'error'
      });
    } finally {
      setSubmitting(false);
      // Reset reCAPTCHA after submission
      if (recaptchaRef.current) {
        recaptchaRef.current.reset();
      }
    }
  }, [validateForm, formValues, uuid, dispatch, t, enqueueSnackbar]);

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

  const languageSwitcher = (
    <FormControl size="small">
      <Select
        value={i18n.language}
        onChange={(e) => {
          i18n.changeLanguage(e.target.value);
        }}
        displayEmpty
        variant="outlined"
        size="small"
        sx={{
          minWidth: 100,
          '& .MuiOutlinedInput-notchedOutline': {
            borderColor: 'divider'
          },
          '&:hover .MuiOutlinedInput-notchedOutline': {
            borderColor: 'text.primary'
          }
        }}
        IconComponent={LanguageIcon}
      >
        {supportedLanguages.map((supportedLanguage) => (
          <MenuItem key={supportedLanguage.code} value={supportedLanguage.code}>
            {supportedLanguage.label}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );

  const form = (
    <RequestPortalPreview
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
      images={formValues.images}
      files={formValues.files}
    />
  );
  return (
    <Box
      sx={{
        minHeight: '100vh',
        height: 'fit-content',
        bgcolor: { xs: 'background.paper', md: 'background.default' }
      }}
    >
      <Helmet>
        <title>
          {t('request_portal')} - {portal.companyName}
        </title>
      </Helmet>
      {/* Navbar */}
      <AppBar
        position="static"
        elevation={0}
        sx={{
          bgcolor: 'background.paper',
          borderBottom: 1,
          py: 2,
          borderColor: 'divider',
          display: { xs: 'none', md: 'block' }
        }}
      >
        <Container maxWidth="lg">
          <Toolbar>
            <Typography
              variant="h6"
              component="div"
              sx={{
                flexGrow: 1,
                fontWeight: 700,
                color: 'text.primary'
              }}
            >
              {t('request_portal')} - {portal.companyName}
            </Typography>

            <Stack direction="row" spacing={2} alignItems="center">
              {/* Powered by Logo */}
              <Typography
                variant="body2"
                display={{ xs: 'none', sm: 'block' }}
                sx={{
                  color: 'text.secondary',
                  cursor: 'pointer',
                  '&:hover': {
                    color: 'primary.main'
                  }
                }}
                onClick={() => {
                  window.open(brandConfig.website, '_blank');
                }}
              >
                Powered by {brandConfig.name}
              </Typography>
              {languageSwitcher}
            </Stack>
          </Toolbar>
        </Container>
      </AppBar>

      <Container
        maxWidth="lg"
        sx={{
          py: 4,
          bgcolor: { xs: 'background.paper', md: 'background.default' }
        }}
      >
        <Grid container spacing={4} alignItems="stretch">
          {/* Left Panel - Company Logo, Title and Welcome Message */}
          <Grid item xs={12} md={6}>
            <Stack direction={'row'} justifyContent={'space-between'}>
              <Avatar
                sx={{
                  width: { xs: 48, md: 140 },
                  height: { xs: 48, md: 140 },
                  mt: { xs: 0, sm: 3, md: 12 }
                }}
                src={portal.companyLogo}
              >
                <BusinessTwoToneIcon sx={{ fontSize: 70 }} />
              </Avatar>
              {isUnderMd && languageSwitcher}
            </Stack>
            <Typography mt={1} fontSize={32} fontWeight={'bold'}>
              {portal.title}
            </Typography>
            <Typography variant={'subtitle1'}>
              {portal.welcomeMessage}
            </Typography>
          </Grid>

          {/* Right Panel - Request Form Preview */}
          <Grid item xs={12} md={6}>
            {isUnderMd ? (
              <Box
                sx={{
                  flex: 1,
                  display: 'flex',
                  flexDirection: 'column'
                }}
              >
                {form}
              </Box>
            ) : (
              <Paper
                sx={{
                  p: { xs: 0, md: 4 },
                  bgcolor: 'background.paper'
                }}
              >
                {form}
              </Paper>
            )}
          </Grid>
        </Grid>
      </Container>

      {/* Invisible reCAPTCHA */}
      {recaptchaSiteKey && (
        <ReCAPTCHA
          ref={recaptchaRef}
          size="invisible"
          sitekey={recaptchaSiteKey}
          onChange={(token) => setRecaptchaToken(token)}
          onExpired={() => setRecaptchaToken(null)}
          onErrored={() => setRecaptchaToken(null)}
        />
      )}
    </Box>
  );
}
