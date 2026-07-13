import { RefreshControl, ScrollView, StyleSheet, View } from 'react-native';
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
  useTheme
} from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import WorkOrder from '../../models/workOrder';
import {
  getPriorityColor,
  getStatusColor,
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
  const isCloseToBottom = ({
    layoutMeasurement,
    contentOffset,
    contentSize
  }) => {
    const paddingToBottom = 20;
    return (
      layoutMeasurement.height + contentOffset.y >=
      contentSize.height - paddingToBottom
    );
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
            style={{ backgroundColor: 'white', borderRadius: 5 }}
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
            workOrders.content.map((workOrder) => (
              <Card
                style={{
                  padding: 5,
                  marginVertical: 5,
                  backgroundColor: 'white'
                }}
                key={workOrder.id}
                onPress={() =>
                  navigation.push('WODetails', {
                    id: workOrder.id,
                    workOrderProp: workOrder
                  })
                }
              >
                <Card.Content>
                  <View
                    style={{ ...styles.row, justifyContent: 'space-between' }}
                  >
                    <Tag
                      text={t(workOrder.status)}
                      color="white"
                      backgroundColor={getStatusColor(workOrder.status, theme)}
                    />
                    <View
                      style={{
                        ...styles.row,
                        justifyContent: 'space-between'
                      }}
                    >
                      <View style={{ marginRight: 10 }}>
                        <Tag
                          text={`#${workOrder.customId}`}
                          color="white"
                          backgroundColor="#545454"
                        />
                      </View>
                      {workOrder.priority !== 'NONE' && (
                        <Tag
                          text={t(workOrder.priority)}
                          color="white"
                          backgroundColor={getPriorityColor(
                            workOrder.priority,
                            theme
                          )}
                        />
                      )}
                    </View>
                  </View>
                  <Text style={{ fontWeight: 'bold', fontSize: 18 }}>
                    {workOrder.title}
                  </Text>
                  {workOrder.dueDate && (
                    <IconWithLabel
                      color={
                        (dayDiff(new Date(workOrder.dueDate), new Date()) <=
                          2 ||
                          new Date() > new Date(workOrder.dueDate)) &&
                        workOrder.status !== 'COMPLETE'
                          ? theme.colors.error
                          : theme.colors.grey
                      }
                      label={getFormattedDate(workOrder.dueDate)}
                      icon="clock-alert-outline"
                    />
                  )}
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
                </Card.Content>
              </Card>
            ))
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
    alignItems: 'center',
    justifyContent: 'center'
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold'
  },
  scrollView: {
    width: '100%',
    height: '100%',
    padding: 5
  },
  row: {
    display: 'flex',
    flexDirection: 'row',
    alignItems: 'center'
  }
});
