import { FlatList, Pressable, StyleSheet } from 'react-native';
import { View } from '../../components/Themed';
import { RootStackScreenProps } from '../../types';
import { useTranslation } from 'react-i18next';
import * as React from 'react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from '../../store';
import { LocationMiniDTO } from '../../models/location';
import { getLocationsMini } from '../../slices/location';
import {
  Avatar,
  Button,
  Checkbox,
  Divider,
  Searchbar,
  Text
} from 'react-native-paper';
import { useAppTheme } from '../../custom-theme';

const ITEM_HEIGHT = 80;

const LocationItem = React.memo(
  ({
    location,
    isSelected,
    multiple,
    onToggle
  }: {
    location: LocationMiniDTO;
    isSelected: boolean;
    multiple: boolean;
    onToggle: (id: number) => void;
  }) => {
    const theme = useAppTheme();
    const handleToggle = useCallback(
      () => onToggle(location.id),
      [location.id, onToggle]
    );

    return (
      <Pressable onPress={handleToggle}>
        <View style={[styles.card, { backgroundColor: theme.colors.surface }]}>
          <View style={styles.cardRow}>
            <Avatar.Icon
              style={{ backgroundColor: theme.colors.background }}
              color={'white'}
              icon={'map-marker-outline'}
              size={50}
            />
            <View style={{ flex: 1 }}>
              <View style={styles.cardHeader}>
                <View style={{ flex: 1 }}>
                  <Text
                    numberOfLines={2}
                    variant="titleMedium"
                    style={styles.cardTitle}
                  >
                    {location.name}
                  </Text>
                  <Text
                    variant={'bodySmall'}
                    style={{ color: 'grey' }}
                  >{`#${location.customId}`}</Text>
                </View>
                {multiple && (
                  <Checkbox
                    status={isSelected ? 'checked' : 'unchecked'}
                    onPress={handleToggle}
                  />
                )}
              </View>
            </View>
          </View>
        </View>
      </Pressable>
    );
  }
);

export default function SelectLocationsModal({
  navigation,
  route
}: RootStackScreenProps<'SelectLocations'>) {
  const { onChange, selected, multiple } = route.params;
  const theme = useAppTheme();
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { locationsMini, loadingGet } = useSelector((state) => state.locations);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [searchQuery, setSearchQuery] = useState<string>('');

  const selectedLocations = useMemo(() => {
    if (!locationsMini.length) return [];
    const locationMap = new Map(locationsMini.map((l) => [l.id, l]));
    return selectedIds.flatMap((id) => {
      const location = locationMap.get(id);
      return location ? [location] : [];
    });
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
            <Text variant="titleMedium">{t('add')}</Text>
          </Pressable>
        )
      });
  }, [selectedLocations]);

  useEffect(() => {
    dispatch(getLocationsMini());
  }, []);

  const toggle = useCallback(
    (id: number) => {
      if (!multiple) {
        const location = locationsMini.find((l) => l.id === id);
        if (location) {
          onChange([location]);
        }
        navigation.goBack();
        return;
      }
      setSelectedIds((prev) =>
        prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
      );
    },
    [multiple, locationsMini, onChange, navigation]
  );

  const selectedSet = useMemo(() => new Set(selectedIds), [selectedIds]);

  const filteredLocations = useMemo(
    () =>
      locationsMini.filter((location) =>
        location.name.toLowerCase().includes(searchQuery.toLowerCase().trim())
      ),
    [locationsMini, searchQuery]
  );

  const renderItem = useCallback(
    ({ item }: { item: LocationMiniDTO }) => (
      <LocationItem
        location={item}
        isSelected={selectedSet.has(item.id)}
        multiple={multiple}
        onToggle={toggle}
      />
    ),
    [selectedSet, multiple, toggle]
  );

  const keyExtractor = useCallback(
    (item: LocationMiniDTO) => String(item.id),
    []
  );

  const ListFooterComponent = useCallback(
    () => (
      <>
        <Divider />
        <Button
          icon={'plus-circle'}
          style={{ margin: 20 }}
          mode={'contained'}
          onPress={() => {
            navigation.navigate('AddLocation', {
              onSuccess: (newLocation) => {
                setSelectedIds((prev) => [...prev, newLocation.id]);
                if (!multiple) {
                  onChange([newLocation]);
                  navigation.goBack();
                }
              }
            });
          }}
        >
          {t('create_location')}
        </Button>
      </>
    ),
    [t, navigation, multiple, onChange]
  );

  return (
    <View
      style={[styles.container, { backgroundColor: theme.colors.background }]}
    >
      <Searchbar
        placeholder={t('search')}
        onChangeText={setSearchQuery}
        value={searchQuery}
        style={{ backgroundColor: theme.colors.background }}
      />
      <FlatList
        data={filteredLocations}
        keyExtractor={keyExtractor}
        renderItem={renderItem}
        refreshing={loadingGet}
        onRefresh={() => dispatch(getLocationsMini())}
        ListFooterComponent={ListFooterComponent}
        initialNumToRender={20}
        maxToRenderPerBatch={20}
        windowSize={10}
        removeClippedSubviews={true}
        getItemLayout={(_data, index) => ({
          length: ITEM_HEIGHT,
          offset: ITEM_HEIGHT * index,
          index
        })}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  card: {
    marginBottom: 1,
    padding: 10,
    height: ITEM_HEIGHT
  },
  cardRow: {
    display: 'flex',
    flexDirection: 'row',
    gap: 6
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start'
  },
  cardTitle: {
    fontWeight: 'bold',
    marginBottom: 4,
    flexShrink: 1
  }
});
