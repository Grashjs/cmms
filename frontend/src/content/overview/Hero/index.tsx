import {
  Box,
  Button,
  CircularProgress,
  Container,
  Grid,
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

const LabelWrapper = styled(Box)(
  ({ theme }) => `
    background-color: ${theme.colors.success.main};
    color: ${theme.palette.success.contrastText};
    font-weight: bold;
    border-radius: 30px;
    text-transform: uppercase;
    display: inline-block;
    font-size: ${theme.typography.pxToRem(11)};
    padding: ${theme.spacing(0.5)} ${theme.spacing(1.5)};
    margin-bottom: ${theme.spacing(2)};
`
);

const ListItemWrapper = styled(Box)(
  () => `
    display: flex;
    align-items: center;
`
);

const MuiAvatar = styled(Box)(
  ({ theme }) => `
    width: ${theme.spacing(8)};
    height: ${theme.spacing(8)};
    border-radius: ${theme.general.borderRadius};
    background-color: #e5f7ff;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: ${theme.spacing(2)};

    img {
      width: 60%;
      height: 60%;
      display: block;
    }
`
);

const TsAvatar = styled(Box)(
  ({ theme }) => `
    width: ${theme.spacing(8)};
    height: ${theme.spacing(8)};
    border-radius: ${theme.general.borderRadius};
    background-color: #dfebf6;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: ${theme.spacing(2)};

    img {
      width: 60%;
      height: 60%;
      display: block;
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
          <Button
            component={RouterLink}
            to={isAuthenticated ? '/app/work-orders' : '/account/register'}
            size="large"
            variant="contained"
          >
            {t('try_grash', { shortBrandName: brandConfig.name })}
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
        </Grid>
        <Grid item md={6}>
          <BoxContent>
            <RouterLink to="/app/work-orders">
              <ImgWrapper>
                <img
                  alt={brandConfig.name}
                  src="/static/images/overview/work_orders_screenshot.png"
                />
              </ImgWrapper>
            </RouterLink>
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
