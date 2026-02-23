import React, { ReactNode } from 'react';
import {
  Box,
  Button,
  Container,
  Grid,
  styled,
  Typography,
  useTheme,
  Stack
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import InventoryIcon from '@mui/icons-material/Inventory';
import QrCodeScannerIcon from '@mui/icons-material/QrCodeScanner';
import NotificationsActiveIcon from '@mui/icons-material/NotificationsActive';
import SmartphoneIcon from '@mui/icons-material/Smartphone';
import CameraAltIcon from '@mui/icons-material/CameraAlt';
import CloudDoneIcon from '@mui/icons-material/CloudDone';
import HubIcon from '@mui/icons-material/Hub';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import PsychologyIcon from '@mui/icons-material/Psychology';
import AddTaskIcon from '@mui/icons-material/AddTask';
import CollectionsIcon from '@mui/icons-material/Collections';
import SpeedIcon from '@mui/icons-material/Speed';
import AutoModeIcon from '@mui/icons-material/AutoMode';
import VerifiedIcon from '@mui/icons-material/Verified';
import HealthAndSafetyIcon from '@mui/icons-material/HealthAndSafety';
import HistoryIcon from '@mui/icons-material/History';
import QueryStatsIcon from '@mui/icons-material/QueryStats';
import RuleIcon from '@mui/icons-material/Rule';
import CompanyLogos from './components/CompanyLogos';

const FeatureRow = styled(Grid)(
  ({ theme }) => `
    padding: ${theme.spacing(10, 0)};
    align-items: center;
`
);

const ImageWrapper = styled(Box)(
  ({ theme }) => `
    img {
      border-radius: ${theme.general.borderRadiusLg};
      box-shadow: 0 20px 50px rgba(0,0,0,0.1);
    }
`
);

const FeatureContent = styled(Box)(
  ({ theme }) => `
    padding: ${theme.spacing(0, 4)};
    @media (max-width: ${theme.breakpoints.values.md}px) {
      padding: ${theme.spacing(4, 0, 0)};
      text-align: center;
    }
`
);

interface FeaturePoint {
  icon: ReactNode;
  text: string;
}

interface Feature {
  imageSizes: { width: number; height: number };
  title: string;
  points: FeaturePoint[];
  image: string;
  imageAlt: string;
}

const FeaturesAlternating = () => {
  const theme = useTheme();

  const features: Feature[] = [
    {
      title: 'Effortless Work Orders',
      points: [
        {
          icon: <AddTaskIcon color="primary" />,
          text: 'Create, assign, and track work orders in seconds.'
        },
        {
          icon: <CollectionsIcon color="primary" />,
          text: 'Attach photos, manuals, and checklists to any task.'
        },
        {
          icon: <SpeedIcon color="primary" />,
          text: 'Monitor progress in real-time with instant status updates.'
        }
      ],
      image: '/static/images/overview/work_order_screenshot.png',
      imageAlt: 'Work Order Management',
      imageSizes: { width: 1920, height: 922 }
    },
    {
      title: 'Asset Lifecycle Tracking',
      points: [
        {
          icon: <HistoryIcon color="primary" />,
          text: 'Maintain a complete digital history of every asset.'
        },
        {
          icon: <QueryStatsIcon color="primary" />,
          text: 'Make data-driven decisions on repair vs. replace.'
        },
        {
          icon: <RuleIcon color="primary" />,
          text: 'Stay audit-ready with automated compliance logs.'
        }
      ],
      image: '/static/images/features/asset-hero.png',
      imageAlt: 'Asset Management',
      imageSizes: { width: 1920, height: 912 }
    },
    {
      title: 'Inventory & Parts',
      points: [
        {
          icon: <InventoryIcon color="primary" />,
          text: 'Track stock levels across multiple locations in real-time.'
        },
        {
          icon: <QrCodeScannerIcon color="primary" />,
          text: 'Scan QR codes for instant part lookup and usage.'
        },
        {
          icon: <NotificationsActiveIcon color="primary" />,
          text: 'Get automated low-stock alerts and reorder instantly.'
        }
      ],
      image: '/static/images/overview/inventory_screenshot.png',
      imageAlt: 'Inventory Management',
      imageSizes: { width: 1920, height: 912 }
    },
    {
      title: 'Maintenance on the Go',
      points: [
        {
          icon: <SmartphoneIcon color="primary" />,
          text: 'Full mobile app functionality for iOS and Android.'
        },
        {
          icon: <CameraAltIcon color="primary" />,
          text: 'Capture photos and scan barcodes directly from the field.'
        },
        {
          icon: <CloudDoneIcon color="primary" />,
          text: 'Sync data instantly and work offline when needed.'
        }
      ],
      image: '/static/mobile_app.jpeg',
      imageAlt: 'Mobile CMMS App',
      imageSizes: { width: 720, height: 1600 }
    }
  ];

  const isPortrait = (sizes: { width: number; height: number }) =>
    sizes.height > sizes.width;

  return (
    <Container maxWidth="lg" sx={{ mt: 6 }}>
      {features.map((feature, index) => {
        const isEven = index % 2 === 0;
        const portrait = isPortrait(feature.imageSizes);
        return (
          <FeatureRow
            container
            key={index}
            direction={isEven ? 'row' : 'row-reverse'}
          >
            <Grid item xs={12} md={6}>
              <ImageWrapper
                sx={{
                  display: 'flex',
                  justifyContent: 'center',
                  alignItems: 'center'
                }}
              >
                <img
                  src={feature.image}
                  alt={feature.imageAlt}
                  width={feature.imageSizes.width}
                  height={feature.imageSizes.height}
                  style={{
                    borderRadius: '16px',
                    ...(portrait
                      ? {
                          // Portrait (mobile screenshot): let height drive sizing
                          width: 'auto',
                          height: 'auto',
                          maxHeight: '500px',
                          maxWidth: '100%'
                        }
                      : {
                          // Landscape (desktop screenshots): fill width
                          width: '100%',
                          height: 'auto',
                          maxHeight: '300px'
                        })
                  }}
                />
              </ImageWrapper>
            </Grid>
            <Grid item xs={12} md={6}>
              <FeatureContent>
                <Typography
                  variant="h2"
                  gutterBottom
                  sx={{
                    fontWeight: 700,
                    fontSize: { xs: '2rem', md: '2.5rem' },
                    color: theme.palette.text.primary,
                    mb: 3
                  }}
                >
                  {feature.title}
                </Typography>
                <Stack spacing={2} sx={{ mb: 4 }}>
                  {feature.points.map((point, pIndex) => (
                    <Box
                      key={pIndex}
                      sx={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: { xs: 'center', md: 'flex-start' },
                        textAlign: 'left'
                      }}
                    >
                      <Box sx={{ mr: 2, display: 'flex', flexShrink: 0 }}>
                        {point.icon}
                      </Box>
                      <Typography
                        variant="body1"
                        sx={{
                          fontSize: '1.1rem',
                          color: theme.palette.text.secondary
                        }}
                      >
                        {point.text}
                      </Typography>
                    </Box>
                  ))}
                </Stack>
                <Button
                  component={RouterLink}
                  to="/account/register"
                  variant="contained"
                  size="large"
                  sx={{
                    px: 4,
                    py: 1.5,
                    fontSize: '1rem',
                    borderRadius: '50px',
                    textTransform: 'none',
                    fontWeight: 'bold'
                  }}
                >
                  Get started for free
                </Button>
              </FeatureContent>
            </Grid>
          </FeatureRow>
        );
      })}

      {/* Final Call to Action Section */}
      <CompanyLogos />
      <Box
        sx={{
          textAlign: 'center',
          borderRadius: theme.general.borderRadiusLg,
          my: 10,
          px: 4
        }}
      >
        <Typography variant="h2" gutterBottom sx={{ fontWeight: 800 }}>
          Ready to optimize your maintenance?
        </Typography>
        <Typography variant="h5" sx={{ mb: 4, opacity: 0.9 }}>
          Join thousands of maintenance professionals using the world's most
          intuitive free CMMS.
        </Typography>
        <Button
          component={RouterLink}
          to="/account/register"
          variant="contained"
          size="large"
          sx={{
            px: 6,
            py: 2,
            fontSize: '1.2rem',
            borderRadius: '50px',
            textTransform: 'none',
            fontWeight: 'bold',
            boxShadow: '0 10px 20px rgba(0,0,0,0.2)'
          }}
        >
          Get started for free - No credit card required
        </Button>
      </Box>
    </Container>
  );
};

export default FeaturesAlternating;
