import {
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  TouchableOpacity
} from 'react-native';
import { View } from '../../components/Themed';
import { RootStackScreenProps } from '../../types';
import { useTranslation } from 'react-i18next';
import * as React from 'react';
import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from '../../store';
import { CustomerMiniDTO } from '../../models/customer';
import { getCustomersMini } from '../../slices/customer';
import { Checkbox, Divider, Text, useTheme } from 'react-native-paper';

export default function SelectCustomersModal({
                                               navigation,
                                               route
                                             }: RootStackScreenProps<'SelectCustomers'>) {
  const { onChange, selected, multiple } = route.params;
  const theme = useTheme();
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { customersMini, loadingGet } = useSelector((state) => state.customers);
  const [selectedCustomers, setSelectedCustomers] = useState<CustomerMiniDTO[]>(
    []
  );
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  useEffect(() => {
    if (customersMini.length) {
      const newSelectedCustomers = selectedIds
        .map((id) => {
          return customersMini.find((customer) => customer.id == id);
        })
        .filter((customer) => !!customer);
      setSelectedCustomers(newSelectedCustomers);
    }
  }, [selectedIds, customersMini]);

  useEffect(() => {
    if (!selectedIds.length) setSelectedIds(selected);
  }, [selected]);

  useEffect(() => {
    if (multiple)
      navigation.setOptions({
        headerRight: () => (
          <Pressable
            disabled={!selectedCustomers.length}
            onPress={() => {
              onChange(selectedCustomers);
              navigation.goBack();
            }}
          >
            <Text variant='titleMedium'>{t('add')}</Text>
          </Pressable>
        )
      });
  }, [selectedCustomers]);

  useEffect(() => {
    dispatch(getCustomersMini());
  }, []);

  const onSelect = (ids: number[]) => {
    setSelectedIds(Array.from(new Set([...selectedIds, ...ids])));
    if (!multiple) {
      onChange([customersMini.find((customer) => customer.id === ids[0])]);
      navigation.goBack();
    }
  };
  const onUnSelect = (ids: number[]) => {
    const newSelectedIds = selectedIds.filter((id) => !ids.includes(id));
    setSelectedIds(newSelectedIds);
  };
  const toggle = (id: number) => {
    if (selectedIds.includes(id)) {
      onUnSelect([id]);
    } else {
      onSelect([id]);
    }
  };

  return (
    <View style={styles.container}>
      <ScrollView
        refreshControl={
          <RefreshControl
            refreshing={loadingGet}
            onRefresh={() => dispatch(getCustomersMini())}
          />
        }
        style={{
          flex: 1,
          backgroundColor: theme.colors.background
        }}
      >
        {customersMini.map((customer) => (
          <TouchableOpacity
            onPress={() => {
              toggle(customer.id);
            }}
            key={customer.id}
            style={{
              borderRadius: 5,
              padding: 15,
              backgroundColor: 'white',
              display: 'flex',
              flexDirection: 'row',
              elevation: 2,
              alignItems: 'center'
            }}
          >
            {multiple && (
              <Checkbox
                status={
                  selectedIds.includes(customer.id) ? 'checked' : 'unchecked'
                }
                onPress={() => {
                  toggle(customer.id);
                }}
              />
            )}
            <Text style={{ flexShrink: 1 }} variant={'titleMedium'}>{customer.name}</Text>
            <Divider />
          </TouchableOpacity>
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
