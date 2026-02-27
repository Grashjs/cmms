"use client";
import { getPricingPlans, getSelfHostedPlans } from '../pricingData';
import {
  Box,
  Button,
  Card,
  CardContent,
  Grid,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Stack,
  Switch,
  Typography,
  useTheme
} from '@mui/material';
import CheckCircleOutlineTwoToneIcon from '@mui/icons-material/CheckCircleOutlineTwoTone';
import {useSearchParams} from 'next/navigation';
import { useRouter, Link } from 'src/i18n/routing';
import { useTranslations } from 'next-intl';
import { fireGa4Event } from 'src/utils/overall';
import {
  apiUrl,
  PADDLE_SECRET_TOKEN,
  paddleEnvironment
} from 'src/config';
import { useEffect, useRef, useState } from 'react';
import EmailModal from './EmailModal';
import { initializePaddle, Paddle } from '@paddle/paddle-js';

interface SubscriptionPlanSelectorProps {
  monthly: boolean;
  setMonthly: (value: ((prevState: boolean) => boolean) | boolean) => void;
  selfHosted?: boolean;
}
export const PRICING_YEAR_MULTIPLIER: number = 10;

export default function SubscriptionPlanSelector({
  monthly,
  setMonthly,
  selfHosted
}: SubscriptionPlanSelectorProps) {
  const theme = useTheme();
  const t = useTranslations();
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedPlan, setSelectedPlan] = useState(null);
  const router = useRouter();
  let paddle = useRef<Paddle | null>(null);
  const searchParams = useSearchParams();
  const _ptxn = searchParams.get('_ptxn');
  useEffect(() => {
    const initPaddle = async () => {
      paddle.current = await initializePaddle({
        token: PADDLE_SECRET_TOKEN,
        eventCallback: function (data) {
          if (data.name == 'checkout.completed') {
            router.push('/payment/success');
          }
        }
      });
      paddle.current.Environment.set(paddleEnvironment);
    };
    if (modalOpen || _ptxn) initPaddle();
  }, [modalOpen, _ptxn]);

  const handleOpenModal = (plan) => {
    setSelectedPlan(plan);
    setModalOpen(true);
  };

  const handleCloseModal = () => {
    setModalOpen(false);
    setSelectedPlan(null);
  };

  const handleCheckout = async (email: string) => {
    if (!selectedPlan) return;

    try {
      // Create Checkout Session on backend
      const response = await fetch(`${apiUrl}paddle/create-checkout-session`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          planId: selectedPlan.id + (monthly ? '-monthly' : '-yearly'),
          email: email
        })
      });

      const data = await response.json();
      if (data.sessionId) {
        paddle.current.Checkout.open({
          transactionId: data.sessionId,
          customer: {
            email: email.trim().toLowerCase()
          }
        });
      }
    } catch (error) {
      console.error('Failed to create checkout session:', error);
    }
    handleCloseModal();
  };

  return (
    <Box>
      <Box
        display={'flex'}
        alignItems={'center'}
        justifyContent={'center'}
        mb={2}
      >
        <Stack direction={'row'} spacing={2} alignItems={'center'}>
          <Typography color={'text.primary'}>{t('monthly')}</Typography>
          <Switch
            checked={!monthly}
            onChange={(event) => setMonthly(!event.target.checked)}
            sx={{ transform: 'scale(1.3)' }}
            size={'medium'}
          />
          <Typography color={'text.primary'}>{t('pricing_1.annually_save_two_months')}</Typography>
        </Stack>
      </Box>
      <Grid container spacing={2} justifyContent="center">
        {(selfHosted ? getSelfHostedPlans(t) : getPricingPlans(t)).map(
          (plan, index) => (
            <Grid item xs={12} md={3} key={plan.id}>
              <Card
                sx={{
                  position: 'relative',
                  transition: 'all .2s',
                  '&:hover': {
                    transform: 'translateY(-10px)'
                  }
                }}
              >
                {plan.popular && (
                  <Box
                    sx={{
                      background: theme.palette.success.main,
                      color: theme.palette.success.contrastText,
                      padding: theme.spacing(0.5, 1),
                      borderRadius: theme.shape.borderRadius,
                      fontSize: theme.typography.pxToRem(9),
                      fontWeight: 'bold',
                      textTransform: 'uppercase',
                      position: 'absolute',
                      top: 10,
                      right: theme.spacing(1)
                    }}
                  >
                    <span>✨ {t('pricing_1.most_popular')}</span>
                  </Box>
                )}
                <CardContent
                  sx={{
                    p: { xs: 2, md: 3 },
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column'
                  }}
                >
                  <Box mb={2}>
                    <Typography variant="h3" component="h3" gutterBottom>
                      {plan.name}
                    </Typography>
                    <Typography variant="subtitle2">
                      {plan.description}
                    </Typography>
                  </Box>

                  <Box mt={2} mb={3}>
                    {!parseFloat(plan.price) ? (
                      <Typography variant="h3" component="div">
                        {plan.price}
                      </Typography>
                    ) : (
                      <>
                        <Typography
                          variant="h2"
                          component="div"
                          color="primary"
                          sx={{ fontWeight: 'bold' }}
                        >
                          $
                          {monthly
                            ? plan.price
                            : parseFloat(plan.price) * PRICING_YEAR_MULTIPLIER}
                        </Typography>
                        <Typography variant="subtitle1" color="text.secondary">
                          {`/${
                            monthly
                              ? t('pricing_1.month_per_user')
                              : t('pricing_1.year_per_user')
                          }`}
                        </Typography>
                      </>
                    )}
                  </Box>

                  <List sx={{ mt: 2, flexGrow: 1 }}>
                    {plan.features
                      .slice(0, selfHosted ? 7 : 5)
                      .map((feature, featureIdx) => (
                        <ListItem
                          key={`${plan.id}-${featureIdx}`}
                          sx={{
                            px: 0,
                            py: 0.6
                          }}
                        >
                          <ListItemIcon sx={{ minWidth: 34 }}>
                            <CheckCircleOutlineTwoToneIcon
                              sx={{ color: 'primary.main' }}
                            />
                          </ListItemIcon>
                          <ListItemText primary={feature} />
                        </ListItem>
                      ))}
                  </List>

                  <Box mt="auto" pt={3}>
                    {selfHosted && plan.id === 'sh-free' ? null : (
                      <Button
                        fullWidth
                        variant="contained"
                        component={
                          selfHosted && plan.id !== 'sh-free'
                            ? 'button'
                            : Link
                        }
                        onClick={async () => {
                          if (plan.id !== 'basic') {
                            fireGa4Event({
                              category: 'Pricing',
                              action: 'Plan_Selection',
                              label: `${plan.id}_Trial`,
                              value:
                                plan.id === 'business'
                                  ? 100
                                  : Number(plan.price)
                            });
                          }

                          // Handle Stripe Checkout for self-hosted paid plans
                          if (selfHosted && plan.id !== 'sh-free') {
                            handleOpenModal(plan);
                          }
                        }}
                        href={
                          selfHosted && plan.id !== 'sh-free'
                            ? undefined
                            : selfHosted
                            ? '/account/register'
                            : '/account/register' +
                              (plan.id !== 'basic'
                                ? `?subscription-plan-id=${plan.id}`
                                : '')
                        }
                        sx={{ mb: 1 }}
                      >
                        {plan.id === 'basic' || plan.id === 'sh-free'
                          ? t('get_started')
                          : selfHosted
                          ? t('pricing_1.get_your_license')
                          : t('try_for_free')}
                      </Button>
                    )}
                    {!selfHosted && (
                      <Typography
                        variant="caption"
                        color="text.secondary"
                        align="center"
                        display="block"
                      >
                        {t('no_credit_card')}
                      </Typography>
                    )}
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          )
        )}
      </Grid>
      <EmailModal
        open={modalOpen}
        onClose={handleCloseModal}
        onSubmit={handleCheckout}
      />
    </Box>
  );
}

