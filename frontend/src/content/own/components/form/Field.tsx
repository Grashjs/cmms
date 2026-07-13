import { InputAdornment, TextField } from '@mui/material';
import { IField } from '../../type';
import { useTranslation } from 'react-i18next';

interface PropsType extends IField {
  onChange: (event: any) => void;
  onBlur?: (event: any) => any;
  value: any | '';
  placeholder?: string;
  error?: any;
  isDisabled?: boolean;
  //   fieldStyle?: any;
  errorMessage?: any;
  fullWidth?: boolean;
  //   helperText?: string;
  variant?: 'outlined' | 'filled' | 'standard';
  required?: boolean;
}

export default (props: PropsType) => {
  const { t }: { t: any } = useTranslation();

  return (
    <TextField
      error={props.error}
      fullWidth={props.fullWidth || true}
      helperText={t(props.error ? props.errorMessage : props.helperText)}
      label={t(`${props.label}`)}
      placeholder={props.placeholder ?? props.label}
      name={props.name}
      onBlur={props.onBlur}
      type={props.type}
      onChange={props.onChange}
      value={props.value ?? ''}
      variant={'outlined'}
      disabled={props.isDisabled}
      required={props.required || false}
      multiline={props.multiple}
      rows={props.multiple && 4}
      InputProps={
        props.icon && {
          startAdornment: (
            <InputAdornment position="start">{props.icon}</InputAdornment>
          )
        }
      }
      inputProps={{
        min: '0'
      }}
    />
  );
};
