import { useState } from 'react';
import { TextInput } from 'react-native-paper';
import { isNumeric, sanitizeDecimalInput } from '../utils/validators';

export default function NumberInput(props: {
  style: any;
  mode: 'flat' | 'outlined';
  error: boolean;
  label: string;
  defaultValue: string;
  placeholder: string;
  onBlur: (e) => void;
  onChangeText: (value: string) => void;
  disabled: boolean;
  multiline: boolean;
}) {
  const [numberInputValue, setNumberInputValue] = useState<string>(
    isNumeric(props.defaultValue) ? props.defaultValue?.toString() || '' : ''
  );

  return (
    <TextInput
      {...props}
      value={numberInputValue}
      keyboardType="decimal-pad"
      onChangeText={(newValue) => {
        // digits and a single decimal separator; "," is accepted as "."
        const formatted = sanitizeDecimalInput(newValue);
        setNumberInputValue(formatted);
        props.onChangeText(
          formatted === '' || formatted === '.' ? '0' : formatted
        );
      }}
    />
  );
}
