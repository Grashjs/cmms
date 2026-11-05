import DateTimePickerModal from 'react-native-modal-datetime-picker';
import { useContext, useState } from 'react';
import { View } from './Themed';
import { IconButton, Text, useTheme } from 'react-native-paper';
import { TouchableOpacity, View as RNView } from 'react-native';
import { useTranslation } from 'react-i18next';
import { CompanySettingsContext } from '../contexts/CompanySettingsContext';

export default function CustomDateTimePicker({
  onChange,
  value,
  label,
  error = false
}: {
  onChange: (date: Date) => void;
  value: Date;
  label: string;
  error?: boolean;
}) {
  const theme = useTheme();
  const { t } = useTranslation();
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
        accessibilityRole="button"
        accessibilityLabel={label}
        style={{
          display: 'flex',
          flexDirection: 'row',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}
      >
        <Text>{label}</Text>
        <RNView style={{ flexDirection: 'row', alignItems: 'center' }}>
          <Text
            style={{
              color: value
                ? theme.colors.primary
                : error
                ? theme.colors.error
                : theme.colors.onSurfaceVariant
            }}
          >
            {value ? getFormattedDate(value) : t('select_date')}
          </Text>
          <IconButton
            icon={value ? 'calendar-edit' : 'calendar-plus'}
            iconColor={error ? theme.colors.error : theme.colors.primary}
            onPress={showDatePicker}
            accessibilityLabel={label}
          />
        </RNView>
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
