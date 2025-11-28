import { Helmet } from 'react-helmet-async';
import PersonTwoToneIcon from '@mui/icons-material/PersonTwoTone';
import { randomInt } from '../../../../utils/generators';
import {
  Box,
  Button,
  Card,
  CircularProgress,
  FormControlLabel,
  Grid,
  Link,
  Radio,
  RadioGroup,
  Slider,
  Stack,
  Typography,
  useTheme
} from '@mui/material';
import { Trans, useTranslation } from 'react-i18next';
import { useContext, useEffect, useState } from 'react';
import PlanFeatures from './PlanFeatures';
import { TitleContext } from '../../../../contexts/TitleContext';
import { useDispatch, useSelector } from '../../../../store';
import { getSubscriptionPlans } from '../../../../slices/subscriptionPlan';
import useAuth from '../../../../hooks/useAuth';
import PermissionErrorMessage from '../../components/PermissionErrorMessage';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { SubscriptionPlan } from '../../../../models/owns/subscriptionPlan';
import { useNavigate } from 'react-router-dom';
import { CompanySettingsContext } from '../../../../contexts/CompanySettingsContext';
import { Order } from '../../../../models/owns/fastspring';
import api from '../../../../utils/api';
import { useBrand } from '../../../../hooks/useBrand';
import { fireGa4Event } from '../../../../utils/overall';

function SubscriptionPlans() {
  const { t }: { t: any } = useTranslation();
  const { company, user, patchSubscription } = useAuth();
  const brandConfig = useBrand();
  const subscription = company.subscription;
  const theme = useTheme();
  const [item, setItem] = useState(null);
  const [usersCount, setUsersCount] = useState<number>(
    company.subscription.usersCount
  );
  const [period, setPeriod] = useState<'monthly' | 'annually'>('monthly');
  const [selectedPlan, setSelectedPlan] = useState<string>('STARTER');
  const [selectedPlanObject, setSelectedPlanObject] =
    useState<SubscriptionPlan>();
  const { subscriptionPlans } = useSelector((state) => state.subscriptionPlans);
  const { setTitle } = useContext(TitleContext);
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { getFormattedCurrency } = useContext(CompanySettingsContext);
  const [submitting, setSubmitting] = useState(false);
  const dispatch = useDispatch();
  const navigate = useNavigate();

  useEffect(() => {
    setTitle(t('plans'));
    if (user.ownsCompany) {
      dispatch(getSubscriptionPlans());
      setPeriod(company.subscription.monthly ? 'monthly' : 'annually');
    }
  }, []);
  useEffect(() => {
    if (subscriptionPlans.length) {
      setSelectedPlan(company.subscription.subscriptionPlan.code);
    }
  }, [subscriptionPlans]);

  const [country, setCountry] = useState('US');
  const buyProduct = () => {
    let path;
    switch (selectedPlanObject.code) {
      case 'STARTER':
        path = 'starter';
        break;
      case 'BUSINESS':
        path = 'business';
        break;
      case 'PROFESSIONAL':
        path = 'professional';
        break;
      default:
        break;
    }
    path = `${path}-${period === 'monthly' ? 'monthly' : 'annually'}`;
    // @ts-ignore
    window.fastspring.builder.push({
      products: [
        {
          path,
          quantity: usersCount
        }
      ],
      paymentContact: {
        email: user.email,
        firstName: user.firstName,
        lastName: user.lastName,
        phoneNumber: user?.phone ?? null
      },
      tags: {
        userId: user.id
      },
      checkout: true
    });
  };
  const onUpgradeRequest = async () => {
    setSubmitting(true);
    const cost = getCost();
    fireGa4Event({
      category: 'Pricing',
      action: 'Upgrade_Request',
      label: 'Upgrade_Request',
      value: period == 'monthly' ? cost : cost * 10
    });

    const payload = {
      code: selectedPlanObject.code,
      monthly: period === 'monthly',
      usersCount
    };
    try {
      const { success } = await api.post<{ success: boolean }>(
        'subscriptions/request-upgrade',
        payload
      );
      if (success) {
        showSnackBar(t('upgrade_request_success'), 'success');
        return;
      }
    } catch (err) {
      showSnackBar(t('failure'), 'error');
      return;
    } finally {
      setSubmitting(false);
    }
  };
  useEffect(() => {
    const onNewOrder = (order: Order) => {
      setUsersCount(order.items[0].quantity);
    };
    // Add SBL script programatically
    const scriptId = 'fsc-api';
    const existingScript = document.getElementById(scriptId);
    if (!existingScript) {
      const storeFrontToUse = 'grash.test.onfastspring.com/popup-grash';
      const script = document.createElement('script');
      script.type = 'text/javascript';
      script.id = scriptId;
      script.src =
        'https://d1f8f9xcsvx3ha.cloudfront.net/sbl/0.9.3/fastspring-builder.js';
      script.dataset.storefront = storeFrontToUse;
      // Make sure to add callback function to window so that the DOM is aware of it
      // @ts-ignore
      window.onNewOrder = onNewOrder;
      script.setAttribute('data-data-callback', 'fastSpringCallBack');
      script.setAttribute('data-popup-webhook-received', 'onNewOrder');
      document.body.appendChild(script);
    }
    return () => {
      document.body.removeChild(existingScript);
    };
  }, []);

  useEffect(() => {
    // @ts-ignore
    window.onPopupClosed = (data: { id: string | null }) => {
      if (data?.id) {
        patchSubscription({
          id: randomInt(),
          usersCount,
          monthly: period === 'monthly',
          subscriptionPlan: selectedPlanObject
        }).then(onSubcriptionPatchSuccess);
      } else {
        onSubcriptionPatchFailure();
      }
    };
    const script = document.getElementById('fsc-api');
    if (script) script.setAttribute('data-popup-closed', 'onPopupClosed');
  }, [selectedPlanObject]);

  const periods = [
    { name: t('monthly'), value: 'monthly' },
    { name: t('annually'), value: 'annually' }
  ];

  useEffect(() => {
    setSelectedPlanObject(
      subscriptionPlans.find((plan) => plan.code == selectedPlan)
    );
  }, [selectedPlan, subscriptionPlans]);

  const getCost = () => {
    const selectedPlanData = subscriptionPlans.find(
      (plan) => plan.code == selectedPlan
    );
    return selectedPlanData
      ? selectedPlanData[
          period == 'monthly' ? 'monthlyCostPerUser' : 'yearlyCostPerUser'
        ] * usersCount
      : 0;
  };

  const onSubcriptionPatchSuccess = () => {
    showSnackBar(t('subscription_change_success'), 'success');
    navigate('/app/work-orders');
  };
  const onSubcriptionPatchFailure = () => {
    showSnackBar(t("The Subscription couldn't be changed"), 'error');
  };

  useEffect(() => {
    fireGa4Event('pricing_view');
  }, []);

  if (user.ownsCompany)
    return (
      <>
        <Helmet>
          <title>{t('plan')}</title>
        </Helmet>
        <Grid
          container
          justifyContent="center"
          alignItems="stretch"
          spacing={2}
          padding={4}
        >
          <Grid item xs={12}>
            <Card
              sx={{
                p: 2,
                display: 'flex',
                flexDirection: 'row',
                alignItems: 'center',
                justifyContent: 'space-between'
              }}
            >
              <Stack direction="row" spacing={1}>
                <Typography variant="h6" fontWeight="bold">
                  {t('current_plan')}
                </Typography>
                <Typography variant="h6">
                  {subscription.subscriptionPlan.name}
                </Typography>
              </Stack>
              <Stack direction="row" spacing={1}>
                <PersonTwoToneIcon />
                <Typography variant="h6">
                  {subscription.usersCount} {t(`Users`)}
                </Typography>
              </Stack>
            </Card>
            <Grid item xs={12}>
              <Card sx={{ p: 2, mt: 2 }}>
                <Box>
                  <Box>
                    <Typography variant="h4" gutterBottom>
                      {t('number_users_who_will_use_grash', {
                        shortBrandName: brandConfig.shortName
                      })}
                    </Typography>
                    <Typography variant="subtitle2">
                      <Trans
                        i18nKey={'pay_only_for_roles'}
                        components={{ italic: <i />, bold: <strong /> }}
                      />
                    </Typography>
                  </Box>
                  <Stack
                    direction="row"
                    spacing={2}
                    sx={{ my: 3 }}
                    alignItems={'center'}
                  >
                    <Slider
                      size="medium"
                      value={usersCount}
                      min={0}
                      step={1}
                      max={150}
                      onChange={(_, value) => setUsersCount(value as number)}
                    />
                    <Typography
                      sx={{
                        p: 1,
                        backgroundColor: theme.colors.alpha.black[5],
                        border: 0.5,
                        display: 'flex',
                        flexDirection: 'row',
                        alignItems: 'center',
                        borderRadius: 1
                      }}
                      fontWeight={'bold'}
                      variant="h6"
                    >
                      {t('users_count_display', { count: usersCount })}
                    </Typography>
                  </Stack>
                </Box>
                <Box>
                  <Typography variant="h4">
                    {t('how_will_you_be_billed')}
                  </Typography>
                  <RadioGroup
                    sx={{ p: 2, my: 1 }}
                    value={period}
                    onChange={(event) => {
                      setPeriod(event.target.value as 'monthly' | 'annually');
                    }}
                    name="period"
                  >
                    <Grid container>
                      <Grid item xs={12} md={6}>
                        <Grid container spacing={1}>
                          {periods.map((item) => (
                            <Grid item xs={12} md={6} key={item.value}>
                              <FormControlLabel
                                sx={{
                                  border: 2,
                                  borderColor:
                                    item.value === period
                                      ? theme.colors.primary.main
                                      : theme.colors.alpha.black[30],
                                  p: 2,
                                  backgroundColor:
                                    item.value === period
                                      ? theme.colors.primary.lighter
                                      : null
                                }}
                                value={item.value}
                                control={<Radio />}
                                label={
                                  <Typography variant="h6" fontWeight="bold">
                                    {item.name}
                                  </Typography>
                                }
                              />
                            </Grid>
                          ))}
                        </Grid>
                      </Grid>
                    </Grid>
                  </RadioGroup>
                </Box>
                <Box>
                  <Typography variant="h4" gutterBottom>
                    {t('which_plan_fits_you')}
                  </Typography>
                  <Typography variant="h6">
                    {t('checkout_our')}{' '}
                    <Link href="/pricing">{t('pricing_page')}</Link>{' '}
                    {t('for_more_details')}
                  </Typography>
                  <RadioGroup
                    sx={{ p: 2, my: 1 }}
                    value={selectedPlan}
                    onChange={(event) => {
                      setSelectedPlan(event.target.value);
                    }}
                    name="plans"
                  >
                    <Grid container spacing={1}>
                      {[...subscriptionPlans]
                        .sort(
                          (a, b) => a.monthlyCostPerUser - b.monthlyCostPerUser
                        )
                        .map((plan) => (
                          <Grid item xs={12} md={4} key={plan.id}>
                            <FormControlLabel
                              sx={{
                                border: 2,
                                borderColor:
                                  plan.code === selectedPlan
                                    ? theme.colors.primary.main
                                    : theme.colors.alpha.black[30],
                                p: 2,
                                backgroundColor:
                                  plan.code === selectedPlan
                                    ? theme.colors.primary.lighter
                                    : null
                              }}
                              value={plan.code}
                              control={<Radio />}
                              label={
                                <Box>
                                  <Typography variant="h6" fontWeight="bold">
                                    {plan.name}
                                  </Typography>
                                  {plan.code === 'BUSINESS' ? (
                                    <Typography variant="subtitle1">
                                      Custom pricing
                                    </Typography>
                                  ) : (
                                    <Typography variant="subtitle1">
                                      <b>
                                        {period == 'monthly'
                                          ? plan.monthlyCostPerUser
                                          : plan.yearlyCostPerUser}{' '}
                                        USD
                                      </b>{' '}
                                      {period == 'monthly'
                                        ? t('per_user_month')
                                        : t('per_user_year')}
                                    </Typography>
                                  )}
                                </Box>
                              }
                            />
                          </Grid>
                        ))}
                    </Grid>
                  </RadioGroup>
                </Box>
                <Box>
                  <Typography variant="h4" gutterBottom>
                    {t('features')}
                  </Typography>
                  <PlanFeatures features={selectedPlanObject?.features ?? []} />
                </Box>
                <Box
                  sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    my: 3
                  }}
                >
                  {selectedPlanObject?.code !== 'BUSINESS' && (
                    <Typography sx={{ my: 2 }} variant="h4" gutterBottom>
                      {t('you_will_be_charged')} <b>{`$ ${getCost()}`}</b>{' '}
                      {period == 'monthly'
                        ? t('monthly_adverb')
                        : t('yearly_adverb')}
                    </Typography>
                  )}
                  <Button
                    onClick={onUpgradeRequest}
                    size="large"
                    variant="contained"
                    startIcon={submitting && <CircularProgress size="1rem" />}
                    disabled={!selectedPlan || submitting}
                  >
                    {t('request_upgrade')}
                  </Button>
                </Box>
              </Card>
            </Grid>
          </Grid>
        </Grid>
      </>
    );
  else return <PermissionErrorMessage message={'no_access_page'} />;
}

export default SubscriptionPlans;
