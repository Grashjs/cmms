import { Text } from 'react-native-paper';
import { FilterField } from '../../../models/page';
import { useTranslation } from 'react-i18next';
import { TouchableOpacity } from 'react-native';
import * as React from 'react';
import { useAppTheme } from '../../../custom-theme';
import { fontWeight, radius, spacing, touchTarget } from '../../../theme/tokens';

interface OwnProps {
  filterFields: FilterField[];
  activeFilterField: FilterField;
  onChange: (filterFields: FilterField[]) => void;
}

export default function QuickFilter({
  filterFields,
  onChange,
  activeFilterField
}: OwnProps) {
  const { t } = useTranslation();
  const theme = useAppTheme();
  const isSelected: boolean = filterFields.some(
    (filterField) => filterField.field === activeFilterField.field
  );
  return (
    <TouchableOpacity
      accessibilityRole="button"
      accessibilityState={{ selected: isSelected }}
      onPress={() => {
        // Building a new array rather than pushing into the prop: the caller
        // holds this same reference in state, and mutating it meant the
        // before/after comparison that decides whether to refetch saw no change.
        const newFilterFields = isSelected
          ? filterFields.filter(
              (filterField) => filterField.field !== activeFilterField.field
            )
          : [...filterFields, activeFilterField];
        onChange(newFilterFields);
      }}
      style={{
        backgroundColor: isSelected
          ? theme.colors.primary
          : theme.colors.background,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        borderRadius: radius.pill,
        paddingHorizontal: spacing.lg,
        minHeight: touchTarget.min,
        margin: spacing.xs
      }}
    >
      <Text
        style={{
          color: isSelected ? theme.colors.white : theme.colors.text,
          fontWeight: fontWeight.bold
        }}
      >
        {t(activeFilterField.field)}
      </Text>
    </TouchableOpacity>
  );
}
