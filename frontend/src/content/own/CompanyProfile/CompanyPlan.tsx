import {
  alpha,
  Avatar,
  Box,
  Button,
  Card,
  CircularProgress,
  Typography,
  useTheme
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { SubscriptionPlan } from '../../../models/owns/subscriptionPlan';
import CardMembershipTwoToneIcon from '@mui/icons-material/CardMembershipTwoTone';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import useAuth from '../../../hooks/useAuth';
import i18n from 'i18next';
import { useContext, useState } from 'react';
import mailToLink from 'mailto-link';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';

interface CompanyPlanProps {
  plan: SubscriptionPlan;
}

function CompanyPlan(props: CompanyPlanProps) {
  const { plan } = props;
  const { company, cancelSubscription, resumeSubscription, user } = useAuth();
  const { requestSubscriptionChange } = useContext(CompanySettingsContext);
  const navigate = useNavigate();
  const theme = useTheme();
  const [loadingCancel, setLoadingCancel] = useState<boolean>(false);
  const [loadingResume, setLoadingResume] = useState<boolean>(false);
  const { t }: { t: any } = useTranslation();
  const getLanguage = i18n.language;
  return (
    <Card
      sx={{
        background: `${theme.colors.gradients.blue4}`,
        color: `${theme.palette.getContrastText(theme.colors.primary.main)}`,
        display: 'flex',
        alignItems: 'flex-start',
        px: 3,
        py: 5,
        mb: 2
      }}
    >
      <Avatar
        sx={{
          mr: 3,
          mt: -1.8,
          width: 62,
          height: 62,
          color: `${theme.colors.warning.main}`,
          background: `${theme.palette.getContrastText(
            theme.colors.warning.main
          )}`
        }}
      >
        <CardMembershipTwoToneIcon
          sx={{
            fontSize: `${theme.typography.pxToRem(30)}`
          }}
        />
      </Avatar>
      <Box>
        <Typography
          sx={{
            pb: 1.5,
            color: `${theme.palette.getContrastText(theme.colors.primary.main)}`
          }}
          variant="h3"
        >
          {t('upgrade_plan')}
        </Typography>
        <Typography
          variant="subtitle2"
          sx={{
            lineHeight: 1.8,
            color: `${alpha(
              theme.palette.getContrastText(theme.colors.primary.main),
              0.8
            )}`
          }}
        >
          {t('you_are_using_plan', {
            planName: plan.name,
            expiration: new Date(company.subscription.endsOn).toLocaleString(
              getLanguage === 'fr' ? 'fr-FR' : undefined
            )
          })}
        </Typography>
        <Box sx={{ mt: 2 }}>
          <Button
            sx={{ mr: 2 }}
            variant="contained"
            to="/app/subscription/plans"
            component={RouterLink}
          >
            {t('upgrade_now')}
          </Button>
          <Button
            onClick={() => navigate('/pricing')}
            variant="contained"
            color="secondary"
            sx={{ mr: 2 }}
          >
            {t('learn_more')}
          </Button>
          {company.subscription.activated &&
            (company.subscription.cancelled ? (
              <Button
                onClick={() => {
                  setLoadingResume(true);
                  resumeSubscription().finally(() => setLoadingResume(false));
                }}
                variant="contained"
                color="success"
                disabled={loadingResume}
                startIcon={
                  loadingResume ? (
                    <CircularProgress color="success" size={'1rem'} />
                  ) : null
                }
              >
                {t('resume_subscription')}
              </Button>
            ) : (
              <Button
                onClick={() => {
                  if (window.confirm(t('confirm_cancel_subscription'))) {
                    setLoadingCancel(true);
                    cancelSubscription().finally(() => setLoadingCancel(false));
                  }
                }}
                variant="contained"
                color="error"
                disabled={loadingCancel}
                startIcon={
                  loadingCancel ? (
                    <CircularProgress color="error" size={'1rem'} />
                  ) : null
                }
              >
                {t('cancel_subscription')}
              </Button>
            ))}
        </Box>
      </Box>
    </Card>
  );
}

export default CompanyPlan;
