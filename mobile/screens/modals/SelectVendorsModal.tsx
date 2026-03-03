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
import { VendorMiniDTO } from '../../models/vendor';
import { getVendorsMini } from '../../slices/vendor';
import { Avatar, Checkbox, Text, useTheme } from 'react-native-paper';

export default function SelectVendorsModal({
  navigation,
  route
}: RootStackScreenProps<'SelectVendors'>) {
  const { onChange, selected, multiple } = route.params;
  const theme = useTheme();
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { vendorsMini, loadingGet } = useSelector((state) => state.vendors);
  const [selectedVendors, setSelectedVendors] = useState<VendorMiniDTO[]>([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  useEffect(() => {
    if (vendorsMini.length) {
      const newSelectedVendors = selectedIds
        .map((id) => {
          return vendorsMini.find((vendor) => vendor.id == id);
        })
        .filter((vendor) => !!vendor);
      setSelectedVendors(newSelectedVendors);
    }
  }, [selectedIds, vendorsMini]);

  useEffect(() => {
    if (!selectedIds.length) setSelectedIds(selected);
  }, [selected]);

  useEffect(() => {
    if (multiple)
      navigation.setOptions({
        headerRight: () => (
          <Pressable
            disabled={!selectedVendors.length}
            onPress={() => {
              onChange(selectedVendors);
              navigation.goBack();
            }}
          >
            <Text variant="titleMedium">{t('add')}</Text>
          </Pressable>
        )
      });
  }, [selectedVendors]);

  useEffect(() => {
    dispatch(getVendorsMini());
  }, []);

  const onSelect = (ids: number[]) => {
    setSelectedIds(Array.from(new Set([...selectedIds, ...ids])));
    if (!multiple) {
      onChange([vendorsMini.find((vendor) => vendor.id === ids[0])]);
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
            onRefresh={() => dispatch(getVendorsMini())}
          />
        }
        style={{
          flex: 1,
          backgroundColor: theme.colors.background
        }}
      >
        {vendorsMini.map((vendor) => (
          <TouchableOpacity
            onPress={() => {
              toggle(vendor.id);
            }}
            key={vendor.id}
          >
            <View style={styles.card}>
              <View style={styles.cardRow}>
                <Avatar.Icon
                  size={50}
                  icon="truck-outline"
                  style={{ backgroundColor: theme.colors.primaryContainer }}
                />
                <View style={{ flex: 1 }}>
                  <View style={styles.cardHeader}>
                    <View style={{ flex: 1 }}>
                      <Text variant="titleMedium" style={styles.cardTitle}>
                        {vendor.companyName}
                      </Text>
                      <Text
                        variant={'bodySmall'}
                        style={{ color: 'grey' }}
                      >{`#${vendor.id}`}</Text>
                    </View>
                    {multiple && (
                      <Checkbox
                        status={
                          selectedIds.includes(vendor.id)
                            ? 'checked'
                            : 'unchecked'
                        }
                        onPress={() => {
                          toggle(vendor.id);
                        }}
                      />
                    )}
                  </View>
                </View>
              </View>
            </View>
          </TouchableOpacity>
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  card: {
    backgroundColor: 'white',
    marginBottom: 1,
    padding: 10
  },
  cardRow: {
    display: 'flex',
    flexDirection: 'row',
    gap: 6,
    alignItems: 'center'
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center'
  },
  cardTitle: {
    fontWeight: 'bold',
    flexShrink: 1
  }
});
