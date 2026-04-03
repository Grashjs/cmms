import { Box, Divider, Grid, Tab, Tabs } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { ChangeEvent, useState, ReactNode } from 'react';
import ApiKeys from './ApiKeys';
import Connectors from './Connectors';
import Webhooks from './Webhooks';
import { Tab as MuiTab } from '@mui/material';
import LeakAddIcon from '@mui/icons-material/LeakAdd';
import KeyIcon from '@mui/icons-material/Key';
import WebhookIcon from '@mui/icons-material/Webhook';
import { PlanFeature } from '../../../../models/owns/subscriptionPlan';
import { PermissionEntity } from '../../../../models/owns/role';
import PermissionErrorMessage from '../../components/PermissionErrorMessage';
import FeatureErrorMessage from '../../components/FeatureErrorMessage';
import useAuth from '../../../../hooks/useAuth';
import { useLicenseEntitlement } from '../../../../hooks/useLicenseEntitlement';

function IntegrationsSettings() {
  const { t }: { t: any } = useTranslation();
  const [currentTab, setCurrentTab] = useState<string>('api-keys');
  const { hasFeature } = useAuth();
  const hasAPIEntitlement = useLicenseEntitlement('API_ACCESS');
  const tabs = [
    {
      value: 'connectors',
      label: t('connectors'),
      icon: <LeakAddIcon />,
      component: <Connectors />
    },
    {
      value: 'api-keys',
      label: t('api_keys'),
      icon: <KeyIcon />,
      component: <ApiKeys />
    },
    {
      value: 'webhooks',
      label: t('webhooks'),
      icon: <WebhookIcon />,
      component: <Webhooks />
    }
  ];

  const handleTabsChange = (_event: ChangeEvent<{}>, value: string): void => {
    setCurrentTab(value);
  };

  const currentComponent = tabs.find(
    (tab) => tab.value === currentTab
  )?.component;

  if (!hasFeature(PlanFeature.API_ACCESS) || !hasAPIEntitlement) {
    return <FeatureErrorMessage message={'upgrade_api'} />;
  }
  return (
    <Grid item xs={12}>
      <Box p={4}>
        <Box>
          <Tabs
            onChange={handleTabsChange}
            value={currentTab}
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
          <Box>{currentComponent}</Box>
        </Box>
      </Box>
    </Grid>
  );
}

export default IntegrationsSettings;
