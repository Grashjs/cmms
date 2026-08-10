import {
  ActivityIndicator,
  FlatList,
  RefreshControl,
  ScrollView,
  StyleSheet,
  View
} from 'react-native';
import { useDispatch, useSelector } from '../../store';
import * as React from 'react';
import { useCallback, useContext, useEffect, useRef, useState } from 'react';
import useAuth from '../../hooks/useAuth';
import { PermissionEntity } from '../../models/role';
import { getMoreWorkOrders, getWorkOrders } from '../../slices/workOrder';
import { FilterField, SearchCriteria } from '../../models/page';
import { IconButton, Searchbar } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import WorkOrder from '../../models/workOrder';
import { onSearchQueryChange } from '../../utils/overall';
import { RootTabScreenProps } from '../../types';
import { useDebouncedEffect } from '../../hooks/useDebouncedEffect';
import _ from 'lodash';
import EnumFilter from './components/EnumFilter';
import QuickFilter from './components/QuickFilter';
import { useAppTheme } from '../../custom-theme';
import { spacing } from '../../theme/tokens';
import { EmptyState, ListSkeleton } from '../../components/ui';
import WorkOrderCard from './components/WorkOrderCard';

export default function WorkOrdersScreen({
  navigation,
  route
}: RootTabScreenProps<'WorkOrders'>) {
  const { t } = useTranslation();
  const [startedSearch, setStartedSearch] = useState<boolean>(false);
  const { workOrders, loadingGet, loadingMore, currentPageNum, lastPage } =
    useSelector((state) => state.workOrders);
  const theme = useAppTheme();
  const dispatch = useDispatch();
  const [searchQuery, setSearchQuery] = useState('');
  const fromHomeInit = useRef<boolean>(false);
  const { user, hasViewOtherPermission } = useAuth();
  // Distinguishes a user-initiated pull from a load triggered by a filter
  // change, so the refresh spinner only appears when it was asked for.
  const [refreshing, setRefreshing] = useState(false);

  const defaultFilterFields: FilterField[] = [
    {
      field: 'priority',
      operation: 'in',
      values: ['NONE', 'LOW', 'MEDIUM', 'HIGH'],
      value: '',
      enumName: 'PRIORITY'
    },
    {
      field: 'status',
      operation: 'in',
      values: ['OPEN', 'IN_PROGRESS', 'ON_HOLD'],
      value: '',
      enumName: 'STATUS'
    },
    {
      field: 'archived',
      operation: 'eq',
      value: false
    }
  ];
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
    if (route.params?.fromHome && !fromHomeInit.current) {
      fromHomeInit.current = true;
      return;
    }
    dispatch(
      getWorkOrders({
        ...criteria,
        pageSize: 10,
        pageNum: 0,
        direction: 'DESC'
      })
    );
    fromHomeInit.current = true;
  }, [criteria]);

  useEffect(() => {
    const filterFields = route.params?.filterFields ?? [];
    if (filterFields.length)
      setCriteria(getCriteriaFromFilterFields(filterFields));
  }, [route]);

  useEffect(() => {
    if (!loadingGet) setRefreshing(false);
  }, [loadingGet]);

  const onRefresh = () => {
    setRefreshing(true);
    setCriteria(getCriteriaFromFilterFields(route.params?.filterFields ?? []));
  };
  const onFilterChange = (newFilters: FilterField[]) => {
    const newCriteria = { ...criteria };
    newCriteria.filterFields = newFilters;
    setCriteria(newCriteria);
  };
  const onResetFilters = () => {
    setSearchQuery('');
    setCriteria(getCriteriaFromFilterFields([]));
  };

  const onQueryChange = (query) => {
    onSearchQueryChange<WorkOrder>(
      query,
      criteria,
      setCriteria,
      setSearchQuery,
      ['title', 'description', 'feedback', 'customId', 'customFieldValues.value']
    );
  };
  useDebouncedEffect(
    () => {
      if (startedSearch) onQueryChange(searchQuery);
    },
    [searchQuery],
    1000
  );

  const filtersAreDefault = _.isEqual(criteria.filterFields, defaultFilterFields);
  const isFiltered = !filtersAreDefault || !!searchQuery;
  const isInitialLoad = loadingGet && !workOrders.content.length;

  const renderItem = useCallback(
    ({ item }: { item: WorkOrder }) => (
      <WorkOrderCard
        workOrder={item}
        onPress={() =>
          navigation.push('WODetails', { id: item.id, workOrderProp: item })
        }
      />
    ),
    [navigation]
  );

  return (
    <View
      style={{ ...styles.container, backgroundColor: theme.colors.background }}
    >
      <Searchbar
        placeholder={t('search')}
        onFocus={() => setStartedSearch(true)}
        onChangeText={setSearchQuery}
        value={searchQuery}
        style={{ backgroundColor: theme.colors.card }}
      />
      {/* Pinned above the list: previously these scrolled away with the
          content, so changing a filter meant scrolling back to the top. */}
      <ScrollView
        horizontal
        style={[styles.filterBar, { backgroundColor: theme.colors.card }]}
        contentContainerStyle={styles.filterBarContent}
        showsHorizontalScrollIndicator={false}
      >
        <IconButton
          icon={filtersAreDefault ? 'filter-outline' : 'filter-check'}
          iconColor={filtersAreDefault ? undefined : theme.colors.white}
          accessibilityLabel={t('filters')}
          style={{
            backgroundColor: filtersAreDefault
              ? theme.colors.background
              : theme.colors.primary
          }}
          onPress={() =>
            navigation.navigate('WorkOrderFilters', {
              filterFields: criteria.filterFields,
              onFilterChange,
              onReset: onResetFilters
            })
          }
        />
        {hasViewOtherPermission(PermissionEntity.WORK_ORDERS) && (
          <QuickFilter
            filterFields={criteria.filterFields}
            activeFilterField={{
              field: 'assignedToUser',
              operation: 'eq',
              value: user.id
            }}
            onChange={onFilterChange}
          />
        )}
        <EnumFilter
          filterFields={criteria.filterFields}
          onChange={onFilterChange}
          completeOptions={['NONE', 'LOW', 'MEDIUM', 'HIGH']}
          initialOptions={['NONE', 'LOW', 'MEDIUM', 'HIGH']}
          fieldName="priority"
          icon="signal"
        />
        <EnumFilter
          filterFields={criteria.filterFields}
          onChange={onFilterChange}
          completeOptions={['OPEN', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETE']}
          initialOptions={['OPEN', 'IN_PROGRESS', 'ON_HOLD']}
          fieldName="status"
          icon="circle-double"
        />
        {!filtersAreDefault && (
          <IconButton
            icon={'close'}
            iconColor={theme.colors.error}
            accessibilityLabel={t('reset')}
            style={{ backgroundColor: theme.colors.background }}
            onPress={() => onFilterChange(defaultFilterFields)}
          />
        )}
      </ScrollView>

      {isInitialLoad ? (
        <ListSkeleton />
      ) : (
        <FlatList
          data={workOrders.content}
          keyExtractor={(item) => item.id.toString()}
          renderItem={renderItem}
          contentContainerStyle={styles.listContent}
          // Only fires while more pages exist, so reaching the bottom of a
          // fully loaded list no longer re-requests the last page.
          onEndReached={() => {
            if (!loadingGet && !loadingMore && !lastPage)
              dispatch(getMoreWorkOrders(criteria, currentPageNum + 1));
          }}
          onEndReachedThreshold={0.4}
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              onRefresh={onRefresh}
              colors={[theme.colors.primary]}
              tintColor={theme.colors.primary}
            />
          }
          ListEmptyComponent={
            <EmptyState
              icon={isFiltered ? 'filter-remove-outline' : 'clipboard-check-outline'}
              title={
                isFiltered ? t('no_element_match_criteria') : t('no_work_orders')
              }
              description={
                isFiltered
                  ? t('no_element_match_criteria_description')
                  : t('no_work_orders_description')
              }
              action={
                isFiltered
                  ? { label: t('reset'), onPress: onResetFilters }
                  : undefined
              }
            />
          }
          ListFooterComponent={
            loadingMore ? (
              <ActivityIndicator
                style={styles.footerLoader}
                color={theme.colors.primary}
              />
            ) : null
          }
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  filterBar: {
    flexGrow: 0,
    marginBottom: spacing.sm
  },
  filterBarContent: {
    alignItems: 'center',
    paddingHorizontal: spacing.sm,
    gap: spacing.xs
  },
  listContent: {
    paddingTop: spacing.xs,
    // Clears the floating action button and tab bar at the bottom of the list.
    paddingBottom: 100,
    flexGrow: 1
  },
  footerLoader: {
    marginVertical: spacing.lg
  }
});
