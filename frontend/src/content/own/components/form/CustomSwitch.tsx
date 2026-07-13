import { Box, Grid, Switch, Typography } from '@mui/material';
import { Field } from 'formik';
import { ChangeEvent } from 'react';

interface CustomSwitchProps {
  title: string;
  description: string;
  name: string;
  handleChange: (event: ChangeEvent<HTMLInputElement>) => void;
  checked: boolean;
}
export default function CustomSwitch(props: CustomSwitchProps) {
  const { name, title, description, handleChange, checked } = props;
  return (
    <Grid item xs={12} sx={{ mb: 2 }}>
      <Box display="flex" flexDirection="row" alignItems="center">
        <Field
          onChange={handleChange}
          checked={checked}
          as={Switch}
          name={name}
        />
        <Box display="flex" flexDirection="column">
          <Typography variant="h6" fontWeight="bold" sx={{ mb: 0.5 }}>
            {title}
          </Typography>
          <Typography variant="h6" fontStyle="italic">
            {description}
          </Typography>
        </Box>
      </Box>
    </Grid>
  );
}
