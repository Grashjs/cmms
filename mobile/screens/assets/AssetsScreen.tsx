import { StyleSheet, View } from 'react-native';
import { useDispatch, useSelector } from '../../store';
import * as React from 'react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import useAuth from '../../hooks/useAuth';
import { PermissionEntity } from '../../models/role';
import { getAssetChildren, getAssets, getMoreAssets } from '../../slices/asset';
import { FilterField, SearchCriteria } from '../../models/page';
import { Button, Searchbar } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import {
  AssetDTO,
  AssetRow,
  getAssetStatusConfig
} from '../../models/asset';
import { onSearchQueryChange } from '../../utils/overall';
import { RootStackScreenProps } from '../../types';
import { useDebouncedEffect } from '../../hooks/useDebouncedEffect';
import { useAppTheme } from '../../custom-theme';
import {
  EmptyState,
  EntityListCard,
  PaginatedEntityList
} from '../../components/ui';

interface AssetCardProps {
  asset: AssetDTO | AssetRow;
  navigation: RootStackScreenProps<'Assets'>['navigation'];
  showChildrenButton?: boolean;
  onViewChildren?: () => void;
}

function AssetCard({
  asset,
  navigation,
  showChildrenButton = false,
  onViewChildren
}: AssetCardProps) {
  const { t } = useTranslation();
  const theme = useAppTheme();

  return (
    <EntityListCard
      title={asset.name}
      subtitle={`#${asset.customId}`}
      imageUrl={asset.image?.url}
      icon="package-variant-closed"
      badge={{
        label: t(asset.status),
        color: getAssetStatusConfig(asset.status).color(theme)
      }}
      meta={
        asset.location
          ? [{ icon: 'map-marker-outline', label: asset.location.name }]
          : undefined
      }
      onPress={() =>
        navigation.push('AssetDetails', { id: asset.id, assetProp: asset })
      }
      footer={
        showChildrenButton && asset.hasChildren ? (
          <Button compact onPress={onViewChildren} accessibilityLabel={t('view_children')}>
            {t('view_children')}
          </Button>
        ) : undefined
      }
    />
  );
}

export default function AssetsScreen({
  navigation,
  route
}: RootStackScreenProps<'Assets'>) {
  const { t } = useTranslation();
  const [startedSearch, setStartedSearch] = useState<boolean>(false);
  const {
    assets,
    assetsHierarchy,
    loadingGet,
    currentPageNum,
    lastPage,
    assetChildrenPageNum,
    assetChildrenLastPage
  } = useSelector((state) => state.assets);
  const theme = useAppTheme();
  const [view, setView] = useState<'hierarchy' | 'list'>('hierarchy');
  const dispatch = useDispatch();
  const [searchQuery, setSearchQuery] = useState('');
  const [refreshing, setRefreshing] = useState(false);
  const { hasViewPermission } = useAuth();
  const defaultFilterFields: FilterField[] = [];
  const getCriteriaFromFilterFields = (filterFields: FilterField[]) => {
    const initialCriteria: SearchCriteria = {
      filterFields: defaultFilterFields,
      pageSize: 10,
      pageNum: 0,
      direction: 'DESC'
    };
    let newFilterFields = [...initialCriteria.filterFields];
    filterFields.forEach(
      (filterField) =>
        (newFilterFields = newFilterFields.filter(
          (ff) => ff.field != filterField.field
        ))
    );
    return {
      ...initialCriteria,
      filterFields: [...newFilterFields, ...filterFields]
    };
  };
  const [criteria, setCriteria] = useState<SearchCriteria>(
    getCriteriaFromFilterFields([])
  );

  useEffect(() => {
    if (hasViewPermission(PermissionEntity.ASSETS) && view === 'list') {
      dispatch(
        getAssets({ ...criteria, pageSize: 10, pageNum: 0, direction: 'DESC' })
      );
    }
  }, [criteria, dispatch, hasViewPermission, view]);

  useEffect(() => {
    if (!loadingGet) setRefreshing(false);
  }, [loadingGet]);

  useEffect(() => {
    if (
      route.params?.id &&
      assetsHierarchy.some(
        (asset) =>
          asset.hierarchy.includes(route.params.id) &&
          asset.id !== route.params.id
      )
    ) {
      return;
    }
    dispatch(
      getAssetChildren(route.params?.id ?? 0, route.params?.hierarchy ?? [])
    );
  }, [assetsHierarchy, dispatch, route.params?.hierarchy, route.params?.id]);

  const currentAssets = useMemo(() => {
    if (route.params?.id) {
      return assetsHierarchy.filter(
        (asset) =>
          asset.hierarchy[asset.hierarchy.length - 2] === route.params.id &&
          asset.id !== route.params.id
      );
    }
    return assetsHierarchy.filter((asset) => asset.hierarchy.length === 1);
  }, [assetsHierarchy, route.params?.id]);

  const onRefresh = () => {
    setRefreshing(true);
    if (view === 'list') {
      setCriteria(getCriteriaFromFilterFields([]));
    } else {
      dispatch(
        getAssetChildren(route.params?.id ?? 0, route.params?.hierarchy ?? [])
      );
    }
  };

  const onQueryChange = (query) => {
    onSearchQueryChange<AssetDTO>(
      query,
      criteria,
      setCriteria,
      setSearchQuery,
      [
        'name',
        'description',
        'model',
        'additionalInfos',
        'barCode',
        'area',
        'serialNumber',
        'manufacturer',
        'power',
        'customId',
        'customFieldValues.value'
      ]
    );
    setView('list');
  };
  useDebouncedEffect(
    () => {
      if (startedSearch) onQueryChange(searchQuery);
    },
    [searchQuery],
    1000
  );

  const handleViewChildren = useCallback(
    (asset: AssetRow) => {
      navigation.push('Assets', {
        id: asset.id,
        hierarchy: asset.hierarchy
      });
    },
    [navigation]
  );

  const isFiltered = !!searchQuery;
  const listData = view === 'list' ? assets.content : currentAssets;

  const renderListItem = useCallback(
    ({ item: asset }: { item: AssetDTO | AssetRow }) => (
      <AssetCard
        asset={asset}
        navigation={navigation}
        showChildrenButton={view === 'hierarchy'}
        onViewChildren={() => handleViewChildren(asset as AssetRow)}
      />
    ),
    [handleViewChildren, navigation, view]
  );

  const loadMore = () => {
    if (loadingGet) return;
    if (view === 'list') {
      if (!lastPage) dispatch(getMoreAssets(criteria, currentPageNum + 1));
    } else if (!assetChildrenLastPage[route.params?.id ?? 0]) {
      dispatch(
        getAssetChildren(
          route.params?.id ?? 0,
          route.params?.hierarchy ?? [],
          (assetChildrenPageNum[route.params?.id ?? 0] ?? 0) + 1
        )
      );
    }
  };

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      <Searchbar
        placeholder={t('search')}
        accessibilityLabel={t('search')}
        onFocus={() => setStartedSearch(true)}
        onChangeText={setSearchQuery}
        value={searchQuery}
        style={{ backgroundColor: theme.colors.card }}
      />
      <PaginatedEntityList
        data={listData}
        keyExtractor={(asset) => asset.id.toString()}
        renderItem={renderListItem}
        loading={loadingGet}
        refreshing={refreshing}
        onRefresh={onRefresh}
        onEndReached={loadMore}
        ListEmptyComponent={
          <EmptyState
            icon={isFiltered ? 'filter-remove-outline' : 'package-variant-closed'}
            title={t('no_element_match_criteria')}
            description={
              isFiltered ? t('no_element_match_criteria_description') : undefined
            }
            action={
              isFiltered
                ? {
                    label: t('reset'),
                    onPress: () => {
                      setSearchQuery('');
                      setView('hierarchy');
                      setCriteria(getCriteriaFromFilterFields([]));
                    }
                  }
                : undefined
            }
          />
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
