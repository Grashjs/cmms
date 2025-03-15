import {
  Box,
  Grid,
  MenuItem,
  Select,
  Typography,
  useTheme
} from '@mui/material';
import { Field } from 'formik';

type FieldType<Type> = { label: string; name: string; type?: Type };
export default function GrayWhiteSelector<Value extends string, Type>({
  fields,
  options,
  onFieldChange,
  getValue
}: {
  fields: FieldType<Type>[];
  options: { label: string; value: Value }[];
  onFieldChange?: (fieldName: string, value: Value, type?: Type) => void;
  getValue: (field: FieldType<Type>) => Value;
}) {
  const theme = useTheme();

  return (
    <>
      {fields.map((field, index) => (
        <Grid
          style={
            index % 2 === 0
              ? { backgroundColor: theme.colors.alpha.black[10] }
              : undefined
          }
          key={field.name}
          item
          xs={12}
          md={12}
          lg={12}
        >
          <Box
            display="flex"
            flexDirection="row"
            justifyContent="space-between"
            alignItems="center"
            padding={0.5}
          >
            <Typography variant="h6">{field.label}</Typography>
            <Field
              style={{ backgroundColor: 'white' }}
              as={Select}
              onChange={(event) =>
                onFieldChange(field.name, event.target.value, field.type)
              }
              value={getValue(field)}
              name={field.name}
            >
              {options.map((option) => (
                <MenuItem key={option.value} value={option.value}>
                  {option.label}
                </MenuItem>
              ))}
            </Field>
          </Box>
        </Grid>
      ))}
    </>
  );
}
