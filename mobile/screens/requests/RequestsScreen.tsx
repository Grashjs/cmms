import {
  ActivityIndicator,
  FlatList,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  View
} from 'react-native';
import { useDispatch, useSelector } from '../../store';
import * as React from 'react';
import { Fragment, useCallback, useContext, useEffect, useState } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import useAuth from '../../hooks/useAuth';
import { PermissionEntity } from '../../models/role';
import { getMoreRequests, getRequests } from '../../slices/request';
import { FilterField, SearchCriteria } from '../../models/page';
import { Badge, IconButton, Searchbar, Text } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import Request from '../../models/request';
import { getPriorityColor, onSearchQueryChange } from '../../utils/overall';
import { RootTabScreenProps } from '../../types';
import { useDebouncedEffect } from '../../hooks/useDebouncedEffect';
import { dayDiff } from '../../utils/dates';
import { getNotifications } from '../../slices/notification';
import _ from 'lodash';
import EnumFilter from '../workOrders/components/EnumFilter';
import { useAppTheme } from '../../custom-theme';
import { spacing } from '../../theme/tokens';
import {
  EmptyState,
  EntityListCard,
  EntityListCardMeta,
  ListSkeleton
} from '../../components/ui';

export default function RequestsScreen({
  navigation,
  route
}: RootTabScreenProps<'Requests'>) {
  const { t } = useTranslation();
  const [startedSearch, setStartedSearch] = useState<boolean>(false);
  const { requests, loadingGet, currentPageNum, lastPage } = useSelector(
    (state) => state.requests
  );
  const theme = useAppTheme();
  const dispatch = useDispatch();
  const { notifications } = useSelector((state) => state.notifications);
  const [searchQuery, setSearchQuery] = useState('');
  const [refreshing, setRefreshing] = useState(false);
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const notificationsCriteria: SearchCriteria = {
    filterFields: [],
    pageSize: 15,
    pageNum: 0,
    direction: 'DESC'
  };
  const { hasViewPermission, user } = useAuth();
  const defaultFilterFields: FilterField[] = [
    {
      field: 'priority',
      operation: 'in',
      values: [],
      value: '',
      enumName: 'PRIORITY'
    },
    {
      field: 'status',
      operation: 'in',
      values: ['APPROVED', 'CANCELLED', 'PENDING'],
      value: '',
      enumName: 'STATUS'
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
    if (hasViewPermission(PermissionEntity.REQUESTS)) {
      dispatch(
        getRequests({
          ...criteria,
          pageSize: 10,
          pageNum: 0,
          direction: 'DESC'
        })
      );
    }
  }, [criteria]);

  useEffect(() => {
    if (!loadingGet) setRefreshing(false);
  }, [loadingGet]);

  useEffect(() => {
    if (user.role.code === 'REQUESTER')
      navigation.setOptions({
        title: t('requests'),
        headerRight: () => (
          <View style={styles.headerActions}>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={t('notifications')}
              onPress={() => navigation.navigate('Notifications')}
              style={{ position: 'relative' }}
            >
              <IconButton icon="bell-outline" />
              <Badge
                style={styles.notificationBadge}
                visible={
                  notifications.content.filter((notification) => !notification.seen)
                    .length > 0
                }
              >
                {
                  notifications.content.filter(
                    (notification) => !notification.seen
                  ).length
                }
              </Badge>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={t('settings')}
              onPress={() => navigation.navigate('Settings')}
            >
              <IconButton icon="cog-outline" />
            </Pressable>
          </View>
        )
      });
  }, [navigation, notifications.content, t, user.role.code]);

  useEffect(() => {
    if (user.role.code === 'REQUESTER')
      dispatch(getNotifications(notificationsCriteria));
  }, [dispatch, user.role.code]);

  const onRefresh = () => {
    setRefreshing(true);
    setCriteria(getCriteriaFromFilterFields([]));
  };

  const getStatusMeta = (request: Request): [string, string] => {
    if (request.workOrder) {
      // @ts-ignore
      return [t('approved'), theme.colors.success];
    }
    if (request.cancelled) {
      return [t('rejected'), theme.colors.error];
    }
    return [t('pending'), theme.colors.primary];
  };

  const onFilterChange = (newFilters: FilterField[]) => {
    setCriteria({ ...criteria, filterFields: newFilters });
  };

  const onResetFilters = () => {
    setSearchQuery('');
    setCriteria(getCriteriaFromFilterFields([]));
  };

  const onQueryChange = (query) => {
    onSearchQueryChange<Request>(query, criteria, setCriteria, setSearchQuery, [
      'title',
      'description',
      'customId',
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

  const filtersAreDefault = _.isEqual(criteria.filterFields, defaultFilterFields);
  const isFiltered = !filtersAreDefault || !!searchQuery;
  const isInitialLoad = loadingGet && !requests.content.length;

  const renderItem = useCallback(
    ({ item: request }: { item: Request }) => {
      const [statusLabel, statusColor] = getStatusMeta(request);
      const meta: EntityListCardMeta[] = [];
      if (request.asset) {
        meta.push({ icon: 'package-variant-closed', label: request.asset.name });
      }
      if (request.location) {
        meta.push({ icon: 'map-marker-outline', label: request.location.name });
      }
      if (request.priority && request.priority !== 'NONE') {
        meta.push({
          icon: 'flag',
          label: t(request.priority),
          color: getPriorityColor(request.priority, theme)
        });
      }
      if (request.dueDate) {
        const overdue =
          (dayDiff(new Date(request.dueDate), new Date()) <= 2 ||
            new Date() > new Date(request.dueDate)) &&
          request.workOrder?.status !== 'COMPLETE';
        meta.push({
          icon: 'clock-alert-outline',
          label: getFormattedDate(request.dueDate),
          color: overdue ? theme.colors.error : undefined
        });
      }
      return (
        <EntityListCard
          title={request.title}
          subtitle={`#${request.customId}`}
          imageUrl={request.image?.url}
          icon="inbox-arrow-down-outline"
          badge={{ label: statusLabel, color: statusColor }}
          meta={meta}
          onPress={() => {
            if (request.workOrder) {
              navigation.push('WODetails', { id: request.workOrder.id });
            } else {
              navigation.push('RequestDetails', {
                id: request.id,
                requestProp: request
              });
            }
          }}
        />
      );
    },
    [getFormattedDate, navigation, t, theme]
  );

  const filterBar = (
    <ScrollView
      horizontal
      style={[styles.filterBar, { backgroundColor: theme.colors.card }]}
      contentContainerStyle={styles.filterBarContent}
      showsHorizontalScrollIndicator={false}
    >
      <EnumFilter
        filterFields={criteria.filterFields}
        onChange={onFilterChange}
        completeOptions={['NONE', 'LOW', 'MEDIUM', 'HIGH']}
        initialOptions={[]}
        fieldName="priority"
        icon="signal"
      />
      <EnumFilter
        filterFields={criteria.filterFields}
        onChange={onFilterChange}
        completeOptions={['APPROVED', 'CANCELLED', 'PENDING']}
        initialOptions={['APPROVED', 'CANCELLED', 'PENDING']}
        fieldName="status"
        icon="circle-double"
      />
      {!filtersAreDefault && (
        <IconButton
          icon="close"
          iconColor={theme.colors.error}
          accessibilityLabel={t('reset')}
          style={{ backgroundColor: theme.colors.background }}
          onPress={() => onFilterChange(defaultFilterFields)}
        />
      )}
    </ScrollView>
  );

  return (
    <View style={[styles.container, { backgroundColor: theme.colors.background }]}>
      {hasViewPermission(PermissionEntity.REQUESTS) ? (
        <Fragment>
          <Searchbar
            placeholder={t('search')}
            accessibilityLabel={t('search')}
            onFocus={() => setStartedSearch(true)}
            onChangeText={setSearchQuery}
            value={searchQuery}
            style={{ backgroundColor: theme.colors.card }}
          />
          {isInitialLoad ? (
            <>
              {filterBar}
              <ListSkeleton />
            </>
          ) : (
            <FlatList
              data={requests.content}
              keyExtractor={(request) => request.id.toString()}
              renderItem={renderItem}
              ListHeaderComponent={filterBar}
              contentContainerStyle={styles.listContent}
              onEndReached={() => {
                if (!loadingGet && !lastPage)
                  dispatch(getMoreRequests(criteria, currentPageNum + 1));
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
                  icon={isFiltered ? 'filter-remove-outline' : 'inbox-arrow-down-outline'}
                  title={t('no_element_match_criteria')}
                  description={
                    isFiltered ? t('no_element_match_criteria_description') : undefined
                  }
                  action={
                    isFiltered
                      ? { label: t('reset'), onPress: onResetFilters }
                      : undefined
                  }
                />
              }
            />
          )}
        </Fragment>
      ) : (
        <EmptyState
          icon="lock-outline"
          title={t('no_access_requests')}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  headerActions: {
    flexDirection: 'row'
  },
  notificationBadge: {
    position: 'absolute',
    bottom: 0,
    right: 0
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
    paddingBottom: 100,
    flexGrow: 1
  }
});
