import React, { useState } from 'react';
import { ScrollView, StyleSheet, TouchableOpacity, View } from 'react-native';
import {
  Button,
  Checkbox,
  Dialog,
  IconButton,
  Portal,
  Text,
  useTheme
} from 'react-native-paper';
import { useTranslation } from 'react-i18next';

export type PreparationStatusFilter = 'all' | 'complete' | 'active';
export type PreparationCopyFilter = 'all' | 'prepared' | 'missing';

function FilterBadge({
  label,
  selected,
  expanded,
  onPress
}: {
  label: string;
  selected: boolean;
  expanded?: boolean;
  onPress: () => void;
}) {
  const { colors } = useTheme();
  const color = selected ? 'white' : 'black';
  return (
    <TouchableOpacity
      accessibilityRole="button"
      accessibilityState={{ selected, expanded }}
      onPress={onPress}
      style={[
        styles.badge,
        {
          backgroundColor: selected ? colors.primary : colors.background,
          paddingRight: expanded === undefined ? 15 : 0
        }
      ]}
    >
      <Text style={[styles.label, { color }]}>{label}</Text>
      {expanded !== undefined && (
        <View
          pointerEvents="none"
          importantForAccessibility="no-hide-descendants"
        >
          <IconButton icon="chevron-double-down" iconColor={color} size={15} />
        </View>
      )}
    </TouchableOpacity>
  );
}

function MultiSelectBadge<T extends string>({
  label,
  options,
  value,
  onChange
}: {
  label: string;
  options: { value: T; label: string; disabled?: boolean }[];
  value: 'all' | T;
  onChange: (value: 'all' | T) => void;
}) {
  const { t } = useTranslation();
  const [draft, setDraft] = useState<T[] | null>(null);
  const initial = options.filter(
    (option) => !option.disabled && (value === 'all' || option.value === value)
  );
  const close = () => {
    if (draft) {
      const checked = options.filter(
        (option) => !option.disabled && draft.includes(option.value)
      );
      if (
        checked.length !== initial.length ||
        checked.some((option) => !initial.includes(option))
      )
        onChange(checked.length === 1 ? checked[0].value : 'all');
    }
    setDraft(null);
  };
  return (
    <>
      <FilterBadge
        label={label}
        selected={value !== 'all'}
        expanded={draft !== null}
        onPress={() => setDraft(initial.map((option) => option.value))}
      />
      {draft !== null && (
        <Portal>
          <Dialog visible onDismiss={close} style={styles.dialog}>
            <Dialog.Title>{label}</Dialog.Title>
            <Dialog.Content>
              {options.map((option) => (
                <Checkbox.Item
                  key={option.value}
                  label={option.label}
                  position="leading"
                  status={
                    draft.includes(option.value) ? 'checked' : 'unchecked'
                  }
                  disabled={option.disabled}
                  onPress={() =>
                    setDraft((previous) => {
                      if (!previous) return previous;
                      return previous.includes(option.value)
                        ? previous.filter((item) => item !== option.value)
                        : [...previous, option.value];
                    })
                  }
                />
              ))}
            </Dialog.Content>
            <Dialog.Actions>
              <Button onPress={close}>{t('close')}</Button>
            </Dialog.Actions>
          </Dialog>
        </Portal>
      )}
    </>
  );
}

export default function OfflinePreparationFilters({
  status,
  copy,
  networkAvailable,
  onStatusChange,
  onCopyChange
}: {
  status: PreparationStatusFilter;
  copy: PreparationCopyFilter;
  networkAvailable: boolean;
  onStatusChange: (value: PreparationStatusFilter) => void;
  onCopyChange: (value: PreparationCopyFilter) => void;
}) {
  const { t } = useTranslation();
  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      style={styles.row}
    >
      <FilterBadge
        label={t('offline_all_interventions')}
        selected={status === 'all' && copy === 'all'}
        onPress={() => {
          onStatusChange('all');
          onCopyChange('all');
        }}
      />
      <MultiSelectBadge
        label={t('status')}
        value={status}
        onChange={onStatusChange}
        options={[
          { value: 'complete', label: t('offline_completed_interventions') },
          { value: 'active', label: t('offline_incomplete_interventions') }
        ]}
      />
      <MultiSelectBadge
        key={networkAvailable ? 'connected' : 'disconnected'}
        label={t('offline_copy_filter')}
        value={copy}
        onChange={onCopyChange}
        options={[
          { value: 'prepared', label: t('offline_filter_prepared') },
          {
            value: 'missing',
            label: t('offline_to_prepare'),
            disabled: !networkAvailable
          }
        ]}
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  row: { backgroundColor: 'white', borderRadius: 5, marginBottom: 2 },
  badge: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 20,
    paddingLeft: 15,
    margin: 5
  },
  label: { fontWeight: 'bold' },
  dialog: { backgroundColor: 'white' }
});
