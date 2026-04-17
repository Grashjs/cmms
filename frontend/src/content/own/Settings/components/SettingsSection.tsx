import { Box, Typography } from '@mui/material';
import { ReactNode } from 'react';

interface SectionProps {
  title: string;
  children: ReactNode;
}

function SettingsSection({ title, children }: SectionProps) {
  return (
    <Box sx={{ mb: 4 }}>
      <Typography variant="h6" sx={{ fontWeight: 'bold', mb: 2 }}>
        {title}
      </Typography>
      {children}
    </Box>
  );
}

export default SettingsSection;
