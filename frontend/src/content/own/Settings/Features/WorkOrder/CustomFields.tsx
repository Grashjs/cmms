import { Box, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';

function CustomFields() {
  const { t }: { t: any } = useTranslation();

  return (
    <Box p={4}>
      <Typography variant="h4">{t('custom_fields')}</Typography>
      {/* Blank page for now */}
    </Box>
  );
}

export default CustomFields;
