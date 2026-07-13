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
import { LocationMiniDTO } from '../../models/location';
import { getLocationsMini } from '../../slices/location';
import { Checkbox, Divider, Searchbar, Text, useTheme } from 'react-native-paper';

export default function SelectLocationsModal({
                                               navigation,
                                               route
                                             }: RootStackScreenProps<'SelectLocations'>) {
  const { onChange, selected, multiple } = route.params;
  const theme = useTheme();
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { locationsMini, loadingGet } = useSelector((state) => state.locations);
  const [selectedLocations, setSelectedLocations] = useState<LocationMiniDTO[]>(
    []
  );
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [searchQuery, setSearchQuery] = useState<string>('');

  useEffect(() => {
    if (locationsMini.length) {
      const newSelectedLocations = selectedIds
        .map((id) => {
          return locationsMini.find((location) => location.id == id);
        })
        .filter((location) => !!location);
      setSelectedLocations(newSelectedLocations);
    }
  }, [selectedIds, locationsMini]);

  useEffect(() => {
    if (!selectedIds.length) setSelectedIds(selected);
  }, [selected]);

  useEffect(() => {
    if (multiple)
      navigation.setOptions({
        headerRight: () => (
          <Pressable
            disabled={!selectedLocations.length}
            onPress={() => {
              onChange(selectedLocations);
              navigation.goBack();
            }}
          >
            <Text variant='titleMedium'>{t('add')}</Text>
          </Pressable>
        )
      });
  }, [selectedLocations]);

  useEffect(() => {
    dispatch(getLocationsMini());
  }, []);

  const onSelect = (ids: number[]) => {
    setSelectedIds(Array.from(new Set([...selectedIds, ...ids])));
    if (!multiple) {
      onChange([locationsMini.find((location) => location.id === ids[0])]);
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
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <Searchbar
        placeholder={t('search')}
        onChangeText={setSearchQuery}
        value={searchQuery}
        style={{ backgroundColor: theme.colors.background }}
      />
      <ScrollView
        refreshControl={
          <RefreshControl
            refreshing={loadingGet}
            onRefresh={() => dispatch(getLocationsMini())}
          />
        }
        style={{
          flex: 1,
          backgroundColor: theme.colors.background
        }}
      >
        {locationsMini.filter(mini => mini.name.toLowerCase().includes(searchQuery.toLowerCase().trim())).map((location) => (
          <TouchableOpacity
            onPress={() => {
              toggle(location.id);
            }}
            key={location.id}
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
                  selectedIds.includes(location.id) ? 'checked' : 'unchecked'
                }
                onPress={() => {
                  toggle(location.id);
                }}
              />
            )}
            <Text style={{ flexShrink: 1 }} variant={'titleMedium'}>{location.name}</Text>
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
