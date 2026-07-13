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
import { Link as RouterLink } from 'react-router-dom';
import LanguageSwitcher from 'src/layouts/ExtendedSidebarLayout/Header/Buttons/LanguageSwitcher';
import { ExpandMore, GitHub } from '@mui/icons-material';
import CheckCircleOutlineTwoToneIcon from '@mui/icons-material/CheckCircleOutlineTwoTone';
import { useEffect, useState } from 'react';
import { pricingPlans, planFeatureCategories } from './pricingData';
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

const PricingWrapper = styled(Box)(
  ({ theme }) => `
    overflow: auto;
    background: ${theme.palette.common.white};
    flex: 1;
    overflow-x: hidden;
`
);

function Pricing() {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();

  const [monthly, setMonthly] = useState<boolean>(true);
  const [selectedPlans, setSelectedPlans] = useState<string[]>([]);

  const isXs = useMediaQuery(theme.breakpoints.only('xs'));
  const isSm = useMediaQuery(theme.breakpoints.only('sm'));
  const isMdDown = useMediaQuery(theme.breakpoints.down('md'));

  // Set default selected plans based on screen size
  useEffect(() => {
    // Find the popular plan
    const popularPlan =
      pricingPlans.find((plan) => plan.popular)?.id || pricingPlans[0].id;

    if (isXs) {
      // For extra small screens, select 2 plans (popular plan + one more)
      const secondPlan =
        pricingPlans.find((plan) => plan.id !== popularPlan)?.id || '';
      setSelectedPlans([popularPlan, secondPlan]);
    } else if (isSm) {
      // For small screens, select 3 plans (popular plan + two more)
      const otherPlans = pricingPlans
        .filter((plan) => plan.id !== popularPlan)
        .slice(0, 2)
        .map((plan) => plan.id);
      setSelectedPlans([popularPlan, ...otherPlans]);
    } else {
      // For medium and up, show all plans
      setSelectedPlans(pricingPlans.map((plan) => plan.id));
    }
  }, [isXs, isSm, isMdDown]);

  return (
    <PricingWrapper>
      <Helmet>
        <title>Pricing - CMMS</title>
      </Helmet>
      <NavBar />

      <Container maxWidth="lg" sx={{ mt: 8 }}>
        <Box textAlign="center" mb={6}>
          <Typography variant="h1" component="h1" gutterBottom>
            {t('Choose your plan and get started')}
          </Typography>
          <Typography variant="subtitle1">
            {t(
              'Our software gives Maintenance and Reliability teams the tools they need to run Operations efficiently and effectively.'
            )}
          </Typography>
        </Box>

        <SubscriptionPlanSelector monthly={monthly} setMonthly={setMonthly} />
        <Box textAlign="center" my={6}>
          <Typography variant="h1" component="h1" gutterBottom>
            {t('Compare Plans and Pricing')}
          </Typography>
          <Typography variant="subtitle1">
            {t('See which plan is right for you')}
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
                  ? 'Select 2 plans to compare'
                  : 'Select 3 plans to compare'}
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
                        ? 'Select 2 plans to compare'
                        : 'Select 3 plans to compare'
                    }
                  />
                }
                renderValue={(selected) => (
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                    {selected.map((value) => {
                      const plan = pricingPlans.find((p) => p.id === value);
                      return <Chip key={value} label={plan?.name} />;
                    })}
                  </Box>
                )}
                sx={{ mb: 2 }}
              >
                {pricingPlans.map((plan) => (
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
                {pricingPlans
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
                          {`/${monthly ? `month per user` : 'year per user'}`}
                        </Typography>
                      )}
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
                          ? t('Get started')
                          : t('Try for free')}
                      </Button>
                    </Grid>
                  ))}
              </Grid>

              {planFeatureCategories.map((category, categoryIndex) => (
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

                      {pricingPlans
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
        <Faq />
      </Container>
    </PricingWrapper>
  );
}

export default Pricing;
