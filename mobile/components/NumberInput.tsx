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
  const [numberInputValue, setNumberInputValue] = useState<number>(
    isNumeric(props.defaultValue) ? Number(props.defaultValue) : 0
  );
  return (
    <TextInput
      {...props}
      value={numberInputValue.toString()}
      keyboardType="decimal-pad"
      onChangeText={(newValue) => {
        const formattedValue = Number(newValue.replace(/[^0-9]/g, ''));
        setNumberInputValue(formattedValue);
        props.onChangeText(formattedValue.toString());
      }}
    />
  );
}
