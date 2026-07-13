import { Box, Grid } from '@mui/material';
import { useTranslation } from 'react-i18next';
import SettingsLayout from '../SettingsLayout';
import FieldsConfigurationForm from '../FieldsConfigurationForm';
import useAuth from '../../../../hooks/useAuth';
import FeatureErrorMessage from '../../components/FeatureErrorMessage';
import { PlanFeature } from '../../../../models/owns/subscriptionPlan';

function WorkOrderSettings() {
  const { t }: { t: any } = useTranslation();
  const { hasFeature } = useAuth();

  const fields = [
    { label: t('asset'), name: 'asset' },
    {
      label: t('location'),
      name: 'location'
    },
    { label: t('primary_worker'), name: 'primaryUser' },
    { label: t('due_date'), name: 'dueDate' },
    { label: t('category'), name: 'category' },
    { label: t('team'), name: 'team' }
  ];

  return (
    <Grid item xs={12}>
      <Box p={4}>
        {hasFeature(PlanFeature.REQUEST_CONFIGURATION) ? (
          <Box>
            <Box p={3}>
              <FieldsConfigurationForm
                initialValues={{}}
                fields={fields.map((field) => {
                  return { ...field, type: 'request' };
                })}
              />
            </Box>
          </Box>
        ) : (
          <FeatureErrorMessage message="Upgrade to configure the Request Form" />
        )}
      </Box>
    </Grid>
  );
}

export default WorkOrderSettings;
