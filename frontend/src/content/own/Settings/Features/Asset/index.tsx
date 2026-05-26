import { Box, Button, Grid } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Formik } from 'formik';
import CustomSwitch from '../../../components/form/CustomSwitch';
import useAuth from '../../../../../hooks/useAuth';
import { GeneralPreferences } from '../../../../../models/owns/generalPreferences';
import SettingsSection from '../../components/SettingsSection';
import { ChevronRight } from '@mui/icons-material';

function AssetSettings() {
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
      title: t('auto_generate_asset_barcode'),
      description: t('auto_generate_asset_barcode_description'),
      name: 'autoGenerateAssetBarcode'
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
              <SettingsSection title={t('customize_form')}>
                <Box display="flex" flexDirection="column" gap={2}>
                  <Button
                    variant="text"
                    endIcon={<ChevronRight />}
                    onClick={() =>
                      navigate('/app/settings/features/asset/custom-fields')
                    }
                    sx={{
                      justifyContent: 'space-between',
                      textTransform: 'none'
                    }}
                  >
                    {t('custom_fields')}
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

export default AssetSettings;
