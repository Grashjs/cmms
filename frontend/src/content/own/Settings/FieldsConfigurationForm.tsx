import { Box, Grid, Typography, useTheme } from '@mui/material';
import { Formik } from 'formik';
import { useTranslation } from 'react-i18next';
import { FC, useContext } from 'react';
import { FieldConfigurationsType } from '../../../contexts/JWTAuthContext';
import useAuth from '../../../hooks/useAuth';
import GrayWhiteSelector from './components/GrayWhiteSelector';
import { FieldType } from '../../../models/owns/fieldConfiguration';
import { getErrorMessage } from '../../../utils/api';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';

interface FieldsConfigurationFormProps {
  initialValues: any;
  fields: { label: string; name: string; type: FieldConfigurationsType }[];
}

const FieldsConfigurationForm: FC<FieldsConfigurationFormProps> = ({
  initialValues,
  fields
}) => {
  const { t }: { t: any } = useTranslation();
  const { patchFieldConfiguration, companySettings } = useAuth();
  const workOrderFieldConfigurations =
    companySettings?.workOrderConfiguration?.workOrderFieldConfigurations;
  const requestFieldConfigurations =
    companySettings?.workOrderRequestConfiguration?.fieldConfigurations;
  const theme = useTheme();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const renderFields = (
    fields: { label: string; name: string; type: FieldConfigurationsType }[]
  ) => {
    const options: { label: string; value: FieldType }[] = [
      { value: FieldType.OPTIONAL, label: t('optional') },
      { value: FieldType.REQUIRED, label: t('required') },
      { value: FieldType.HIDDEN, label: t('hidden') }
    ];
    return (
      !!companySettings && (
        <GrayWhiteSelector
          fields={fields}
          options={options}
          onFieldChange={(field, value, type: FieldConfigurationsType) =>
            patchFieldConfiguration(field, value, type).catch((err) =>
              showSnackBar(getErrorMessage(err), 'error')
            )
          }
          getValue={(field) =>
            field.type === 'workOrder'
              ? workOrderFieldConfigurations.find(
                  (fieldConfiguration) =>
                    fieldConfiguration.fieldName === field.name
                ).fieldType
              : requestFieldConfigurations.find(
                  (fieldConfiguration) =>
                    fieldConfiguration.fieldName === field.name
                ).fieldType
          }
        />
      )
    );
  };
  return (
    <Box>
      <Typography variant="h5" sx={{ mb: 2 }}>
        {t('field_configuration_description')}
      </Typography>
      <Formik initialValues={initialValues} onSubmit={() => null}>
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
              {renderFields(fields)}
            </Grid>
          </form>
        )}
      </Formik>
    </Box>
  );
};

export default FieldsConfigurationForm;
