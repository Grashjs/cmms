import { useTranslation } from 'react-i18next';
import { SetStateAction, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
  Avatar,
  Box,
  Button,
  Card,
  Container,
  Grid,
  List,
  ListItem,
  ListItemText,
  styled,
  Tab,
  Tabs,
  Typography
} from '@mui/material';
import CheckTwoToneIcon from '@mui/icons-material/CheckTwoTone';
import { AE, CN, DE, ES, FR, US, BR } from 'country-flag-icons/react/3x2';
import SubscriptionPlans from '../SubscriptionPlans';
import SubscriptionPlanSelector from '../../pricing/components/SubscriptionPlanSelector';

const AvatarSuccess = styled(Avatar)(
  ({ theme }) => `
    background: ${theme.colors.success.light};
    width: ${theme.spacing(4)};
    height: ${theme.spacing(4)};
`
);

const BoxRtl = styled(Box)(
  ({ theme }) => `
    background: ${theme.colors.alpha.white[100]};
`
);

const CardImageWrapper = styled(Card)(
  () => `
    display: flex;
    position: relative;
    z-index: 6;

    img {
      width: 100%;
      height: auto;
    }
`
);

const CardImg = styled(Card)(
  ({ theme }) => `
    position: absolute;
    border-radius: 100%;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border: 1px solid ${theme.colors.alpha.black[10]};
    transition: ${theme.transitions.create(['border'])};

    &:hover {
      border-color: ${theme.colors.primary.main};
    }
`
);

const TypographyH1Primary = styled(Typography)(
  ({ theme }) => `
    font-size: ${theme.typography.pxToRem(36)};
`
);

const TypographyH2 = styled(Typography)(
  ({ theme }) => `
    font-size: ${theme.typography.pxToRem(17)};
`
);

const BoxHighlights = styled(Box)(
  () => `
    position: relative;
    z-index: 5;
`
);

const BlowWrapper = styled(Box)(
  ({ theme }) => `
    position: relative;
    margin-top: ${theme.spacing(5)};
`
);

const Blob1 = styled(Box)(
  ({ theme }) => `
  background: ${theme.palette.background.default};
  width: 260px;
    height: 260px;
    position: absolute;
    z-index: 5;
    top: -${theme.spacing(9)};
    right: -${theme.spacing(7)};
    border-radius: 30% 70% 82% 18% / 26% 22% 78% 74%;
`
);

const Blob2 = styled(Box)(
  ({ theme }) => `
    background: ${theme.palette.background.default};
    width: 140px;
    bottom: -${theme.spacing(5)};
    left: -${theme.spacing(5)};
    position: absolute;
    z-index: 5;
    height: 140px;
    border-radius: 77% 23% 30% 70% / 81% 47% 53% 19% ;
`
);

const ScreenshotWrapper = styled(Card)(
  ({ theme }) => `
    perspective: 700px;
    display: flex;
    overflow: visible;
    background: ${theme.palette.background.default};
`
);

const Screenshot = styled('img')(
  ({ theme }) => `
    width: 100%;
    height: auto;
    transform: rotateY(-35deg);
    border-radius: ${theme.general.borderRadius};
`
);

const TypographyHeading = styled(Typography)(
  ({ theme }) => `
    font-size: ${theme.typography.pxToRem(36)};
`
);

const TypographySubHeading = styled(Typography)(
  ({ theme }) => `
    font-size: ${theme.typography.pxToRem(17)};
`
);

const TypographyFeature = styled(Typography)(
  ({ theme }) => `
    font-size: ${theme.typography.pxToRem(50)};
    color: ${theme.colors.primary.main};
    font-weight: bold;
    margin-bottom: -${theme.spacing(1)};
    display: block;
`
);

const AvatarWrapperSuccess = styled(Avatar)(
  ({ theme }) => `
      background-color: ${theme.colors.success.lighter};
      color:  ${theme.colors.success.main};
`
);
const TabsContainerWrapper = styled(Box)(
  ({ theme }) => `

  .MuiTabs-root {
    height: 40px;
    width: 80%;
    min-height: 40px;
    align-self: center;

    .MuiTabs-flexContainer {
      justify-content: center;
    }
  }

  .MuiTabs-indicator {
    min-height: 40px;
    height: 40px;
    box-shadow: none;
    border-radius: 50px;
    border: 0;
    background: ${theme.colors.primary.main};
  }

  .MuiTab-root {
    &.MuiButtonBase-root {
        position: relative;
        height: 40px;
        min-height: 40px;
        border-radius: 20px;
        font-size: ${theme.typography.pxToRem(16)};
        color: ${theme.colors.primary.main};
        padding: 0 ${theme.spacing(1.5)};

        .MuiTouchRipple-root {
          opacity: 0;
        }

        &:hover {
          color: ${theme.colors.alpha.black[100]};
        }
    }

    &.Mui-selected {
        color: ${theme.colors.alpha.white[100]};

        &:hover {
          color: ${theme.colors.alpha.white[100]};
        }
    }
}
`
);

const BoxLayouts = styled(Box)(
  ({ theme }) => `
      background: ${theme.colors.gradients.blue1};
      padding: ${theme.spacing(16, 0)};
      margin: ${theme.spacing(10, 0, 0)};
      position: relative;

      .typo-heading,
      .typo-feature {
        color: ${theme.colors.alpha.trueWhite[100]};
      }

      .typo-subheading {
        color: ${theme.colors.alpha.trueWhite[70]};
      }
`
);

const BoxLayoutsImage = styled(Box)(
  () => `
    background-size: cover;
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    height: 100%;
    opacity: .5;
`
);

const BoxLayoutsContent = styled(Container)(
  ({ theme }) => `
      z-index: 5;
      position: relative;
      color: ${theme.colors.alpha.trueWhite[100]};
`
);

const BoxWave = styled(Box)(
  ({ theme }) => `
    position: absolute;
    top: -10px;
    left: 0;
    width: 100%;
    z-index: 5;

    svg path {
	    fill: ${theme.colors.alpha.white[100]};
	}
`
);

const BoxWaveAlt = styled(Box)(
  ({ theme }) => `
    position: absolute;
    bottom: -10px;
    left: 0;
    width: 100%;
    z-index: 2;

    svg path {
	    fill: ${theme.colors.alpha.white[100]};
	}
`
);

const LayoutImgButton = styled(Box)(
  ({ theme }) => `
    overflow: hidden;
    border-radius: ${theme.general.borderRadiusXl};
    display: block;
    position: relative;
    box-shadow: 0 0rem 14rem 0 rgb(0 0 0 / 20%), 0 0.8rem 2.3rem rgb(0 0 0 / 3%), 0 0.2rem 0.7rem rgb(0 0 0 / 15%);

    .MuiTypography-root {
      position: absolute;
      right: ${theme.spacing(3)};
      bottom: ${theme.spacing(3)};
      color: ${theme.colors.alpha.trueWhite[100]};;
      background: rgba(0,0,0,.8);
      padding: ${theme.spacing(2, 4.5)};
      border-radius: ${theme.general.borderRadiusXl};
      z-index: 5;
    }

    img {
      width: 100%;
      height: auto;
      display: block;
      opacity: 1;
      transition: opacity .2s;
    }

    &:hover {
      img {
        opacity: .8;
      }
    }
`
);

const icons = {
  Auth0: '/static/images/logo/auth0.svg',
  FirebaseAuth: '/static/images/logo/firebase.svg',
  JWT: '/static/images/logo/jwt.svg',
  Amplify: '/static/images/logo/amplify.svg'
};

function Highlights() {
  const { t }: { t: any } = useTranslation();

  const [currentTab, setCurrentTab] = useState('work-orders');
  const [monthly, setMonthly] = useState<boolean>(true);
  const tabs = [
    { value: 'work-orders', label: t('work_orders') },
    { value: 'request', label: t('request_system') },
    { value: 'mobile', label: t('mobile_app') },
    { value: 'asset', label: t('asset_management') },
    { value: 'preventative', label: t('preventive_maintenance') },
    { value: 'part', label: t('parts_inventory') },
    { value: 'dashboard', label: t('custom_dashboards') }
  ];

  const featuresConfiguration = {
    'work-orders': {
      title: 'work-orders.title',
      descriptions: [
        'work-orders.descriptions.0',
        'work-orders.descriptions.1'
      ],
      checks: [
        'work-orders.checks.0',
        'work-orders.checks.1',
        'work-orders.checks.2',
        'work-orders.checks.3',
        'work-orders.checks.4'
      ],
      image: '/static/images/overview/work_order_screenshot.png'
    },
    request: {
      title: 'work-requests.title',
      descriptions: [
        'work-requests.descriptions.0',
        'work-requests.descriptions.1'
      ],
      checks: [
        'work-requests.checks.0',
        'work-requests.checks.1',
        'work-requests.checks.2'
      ],
      image: '/static/images/overview/performance.png'
    },
    mobile: {
      title: 'mobile-app.title',
      descriptions: ['mobile-app.descriptions.0', 'mobile-app.descriptions.1'],
      checks: [
        'mobile-app.checks.0',
        'mobile-app.checks.1',
        'mobile-app.checks.2',
        'mobile-app.checks.3',
        'mobile-app.checks.4',
        'mobile-app.checks.5'
      ],
      image: '/static/images/overview/mobile_home.png'
    },
    asset: {
      title: 'eam.title',
      descriptions: ['eam.descriptions.0', 'eam.descriptions.1'],
      checks: [
        'eam.checks.0',
        'eam.checks.1',
        'eam.checks.2',
        'eam.checks.3',
        'eam.checks.4',
        'eam.checks.5'
      ],
      image: '/static/images/overview/performance.png'
    },
    preventative: {
      title: 'pm.title',
      descriptions: ['pm.descriptions.0', 'pm.descriptions.1'],
      checks: [
        'pm.checks.0',
        'pm.checks.1',
        'pm.checks.2',
        'pm.checks.3',
        'pm.checks.4',
        'pm.checks.5'
      ],
      image: '/static/images/overview/performance.png'
    },
    part: {
      title: 'part.title',
      descriptions: ['part.descriptions.0', 'part.descriptions.1'],
      checks: [
        'part.checks.0',
        'part.checks.1',
        'part.checks.2',
        'part.checks.3',
        'part.checks.4',
        'part.checks.5',
        'part.checks.6',
        'part.checks.7'
      ],
      image: '/static/images/overview/inventory_screenshot.png'
    },
    dashboard: {
      title: 'dashboard.title',
      descriptions: [
        'dashboard.descriptions.0',
        'dashboard.descriptions.1',
        'dashboard.descriptions.2'
      ],
      checks: [
        'dashboard.checks.0',
        'dashboard.checks.1',
        'dashboard.checks.2',
        'dashboard.checks.3',
        'dashboard.checks.4',
        'dashboard.checks.5'
      ],
      image: '/static/images/overview/analytics_screenshot.png'
    }
  };
  const CheckItem = ({ description }: { description: string }) => {
    return (
      <ListItem>
        <AvatarSuccess
          sx={{
            mr: 2
          }}
        >
          <CheckTwoToneIcon />
        </AvatarSuccess>
        <ListItemText primary={t(description)} />
      </ListItem>
    );
  };

  const Feature = ({
    title,
    descriptions,
    checks,
    image
  }: {
    title: string;
    descriptions: string[];
    checks: string[];
    image: string;
  }) => {
    return (
      <Grid
        sx={{
          mt: 8
        }}
        container
        spacing={4}
      >
        <Grid item xs={12} md={6}>
          <Typography sx={{ mb: 1 }} variant="h2">
            {t(title)}.
          </Typography>
          {descriptions.map((description, index) => (
            <Box key={index}>
              <Typography variant="subtitle2">{t(description)}</Typography>
              <br />
            </Box>
          ))}
          <List
            disablePadding
            sx={{
              mt: 2
            }}
          >
            {checks.map((desc, index) => (
              <CheckItem key={index} description={desc} />
            ))}
          </List>
        </Grid>
        <Grid item xs={12} md={6}>
          <BlowWrapper>
            <Blob1 />
            <Blob2 />
            <CardImageWrapper>
              <img src={image} alt="Atlas" />
            </CardImageWrapper>
          </BlowWrapper>
        </Grid>
      </Grid>
    );
  };
  const handleTabsChange = (_event: any, value: SetStateAction<string>) => {
    setCurrentTab(value);
  };

  return (
    <BoxHighlights>
      <BoxLayouts>
        <BoxWave>
          <svg
            viewBox="0 0 1440 172"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M0 0H1440V52.1874C1440 52.1874 873.5 172 720 172C566.5 172 0 52.1874 0 52.1874V0Z"
              fill="white"
            />
          </svg>
        </BoxWave>
        <BoxLayoutsImage
          sx={{
            backgroundImage: 'url("/static/images/placeholders/covers/7.jpg")'
          }}
        />
        <BoxLayoutsContent maxWidth="lg">
          <Grid
            justifyContent="center"
            alignItems="center"
            spacing={6}
            container
          >
            <Grid item xs={12} md={6}>
              <TypographyFeature
                className="typo-feature"
                sx={{
                  mt: { lg: 10 }
                }}
              >
                {t('home.what')}
              </TypographyFeature>
              <TypographyHeading
                className="typo-heading"
                sx={{
                  mb: 1
                }}
                variant="h3"
              >
                {t('home.you_will_have')}
              </TypographyHeading>
              <TypographySubHeading
                className="typo-subheading"
                sx={{
                  lineHeight: 1.5
                }}
                variant="h4"
                color="text.secondary"
                fontWeight="normal"
              >
                {t('home.you_will_have_description')}
              </TypographySubHeading>
            </Grid>
            <Grid item xs={12} md={6}>
              <LayoutImgButton>
                <Typography variant="h4">{t('work_orders')}</Typography>
                <img
                  src="/static/images/overview/work_order_screenshot.png"
                  alt={t('work_orders')}
                />
              </LayoutImgButton>
            </Grid>
            <Grid item xs={12} md={6}>
              <LayoutImgButton>
                <Typography variant="h4">{t('custom_dashboards')}</Typography>
                <img
                  src="/static/images/overview/analytics_screenshot.png"
                  alt={t('custom_dashboards')}
                />
              </LayoutImgButton>
            </Grid>
            <Grid item xs={12} md={6}>
              <LayoutImgButton>
                <Typography variant="h4">{t('asset_management')}</Typography>
                <img
                  src="/static/images/overview/extended_sidebar.png"
                  alt={t('asset_management')}
                />
              </LayoutImgButton>
            </Grid>
          </Grid>
        </BoxLayoutsContent>
        <BoxWaveAlt>
          <svg
            viewBox="0 0 1440 172"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M1440 172H0V119.813C0 119.813 566.5 0 720 0C873.5 0 1440 119.813 1440 119.813V172Z"
              fill="white"
            />
          </svg>
        </BoxWaveAlt>
      </BoxLayouts>
      <Container
        maxWidth="lg"
        sx={{
          py: { xs: 8, md: 10 }
        }}
      >
        <Grid
          spacing={0}
          direction={{ xs: 'column-reverse', md: 'row' }}
          justifyContent="center"
          container
        >
          <Grid item xs={12} md={6}>
            <Box>
              <TypographyHeading
                sx={{
                  mb: 1
                }}
                variant="h3"
              >
                {t('home.work')}
              </TypographyHeading>
              <TypographyFeature>{t('home.smarter')}</TypographyFeature>
              <TypographySubHeading
                sx={{
                  lineHeight: 1.5,
                  pr: 8
                }}
                variant="h4"
                color="text.secondary"
                fontWeight="normal"
              >
                {t('home.smarter_description')}
              </TypographySubHeading>
            </Box>
          </Grid>
        </Grid>
        <TypographyH1Primary
          id="key-features"
          textAlign="center"
          sx={{
            mt: 8,
            mb: 2
          }}
          variant="h1"
        >
          {t('key_features')}
        </TypographyH1Primary>
        <Container maxWidth="sm">
          <TypographyH2
            sx={{
              pb: 4,
              lineHeight: 1.5
            }}
            textAlign="center"
            variant="h4"
            color="text.secondary"
            fontWeight="normal"
          >
            {t('key_features_description')}
          </TypographyH2>
        </Container>
        <Box
          sx={{
            justifyContent: 'flex-start',
            '.MuiTabs-scrollableX': {
              overflow: 'auto !important'
            }
          }}
        >
          <Tabs
            onChange={handleTabsChange}
            value={currentTab}
            variant="scrollable"
            scrollButtons={true}
            allowScrollButtonsMobile
            textColor="primary"
            indicatorColor="primary"
          >
            {tabs.map((tab) => (
              <Tab key={tab.value} label={tab.label} value={tab.value} />
            ))}
          </Tabs>
        </Box>
        {Object.entries(featuresConfiguration).map(([feature, config]) => {
          return (
            <>
              {currentTab === feature && (
                <Feature
                  title={config.title}
                  descriptions={config.descriptions}
                  checks={config.checks}
                  image={config.image}
                />
              )}
            </>
          );
        })}
        {/*<SubscriptionPlans />*/}
        {currentTab === 'rtl' && (
          <BoxRtl
            sx={{
              pt: 10
            }}
          >
            <Container maxWidth="lg">
              <Grid container spacing={8}>
                <Grid
                  item
                  xs={12}
                  md={5}
                  sx={{
                    display: 'flex',
                    alignItems: 'center'
                  }}
                >
                  <Box>
                    <TypographyH1Primary variant="h1">
                      {t('Right-To-Left Layouts & Translation-Ready')}
                    </TypographyH1Primary>
                    <Typography
                      variant="subtitle2"
                      sx={{
                        py: 2
                      }}
                    >
                      {t(
                        "Follow our documentation files to find out how to switch to a RTL layout. It's easy!"
                      )}
                    </Typography>
                    <Typography
                      variant="h4"
                      sx={{
                        my: 2
                      }}
                    >
                      {t('Languages already integrated')}:
                    </Typography>
                    <Box
                      sx={{
                        svg: {
                          width: 44,
                          mr: 1
                        }
                      }}
                    >
                      <US title="USA" />
                      <DE title="Germany" />
                      <ES title="Spain" />
                      <FR title="France" />
                      <CN title="China" />
                      <AE title="United Arab Emirates" />
                      <BR title="Brazil" />
                    </Box>
                    <Typography
                      sx={{
                        pt: 1
                      }}
                      variant="subtitle1"
                    >
                      {t(
                        'You can add and define translations for any language required. '
                      )}
                    </Typography>
                  </Box>
                </Grid>
                <Grid item xs={12} md={7}>
                  <ScreenshotWrapper>
                    <Screenshot
                      src="/static/images/overview/rtl-preview.jpg"
                      alt="RTL Preview"
                    />
                  </ScreenshotWrapper>
                </Grid>
              </Grid>
            </Container>
          </BoxRtl>
        )}
      </Container>
      <TypographyH1Primary
        textAlign="center"
        sx={{
          mt: 8,
          mb: 2
        }}
        variant="h1"
      >
        {t('choose_your_plan')}
      </TypographyH1Primary>
      <Box px={4}>
        <SubscriptionPlanSelector monthly={monthly} setMonthly={setMonthly} />
      </Box>

      <Container
        sx={{
          pt: { xs: 6, md: 12 },
          pb: { xs: 5, md: 15 }
        }}
        maxWidth="md"
      >
        <TypographyH1Primary
          textAlign="center"
          sx={{
            mb: 2
          }}
          variant="h1"
        >
          {t('leading_maintenance')}
        </TypographyH1Primary>
        <Container
          sx={{
            mb: 6,
            textAlign: 'center'
          }}
          maxWidth="sm"
        >
          <Button
            component={RouterLink}
            size="large"
            to="/account/register"
            variant="contained"
          >
            {t('register')}
          </Button>
        </Container>
      </Container>
    </BoxHighlights>
  );
}

export default Highlights;
