import {
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator
} from 'react-native';
import { View } from '../../components/Themed';
import { RootStackScreenProps } from '../../types';
import { useTranslation } from 'react-i18next';
import * as React from 'react';
import { useEffect, useState, useMemo } from 'react';
import { useDispatch, useSelector } from '../../store';
import { AssetMiniDTO } from '../../models/asset';
import { getAssetsMini } from '../../slices/asset';
import {
  Checkbox,
  Searchbar,
  Text,
  useTheme,
  SegmentedButtons,
  IconButton
} from 'react-native-paper';

// Interface extending AssetMiniDTO to explicitly include derived 'hasChildren'
interface AssetHierarchyNode extends AssetMiniDTO {
  hasChildren: boolean;
}

export default function SelectAssetsModal({
  navigation,
  route
}: RootStackScreenProps<'SelectAssets'>) {
  const { onChange, selected, multiple, locationId } = route.params;
  const theme = useTheme();
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const { assetsMini, loadingGet } = useSelector((state) => state.assets);
  // Derive the hierarchy structure and 'hasChildren' property from the flat list
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
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [view, setView] = useState<'list' | 'hierarchy'>('list');
  const [currentHierarchyLevel, setCurrentHierarchyLevel] = useState<
    AssetHierarchyNode[]
  >([]);
  // currentHierarchyParent holds the node the user navigated into (null for top level)
  const [currentHierarchyParent, setCurrentHierarchyParent] =
    useState<AssetHierarchyNode | null>(null);

  // Find an asset by ID in the derived hierarchy list
  const findAssetById = (id: number): AssetHierarchyNode | undefined => {
    return assetsHierarchy.find((a) => a.id === id);
  };
  // Initialize selected IDs from route params
  useEffect(() => {
    if (selected) {
      // Check if selected prop has value
      setSelectedIds(selected);
    }
  }, [selected]); // Depend only on selected prop

  // Fetch initial asset data
  useEffect(() => {
    dispatch(getAssetsMini(locationId));
  }, [locationId, dispatch]);

  // Update header button for multiple selection
  useEffect(() => {
    if (multiple) {
      const currentlySelectedAssets = selectedIds
        .map((id) => findAssetById(id))
        .filter((asset): asset is AssetHierarchyNode => !!asset); // Use AssetHierarchyNode here

      navigation.setOptions({
        headerRight: () => (
          <Pressable
            disabled={!currentlySelectedAssets.length}
            onPress={() => {
              // Pass back the selected assets (ensure format matches onChange expectation)
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
        )
      });
    }
    // Re-run when selectedIds or assetsHierarchy changes (needed if assets load after initial render)
  }, [
    selectedIds,
    multiple,
    navigation,
    onChange,
    assetsHierarchy,
    theme.colors.primary,
    t
  ]);

  // Update hierarchy view when hierarchy data or parent changes
  useEffect(() => {
    // This effect runs when view is 'hierarchy' OR when assetsHierarchy/currentHierarchyParent changes
    // Filter assetsHierarchy to find children of the current parent (or top-level items if parent is null)
    const children = assetsHierarchy.filter(
      (asset) => asset.parentId === (currentHierarchyParent?.id ?? null)
    );
    setCurrentHierarchyLevel(children);
  }, [assetsHierarchy, currentHierarchyParent]); // Removed view dependency, logic now depends only on data/parent

  // --- Event Handlers ---

  const onSelect = (ids: number[]) => {
    const newSelectedIds = Array.from(new Set([...selectedIds, ...ids]));
    setSelectedIds(newSelectedIds);
    if (!multiple) {
      const selectedAsset = findAssetById(ids[0]);
      if (selectedAsset) {
        onChange([selectedAsset]); // Pass the found asset
        navigation.goBack();
      }
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

  const handleSearchChange = (query: string) => {
    setSearchQuery(query);
    // If user starts searching, always switch back to list view
    if (query) {
      setView('list');
    }
  };

  const handleViewChange = (newView: 'list' | 'hierarchy') => {
    setView(newView);
    // Clear search and reset hierarchy navigation when switching views
    setSearchQuery('');
    if (newView === 'hierarchy') {
      setCurrentHierarchyParent(null); // Reset to top level
    }
  };

  const navigateHierarchyDown = (asset: AssetHierarchyNode) => {
    setCurrentHierarchyParent(asset);
  };

  const navigateHierarchyUp = () => {
    if (!currentHierarchyParent) return;
    // Find the parent of the current parent using the derived assetsHierarchy list
    const grandparent = currentHierarchyParent.parentId
      ? findAssetById(currentHierarchyParent.parentId)
      : null;
    setCurrentHierarchyParent(grandparent || null); // Set to null if no grandparent (i.e., moving to top level)
  };

  // --- Refresh Handler ---
  const onRefresh = () => {
    // Always refresh the base data
    dispatch(getAssetsMini(locationId));
    // Reset hierarchy navigation if in hierarchy view
    if (view === 'hierarchy') {
      setCurrentHierarchyParent(null);
    }
  };

  // --- Rendering ---

  // Reusable function to render an item in either list or hierarchy view
  const renderListItem = (
    asset: AssetHierarchyNode,
    isHierarchyView = false
  ) => (
    <TouchableOpacity
      onPress={() => {
        // Always allow selection by clicking the item body
        toggle(asset.id);
      }}
      key={asset.id}
      style={styles.itemContainer}
    >
      <View style={styles.itemContent}>
        {multiple && (
          <Checkbox
            status={selectedIds.includes(asset.id) ? 'checked' : 'unchecked'}
            onPress={() => toggle(asset.id)} // Checkbox also toggles selection
          />
        )}
        <Text style={styles.itemText} variant={'titleMedium'}>
          {asset.name}
        </Text>
      </View>
      {/* Show drill-down icon only in hierarchy view if the item has children */}
      {isHierarchyView && asset.hasChildren && (
        <IconButton
          icon="chevron-right"
          size={24}
          onPress={(e) => {
            e.stopPropagation(); // Prevent triggering the main TouchableOpacity onPress
            navigateHierarchyDown(asset); // Navigate down when chevron is pressed
          }}
        />
      )}
    </TouchableOpacity>
  );

  // Filtered list for the 'list' view based on search query
  const filteredListAssets = useMemo(() => {
    if (!searchQuery) {
      return assetsHierarchy; // Use derived hierarchy list for consistency if no search
    }
    return assetsHierarchy.filter((asset) =>
      asset.name.toLowerCase().includes(searchQuery.toLowerCase().trim())
    );
  }, [assetsHierarchy, searchQuery]);

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
        onValueChange={(value) =>
          handleViewChange(value as 'list' | 'hierarchy')
        }
        buttons={[
          { value: 'list', label: t('list') },
          { value: 'hierarchy', label: t('hierarchy') }
        ]}
        style={styles.segmentedButtons}
      />

      {/* Back button for hierarchy navigation */}
      {view === 'hierarchy' && currentHierarchyParent && (
        <TouchableOpacity
          onPress={navigateHierarchyUp}
          style={styles.backButton}
        >
          <IconButton icon="arrow-left" size={20} />
          {/* Find the name of the parent's parent for the back button label */}
          <Text variant="titleMedium">
            {t('back_to')}{' '}
            {findAssetById(currentHierarchyParent.parentId ?? -1)?.name ??
              t('top_level')}
          </Text>
        </TouchableOpacity>
      )}

      <ScrollView
        refreshControl={
          <RefreshControl refreshing={loadingGet} onRefresh={onRefresh} />
        }
        style={styles.scrollView}
      >
        {/* Conditional rendering based on view state */}
        {view === 'list' &&
          (loadingGet && !filteredListAssets.length ? (
            <ActivityIndicator animating={true} style={{ marginTop: 20 }} />
          ) : (
            // Render the filtered list (which might be the full list if no search)
            filteredListAssets.map((asset) => renderListItem(asset, false)) // Pass false for isHierarchyView
          ))}

        {view === 'hierarchy' &&
          (loadingGet && !currentHierarchyLevel.length ? (
            <ActivityIndicator animating={true} style={{ marginTop: 20 }} />
          ) : (
            // Render the current level of the hierarchy
            currentHierarchyLevel.map((asset) => renderListItem(asset, true)) // Pass true for isHierarchyView
          ))}

        {view === 'list' &&
          !loadingGet &&
          !filteredListAssets.length &&
          !!searchQuery && (
            <Text style={styles.noResultsText}>{t('no_results_found')}</Text>
          )}
        {view === 'list' &&
          !loadingGet &&
          !filteredListAssets.length &&
          !searchQuery && (
            <Text style={styles.noResultsText}>{t('no_assets_available')}</Text>
          )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  segmentedButtons: {
    marginHorizontal: 10,
    marginBottom: 10
  },
  scrollView: {
    flex: 1
  },
  itemContainer: {
    marginHorizontal: 10,
    marginVertical: 4,
    borderRadius: 5,
    paddingVertical: 10,
    paddingHorizontal: 15,
    backgroundColor: 'white',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    elevation: 2
  },
  itemContent: {
    flexDirection: 'row',
    alignItems: 'center',
    flexShrink: 1
  },
  itemText: {
    marginLeft: 10,
    flexShrink: 1
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
