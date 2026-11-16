import DateTimePickerModal from 'react-native-modal-datetime-picker';
import { useContext, useState } from 'react';
import { View } from './Themed';
import { Text, useTheme } from 'react-native-paper';
import { TouchableOpacity } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { CompanySettingsContext } from '../contexts/CompanySettingsContext';

export default function CustomDateTimePicker({
  onChange,
  value,
  label,
  showEmptyIcon = false
}: {
  onChange: (date: Date) => void;
  value: Date | null;
  label: string;
  showEmptyIcon?: boolean;
}) {
  const theme = useTheme();
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const [isDatePickerVisible, setDatePickerVisibility] = useState(false);
  const showDatePicker = () => {
    setDatePickerVisibility(true);
  };
  const hideDatePicker = () => {
    setDatePickerVisibility(false);
  };

  return (
    <View>
      <TouchableOpacity
        onPress={showDatePicker}
        style={{
          display: 'flex',
          flexDirection: 'row',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}
      >
        <Text>{label}</Text>
        {value && (
          <Text style={{ color: theme.colors.primary }}>
            {getFormattedDate(value)}
          </Text>
        )}
        {!value && showEmptyIcon && (
          <MaterialCommunityIcons name="calendar-clock" size={24} color={theme.colors.primary} />
        )}
      </TouchableOpacity>
      <DateTimePickerModal
        isVisible={isDatePickerVisible}
        mode="datetime"
        onConfirm={(newValue) => {
          onChange(newValue);
          hideDatePicker();
        }}
        date={value ?? new Date()}
        onCancel={hideDatePicker}
      />
    </View>
  );
}
