import { Box, Grid, Switch, SxProps, Theme, Typography } from '@mui/material';
import { Field } from 'formik';
import { ChangeEvent } from 'react';

interface CustomSwitchProps {
  title: string;
  description: string;
  name: string;
  handleChange: (event: ChangeEvent<HTMLInputElement>) => void;
  checked: boolean;
  sx?: SxProps<Theme>;
}
export default function CustomSwitch(props: CustomSwitchProps) {
  const { name, title, description, handleChange, checked, sx } = props;
  return (
    <Grid item xs={12} sx={{ mb: 2, ...sx }}>
      <Box display="flex" flexDirection="row" alignItems="center">
        <Field
          onChange={handleChange}
          checked={checked}
          as={Switch}
          name={name}
        />
        <Box display="flex" flexDirection="column">
          <Typography
            variant="h6"
            fontWeight="bold"
            sx={{ mb: description ? 0.5 : 0 }}
          >
            {title}
          </Typography>
          {description && (
            <Typography variant="h6" fontStyle="italic">
              {description}
            </Typography>
          )}
        </Box>
      </Box>
    </Grid>
  );
}
