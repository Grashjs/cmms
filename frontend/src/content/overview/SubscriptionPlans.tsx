import React, { useEffect } from 'react';
import { useDispatch, useSelector } from '../../store';
import { getSubscriptionPlans } from '../../slices/subscriptionPlan';
import {
  Box,
  Button,
  Card,
  CardContent,
  Grid,
  Stack,
  Typography,
  useTheme
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Link as RouterLink } from 'react-router-dom';

export default function SubscriptionPlans() {
  const { subscriptionPlans: unorderedSubscriptionPlans } = useSelector(
    (state) => state.subscriptionPlans
  );
  const dispatch = useDispatch();
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const subscriptionPlans = unorderedSubscriptionPlans.slice();

  subscriptionPlans.sort(function (a, b) {
    return a.monthlyCostPerUser - b.monthlyCostPerUser;
  });
  useEffect(() => {
    dispatch(getSubscriptionPlans());
  }, []);
  return (
    <Box mt={4}>
      <Typography variant={'h1'}>{t('choose_your_plan')}</Typography>
      <Grid container spacing={2} mt={2}>
        {subscriptionPlans.map((plan) => (
          <Grid item xs={12} md={4} key={plan.id}>
            <Card>
              <CardContent>
                <Typography fontWeight={'bold'} fontSize={24}>
                  {plan.name}
                </Typography>
                {plan.code === 'BUSINESS' ? (
                  <Box sx={{ my: 3 }}>
                    <Typography fontSize={15}>
                      {t('request_pricing')}
                    </Typography>
                  </Box>
                ) : (
                  <Stack
                    direction={'row'}
                    alignItems={'center'}
                    spacing={1}
                    mt={1}
                    mb={4}
                  >
                    {' '}
                    <Typography
                      fontWeight={'bold'}
                      fontSize={45}
                      color={'primary'}
                    >{`$${plan.monthlyCostPerUser}`}</Typography>
                    <Typography fontSize={15}>{t('per_user_month')}</Typography>
                  </Stack>
                )}
                <Typography fontSize={15}>
                  {t(`${plan.code}_description`)}
                </Typography>
                {plan.code === 'BUSINESS' ? (
                  <Button
                    sx={{ mt: 2 }}
                    onClick={() =>
                      window.open(
                        'mailto:contact@atlas-cmms.com?subject=Business%20plan&body=Hi.%0D%0AI%20would%20like%20to%20have%20access%20to%20the%20Business%20plan.%20I%20need%20...',
                        '_blank'
                      )
                    }
                    fullWidth
                    variant={'contained'}
                  >
                    {t('book_demo')}
                  </Button>
                ) : (
                  <Box
                    display={'flex'}
                    flexDirection={'column'}
                    justifyContent={'center'}
                    alignItems={'center'}
                  >
                    <Button
                      fullWidth
                      sx={{ mt: 2 }}
                      variant={'outlined'}
                      component={RouterLink}
                      to="/account/register"
                    >
                      {t('try_for_free')}
                    </Button>
                    <Typography mt={1}>{t('no_credit_card')}</Typography>
                  </Box>
                )}
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}
