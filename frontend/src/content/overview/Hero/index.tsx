import {
  Box,
  Button,
  CircularProgress,
  Container,
  Grid,
  Stack,
  styled,
  Typography
} from '@mui/material';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import useScrollToLocation from 'src/hooks/useScrollToLocation';
import useAuth from '../../../hooks/useAuth';
import { useBrand } from '../../../hooks/useBrand';
import api, { authHeader } from '../../../utils/api';
import { fireGa4Event } from '../../../utils/overall';

const TypographyH1 = styled(Typography)(
  ({ theme }) => `
    font-size: ${theme.typography.pxToRem(50)};
`
);

const TypographyH2 = styled(Typography)(
  ({ theme }) => `
    font-size: ${theme.typography.pxToRem(17)};
`
);

const ImgWrapper = styled(Box)(
  ({ theme }) => `
    position: relative;
    z-index: 5;
    width: 100%;
    overflow: hidden;
    border-radius: ${theme.general.borderRadiusLg};
    box-shadow: 0 0rem 14rem 0 rgb(255 255 255 / 20%), 0 0.8rem 2.3rem rgb(111 130 156 / 3%), 0 0.2rem 0.7rem rgb(17 29 57 / 15%);
    aspect-ratio: 1920 / 922;

    img {
      display: block;
      width: 100%;
    }
  `
);

const BoxAccent = styled(Box)(
  ({ theme }) => `
    border-radius: ${theme.general.borderRadiusLg};
    background: ${theme.palette.background.default};
    width: 100%;
    height: 100%;
    position: absolute;
    left: -40px;
    bottom: -40px;
    display: block;
    z-index: 4;
  `
);

const BoxContent = styled(Box)(
  () => `
  width: 150%;
  position: relative;
`
);

const MobileImgWrapper = styled(Box)(
  ({ theme }) => `
    position: absolute;
    z-index: 6;
    width: 15%;
    left: -14%;
    bottom: -25%;
         ${theme.breakpoints.down('md')} {
    left: 45%;
    bottom: -50%;
  }
    transform: translateY(-50%);
    overflow: hidden;
    border-radius: ${theme.general.borderRadiusLg};
    box-shadow: 0 0rem 14rem 0 rgb(0 0 0 / 20%), 0 0.8rem 2.3rem rgb(0 0 0 / 3%), 0 0.2rem 0.7rem rgb(0 0 0 / 15%);
    aspect-ratio: 720 / 1600;

    img {
      display: block;
      width: 100%;
    }
  `
);

function Hero() {
  const { t }: { t: any } = useTranslation();
  const { isAuthenticated, loginInternal } = useAuth();
  const brandConfig = useBrand();
  const navigate = useNavigate();
  const [generatingAccount, setGeneratingAccount] = useState<boolean>(false);
  useScrollToLocation();
  const [shouldNavigate, setShouldNavigate] = useState(false);

  useEffect(() => {
    if (shouldNavigate && isAuthenticated) {
      navigate('/app/work-orders');
      setGeneratingAccount(false);
      setShouldNavigate(false);
    }
  }, [isAuthenticated, shouldNavigate, navigate]);

  const onSeeLiveDemo = async () => {
    setGeneratingAccount(true);
    try {
      fireGa4Event('live_demo_view');
      const { success, message } = await api.get<{
        success: boolean;
        message: string;
      }>('demo/generate-account', { headers: authHeader(true) });

      if (success) {
        loginInternal(message);
        setShouldNavigate(true);
      } else {
        setGeneratingAccount(false);
      }
    } catch (error) {
      setGeneratingAccount(false);
    }
  };
  return (
    <Container maxWidth="lg">
      <Grid
        spacing={{ xs: 6, md: 10 }}
        justifyContent="center"
        alignItems="center"
        container
      >
        <Grid item md={6} pr={{ xs: 0, md: 3 }}>
          <TypographyH1
            sx={{
              mb: 2
            }}
            variant="h1"
          >
            {t('home.built')}
          </TypographyH1>
          <TypographyH2
            sx={{
              lineHeight: 1.5,
              pb: 4
            }}
            variant="h4"
            color="text.secondary"
            fontWeight="normal"
          >
            {t('home_description', { shortBrandName: brandConfig.name })}
          </TypographyH2>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1}>
            <Button
              component={RouterLink}
              to={isAuthenticated ? '/app/work-orders' : '/account/register'}
              size="large"
              variant="contained"
            >
              {t('try_for_free')}
            </Button>
            <Button
              sx={{
                ml: 2
              }}
              component="a"
              startIcon={
                generatingAccount && (
                  <CircularProgress size={'1rem'} color="primary" />
                )
              }
              onClick={onSeeLiveDemo}
              size="medium"
              variant="text"
            >
              {t('see_live_demo')}
            </Button>
            <Button
              sx={{
                ml: 2
              }}
              href={`mailto:${brandConfig.mail}`}
              size="medium"
              variant="text"
              onClick={() => {
                fireGa4Event('contact_us_click');
                window.location.href = `mailto:${brandConfig.mail}`;
              }}
            >
              {t('contact_us')}
            </Button>
          </Stack>
        </Grid>
        <Grid item md={6}>
          <BoxContent>
            <RouterLink to="/app/work-orders">
              <ImgWrapper>
                <img
                  alt={brandConfig.name}
                  src="/static/images/overview/work_orders_screenshot.png"
                  loading={'eager'}
                />
              </ImgWrapper>
            </RouterLink>
            <MobileImgWrapper>
              <img
                alt="Mobile App"
                src="/static/mobile_app.jpeg"
                loading={'eager'}
              />
            </MobileImgWrapper>
            <BoxAccent
              sx={{
                display: { xs: 'none', md: 'block' }
              }}
            />
          </BoxContent>
        </Grid>
      </Grid>
    </Container>
  );
}

export default Hero;
