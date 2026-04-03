import { Box, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';

function Webhooks() {
  const { t }: { t: any } = useTranslation();

  return (
    <Box display="flex" flexDirection="column" alignItems="center" justifyContent="center" py={8}>
      <Typography variant="h4" color="textSecondary" gutterBottom>
        {t('webhooks')}
      </Typography>
      <Typography variant="body2" color="textSecondary">
        Coming soon...
      </Typography>
    </Box>
  );
}

export default Webhooks;
