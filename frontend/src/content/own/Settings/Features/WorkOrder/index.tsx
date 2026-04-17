import { Box, Button, Grid } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Formik } from 'formik';
import CustomSwitch from '../../../components/form/CustomSwitch';
import useAuth from '../../../../../hooks/useAuth';
import { GeneralPreferences } from '../../../../../models/owns/generalPreferences';
import SettingsSection from '../../components/SettingsSection';
import { ChevronRight } from '@mui/icons-material';

function WorkOrderSettings() {
  const { t }: { t: any } = useTranslation();
  const navigate = useNavigate();
  const { patchGeneralPreferences, companySettings } = useAuth();
  const { generalPreferences } = companySettings;

  const switches: {
    title: string;
    description: string;
    name: keyof GeneralPreferences;
  }[] = [
    {
      title: t('auto_assign_wo'),
      description: t('auto_assign_wo_description'),
      name: 'autoAssignWorkOrders'
    },
    {
      title: t('disable_closed_wo_notification'),
      description: t('disable_closed_wo_notification_description'),
      name: 'disableClosedWorkOrdersNotif'
    },
    {
      title: t('ask_feedback_wo_closed'),
      description: t('ask_feedback_wo_closed_description'),
      name: 'askFeedBackOnWOClosed'
    },
    {
      title: t('include_labor_in_total_cost'),
      description: t('include_labor_in_total_cost_description'),
      name: 'laborCostInTotalCost'
    },
    {
      title: t('simplify_wo'),
      description: t('simplify_wo_description'),
      name: 'simplifiedWorkOrder'
    }
  ];

  const onSubmit = async (
    _values,
    { resetForm, setErrors, setStatus, setSubmitting }
  ) => {};

  return (
    <Grid item xs={12}>
      <Box p={4}>
        <Formik
          initialValues={generalPreferences}
          validationSchema={undefined}
          onSubmit={onSubmit}
        >
          {({
            errors,
            handleBlur,
            handleChange,
            handleSubmit,
            isSubmitting,
            touched,
            values
          }) => (
            <form onSubmit={handleSubmit}>
              <SettingsSection title={t('preferences')}>
                <Grid container spacing={2}>
                  {switches.map((element) => (
                    <CustomSwitch
                      key={element.name}
                      title={element.title}
                      description={element.description}
                      checked={values[element.name]}
                      name={element.name}
                      handleChange={(event) => {
                        handleChange(event);
                        patchGeneralPreferences({
                          [element.name]: event.target.checked
                        });
                      }}
                    />
                  ))}
                </Grid>
              </SettingsSection>
              <SettingsSection title={t('customize_work_order_form')}>
                <Box display="flex" flexDirection="column" gap={2}>
                  <Button
                    variant="text"
                    endIcon={<ChevronRight />}
                    onClick={() =>
                      navigate(
                        '/app/settings/features/work-order/custom-fields'
                      )
                    }
                    sx={{
                      justifyContent: 'space-between',
                      textTransform: 'none'
                    }}
                  >
                    {t('custom_fields')}
                  </Button>
                  <Button
                    variant="text"
                    endIcon={<ChevronRight />}
                    onClick={() =>
                      navigate(
                        '/app/settings/features/work-order/configure-fields'
                      )
                    }
                    sx={{
                      justifyContent: 'space-between',
                      textTransform: 'none'
                    }}
                  >
                    {t('configure_fields')}
                  </Button>
                </Box>
              </SettingsSection>
            </form>
          )}
        </Formik>
      </Box>
    </Grid>
  );
}

export default WorkOrderSettings;
