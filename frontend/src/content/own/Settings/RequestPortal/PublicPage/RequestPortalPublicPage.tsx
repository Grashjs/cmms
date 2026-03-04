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
} from 'src/content/own/components/form';
import RequestPortalPreview from '../../../components/form/RequestPortalPreview';
import { LocationMiniDTO } from '../../../../../models/owns/location';
import { AssetMiniDTO } from '../../../../../models/owns/asset';
import { useDispatch, useSelector } from '../../../../../store';
import BuildOutlinedIcon from '@mui/icons-material/BuildOutlined';
import { Business } from '@mui/icons-material';
import BusinessTwoToneIcon from '@mui/icons-material/BusinessTwoTone';

interface FormValues {
  title: string;
  description?: string;
  contact?: string;
  location?: LocationMiniDTO | null;
  asset?: AssetMiniDTO | null;
  image?: File[];
  files?: File[];
}

export default function RequestPortalPublicPage() {
  const { t } = useTranslation();
  const theme = useTheme();
  const { uuid } = useParams<{ uuid: string }>();

  const dispatch = useDispatch();
  const { singleRequestPortal: portal, loadingGet } = useSelector(
    (state: any) => state.requestPortals
  );

  const [fieldConfigs, setFieldConfigs] = useState<PreviewFieldConfig[]>([]);
  const [formValues, setFormValues] = useState<FormValues>({
    title: '',
    description: '',
    contact: '',
    location: null,
    asset: null,
    image: [],
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
      setFieldConfigs((prev) =>
        prev.map((config, i) =>
          i === index ? { ...config, location } : config
        )
      );
    },
    []
  );

  const handleAssetSelect = useCallback(
    (index: number, asset: AssetMiniDTO | null) => {
      setFieldConfigs((prev) =>
        prev.map((config, i) => (i === index ? { ...config, asset } : config))
      );
    },
    []
  );

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
          {t('portail_de_demandes')} {portal.companyName || t('company')}
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
                // boxShadow: theme.shadows[24]
              }}
            >
              <RequestPortalPreview
                title={portal.title}
                welcomeMessage={portal.welcomeMessage}
                fieldConfigs={fieldConfigs}
                preview={false}
                onLocationSelect={handleLocationSelect}
                onAssetSelect={handleAssetSelect}
              />
            </Paper>
          </Grid>
        </Grid>
      </Container>
    </Box>
  );
}
