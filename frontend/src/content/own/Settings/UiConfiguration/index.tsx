import { Box, Grid } from '@mui/material';
import { useTranslation } from 'react-i18next';
import SettingsLayout from '../SettingsLayout';
import useAuth from '../../../../hooks/useAuth';
import { UiConfiguration } from '../../../../models/owns/uiConfiguration';
import { Formik } from 'formik';
import { FieldConfigurationsType } from '../../../../contexts/JWTAuthContext';
import GrayWhiteSelector from '../components/GrayWhiteSelector';
import { useDispatch } from '../../../../store';
import Loading from '../../Analytics/Loading';

function UiConfigurationSettings() {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const {
    patchUiConfiguration,
    user: { uiConfiguration }
  } = useAuth();
  const fields: { label: string; name: keyof Omit<UiConfiguration, 'id'> }[] = [
    { label: t('requests'), name: 'requests' },
    {
      label: t('location'),
      name: 'locations'
    },
    {
      label: t('meters'),
      name: 'meters'
    },
    { label: t('vendors_customers'), name: 'vendorsAndCustomers' }
  ];

  const options: { label: string; value: string }[] = [
    { label: t('show'), value: true.toString() },
    { label: t('hide'), value: false.toString() }
  ];

  return (
    <Formik initialValues={{}} onSubmit={() => null}>
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
          <Grid container spacing={1}>
            <Grid item xs={12}>
              <Box p={4}>
                {uiConfiguration ? (
                  <GrayWhiteSelector
                    fields={fields}
                    options={options}
                    onFieldChange={(
                      field,
                      value,
                      type: FieldConfigurationsType
                    ) =>
                      patchUiConfiguration({
                        ...uiConfiguration,
                        [field]: value === 'true'
                      })
                    }
                    getValue={(field) => uiConfiguration[field.name]}
                  />
                ) : (
                  <Loading />
                )}
              </Box>
            </Grid>
          </Grid>
        </form>
      )}
    </Formik>
  );
}

export default UiConfigurationSettings;
