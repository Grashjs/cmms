import {
  FlatList,
  Pressable,
  StyleSheet,
  ActivityIndicator,
  View as RNView
} from 'react-native';
import { View } from '../../components/Themed';
import { RootStackScreenProps } from '../../types';
import { useTranslation } from 'react-i18next';
import * as React from 'react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from '../../store';
import { AssetMiniDTO } from '../../models/asset';
import { getAssetsMini } from '../../slices/asset';
import {
  Avatar,
  Button,
  Checkbox,
  Divider,
  IconButton,
  Searchbar,
  SegmentedButtons,
  Text
} from 'react-native-paper';
import { useAppTheme } from '../../custom-theme';
import { boolean, number } from 'yup';

interface AssetHierarchyNode extends AssetMiniDTO {
  hasChildren: boolean;
}

const ITEM_HEIGHT = 78;
const HIERARCHY_ITEM_HEIGHT = 100;

const AssetItem = React.memo(
  ({
    asset,
    isSelected,
    multiple,
    showChevron,
    onToggle,
    onNavigateDown,
    hierarchy
  }: {
    asset: AssetHierarchyNode;
    isSelected: boolean;
    multiple: boolean;
    showChevron: boolean;
    onToggle: (id: number) => void;
    onNavigateDown: (asset: AssetHierarchyNode) => void;
    hierarchy?: boolean;
  }) => {
    const theme = useAppTheme();
    const handleToggle = useCallback(
      () => onToggle(asset.id),
      [asset.id, onToggle]
    );
    const handleNavigateDown = useCallback(
      (e: any) => {
        e.stopPropagation();
        onNavigateDown(asset);
      },
      [asset, onNavigateDown]
    );

    return (
      <Pressable onPress={handleToggle}>
        <View
          style={[
            styles.card,
            hierarchy ? { height: HIERARCHY_ITEM_HEIGHT } : {}
          ]}
        >
          <View style={styles.cardRow}>
            <Avatar.Icon
              style={{ backgroundColor: theme.colors.background }}
              color={'white'}
              icon={'package-variant-closed'}
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
                    {asset.name}
                  </Text>
                  <Text
                    variant={'bodySmall'}
                    style={{ color: 'grey' }}
                  >{`#${asset.customId}`}</Text>
                </View>
                {multiple && (
                  <Checkbox
                    status={isSelected ? 'checked' : 'unchecked'}
                    onPress={handleToggle}
                  />
                )}
              </View>
              {showChevron && (
                <View style={styles.cardFooter}>
                  <View style={{ flex: 1 }} />
                  <IconButton
                    icon="chevron-right"
                    size={24}
                    style={{ margin: 0 }}
                    onPress={handleNavigateDown}
                  />
                </View>
              )}
            </View>
          </View>
        </View>
      </Pressable>
    );
  }
);

export default function SelectAssetsModal({
  navigation,
  route
}: RootStackScreenProps<'SelectAssets'>) {
  const { onChange, selected, multiple, locationId } = route.params;
  const theme = useAppTheme();
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { assetsMini, loadingGet } = useSelector((state) => state.assets);

  const assetsHierarchy: AssetHierarchyNode[] = useMemo(() => {
    const assetMap = new Map(assetsMini.map((asset) => [asset.id, asset]));
    const childrenMap = new Map<number, boolean>();
    assetsMini.forEach((asset) => {
      if (asset.parentId !== null && assetMap.has(asset.parentId)) {
        childrenMap.set(asset.parentId, true);
      }
    });
    return assetsMini.map((asset) => ({
      ...asset,
      hasChildren: childrenMap.has(asset.id) || false
    }));
  }, [assetsMini]);

  const assetsMap = useMemo(
    () => new Map(assetsHierarchy.map((a) => [a.id, a])),
    [assetsHierarchy]
  );

  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [view, setView] = useState<'list' | 'hierarchy'>('list');
  const [currentHierarchyParent, setCurrentHierarchyParent] =
    useState<AssetHierarchyNode | null>(null);

  const currentHierarchyLevel = useMemo(
    () =>
      assetsHierarchy.filter(
        (asset) => asset.parentId === (currentHierarchyParent?.id ?? null)
      ),
    [assetsHierarchy, currentHierarchyParent]
  );

  useEffect(() => {
    if (selected) {
      setSelectedIds(selected);
    }
  }, [selected]);

  useEffect(() => {
    dispatch(getAssetsMini(locationId));
  }, [locationId, dispatch]);

  const currentlySelectedAssets = useMemo(
    () =>
      selectedIds.flatMap((id) => {
        const asset = assetsMap.get(id);
        return asset ? [asset] : [];
      }),
    [selectedIds, assetsMap]
  );

  useEffect(() => {
    navigation.setOptions({
      headerRight: () => (
        <RNView style={{ flexDirection: 'row', alignItems: 'center' }}>
          <IconButton
            icon="barcode-scan"
            size={24}
            onPress={() =>
              navigation.navigate('ScanAsset', {
                onChange: (asset) => {
                  onChange([asset]);
                  navigation.pop(3);
                }
              })
            }
            style={{ marginRight: 5 }}
          />
          {multiple && (
            <Pressable
              disabled={!currentlySelectedAssets.length}
              onPress={() => {
                onChange(currentlySelectedAssets);
                navigation.goBack();
              }}
              style={({ pressed }) => ({ opacity: pressed ? 0.5 : 1 })}
            >
              <Text
                variant="titleMedium"
                style={{ color: theme.colors.primary, marginRight: 10 }}
              >
                {t('add')} ({currentlySelectedAssets.length})
              </Text>
            </Pressable>
          )}
        </RNView>
      )
    });
  }, [
    currentlySelectedAssets,
    multiple,
    navigation,
    onChange,
    theme.colors.primary,
    t
  ]);

  const toggle = useCallback(
    (id: number) => {
      if (!multiple) {
        const asset = assetsMap.get(id);
        if (asset) {
          onChange([asset]);
        }
        navigation.goBack();
        return;
      }
      setSelectedIds((prev) =>
        prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
      );
    },
    [multiple, assetsMap, onChange, navigation]
  );

  const handleSearchChange = useCallback((query: string) => {
    setSearchQuery(query);
    if (query) {
      setView('list');
    }
  }, []);

  const handleViewChange = useCallback((newView: string) => {
    setView(newView as 'list' | 'hierarchy');
    setSearchQuery('');
    setCurrentHierarchyParent(null);
  }, []);

  const navigateHierarchyDown = useCallback((asset: AssetHierarchyNode) => {
    setCurrentHierarchyParent(asset);
  }, []);

  const navigateHierarchyUp = useCallback(() => {
    setCurrentHierarchyParent((prev) => {
      if (!prev) return prev;
      return assetsMap.get(prev.parentId ?? -1) ?? null;
    });
  }, [assetsMap]);

  const onRefresh = useCallback(() => {
    dispatch(getAssetsMini(locationId));
    if (view === 'hierarchy') {
      setCurrentHierarchyParent(null);
    }
  }, [locationId, dispatch, view]);

  const selectedSet = useMemo(() => new Set(selectedIds), [selectedIds]);

  const filteredListAssets = useMemo(() => {
    if (!searchQuery) return assetsHierarchy;
    return assetsHierarchy.filter((asset) =>
      asset.name.toLowerCase().includes(searchQuery.toLowerCase().trim())
    );
  }, [assetsHierarchy, searchQuery]);

  const renderItem = useCallback(
    ({ item }: { item: AssetHierarchyNode }, hierarchy?: boolean) => (
      <AssetItem
        asset={item}
        isSelected={selectedSet.has(item.id)}
        multiple={multiple}
        showChevron={view === 'hierarchy' && item.hasChildren}
        onToggle={toggle}
        onNavigateDown={navigateHierarchyDown}
        hierarchy={hierarchy}
      />
    ),
    [selectedSet, multiple, view, toggle, navigateHierarchyDown]
  );

  const keyExtractor = useCallback(
    (item: AssetHierarchyNode) => String(item.id),
    []
  );

  const ListEmptyComponent = useCallback(() => {
    if (loadingGet) {
      return <ActivityIndicator animating={true} style={{ marginTop: 20 }} />;
    }
    if (view === 'hierarchy') return null;
    if (searchQuery) {
      return <Text style={styles.noResultsText}>{t('no_results_found')}</Text>;
    }
    return <Text style={styles.noResultsText}>{t('no_assets_available')}</Text>;
  }, [loadingGet, view, searchQuery, t]);

  const ListFooterComponent = useCallback(
    () => (
      <>
        <Divider />
        <Button
          icon={'plus-circle'}
          style={{ margin: 20 }}
          mode={'contained'}
          onPress={() => {
            navigation.navigate('AddAsset', {
              onSuccess: (newAsset) => {
                setSelectedIds((prev) => [...prev, newAsset.id]);
                if (!multiple) {
                  onChange([newAsset]);
                  navigation.goBack();
                }
              }
            });
          }}
        >
          {t('create_asset')}
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
        onChangeText={handleSearchChange}
        value={searchQuery}
        style={{ backgroundColor: theme.colors.background, margin: 5 }}
      />
      <SegmentedButtons
        value={view}
        onValueChange={handleViewChange}
        buttons={[
          { value: 'list', label: t('list') },
          { value: 'hierarchy', label: t('hierarchy') }
        ]}
        style={styles.segmentedButtons}
      />
      {view === 'hierarchy' && currentHierarchyParent && (
        <Pressable onPress={navigateHierarchyUp} style={styles.backButton}>
          <IconButton icon="arrow-left" size={20} />
          <Text variant="titleMedium">
            {t('back_to')}{' '}
            {assetsMap.get(currentHierarchyParent.parentId ?? -1)?.name ??
              t('top_level')}
          </Text>
        </Pressable>
      )}
      {view === 'list' ? (
        <FlatList
          key="list"
          data={filteredListAssets}
          renderItem={renderItem}
          keyExtractor={keyExtractor}
          refreshing={loadingGet}
          onRefresh={onRefresh}
          ListEmptyComponent={ListEmptyComponent}
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
      ) : (
        <FlatList
          key="hierarchy"
          data={currentHierarchyLevel}
          renderItem={(item) => renderItem(item, true)}
          keyExtractor={keyExtractor}
          refreshing={loadingGet}
          onRefresh={onRefresh}
          ListEmptyComponent={ListEmptyComponent}
          ListFooterComponent={ListFooterComponent}
          initialNumToRender={20}
          maxToRenderPerBatch={20}
          windowSize={10}
          removeClippedSubviews={true}
          getItemLayout={(_data, index) => ({
            length: HIERARCHY_ITEM_HEIGHT,
            offset: HIERARCHY_ITEM_HEIGHT * index,
            index
          })}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  segmentedButtons: {
    marginHorizontal: 10,
    marginBottom: 10
  },
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
  },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
    alignItems: 'center',
    marginTop: 5
  },
  backButton: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 10,
    paddingVertical: 5
  },
  noResultsText: {
    textAlign: 'center',
    marginTop: 20,
    fontSize: 16,
    color: 'grey'
  }
});
