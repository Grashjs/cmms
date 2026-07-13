import { Box, Divider, Grid, Tab, Tabs } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { ChangeEvent, useState } from 'react';
import FieldsConfigurationForm from '../FieldsConfigurationForm';
import FeatureErrorMessage from '../../components/FeatureErrorMessage';
import useAuth from '../../../../hooks/useAuth';
import { PlanFeature } from '../../../../models/owns/subscriptionPlan';

function WorkOrderSettings() {
  const { t }: { t: any } = useTranslation();
  const [currentTab, setCurrentTab] = useState<string>('create');
  const { hasFeature } = useAuth();
  const tabs = [
    { value: 'create', label: t('creating_wo') },
    { value: 'complete', label: t('completing_wo') }
  ];
  const handleTabsChange = (_event: ChangeEvent<{}>, value: string): void => {
    setCurrentTab(value);
  };

  const createFields = [
    { label: t('description'), name: 'description' },
    {
      label: t('priority'),
      name: 'priority'
    },
    { label: t('images'), name: 'images' },
    { label: t('asset'), name: 'asset' },
    { label: t('primary_worker'), name: 'primaryUser' },
    { label: t('additional_workers'), name: 'assignedTo' },
    { label: t('team'), name: 'team' },
    { label: t('location'), name: 'location' },
    { label: t('due_date'), name: 'dueDate' },
    { label: t('category'), name: 'category' },
    { label: t('purchase_order'), name: 'purchaseOrder' },
    { label: t('files'), name: 'files' },
    { label: t('signature'), name: 'signature' }
  ];

  const completeFields = [
    { label: t('files'), name: 'completeFiles' },
    {
      label: t('tasks'),
      name: 'completeTasks'
    },
    { label: t('time'), name: 'completeTime' },
    { label: t('parts'), name: 'completeParts' },
    { label: t('cost'), name: 'completeCost' }
  ];

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
              <Tab key={tab.value} label={tab.label} value={tab.value} />
            ))}
          </Tabs>
          <Divider sx={{ mt: 1 }} />
          {!hasFeature(PlanFeature.REQUEST_CONFIGURATION) ? (
            <FeatureErrorMessage message="Upgrade to configure the work orders" />
          ) : (
            <Box p={3}>
              {currentTab === 'create' && (
                <FieldsConfigurationForm
                  initialValues={{}}
                  fields={createFields.map((field) => {
                    return { ...field, type: 'workOrder' };
                  })}
                />
              )}
              {currentTab === 'complete' && (
                <FieldsConfigurationForm
                  initialValues={{}}
                  fields={completeFields.map((field) => {
                    return { ...field, type: 'workOrder' };
                  })}
                />
              )}
            </Box>
          )}
        </Box>
      </Box>
    </Grid>
  );
}

export default WorkOrderSettings;
