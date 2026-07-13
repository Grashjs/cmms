import {
  Checkbox,
  Dialog,
  IconButton,
  Portal,
  Text,
  useTheme
} from 'react-native-paper';
import { FilterField } from '../../models/page';
import { useTranslation } from 'react-i18next';
import { TouchableOpacity } from 'react-native';
import * as React from 'react';
import { useEffect, useState } from 'react';
import { pushOrRemove } from '../../utils/overall';
import _ from 'lodash';

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
  const theme = useTheme();
  const isSelected: boolean = filterFields.some(filterField => filterField.field === activeFilterField.field);
  return (
    <TouchableOpacity
      onPress={() => {
        let newFilterFields = filterFields;
        if (isSelected) {
          newFilterFields = newFilterFields.filter(filterField => filterField.field !== activeFilterField.field);
        } else {
          newFilterFields.push(activeFilterField);
        }
        onChange(newFilterFields);
      }}
      style={{
        backgroundColor: isSelected ? theme.colors.primary : theme.colors.background,
        display: 'flex',
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        borderRadius: 20,
        paddingHorizontal: 15,
        margin: 5
      }}
    >
      <Text
        style={{ color: isSelected ? 'white' : 'black', fontWeight: 'bold' }}
      >
        {t(activeFilterField.field)}
      </Text>
    </TouchableOpacity>
  );
}
