import {
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
  TouchableRipple
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
import EnumFilter from './EnumFilter';
import { dayDiff } from '../../utils/dates';
import { IconWithLabel } from '../../components/IconWithLabel';
import QuickFilter from './QuickFilter';
import { useAppTheme } from '../../custom-theme';
import { getUserInitials } from '../../utils/displayers';
import { UserMiniDTO } from '../../models/user';

export default function WorkOrdersScreen({
  navigation,
  route
}: RootTabScreenProps<'WorkOrders'>) {
  const { t } = useTranslation();
  const [startedSearch, setStartedSearch] = useState<boolean>(false);
  const { workOrders, loadingGet, currentPageNum, lastPage } = useSelector(
    (state) => state.workOrders
  );
  const theme = useAppTheme();
  const dispatch = useDispatch();
  const [searchQuery, setSearchQuery] = useState('');
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

  const onRefresh = () => {
    setCriteria(getCriteriaFromFilterFields(route.params?.filterFields ?? []));
  };
  const onFilterChange = (newFilters: FilterField[]) => {
    const newCriteria = { ...criteria };
    newCriteria.filterFields = newFilters;
    setCriteria(newCriteria);
  };

  const onQueryChange = (query) => {
    onSearchQueryChange<WorkOrder>(
      query,
      criteria,
      setCriteria,
      setSearchQuery,
      ['title', 'description', 'feedback']
    );
  };
  useDebouncedEffect(
    () => {
      if (startedSearch) onQueryChange(searchQuery);
    },
    [searchQuery],
    1000
  );
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
        <ScrollView
          contentContainerStyle={{ paddingBottom: 100 }}
          style={styles.scrollView}
          onScroll={({ nativeEvent }) => {
            if (isCloseToBottom(nativeEvent)) {
              if (!loadingGet && !lastPage)
                dispatch(getMoreWorkOrders(criteria, currentPageNum + 1));
            }
          }}
          refreshControl={
            <RefreshControl
              refreshing={loadingGet}
              onRefresh={onRefresh}
              colors={[theme.colors.primary]}
            />
          }
          scrollEventThrottle={400}
        >
          <ScrollView
            horizontal
            style={{
              backgroundColor: 'white',
              borderRadius: 5,
              marginBottom: 2
            }}
            showsHorizontalScrollIndicator={false}
          >
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
              onPress={() =>
                navigation.navigate('WorkOrderFilters', {
                  filterFields: criteria.filterFields,
                  onFilterChange
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
          </ScrollView>
          {!!workOrders.content.length ? (
            workOrders.content.map((workOrder) => {
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
                  onPress={() =>
                    navigation.push('WODetails', {
                      id: workOrder.id,
                      workOrderProp: workOrder
                    })
                  }
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
                      {workOrder.image ? (
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
                          <View>
                            <Text
                              variant="titleMedium"
                              style={styles.cardTitle}
                            >
                              {workOrder.title}
                            </Text>
                            <Text
                              variant={'bodySmall'}
                              style={{ color: 'grey' }}
                            >{`#${workOrder.customId}`}</Text>
                          </View>
                          <Tag
                            text={t(workOrder.status)}
                            color="white"
                            backgroundColor={getStatusColor(
                              workOrder.status,
                              theme
                            )}
                          />
                        </View>
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
                                  {user.image ? (
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
                {t('no_element_match_criteria')}
              </Text>
            </View>
          )}
        </ScrollView>
      </Fragment>
    </View>
  );
}

const styles = StyleSheet.create({
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
