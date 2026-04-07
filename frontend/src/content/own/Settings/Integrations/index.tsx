import { Box, Grid, Tab, Tabs } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { ChangeEvent, useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import WebhookIcon from '@mui/icons-material/Webhook';
import KeyIcon from '@mui/icons-material/Key';
import LeakAddIcon from '@mui/icons-material/LeakAdd';
import Connectors from './Connectors';
import { PlanFeature } from '../../../../models/owns/subscriptionPlan';
import FeatureErrorMessage from '../../components/FeatureErrorMessage';
import useAuth from '../../../../hooks/useAuth';
import { useLicenseEntitlement } from '../../../../hooks/useLicenseEntitlement';

function IntegrationsLayout() {
  const { t }: { t: any } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { hasFeature } = useAuth();
  const hasAPIEntitlement = useLicenseEntitlement('API_ACCESS');

  const tabs = [
    {
      value: 'connectors',
      label: t('connectors'),
      icon: <LeakAddIcon />
    },
    {
      value: 'api-keys',
      label: t('api_keys'),
      icon: <KeyIcon />
    },
    {
      value: 'webhooks',
      label: t('webhooks'),
      icon: <WebhookIcon />
    }
  ];

  // Determine active tab from URL
  const pathSegments = location.pathname.split('/');
  const currentTab = pathSegments[pathSegments.length - 1] || 'connectors';
  const isValidTab = tabs.some((tab) => tab.value === currentTab);
  const activeTab = isValidTab ? currentTab : 'connectors';

  const handleTabsChange = (_event: ChangeEvent<{}>, value: string): void => {
    navigate(`/app/settings/integrations/${value}`);
  };

  if (!hasFeature(PlanFeature.API_ACCESS) || !hasAPIEntitlement) {
    return <FeatureErrorMessage message={'upgrade_api'} />;
  }

  return (
    <Grid item xs={12}>
      <Box p={4}>
        <Box>
          <Tabs
            onChange={handleTabsChange}
            value={activeTab}
            variant="scrollable"
            scrollButtons="auto"
            textColor="primary"
            indicatorColor="primary"
          >
            {tabs.map((tab) => (
              <Tab
                key={tab.value}
                icon={tab.icon}
                iconPosition="start"
                label={tab.label}
                value={tab.value}
              />
            ))}
          </Tabs>
          <Box>
            <Outlet />
          </Box>
        </Box>
      </Box>
    </Grid>
  );
}

export default IntegrationsLayout;
