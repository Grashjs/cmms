import { useState } from 'react';
import { TextInput } from 'react-native-paper';
import { isNumeric } from '../utils/validators';

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
        // allow digits and a single decimal point
        let formatted = newValue.replace(/[^0-9.]/g, '');
        const firstDot = formatted.indexOf('.');
        if (firstDot !== -1) {
          formatted =
            formatted.slice(0, firstDot + 1) +
            formatted.slice(firstDot + 1).replace(/\./g, '');
        }
        setNumberInputValue(formatted);
        props.onChangeText(
          formatted === '' || formatted === '.' ? '0' : formatted
        );
      }}
    />
  );
}
