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
import { MeterMiniDTO } from '../../models/meter';
import { getMetersMini } from '../../slices/meter';
import { Checkbox, Divider, Text, useTheme } from 'react-native-paper';

export default function SelectMetersModal({
                                            navigation,
                                            route
                                          }: RootStackScreenProps<'SelectMeters'>) {
  const { onChange, selected, multiple } = route.params;
  const theme = useTheme();
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { metersMini, loadingGet } = useSelector((state) => state.meters);
  const [selectedMeters, setSelectedMeters] = useState<MeterMiniDTO[]>([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);

  useEffect(() => {
    if (metersMini.length) {
      const newSelectedMeters = selectedIds
        .map((id) => {
          return metersMini.find((meter) => meter.id == id);
        })
        .filter((meter) => !!meter);
      setSelectedMeters(newSelectedMeters);
    }
  }, [selectedIds, metersMini]);

  useEffect(() => {
    if (!selectedIds.length) setSelectedIds(selected);
  }, [selected]);

  useEffect(() => {
    if (multiple)
      navigation.setOptions({
        headerRight: () => (
          <Pressable
            disabled={!selectedMeters.length}
            onPress={() => {
              onChange(selectedMeters);
              navigation.goBack();
            }}
          >
            <Text variant='titleMedium'>{t('add')}</Text>
          </Pressable>
        )
      });
  }, [selectedMeters]);

  useEffect(() => {
    dispatch(getMetersMini());
  }, []);

  const onSelect = (ids: number[]) => {
    setSelectedIds(Array.from(new Set([...selectedIds, ...ids])));
    if (!multiple) {
      onChange([metersMini.find((meter) => meter.id === ids[0])]);
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
            onRefresh={() => dispatch(getMetersMini())}
          />
        }
        style={{
          flex: 1,
          backgroundColor: theme.colors.background
        }}
      >
        {metersMini.map((meter) => (
          <TouchableOpacity
            onPress={() => {
              toggle(meter.id);
            }}
            key={meter.id}
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
                  selectedIds.includes(meter.id) ? 'checked' : 'unchecked'
                }
                onPress={() => {
                  toggle(meter.id);
                }}
              />
            )}
            <Text style={{ flexShrink: 1 }} variant={'titleMedium'}>{meter.name}</Text>
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
