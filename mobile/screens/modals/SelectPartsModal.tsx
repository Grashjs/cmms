import {
  FlatList,
  Pressable,
  StyleSheet,
  useWindowDimensions
} from 'react-native';
import { View } from '../../components/Themed';
import { RootStackScreenProps } from '../../types';
import { useTranslation } from 'react-i18next';
import * as React from 'react';
import { useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { TabBar, TabView } from 'react-native-tab-view';
import { useDispatch, useSelector } from '../../store';
import { PartMiniDTO } from '../../models/part';
import { getPartsMini } from '../../slices/part';
import {
  ActivityIndicator,
  Avatar,
  Button,
  Checkbox,
  Searchbar,
  Text,
  useTheme
} from 'react-native-paper';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import { getMultiPartsMini } from '../../slices/multipart';
import SetType from '../../models/setType';

const PartItem = React.memo(
  ({
    part,
    isSelected,
    onToggle,
    onNavigate,
    formattedCost
  }: {
    part: PartMiniDTO;
    isSelected: boolean;
    onToggle: (id: number) => void;
    onNavigate: (id: number) => void;
    formattedCost: string;
  }) => {
    const theme = useTheme();
    const { t } = useTranslation();

    const handleToggle = useCallback(
      () => onToggle(part.id),
      [part.id, onToggle]
    );
    const handleNavigate = useCallback(
      () => onNavigate(part.id),
      [part.id, onNavigate]
    );

    return (
      <Pressable onPress={handleToggle}>
        <View style={styles.card}>
          <View style={styles.cardRow}>
            <Checkbox status={isSelected ? 'checked' : 'unchecked'} />
            <Avatar.Icon
              size={50}
              icon="archive-outline"
              style={{ backgroundColor: theme.colors.primaryContainer }}
            />
            <View style={{ flex: 1 }}>
              <View style={styles.cardHeader}>
                <View style={{ flex: 1 }}>
                  <Text
                    numberOfLines={2}
                    variant="titleMedium"
                    style={styles.cardTitle}
                  >
                    {part.name}
                  </Text>
                  <Text variant="bodyMedium" style={{ color: 'grey' }}>
                    {formattedCost}
                  </Text>
                </View>
                <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                  <Button
                    mode="text"
                    buttonColor="white"
                    onPress={handleNavigate}
                    style={{ marginRight: 8 }}
                  >
                    {t('details')}
                  </Button>
                </View>
              </View>
            </View>
          </View>
        </View>
      </Pressable>
    );
  }
);

const SetItem = React.memo(
  ({
    multiPart,
    isSelected,
    onToggle
  }: {
    multiPart: SetType;
    isSelected: boolean;
    onToggle: (multiPart: SetType, checked: boolean) => void;
  }) => {
    const theme = useTheme();
    const handleToggle = useCallback(
      () => onToggle(multiPart, isSelected),
      [multiPart, isSelected, onToggle]
    );

    return (
      <Pressable onPress={handleToggle}>
        <View style={styles.card}>
          <View style={styles.cardRow}>
            <Avatar.Icon
              size={50}
              icon="archive-outline"
              style={{ backgroundColor: theme.colors.primaryContainer }}
            />
            <View style={{ flex: 1 }}>
              <View style={styles.cardHeader}>
                <View style={{ flex: 1 }}>
                  <Text variant="titleMedium" style={styles.cardTitle}>
                    {multiPart.name}
                  </Text>
                </View>
                <Checkbox status={isSelected ? 'checked' : 'unchecked'} />
              </View>
            </View>
          </View>
        </View>
      </Pressable>
    );
  }
);

const PartsRoute = React.memo(
  ({
    toggle,
    partsMini,
    navigation,
    selectedIds
  }: {
    toggle: (id: number) => void;
    partsMini: PartMiniDTO[];
    selectedIds: number[];
    navigation: any;
  }) => {
    const { getFormattedCurrency } = useContext(CompanySettingsContext);
    const { t } = useTranslation();
    const theme = useTheme();
    const [searchQuery, setSearchQuery] = useState<string>('');

    const selectedSet = useMemo(() => new Set(selectedIds), [selectedIds]);

    const filteredParts = useMemo(
      () =>
        partsMini.filter((part) =>
          part.name.toLowerCase().includes(searchQuery.toLowerCase().trim())
        ),
      [partsMini, searchQuery]
    );

    const handleNavigate = useCallback(
      (id: number) => navigation.navigate('PartDetails', { id }),
      [navigation]
    );

    const renderItem = useCallback(
      ({ item }: { item: PartMiniDTO }) => (
        <PartItem
          part={item}
          isSelected={selectedSet.has(item.id)}
          onToggle={toggle}
          onNavigate={handleNavigate}
          formattedCost={getFormattedCurrency(item.cost)}
        />
      ),
      [selectedSet, toggle, handleNavigate, getFormattedCurrency]
    );

    const keyExtractor = useCallback(
      (item: PartMiniDTO) => String(item.id),
      []
    );

    return (
      <View style={{ flex: 1 }}>
        <Searchbar
          placeholder={t('search')}
          onChangeText={setSearchQuery}
          value={searchQuery}
          style={{ backgroundColor: theme.colors.background }}
        />
        <FlatList
          data={filteredParts}
          keyExtractor={keyExtractor}
          renderItem={renderItem}
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
);

const SetsRoute = React.memo(
  ({
    toggle,
    multiParts,
    selectedIds
  }: {
    toggle: (multiPart: SetType, checked: boolean) => void;
    multiParts: SetType[];
    selectedIds: number[];
  }) => {
    const { t } = useTranslation();
    const theme = useTheme();
    const [searchQuery, setSearchQuery] = useState<string>('');

    const selectedSet = useMemo(() => new Set(selectedIds), [selectedIds]);

    // A set is "selected" when every one of its parts is in selectedIds
    const selectedMultiPartIds = useMemo(
      () =>
        new Set(
          multiParts
            .filter(
              (mp) =>
                mp.parts.length > 0 &&
                mp.parts.every((p) => selectedSet.has(p.id))
            )
            .map((mp) => mp.id)
        ),
      [multiParts, selectedSet]
    );

    const filteredMultiParts = useMemo(
      () =>
        multiParts.filter((mp) =>
          mp.name.toLowerCase().includes(searchQuery.toLowerCase().trim())
        ),
      [multiParts, searchQuery]
    );

    const renderItem = useCallback(
      ({ item }: { item: SetType }) => (
        <SetItem
          multiPart={item}
          isSelected={selectedMultiPartIds.has(item.id)}
          onToggle={toggle}
        />
      ),
      [selectedMultiPartIds, toggle]
    );

    const keyExtractor = useCallback((item: SetType) => String(item.id), []);

    return (
      <View style={{ flex: 1 }}>
        <Searchbar
          placeholder={t('search')}
          onChangeText={setSearchQuery}
          value={searchQuery}
          style={{ backgroundColor: theme.colors.background }}
        />
        <FlatList
          data={filteredMultiParts}
          keyExtractor={keyExtractor}
          renderItem={renderItem}
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
);

const ITEM_HEIGHT = 78;

export default function SelectParts({
  navigation,
  route
}: RootStackScreenProps<'SelectParts'>) {
  const { onChange, selected } = route.params;
  const theme = useTheme();
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { partsMini, loadingGet } = useSelector((state) => state.parts);
  const { miniMultiParts: multiParts, loadingMultiparts } = useSelector(
    (state) => state.multiParts
  );
  const [tabIndex, setTabIndex] = useState(0);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const layout = useWindowDimensions();

  const [tabs] = useState([
    { key: 'parts', title: t('parts') },
    { key: 'sets', title: t('sets_of_parts') }
  ]);

  const selectedParts = useMemo(() => {
    if (!partsMini.length) return [];
    const partMap = new Map(partsMini.map((p) => [p.id, p]));
    return selectedIds.flatMap((id) => {
      const part = partMap.get(id);
      return part ? [part] : [];
    });
  }, [selectedIds, partsMini]);

  useEffect(() => {
    if (!selectedIds.length) setSelectedIds(selected);
  }, [selected]);

  useEffect(() => {
    navigation.setOptions({
      headerRight: () => (
        <Pressable
          disabled={!selectedParts.length}
          onPress={() => {
            onChange(selectedParts);
            navigation.goBack();
          }}
        >
          <Text variant="titleMedium">{t('add')}</Text>
        </Pressable>
      )
    });
  }, [selectedParts]);

  useEffect(() => {
    dispatch(getPartsMini());
    dispatch(getMultiPartsMini());
  }, []);

  const onSelect = useCallback((ids: number[]) => {
    setSelectedIds((prev) => Array.from(new Set([...prev, ...ids])));
  }, []);

  const onUnSelect = useCallback((ids: number[]) => {
    const removeSet = new Set(ids);
    setSelectedIds((prev) => prev.filter((id) => !removeSet.has(id)));
  }, []);

  const toggle = useCallback((id: number) => {
    setSelectedIds((prev) => {
      if (prev.includes(id)) {
        return prev.filter((x) => x !== id);
      }
      return [...prev, id];
    });
  }, []);

  const toggleMultipart = useCallback(
    (multiPart: SetType, isCurrentlyChecked: boolean) => {
      const ids = multiPart.parts.map((p) => p.id);
      if (isCurrentlyChecked) {
        onUnSelect(ids);
      } else {
        onSelect(ids);
      }
    },
    [onSelect, onUnSelect]
  );

  const renderScene = useCallback(
    ({ route: sceneRoute }) => {
      switch (sceneRoute.key) {
        case 'parts':
          return (
            <PartsRoute
              toggle={toggle}
              navigation={navigation}
              partsMini={partsMini}
              selectedIds={selectedIds}
            />
          );
        case 'sets':
          return (
            <SetsRoute
              toggle={toggleMultipart}
              multiParts={multiParts}
              selectedIds={selectedIds}
            />
          );
      }
    },
    [toggle, toggleMultipart, navigation, partsMini, multiParts, selectedIds]
  );

  const renderTabBar = useCallback(
    (props) => (
      <TabBar
        {...props}
        indicatorStyle={{ backgroundColor: 'white' }}
        style={{ backgroundColor: theme.colors.primary }}
      />
    ),
    [theme.colors.primary]
  );

  return (
    <View
      style={{ ...styles.container, backgroundColor: theme.colors.background }}
    >
      {((loadingGet && tabIndex === 0) ||
        (loadingMultiparts && tabIndex === 1)) && (
        <ActivityIndicator
          style={{ position: 'absolute', top: '45%', left: '45%', zIndex: 10 }}
          size="large"
        />
      )}
      <TabView
        style={{ flex: 1 }}
        renderTabBar={renderTabBar}
        navigationState={{ index: tabIndex, routes: tabs }}
        renderScene={renderScene}
        onIndexChange={setTabIndex}
        initialLayout={{ width: layout.width }}
        lazy
      />
      <Button
        icon={'plus-circle'}
        style={{ margin: 20 }}
        mode={'contained'}
        onPress={() => {
          navigation.navigate('AddPart', {
            openedFromSelector: true,
            onSuccess: (newPart) => {
              setSelectedIds((prev) => [...prev, newPart.id]);
            }
          });
        }}
      >
        {t('create_part')}
      </Button>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  card: {
    backgroundColor: 'white',
    marginBottom: 1,
    padding: 10,
    height: ITEM_HEIGHT
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
