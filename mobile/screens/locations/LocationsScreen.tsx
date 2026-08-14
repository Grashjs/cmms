import { StyleSheet, View } from 'react-native';
import { useDispatch, useSelector } from '../../store';
import * as React from 'react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import useAuth from '../../hooks/useAuth';
import { PermissionEntity } from '../../models/role';
import {
  getLocationChildren,
  getLocations,
  getMoreLocations
} from '../../slices/location';
import { FilterField, SearchCriteria } from '../../models/page';
import { Button, Searchbar } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import Location from '../../models/location';
import { onSearchQueryChange } from '../../utils/overall';
import { RootStackScreenProps } from '../../types';
import { useDebouncedEffect } from '../../hooks/useDebouncedEffect';
import { useAppTheme } from '../../custom-theme';
import {
  EmptyState,
  EntityListCard,
  PaginatedEntityList
} from '../../components/ui';

interface LocationRow extends Location {
  hierarchy?: number[];
  hasChildren?: boolean;
}

interface LocationCardProps {
  location: LocationRow;
  navigation: RootStackScreenProps<'Locations'>['navigation'];
  showChildrenButton?: boolean;
  onViewChildren?: () => void;
}

function LocationCard({
  location,
  navigation,
  showChildrenButton = false,
  onViewChildren
}: LocationCardProps) {
  const { t } = useTranslation();

  return (
    <EntityListCard
      title={location.name}
      subtitle={`#${location.customId}`}
      icon="map-marker-outline"
      meta={
        location.address
          ? [{ icon: 'map-legend', label: location.address }]
          : undefined
      }
      onPress={() =>
        navigation.push('LocationDetails', {
          id: location.id,
          locationProp: location
        })
      }
      footer={
        showChildrenButton && location.hasChildren ? (
          <Button compact onPress={onViewChildren} accessibilityLabel={t('view_children')}>
            {t('view_children')}
          </Button>
        ) : undefined
      }
    />
  );
}

export default function LocationsScreen({
  navigation,
  route
}: RootStackScreenProps<'Locations'>) {
  const { t } = useTranslation();
  const [startedSearch, setStartedSearch] = useState<boolean>(false);
  const {
    locations,
    locationsHierarchy,
    loadingGet,
    currentPageNum,
    lastPage,
    locationChildrenPageNum,
    locationChildrenLastPage
  } = useSelector((state) => state.locations);
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
    if (hasViewPermission(PermissionEntity.LOCATIONS) && view === 'list') {
      dispatch(
        getLocations({
          ...criteria,
          pageSize: 10,
          pageNum: 0,
          direction: 'DESC'
        })
      );
    }
  }, [criteria, dispatch, hasViewPermission, view]);

  useEffect(() => {
    if (!loadingGet) setRefreshing(false);
  }, [loadingGet]);

  useEffect(() => {
    if (
      route.params?.id &&
      locationsHierarchy.some(
        (location) =>
          location.hierarchy.includes(route.params.id) &&
          location.id !== route.params.id
      )
    ) {
      return;
    }
    dispatch(
      getLocationChildren(route.params?.id ?? 0, route.params?.hierarchy ?? [])
    );
  }, [dispatch, locationsHierarchy, route.params?.hierarchy, route.params?.id]);

  const currentLocations = useMemo(() => {
    if (route.params?.id) {
      return locationsHierarchy.filter(
        (location) =>
          location.hierarchy[location.hierarchy.length - 2] === route.params.id &&
          location.id !== route.params.id
      );
    }
    return locationsHierarchy.filter((location) => location.hierarchy.length === 1);
  }, [locationsHierarchy, route.params?.id]);

  const onRefresh = () => {
    setRefreshing(true);
    if (view === 'list') {
      setCriteria(getCriteriaFromFilterFields([]));
    } else {
      dispatch(
        getLocationChildren(route.params?.id ?? 0, route.params?.hierarchy ?? [])
      );
    }
  };

  const onQueryChange = (query) => {
    onSearchQueryChange<Location>(
      query,
      criteria,
      setCriteria,
      setSearchQuery,
      ['name', 'address', 'customId', 'customFieldValues.value']
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
    (location: LocationRow) => {
      navigation.push('Locations', {
        id: location.id,
        hierarchy: location.hierarchy
      });
    },
    [navigation]
  );

  const isFiltered = !!searchQuery;
  const listData = view === 'list' ? locations.content : currentLocations;

  const renderListItem = useCallback(
    ({ item: location }: { item: LocationRow }) => (
      <LocationCard
        location={location}
        navigation={navigation}
        showChildrenButton={view === 'hierarchy'}
        onViewChildren={() => handleViewChildren(location)}
      />
    ),
    [handleViewChildren, navigation, view]
  );

  const loadMore = () => {
    if (loadingGet) return;
    if (view === 'list') {
      if (!lastPage) dispatch(getMoreLocations(criteria, currentPageNum + 1));
    } else if (!locationChildrenLastPage[route.params?.id ?? 0]) {
      dispatch(
        getLocationChildren(
          route.params?.id ?? 0,
          route.params?.hierarchy ?? [],
          (locationChildrenPageNum[route.params?.id ?? 0] ?? 0) + 1
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
        keyExtractor={(location) => location.id.toString()}
        renderItem={renderListItem}
        loading={loadingGet}
        refreshing={refreshing}
        onRefresh={onRefresh}
        onEndReached={loadMore}
        ListEmptyComponent={
          <EmptyState
            icon={isFiltered ? 'filter-remove-outline' : 'map-marker-outline'}
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
