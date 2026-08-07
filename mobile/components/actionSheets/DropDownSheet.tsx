import React from 'react';
import { TouchableOpacity, View } from 'react-native';
import ActionSheet, {
  SheetManager,
  SheetProps
} from 'react-native-actions-sheet';
import { Text, useTheme } from 'react-native-paper';

interface DropDownItem {
  label: string;
  value: string;
}

const DropDownSheet = (
  props: SheetProps<{
    items: DropDownItem[];
    value: string;
    setValue: (value: string) => void;
  }>
) => {
  const theme = useTheme();
  const { value, items, setValue } = props.payload;

  const handleSelect = (selectedValue: string) => {
    setValue(selectedValue);
    SheetManager.hide(props.sheetId);
  };

  return (
    <ActionSheet
      id={props.sheetId}
      containerStyle={{
        backgroundColor: theme.colors.surface,
        borderTopLeftRadius: 14,
        borderTopRightRadius: 14
      }}
      indicatorStyle={{ backgroundColor: theme.colors.onSurfaceVariant }}
    >
      <View style={{ padding: 20, backgroundColor: theme.colors.surface }}>
        {items.map((item) => (
          <TouchableOpacity
            key={item.value}
            onPress={() => handleSelect(item.value)}
            style={{
              paddingVertical: 15,
              borderRadius: 8,
              backgroundColor:
                value === item.value
                  ? theme.colors.primaryContainer
                  : theme.colors.surface,
              marginBottom: 5
            }}
          >
            <Text
              style={{
                fontWeight: value === item.value ? 'bold' : 'normal',
                color:
                  value === item.value
                    ? theme.colors.primary
                    : theme.colors.onSurface
              }}
            >
              {item.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>
    </ActionSheet>
  );
};

export default DropDownSheet;
