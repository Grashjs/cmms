import {
  Checkbox,
  Dialog,
  IconButton,
  Portal,
  Text
} from 'react-native-paper';
import { FilterField } from '../../../models/page';
import { useTranslation } from 'react-i18next';
import { TouchableOpacity } from 'react-native';
import * as React from 'react';
import { useEffect, useState } from 'react';
import { pushOrRemove } from '../../../utils/overall';
import _ from 'lodash';
import { useAppTheme } from '../../../custom-theme';
import { fontWeight, radius, spacing, touchTarget } from '../../../theme/tokens';

interface OwnProps {
  filterFields: FilterField[];
  onChange: (filterFields: FilterField[]) => void;
  completeOptions: string[];
  initialOptions: string[];
  fieldName: string;
  icon: string;
}

export default function EnumFilter({
  filterFields,
  onChange,
  completeOptions,
  fieldName,
  initialOptions,
  icon
}: OwnProps) {
  const { t } = useTranslation();
  const theme = useAppTheme();
  const [openDialog, setOpenDialog] = useState<boolean>(false);
  const [newFilterFields, setNewFilterFields] =
    useState<FilterField[]>(filterFields);
  const [statuses, setStatuses] = useState<boolean[]>([]);
  //do not trigger change if statuses didn't change
  const [statusesJustOnOpen, setStatusesJustOnOpen] = useState<boolean[]>(null);
  const isSelected = !_.isEqual(
    statuses,
    completeOptions.map((option) => initialOptions.includes(option))
  );
  const switchValue = (index: number, option: string) => {
    // The edited set has to be written back to state. It previously only
    // existed as a local inside this function, so dismissing the dialog
    // handed the caller the untouched original and the chosen statuses were
    // discarded; the checkboxes moved but the list never changed.
    setNewFilterFields((current) => {
      const updated = [...current];
      const filterFieldIndex = updated.findIndex(
        (filterField) => filterField.field === fieldName
      );
      if (filterFieldIndex === -1) return current;
      updated[filterFieldIndex] = {
        ...updated[filterFieldIndex],
        values: pushOrRemove(
          updated[filterFieldIndex].values,
          !statuses[index],
          option
        )
      };
      return updated;
    });
    const newStatuses = [...statuses];
    newStatuses[index] = !newStatuses[index];
    setStatuses(newStatuses);
  };

  useEffect(() => {
    setNewFilterFields(filterFields);
    setStatuses(
      completeOptions.map((option) =>
        filterFields.some(
          (filterField) =>
            filterField.field === fieldName &&
            filterField.values.includes(option)
        )
      )
    );
  }, [filterFields]);

  const renderDialog = () => {
    return (
      <Portal>
        <Dialog
          visible={openDialog}
          onDismiss={() => {
            setOpenDialog(false);
            if (!_.isEqual(statusesJustOnOpen, statuses)) {
              onChange(newFilterFields);
              setStatusesJustOnOpen(null);
            }
          }}
          style={{ backgroundColor: theme.colors.card }}
        >
          <Dialog.Title>{t('select')}</Dialog.Title>
          <Dialog.Content>
            {completeOptions.map((option, index) => (
              <TouchableOpacity
                key={index}
                style={{
                  marginTop: 5,
                  padding: 10,
                  display: 'flex',
                  borderRadius: 5,
                  flexDirection: 'row',
                  alignItems: 'center'
                }}
                onPress={() => switchValue(index, option)}
              >
                <Checkbox
                  status={statuses[index] ? 'checked' : 'unchecked'}
                  onPress={() => switchValue(index, option)}
                />
                <Text>{t(option)}</Text>
              </TouchableOpacity>
            ))}
          </Dialog.Content>
        </Dialog>
      </Portal>
    );
  };
  return (
    <TouchableOpacity
      onPress={() => {
        setOpenDialog(true);
        setStatusesJustOnOpen(
          completeOptions.map((option) =>
            filterFields.some(
              (filterField) =>
                filterField.field === fieldName &&
                filterField.values.includes(option)
            )
          )
        );
      }}
      accessibilityRole="button"
      accessibilityState={{ selected: isSelected }}
      style={{
        backgroundColor: isSelected
          ? theme.colors.primary
          : theme.colors.background,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        borderRadius: radius.pill,
        paddingLeft: spacing.lg,
        minHeight: touchTarget.min,
        margin: spacing.xs
      }}
    >
      {renderDialog()}
      <Text
        style={{
          color: isSelected ? theme.colors.white : theme.colors.text,
          fontWeight: fontWeight.bold
        }}
      >
        {t(fieldName)}
      </Text>
      <IconButton
        icon={'chevron-double-down'}
        iconColor={isSelected ? theme.colors.white : theme.colors.text}
        size={15}
      />
    </TouchableOpacity>
  );
}
