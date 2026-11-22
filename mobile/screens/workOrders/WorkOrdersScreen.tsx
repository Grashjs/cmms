import {
  Alert,
  Image,
  RefreshControl,
  ScrollView,
  StyleSheet,
  TouchableHighlight,
  TouchableOpacity,
  View
} from 'react-native';
import { useDispatch, useSelector } from '../../store';
import * as React from 'react';
import { Fragment, useContext, useEffect, useRef, useState } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import useAuth from '../../hooks/useAuth';
import { PermissionEntity } from '../../models/role';
import { getMoreWorkOrders, getWorkOrders } from '../../slices/workOrder';
import { FilterField, SearchCriteria } from '../../models/page';
import {
  Card,
  IconButton,
  Searchbar,
  Text,
  useTheme,
  Avatar,
  TouchableRipple,
  Button,
  Checkbox
} from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import WorkOrder from '../../models/workOrder';
import {
  getPriorityColor,
  getStatusColor,
  isCloseToBottom,
  onSearchQueryChange
} from '../../utils/overall';
import { RootTabScreenProps } from '../../types';
import Tag from '../../components/Tag';
import { useDebouncedEffect } from '../../hooks/useDebouncedEffect';
import _ from 'lodash';
import EnumFilter from './components/EnumFilter';
import OfflinePreparationFilters, {
  type PreparationCopyFilter,
  type PreparationStatusFilter
} from './components/OfflinePreparationFilters';
import { dayDiff } from '../../utils/dates';
import { IconWithLabel } from '../../components/IconWithLabel';
import QuickFilter from './components/QuickFilter';
import { useAppTheme } from '../../custom-theme';
import { getUserInitials } from '../../utils/displayers';
import { UserMiniDTO } from '../../models/user';
import { useOffline } from '../../offline/OfflineProvider';
import {
  usePreparationsMissingFiles,
  useWorkOrderActivity
} from '../../offline/hooks';
import { OFFLINE_LIMITS } from '../../offline/types';
import { sessionGeneration } from '../../offline/sessionBridge';

export default function WorkOrdersScreen({
  navigation,
  route
}: RootTabScreenProps<'WorkOrders'>) {
  const { t } = useTranslation();
  const offline = useOffline();
  const activity = useWorkOrderActivity();
  const missingFiles = usePreparationsMissingFiles();
  const generation = sessionGeneration();
  const [manage, setManage] = useState(false);
  const [copyFilter, setCopyFilter] = useState<PreparationCopyFilter>('all');
  const [statusFilter, setStatusFilter] =
    useState<PreparationStatusFilter>('all');
  const [selected, setSelected] = useState<number[]>([]);
  const [adding, setAdding] = useState(false);
  const managing = offline.enabled && manage;
  const [offlineFilter, setOfflineFilter] = useState(false);
  const disconnected = offline.enabled && !offline.networkAvailable;
  const visibleCopyFilter =
    disconnected && copyFilter === 'missing' ? 'prepared' : copyFilter;
  const onlyOffline =
    offline.enabled &&
    (disconnected ||
      (managing ? visibleCopyFilter === 'prepared' : offlineFilter));
  const preparedIdsInPage = useSelector(
    (state) => state.workOrders.preparedIdsInPage ?? []
  );
  const orderedIdsInPage = useSelector(
    (state) => state.workOrders.orderedIdsInPage ?? []
  );
  const [startedSearch, setStartedSearch] = useState<boolean>(false);
  const { workOrders, loadingGet, currentPageNum, lastPage } = useSelector(
    (state) => state.workOrders
  );
  const [searchQuery, setSearchQuery] = useState('');
  const visiblePrepared = offline.readable
    ? offline.prepared.filter((item) => item.available && item.workOrder)
    : [];
  const localMatches = visiblePrepared.filter(
    ({ workOrder }) =>
      !searchQuery ||
      `${workOrder.title} ${workOrder.description || ''} ${
        workOrder.customId || ''
      }`
        .toLowerCase()
        .includes(searchQuery.toLowerCase())
  );
  const connectedWorkOrders = [
    ...workOrders.content,
    ...visiblePrepared
      .filter((item) => preparedIdsInPage.includes(item.id))
      .map((item) => item.workOrder)
  ];
  const connectedById = new Map(
    connectedWorkOrders.map((item) => [item.id, item])
  );
  const copyIds = new Set(offline.prepared.map((item) => item.id));
  const requests = new Map(
    offline.preparations.map((request) => [request.workOrderId, request])
  );
  const atLimit = offline.preparations.some(
    (request) => request.state === 'FULL'
  );
  const listedWorkOrders = (
    onlyOffline
      ? localMatches.map((item) => item.workOrder)
      : orderedIdsInPage.length
      ? orderedIdsInPage.map((id) => connectedById.get(id)).filter(Boolean)
      : connectedWorkOrders
  ).filter(
    (item) =>
      !managing ||
      ((visibleCopyFilter !== 'missing' || !copyIds.has(item.id)) &&
        (statusFilter === 'all' ||
          (item.status === 'COMPLETE') === (statusFilter === 'complete')))
  );
  if (disconnected) listedWorkOrders.sort((a, b) => b.id - a.id);
  const selectableIds = new Set(
    managing
      ? listedWorkOrders
          .filter(({ id }) => {
            const request = requests.get(id);
            return !copyIds.has(id) && (!request || request.state === 'FAILED');
          })
          .map(({ id }) => id)
      : []
  );
  const allVisibleSelected = [...selectableIds].every((id) =>
    selected.includes(id)
  );
  const theme = useAppTheme();
  const dispatch = useDispatch();
  const fromHomeInit = useRef<boolean>(false);
  const { getFormattedDate, getUserNameById } = useContext(
    CompanySettingsContext
  );
  const { hasViewPermission, user, hasViewOtherPermission } = useAuth();
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
  const listCriteria = React.useMemo(() => {
    if (!managing) return criteria;
    const filterFields = criteria.filterFields.filter(
      (filter) => filter.field !== 'status' && filter.field !== 'archived'
    );
    if (statusFilter !== 'all')
      filterFields.push({
        field: 'status',
        operation: 'in',
        enumName: 'STATUS',
        value: '',
        values:
          statusFilter === 'complete'
            ? ['COMPLETE']
            : ['OPEN', 'IN_PROGRESS', 'ON_HOLD']
      });
    return { ...criteria, filterFields };
  }, [criteria, managing, statusFilter]);
  useEffect(() => {
    setManage(false);
    setSelected([]);
    setCopyFilter('all');
    setStatusFilter('all');
  }, [offline.enabled, generation]);
  useEffect(() => {
    if (!offline.initialized || onlyOffline) return;
    if (route.params?.fromHome && !fromHomeInit.current) {
      fromHomeInit.current = true;
      return;
    }
    dispatch(
      getWorkOrders({
        ...listCriteria,
        pageSize: 10,
        pageNum: 0,
        direction: 'DESC'
      })
    ).catch(offline.reportError);
    fromHomeInit.current = true;
  }, [listCriteria, onlyOffline, offline.initialized, offline.enabled]);

  const fetchMore = () => {
    if (!onlyOffline && !loadingGet && !lastPage)
      void dispatch(getMoreWorkOrders(listCriteria, currentPageNum + 1)).catch(
        offline.reportError
      );
  };
  const enqueueSelection = async () => {
    if (adding || !selected.length) return;
    setAdding(true);
    try {
      await offline.prepareMany(selected);
      if (generation === sessionGeneration()) setSelected([]);
    } catch (error) {
      if (generation === sessionGeneration()) offline.reportError(error);
    } finally {
      setAdding(false);
    }
  };
  const removeCopy = (id: number) => {
    Alert.alert(t('offline_remove'), t('offline_remove_clean_confirm'), [
      { text: t('cancel'), style: 'cancel' },
      {
        text: t('offline_remove'),
        style: 'destructive',
        onPress: () => {
          if (generation !== sessionGeneration()) return;
          void offline
            .removeCleanPreparation(id)
            .then(() => {
              if (
                generation === sessionGeneration() &&
                !onlyOffline &&
                offline.networkAvailable
              )
                return dispatch(getWorkOrders(listCriteria));
            })
            .catch((error) => {
              if (generation === sessionGeneration())
                offline.reportError(error);
            });
        }
      }
    ]);
  };

  useEffect(() => {
    const filterFields = route.params?.filterFields ?? [];
    if (filterFields.length)
      setCriteria(getCriteriaFromFilterFields(filterFields));
  }, [route]);

  const onRefresh = () => {
    if (onlyOffline) {
      void offline.sync();
      return;
    }
    setCriteria(getCriteriaFromFilterFields(route.params?.filterFields ?? []));
  };
  const onFilterChange = (newFilters: FilterField[]) => {
    const newCriteria = { ...criteria };
    newCriteria.filterFields = newFilters;
    setCriteria(newCriteria);
  };
  const onResetFilters = () => {
    setCriteria(getCriteriaFromFilterFields([]));
  };

  const onQueryChange = (query) => {
    onSearchQueryChange<WorkOrder>(
      query,
      criteria,
      setCriteria,
      setSearchQuery,
      [
        'title',
        'description',
        'feedback',
        'customId',
        'customFieldValues.value'
      ]
    );
  };
  useDebouncedEffect(
    () => {
      if (startedSearch) onQueryChange(searchQuery);
    },
    [searchQuery],
    1000
  );
  if (!offline.initialized) return <Text>{t('offline_preparing')}</Text>;
  return (
    <View
      style={{ ...styles.container, backgroundColor: theme.colors.background }}
    >
      <Fragment>
        <Searchbar
          placeholder={t('search')}
          onFocus={() => setStartedSearch(true)}
          onChangeText={setSearchQuery}
          value={searchQuery}
          style={{ backgroundColor: theme.colors.background }}
        />
        {offline.enabled && (
          <View style={styles.managePanel}>
            <View style={[styles.manageInset, styles.manageActions]}>
              <Button
                mode={managing ? 'outlined' : 'text'}
                icon="download-outline"
                onPress={() => {
                  if (managing) setSelected([]);
                  setManage(!managing);
                }}
              >
                {t(managing ? 'close' : 'offline_manage_preparations')}
              </Button>
              <Text variant="labelMedium">
                {t('offline_preparation_usage', {
                  used: offline.prepared.length,
                  limit: OFFLINE_LIMITS.preparedWorkOrders
                })}
              </Text>
            </View>
            {atLimit && (
              <Text variant="bodySmall" style={styles.manageInset}>
                {t('offline_preparation_capacity')}
              </Text>
            )}
            {managing && (
              <>
                <OfflinePreparationFilters
                  status={statusFilter}
                  copy={visibleCopyFilter}
                  networkAvailable={offline.networkAvailable}
                  onStatusChange={setStatusFilter}
                  onCopyChange={setCopyFilter}
                />
                {selectableIds.size > 0 && (
                  <Button
                    compact
                    style={styles.manageInset}
                    disabled={adding}
                    onPress={() =>
                      setSelected((ids) =>
                        allVisibleSelected
                          ? ids.filter((id) => !selectableIds.has(id))
                          : [...new Set([...ids, ...selectableIds])]
                      )
                    }
                  >
                    {t(
                      allVisibleSelected
                        ? 'offline_deselect_all'
                        : 'offline_select_all'
                    )}
                  </Button>
                )}
                {!!selected.length && (
                  <Button
                    mode="contained"
                    style={styles.manageInset}
                    loading={adding}
                    disabled={adding}
                    onPress={() => void enqueueSelection()}
                  >
                    {t('offline_prepare')} ({selected.length})
                  </Button>
                )}
                {offline.preparations.some(
                  (request) => request.state !== 'FAILED'
                ) && (
                  <Button
                    compact
                    style={styles.manageInset}
                    disabled={!offline.networkAvailable || offline.syncing}
                    onPress={() => void offline.sync()}
                  >
                    {t('offline_sync')}
                  </Button>
                )}
              </>
            )}
          </View>
        )}
        <ScrollView
          contentContainerStyle={{ paddingBottom: 100 }}
          style={[styles.scrollView, managing && styles.managedList]}
          onScroll={({ nativeEvent }) => {
            if (isCloseToBottom(nativeEvent)) fetchMore();
          }}
          refreshControl={
            <RefreshControl
              refreshing={onlyOffline ? offline.syncing : loadingGet}
              onRefresh={onRefresh}
              colors={[theme.colors.primary]}
            />
          }
          scrollEventThrottle={400}
        >
          {!managing && (!onlyOffline || offline.networkAvailable) && (
            <ScrollView
              horizontal
              style={{
                backgroundColor: 'white',
                borderRadius: 5,
                marginBottom: 2
              }}
              showsHorizontalScrollIndicator={false}
            >
              {!onlyOffline && (
                <IconButton
                  icon={
                    _.isEqual(criteria.filterFields, defaultFilterFields)
                      ? 'filter-outline'
                      : 'filter-check'
                  }
                  iconColor={
                    _.isEqual(criteria.filterFields, defaultFilterFields)
                      ? undefined
                      : 'white'
                  }
                  style={{
                    backgroundColor: _.isEqual(
                      criteria.filterFields,
                      defaultFilterFields
                    )
                      ? theme.colors.background
                      : theme.colors.primary
                  }}
                  onPress={() => navigation.navigate('WorkOrderFilters', {
                    filterFields: criteria.filterFields, onFilterChange, onReset: onResetFilters
                  })}
                />
              )}
              {offline.enabled && offline.networkAvailable && (
                <TouchableOpacity
                  onPress={() => setOfflineFilter((current) => !current)}
                  style={{
                    backgroundColor: offlineFilter
                      ? theme.colors.primary
                      : theme.colors.background,
                    borderRadius: 20,
                    paddingHorizontal: 15,
                    margin: 5,
                    justifyContent: 'center'
                  }}
                >
                  <Text style={{ color: offlineFilter ? 'white' : 'black', fontWeight: 'bold' }}>
                    {t('offline_filter_prepared')}
                  </Text>
                </TouchableOpacity>
              )}
              {!onlyOffline && (
                <>
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
                    completeOptions={[
                      'OPEN',
                      'IN_PROGRESS',
                      'ON_HOLD',
                      'COMPLETE'
                    ]}
                    initialOptions={['OPEN', 'IN_PROGRESS', 'ON_HOLD']}
                    fieldName="status"
                    icon="circle-double"
                  />
                  {!_.isEqual(criteria.filterFields, defaultFilterFields) && (
                    <IconButton
                      icon={'close'}
                      iconColor={theme.colors.error}
                      style={{
                        backgroundColor: theme.colors.background
                      }}
                      onPress={() => onFilterChange(defaultFilterFields)}
                    />
                  )}
                </>
              )}
            </ScrollView>
          )}
          {offline.prepared
            .filter((item) => !item.available || !offline.readable)
            .map((item) => (
              <Button
                key={`unavailable:${item.id}`}
                onPress={() =>
                  navigation.navigate('WODetails', { id: item.id })
                }
              >
                {t('offline_unavailable')} #{item.id}
              </Button>
            ))}
          {!!listedWorkOrders.length ? (
            listedWorkOrders.map((workOrder) => {
              const local = visiblePrepared.some(
                (item) => item.id === workOrder.id
              );
              const localActivity = local
                ? activity?.get(workOrder.id)
                : undefined;
              const status = localActivity?.status ?? workOrder.status;
              const badge = localActivity?.badge ?? 'offline_badge_prepared';
              const preparation = requests.get(workOrder.id);
              const selectable = selectableIds.has(workOrder.id);
              const toggleSelection = () =>
                setSelected((ids) =>
                  ids.includes(workOrder.id)
                    ? ids.filter((id) => id !== workOrder.id)
                    : [...ids, workOrder.id]
                );
              // Combine primaryUser and assignedTo without duplicates
              const allUsers: UserMiniDTO[] = [];
              const userIds = new Set();

              if (workOrder.primaryUser) {
                allUsers.push(workOrder.primaryUser);
                userIds.add(workOrder.primaryUser.id);
              }

              if (workOrder.assignedTo?.length) {
                workOrder.assignedTo.forEach((user) => {
                  if (!userIds.has(user.id)) {
                    allUsers.push(user);
                    userIds.add(user.id);
                  }
                });
              }

              return (
                <TouchableOpacity
                  accessibilityRole={selectable ? 'checkbox' : undefined}
                  accessibilityState={
                    selectable
                      ? { checked: selected.includes(workOrder.id) }
                      : undefined
                  }
                  onPress={() => {
                    if (managing) {
                      if (selectable) toggleSelection();
                      return;
                    }
                    navigation.push('WODetails', {
                      id: workOrder.id,
                      ...(offline.enabled &&
                      visiblePrepared.some((item) => item.id === workOrder.id)
                        ? {}
                        : { workOrderProp: workOrder })
                    });
                  }}
                  key={workOrder.id}
                >
                  <View style={styles.card}>
                    {/* Header: Title, ID, and Status */}
                    <View
                      style={{
                        display: 'flex',
                        flexDirection: 'row',
                        gap: 6
                      }}
                    >
                      {workOrder.image &&
                      (!offline.enabled || offline.networkAvailable) ? (
                        <Avatar.Image
                          size={50}
                          source={{ uri: workOrder.image?.url }}
                        />
                      ) : (
                        <Avatar.Icon
                          style={{
                            backgroundColor: theme.colors.background
                          }}
                          color={'white'}
                          icon={'clipboard-text-outline'}
                          size={50}
                        />
                      )}
                      <View style={{ flex: 1 }}>
                        <View style={styles.cardHeader}>
                          {selectable && (
                            <View
                              pointerEvents="none"
                              importantForAccessibility="no-hide-descendants"
                            >
                              <Checkbox.Android
                                status={
                                  selected.includes(workOrder.id)
                                    ? 'checked'
                                    : 'unchecked'
                                }
                              />
                            </View>
                          )}
                          <View style={{ flex: 1, marginRight: 8 }}>
                            <Text
                              variant="titleMedium"
                              style={styles.cardTitle}
                              numberOfLines={2}
                            >
                              {workOrder.title}
                            </Text>
                            <Text
                              variant={'bodySmall'}
                              style={{ color: 'grey' }}
                            >{`#${workOrder.customId}`}</Text>
                          </View>
                          <Tag
                            text={t(status)}
                            color="white"
                            backgroundColor={getStatusColor(status, theme)}
                          />
                        </View>
                        {local &&
                          (badge !== 'offline_badge_prepared' ||
                            !missingFiles?.has(workOrder.id)) && (
                            <Text variant="labelSmall">{t(badge)}</Text>
                          )}
                        {local && missingFiles?.has(workOrder.id) && (
                          <Text variant="labelSmall">
                            {t('offline_files_not_downloaded')}
                          </Text>
                        )}
                        {!local && preparation && (
                          <Text variant="labelSmall">
                            {t(
                              preparation.state === 'FAILED'
                                ? 'offline_preparation_failed'
                                : 'offline_preparation_waiting'
                            )}
                          </Text>
                        )}
                        {/* Body: Asset and Location */}
                        <View style={styles.cardBody}>
                          {workOrder.asset && (
                            <IconWithLabel
                              label={workOrder.asset.name}
                              icon="package-variant-closed"
                              color={theme.colors.grey}
                            />
                          )}
                          {workOrder.location && (
                            <IconWithLabel
                              label={workOrder.location.name}
                              icon="map-marker-outline"
                              color={theme.colors.grey}
                            />
                          )}
                          {workOrder.priority &&
                            workOrder.priority !== 'NONE' && (
                              <Tag
                                text={t(workOrder.priority)}
                                color={getPriorityColor(
                                  workOrder.priority,
                                  theme
                                )}
                                backgroundColor={'transparent'}
                              />
                            )}
                        </View>
                        {/* Footer: Due Date and Assignees */}
                        <View style={styles.cardFooter}>
                          {workOrder.dueDate && (
                            <IconWithLabel
                              color={
                                (dayDiff(
                                  new Date(workOrder.dueDate),
                                  new Date()
                                ) <= 2 ||
                                  new Date() > new Date(workOrder.dueDate)) &&
                                workOrder.status !== 'COMPLETE'
                                  ? theme.colors.error
                                  : theme.colors.grey
                              }
                              label={getFormattedDate(workOrder.dueDate)}
                              icon="clock-alert-outline"
                            />
                          )}
                          <View style={{ flex: 1 }} />
                          {allUsers.length > 0 && (
                            <View style={styles.assigneeContainer}>
                              {allUsers.slice(0, 3).map((user, index) => (
                                <View
                                  key={user.id}
                                  style={{ marginLeft: index > 0 ? -8 : 0 }}
                                >
                                  {user.image &&
                                  (!offline.enabled ||
                                    offline.networkAvailable) ? (
                                    <Avatar.Image
                                      source={{ uri: user.image.url }}
                                      size={24}
                                    />
                                  ) : (
                                    <Avatar.Text
                                      size={24}
                                      label={getUserInitials(user)}
                                    />
                                  )}
                                </View>
                              ))}
                              {allUsers.length > 3 && (
                                <Text
                                  variant="bodySmall"
                                  style={{ marginLeft: 8 }}
                                >
                                  +{allUsers.length - 3}
                                </Text>
                              )}
                            </View>
                          )}
                        </View>
                      </View>
                    </View>
                    {managing && local && (
                      <Button compact onPress={() => removeCopy(workOrder.id)}>
                        {t('offline_remove')}
                      </Button>
                    )}
                  </View>
                </TouchableOpacity>
              );
            })
          ) : loadingGet ? null : (
            <View
              style={{
                backgroundColor: 'white',
                padding: 20,
                borderRadius: 10
              }}
            >
              <Text variant={'titleLarge'}>
                {t(
                  onlyOffline && !visiblePrepared.length
                    ? 'offline_empty_list'
                    : 'no_element_match_criteria'
                )}
              </Text>
            </View>
          )}
          {managing && !onlyOffline && !lastPage && (
            <Button
              loading={loadingGet}
              disabled={loadingGet}
              onPress={fetchMore}
            >
              {t('more')}
            </Button>
          )}
        </ScrollView>
      </Fragment>
    </View>
  );
}

const styles = StyleSheet.create({
  managePanel: { gap: 5 },
  manageInset: { marginHorizontal: 12 },
  manageActions: {
    flexDirection: 'row',
    alignItems: 'center',
    flexWrap: 'wrap',
    justifyContent: 'space-between'
  },
  container: {
    flex: 1,
    // alignItems: 'center',
    justifyContent: 'center'
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold'
  },
  scrollView: {
    width: '100%',
    height: '100%'
  },
  managedList: { marginTop: 8 },
  row: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center'
  },
  card: {
    backgroundColor: 'white',
    marginBottom: 1,
    padding: 10
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 8
  },
  cardTitle: {
    fontWeight: 'bold',
    marginBottom: 4,
    flexShrink: 1
  },
  divider: {
    height: 1,
    backgroundColor: '#e0e0e0',
    marginVertical: 8
  },
  cardBody: {
    gap: 10
  },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 10
  },
  assigneeContainer: {
    flexDirection: 'row',
    alignItems: 'center'
  }
});
