import { StyleSheet, View } from 'react-native';
import { useDispatch, useSelector } from '../../store';
import * as React from 'react';
import { useCallback, useEffect, useState } from 'react';
import useAuth from '../../hooks/useAuth';
import { PermissionEntity } from '../../models/role';
import { getMeters, getMoreMeters } from '../../slices/meter';
import { FilterField, SearchCriteria } from '../../models/page';
import { Searchbar } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import Meter from '../../models/meter';
import { canAddReading, onSearchQueryChange } from '../../utils/overall';
import { RootStackScreenProps } from '../../types';
import { useDebouncedEffect } from '../../hooks/useDebouncedEffect';
import { useAppTheme } from '../../custom-theme';
import {
  EmptyState,
  EntityListCard,
  PaginatedEntityList
} from '../../components/ui';

export default function MetersScreen({
  navigation
}: RootStackScreenProps<'Meters'>) {
  const { t } = useTranslation();
  const [startedSearch, setStartedSearch] = useState<boolean>(false);
  const { meters, loadingGet, currentPageNum, lastPage } = useSelector(
    (state) => state.meters
  );
  const theme = useAppTheme();
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
    if (hasViewPermission(PermissionEntity.METERS)) {
      dispatch(
        getMeters({ ...criteria, pageSize: 10, pageNum: 0, direction: 'DESC' })
      );
    }
  }, [criteria]);

  useEffect(() => {
    if (!loadingGet) setRefreshing(false);
  }, [loadingGet]);

  const onRefresh = () => {
    setRefreshing(true);
    setCriteria(getCriteriaFromFilterFields([]));
  };

  const onQueryChange = (query) => {
    onSearchQueryChange<Meter>(query, criteria, setCriteria, setSearchQuery, [
      'name',
      'unit',
      'category',
      'customFieldValues.value'
    ]);
  };
  useDebouncedEffect(
    () => {
      if (startedSearch) onQueryChange(searchQuery);
    },
    [searchQuery],
    1000
  );

  const isFiltered = !!searchQuery;
  const renderItem = useCallback(
    ({ item: meter }: { item: Meter }) => {
      const meta = [];
      if (meter.asset) {
        meta.push({ icon: 'package-variant-closed' as const, label: meter.asset.name });
      }
      if (meter.location) {
        meta.push({ icon: 'map-marker-outline' as const, label: meter.location.name });
      }
      const pastDue = canAddReading(meter);
      return (
        <EntityListCard
          title={meter.name}
          imageUrl={meter.image?.url}
          icon="gauge"
          badge={
            pastDue
              ? { label: t('past_due'), color: theme.colors.error }
              : undefined
          }
          meta={meta}
          onPress={() =>
            navigation.push('MeterDetails', {
              id: meter.id,
              meterProp: meter,
              onNewReading: () => dispatch(getMeters(criteria))
            })
          }
        />
      );
    },
    [criteria, dispatch, navigation, t, theme.colors.error]
  );

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
        data={meters.content}
        keyExtractor={(meter) => meter.id.toString()}
        renderItem={renderItem}
        loading={loadingGet}
        refreshing={refreshing}
        onRefresh={onRefresh}
        onEndReached={() => {
          if (!loadingGet && !lastPage)
            dispatch(getMoreMeters(criteria, currentPageNum + 1));
        }}
        ListEmptyComponent={
          <EmptyState
            icon={isFiltered ? 'filter-remove-outline' : 'gauge'}
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
