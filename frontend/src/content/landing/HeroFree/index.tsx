import {
  Box,
  Button,
  Container,
  Grid,
  styled,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Link as RouterLink } from 'react-router-dom';

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

    img {
      display: block;
      width: 100%;
    }
  `
);

function HeroFree() {
  const { t }: { t: any } = useTranslation();

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
            Free CMMS for Work Order Management
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
            A Free CMMS to manage work orders at your factory, shop floor,
            building, car fleet, restaurant chain, solar and wind sites or just
            about any industry.
          </TypographyH2>
          <Button
            component={RouterLink}
            to={'/account/register'}
            size="large"
            variant="contained"
          >
            Get Started for Free - No Credit Card Required!
          </Button>
        </Grid>
        <Grid item md={6}>
          <BoxContent>
            <RouterLink to="/account/register">
              <ImgWrapper>
                <img
                  alt="Work Orders"
                  src="/static/images/overview/work_orders_screenshot.png"
                />
              </ImgWrapper>
            </RouterLink>
            <MobileImgWrapper>
              <img alt="Mobile App" src="/static/mobile_app.jpeg" />
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

export default HeroFree;
