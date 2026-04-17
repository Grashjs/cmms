import { Box, Button, Grid } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { ChevronRight } from '@mui/icons-material';
import SettingsSection from '../../components/SettingsSection';

function AssetSettings() {
  const { t }: { t: any } = useTranslation();
  const navigate = useNavigate();

  return (
    <Grid item xs={12}>
      <Box p={4}>
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
              {t('configure_fields')}
            </Button>
          </Box>
        </SettingsSection>
      </Box>
    </Grid>
  );
}

export default AssetSettings;
