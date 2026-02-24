import {
  Box,
  Button,
  Card,
  CardContent,
  Container,
  Grid,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Stack,
  Tab,
  Tabs,
  Typography,
  styled,
  useTheme,
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Switch
} from '@mui/material';
import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';
import Logo from 'src/components/LogoSign';
import { Link as RouterLink, useLocation, useNavigate } from 'react-router-dom';
import LanguageSwitcher from 'src/layouts/ExtendedSidebarLayout/Header/Buttons/LanguageSwitcher';
import { ExpandMore, GitHub } from '@mui/icons-material';
import CheckCircleOutlineTwoToneIcon from '@mui/icons-material/CheckCircleOutlineTwoTone';
import { useEffect, useState } from 'react';
import {
  getPricingPlans,
  getPlanFeatureCategories,
  getSelfHostedPlans
} from './pricingData';
import NavBar from '../../components/NavBar';
import Faq from './components/Faq';
import SubscriptionPlanSelector, {
  PRICING_YEAR_MULTIPLIER
} from './components/SubscriptionPlanSelector';
import {
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  OutlinedInput,
  Chip,
  useMediaQuery
} from '@mui/material';
import { fireGa4Event } from '../../utils/overall';
import { Footer } from '../../components/Footer';
import CompanyLogos from '../landing/components/CompanyLogos';
import { useBrand } from 'src/hooks/useBrand';

const PricingWrapper = styled(Box)(
  ({ theme }) => `
    overflow: auto;
    background: ${theme.palette.common.white};
    flex: 1;
    overflow-x: hidden;
`
);

function Pricing() {
  const { t, i18n }: { t: any; i18n: any } = useTranslation();
  const theme = useTheme();
  const location = useLocation();
  const navigate = useNavigate();
  const langPrefix = i18n.language && i18n.language !== 'en' ? `${i18n.language}/` : '';

  const queryParams = new URLSearchParams(location.search);
  const type: 'selfhosted' | 'cloud' =
    queryParams.get('type') === 'selfhosted' ? 'selfhosted' : 'cloud';
  const [monthly, setMonthly] = useState<boolean>(true);
  const typePlans =
    type === 'cloud' ? getPricingPlans(t) : getSelfHostedPlans(t);
  const [selectedPlans, setSelectedPlans] = useState<string[]>([]);

  const isXs = useMediaQuery(theme.breakpoints.only('xs'));
  const isSm = useMediaQuery(theme.breakpoints.only('sm'));
  const isMdDown = useMediaQuery(theme.breakpoints.down('md'));
  const brandConfig = useBrand();

  const handleTabsChange = (
    _event: React.ChangeEvent<{}>,
    value: string
  ): void => {
    navigate(`${location.pathname}?type=${value}`);
  };

  // Set default selected plans based on screen size
  useEffect(() => {
    // Find the popular plan
    const popularPlan =
      typePlans.find((plan) => plan.popular)?.id || typePlans[0].id;

    if (isXs) {
      // For extra small screens, select 2 plans (popular plan + one more)
      const secondPlan =
        typePlans.find((plan) => plan.id !== popularPlan)?.id || '';
      setSelectedPlans([popularPlan, secondPlan].filter(Boolean));
    } else if (isSm) {
      // For small screens, select 3 plans (popular plan + two more)
      const otherPlans = typePlans
        .filter((plan) => plan.id !== popularPlan)
        .slice(0, 2)
        .map((plan) => plan.id);
      setSelectedPlans([popularPlan, ...otherPlans]);
    } else {
      // For medium and up, show all plans
      setSelectedPlans(typePlans.map((plan) => plan.id));
    }
  }, [isXs, isSm, isMdDown, type]);

  useEffect(() => {
    fireGa4Event('pricing_view');
  }, []);
  return (
    <PricingWrapper>
      <Helmet>
        <title>{t('pricing.title')}</title>
        <meta
          name="description"
          content={t('pricing.description')}
        />
        <link rel="canonical" href={`https://atlas-cmms.com/${langPrefix}pricing`} />
        <script type="application/ld+json">
          {`
            {
              "@context": "https://schema.org",
              "@type": "Product",
              "name": "Atlas CMMS",
              "description": "Flexible pricing plans for Atlas CMMS. Choose between Cloud and Self-Hosted versions of our open-source CMMS to optimize your maintenance operations.",
              "url": "https://atlas-cmms.com/pricing",
              "image": "https://atlas-cmms.com/static/images/logo/logo.png",
              "offers": [
                {
                  "@type": "Offer",
                  "name": "Basic",
                  "price": "0",
                  "priceCurrency": "USD",
                  "description": "For small teams getting started with maintenance management."
                },
                {
                  "@type": "Offer",
                  "name": "Starter",
                  "price": "10",
                  "priceCurrency": "USD",
                  "description": "For growing teams that need more advanced features."
                },
                {
                  "@type": "Offer",
                  "name": "Professional",
                  "price": "15",
                  "priceCurrency": "USD",
                  "description": "For established teams that require more customization and support."
                },
                {
                  "@type": "Offer",
                  "name": "Business",
                  "price": "40",
                  "priceCurrency": "USD",
                  "description": "For large organizations with complex needs and integrations."
                }
              ],
              "publisher": {
                "@type": "Organization",
                "name": "Atlas CMMS"
              }
            }
          `}
        </script>
      </Helmet>
      <NavBar />

      <Container maxWidth="lg" sx={{ mt: 8 }}>
        <Box textAlign="center" mb={6}>
          <Typography variant="h1" component="h1" gutterBottom>
            {t('pricing.choose_plan_and_get_started')}
          </Typography>
          <Typography variant="subtitle1">
            {t('pricing.slogan_effective_maintenance')}
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', justifyContent: 'center', mb: 4 }}>
          <Tabs
            value={type}
            onChange={handleTabsChange}
            indicatorColor="primary"
            textColor="primary"
          >
            <Tab label={t('cloud')} value="cloud" />
            <Tab label={t('self_hosted')} value="selfhosted" />
          </Tabs>
        </Box>

        <SubscriptionPlanSelector
          monthly={monthly}
          setMonthly={setMonthly}
          selfHosted={type === 'selfhosted'}
        />
        <CompanyLogos sx={{ mt: 4 }} />
        <Box textAlign="center" my={6}>
          <Typography variant="h1" component="h1" gutterBottom>
            {t('pricing.compare_plans_and_pricing')}
          </Typography>
          <Typography variant="subtitle1">
            {t('pricing.see_which_plan_is_right_for_you')}
          </Typography>

          {/* Plan selection dropdown for small/medium screens */}
          <Box
            sx={{
              mt: 3,
              display: { xs: 'block', md: 'none' },
              mx: 'auto',
              maxWidth: { xs: '100%', sm: '80%' }
            }}
          >
            <FormControl fullWidth>
              <InputLabel id="plan-comparison-select-label">
                {isXs
                  ? t('pricing.select_two_plans_to_compare')
                  : t('pricing.select_three_plans_to_compare')}
              </InputLabel>
              <Select
                labelId="plan-comparison-select-label"
                id="plan-comparison-select"
                multiple
                value={selectedPlans}
                onChange={(e) => setSelectedPlans(e.target.value as string[])}
                input={
                  <OutlinedInput
                    label={
                      isXs
                        ? t('pricing.select_two_plans_to_compare')
                        : t('pricing.select_three_plans_to_compare')
                    }
                  />
                }
                renderValue={(selected) => (
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                    {selected.map((value) => {
                      const plan = typePlans.find((p) => p.id === value);
                      return <Chip key={value} label={plan?.name} />;
                    })}
                  </Box>
                )}
                sx={{ mb: 2 }}
              >
                {typePlans.map((plan) => (
                  <MenuItem
                    key={plan.id}
                    value={plan.id}
                    disabled={
                      selectedPlans.length >= (isXs ? 2 : 3) &&
                      !selectedPlans.includes(plan.id)
                    }
                  >
                    {plan.name} {plan.popular && '✨'}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
        </Box>
        <Card>
          <CardContent sx={{ p: 4 }}>
            <Box>
              <Grid container>
                <Grid item xs={12} md={4}>
                  {/* Empty grid for alignment */}
                </Grid>
                {/* Filter plans based on selection for small/medium screens */}
                {typePlans
                  .filter(
                    (plan) => !isMdDown || selectedPlans.includes(plan.id)
                  )
                  .map((plan) => (
                    <Grid
                      item
                      xs={6}
                      sm={4}
                      md={2}
                      key={`compare-header-${plan.id}`}
                      sx={{ textAlign: 'center' }}
                    >
                      <Typography variant="h5" gutterBottom>
                        {plan.name}
                      </Typography>
                      {!parseFloat(plan.price) ? (
                        <Typography variant="body2" color="textSecondary">
                          {plan.price}
                        </Typography>
                      ) : (
                        <Typography variant="h6" color="primary">
                          $
                          {monthly
                            ? plan.price
                            : parseFloat(plan.price) * PRICING_YEAR_MULTIPLIER}
                          {`/${
                            monthly
                              ? t('pricing.month_per_user')
                              : t('pricing.year_per_user')
                          }`}
                        </Typography>
                      )}
                      {type === 'cloud' && (
                        <Button
                          size="small"
                          variant="outlined"
                          component={RouterLink}
                          to={
                            '/account/register' +
                            (plan.id !== 'basic'
                              ? `?subscription-plan-id=${plan.id}`
                              : '')
                          }
                          sx={{ mt: 1, mb: 2 }}
                        >
                          {plan.id === 'basic'
                            ? t('get_started')
                            : t('try_for_free')}
                        </Button>
                      )}
                    </Grid>
                  ))}
              </Grid>

              {getPlanFeatureCategories(t).map((category, categoryIndex) => (
                <Box key={`category-${categoryIndex}`} sx={{ mb: 4 }}>
                  <Typography
                    variant="h6"
                    sx={{ mb: 2, mt: 3, fontWeight: 'bold' }}
                  >
                    {category.name}
                  </Typography>

                  {category.features.map((feature, featureIndex) => (
                    <Grid
                      container
                      key={`feature-${categoryIndex}-${featureIndex}`}
                      sx={{
                        py: 1,
                        borderBottom: `1px solid ${theme.colors.alpha.black[10]}`,
                        backgroundColor: featureIndex % 2 ? '#F2F5F9' : 'white',
                        '&:hover': {
                          backgroundColor: theme.colors.alpha.black[5]
                        }
                      }}
                    >
                      <Grid item xs={12} md={4}>
                        <Typography variant="body2">{feature.name}</Typography>
                      </Grid>

                      {typePlans
                        .filter(
                          (plan) => !isMdDown || selectedPlans.includes(plan.id)
                        )
                        .map((plan) => (
                          <Grid
                            item
                            xs={6}
                            sm={4}
                            md={2}
                            key={`feature-${categoryIndex}-${featureIndex}-${plan.id}`}
                            sx={{
                              textAlign: 'center',
                              display: 'flex',
                              justifyContent: 'center',
                              alignItems: 'center'
                            }}
                          >
                            {feature.availability[plan.id] === true && (
                              <CheckCircleOutlineTwoToneIcon
                                fontSize={isMdDown ? 'small' : 'medium'}
                                color="primary"
                              />
                            )}
                            {feature.availability[plan.id] === false && (
                              <Typography variant="body2">–</Typography>
                            )}
                            {typeof feature.availability[plan.id] ===
                              'string' && (
                              <Typography variant="body2">
                                {feature.availability[plan.id]}
                              </Typography>
                            )}
                          </Grid>
                        ))}
                    </Grid>
                  ))}
                </Box>
              ))}
            </Box>
          </CardContent>
        </Card>
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            mt: 8,
            mb: 4
          }}
        >
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <Button
              component={RouterLink}
              to="/account/register"
              size="large"
              variant="contained"
              sx={{ px: 4 }}
            >
              {t('get_started')}
            </Button>
            <Button
              href={`mailto:${brandConfig.mail}`}
              size="large"
              variant="outlined"
              onClick={() => {
                fireGa4Event('contact_us_click');
                window.location.href = `mailto:${brandConfig.mail}`;
              }}
              sx={{ px: 4 }}
            >
              {t('talk_to_sales')}
            </Button>
          </Stack>
        </Box>
        <Faq />
      </Container>
      <Footer />
    </PricingWrapper>
  );
}

export default Pricing;
