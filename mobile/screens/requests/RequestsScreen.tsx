import {
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  View
} from 'react-native';
import { useDispatch, useSelector } from '../../store';
import * as React from 'react';
import { Fragment, useContext, useEffect, useState } from 'react';
import { CompanySettingsContext } from '../../contexts/CompanySettingsContext';
import useAuth from '../../hooks/useAuth';
import { PermissionEntity } from '../../models/role';
import { getMoreRequests, getRequests } from '../../slices/request';
import { FAB } from 'react-native-paper';
import { FilterField, SearchCriteria } from '../../models/page';
import {
  Badge,
  Card,
  IconButton,
  Searchbar,
  Text,
  useTheme
} from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import Request from '../../models/request';
import { IconSource } from 'react-native-paper/src/components/Icon';
import { getPriorityColor, onSearchQueryChange } from '../../utils/overall';
import { RootTabScreenProps } from '../../types';
import Tag from '../../components/Tag';
import { useDebouncedEffect } from '../../hooks/useDebouncedEffect';
import { dayDiff } from '../../utils/dates';
import { getNotifications } from '../../slices/notification';
import { SheetManager } from 'react-native-actions-sheet';
import _ from 'lodash';
import EnumFilter from '../workOrders/EnumFilter';
import { IconWithLabel } from '../../components/IconWithLabel';
import { useAppTheme } from '../../custom-theme';

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
  const { getFormattedDate, getUserNameById } = useContext(
    CompanySettingsContext
  );
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
    if (user.role.code === 'REQUESTER')
      navigation.setOptions({
        title: t('requests'),
        headerRight: () => (
          <View
            style={{
              display: 'flex',
              flexDirection: 'row'
            }}
          >
            <Pressable
              onPress={() => navigation.navigate('Notifications')}
              style={{ position: 'relative' }}
            >
              <IconButton icon={'bell-outline'} />
              <Badge
                style={{
                  position: 'absolute',
                  bottom: 0,
                  right: 0,
                  backgroundColor: theme.colors.error
                }}
                visible={
                  notifications.content.filter(
                    (notification) => !notification.seen
                  ).length > 0
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
              onPress={() => {
                navigation.navigate('Settings');
              }}
            >
              <IconButton icon="cog-outline" />
            </Pressable>
          </View>
        )
      });
  }, []);

  useEffect(() => {
    if (user.role.code === 'REQUESTER')
      dispatch(getNotifications(notificationsCriteria));
  }, []);

  const onRefresh = () => {
    setCriteria(getCriteriaFromFilterFields([]));
  };

  const getStatusMeta = (request: Request): [string, string] => {
    if (request.workOrder) {
      // @ts-ignore
      return [t('approved'), theme.colors.success];
    } else if (request.cancelled) {
      return [t('rejected'), theme.colors.error];
    } else return [t('pending'), theme.colors.primary];
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
  const onFilterChange = (newFilters: FilterField[]) => {
    const newCriteria = { ...criteria };
    newCriteria.filterFields = newFilters;
    setCriteria(newCriteria);
  };
  const onQueryChange = (query) => {
    onSearchQueryChange<Request>(query, criteria, setCriteria, setSearchQuery, [
      'title',
      'description'
    ]);
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
      {hasViewPermission(PermissionEntity.REQUESTS) ? (
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
                  dispatch(getMoreRequests(criteria, currentPageNum + 1));
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
            {!!requests.content.length ? (
              requests.content.map((request) => (
                <Card
                  style={{
                    padding: 5,
                    marginVertical: 5,
                    backgroundColor: 'white'
                  }}
                  key={request.id}
                  onPress={() => {
                    if (request.workOrder) {
                      navigation.push('WODetails', {
                        id: request.workOrder.id
                      });
                    } else
                      navigation.push('RequestDetails', {
                        id: request.id,
                        requestProp: request
                      });
                  }}
                >
                  <Card.Content>
                    <View
                      style={{ ...styles.row, justifyContent: 'space-between' }}
                    >
                      <View
                        style={{
                          ...styles.row,
                          justifyContent: 'space-between'
                        }}
                      >
                        <View style={{ marginRight: 10 }}>
                          <Tag
                            text={`#${request.customId}`}
                            color="white"
                            backgroundColor="#545454"
                          />
                        </View>
                        <View style={{ marginRight: 10 }}>
                          <Tag
                            text={t(request.priority)}
                            color="white"
                            backgroundColor={getPriorityColor(
                              request.priority,
                              theme
                            )}
                          />
                        </View>
                        <Tag
                          text={getStatusMeta(request)[0]}
                          color="white"
                          backgroundColor={getStatusMeta(request)[1]}
                        />
                      </View>
                    </View>
                    <Text style={{ fontWeight: 'bold', fontSize: 18 }}>
                      {request.title}
                    </Text>
                    {request.dueDate && (
                      <IconWithLabel
                        color={
                          (dayDiff(new Date(request.dueDate), new Date()) <=
                            2 ||
                            new Date() > new Date(request.dueDate)) &&
                          request.workOrder?.status !== 'COMPLETE'
                            ? theme.colors.error
                            : theme.colors.grey
                        }
                        label={getFormattedDate(request.dueDate)}
                        icon="clock-alert-outline"
                      />
                    )}
                    {request.asset && (
                      <IconWithLabel
                        label={request.asset.name}
                        icon="package-variant-closed"
                        color={theme.colors.grey}
                      />
                    )}
                    {request.location && (
                      <IconWithLabel
                        label={request.location.name}
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
          {/*{user.role.code === 'REQUESTER' && <FAB*/}
          {/*  icon='plus'*/}
          {/*  style={[styles.fab, { backgroundColor: theme.colors.primary }]}*/}
          {/*  color={'white'}*/}
          {/*  onPress={() => navigation.navigate('AddRequest')}*/}
          {/*/>}*/}
        </Fragment>
      ) : (
        <View
          style={{ backgroundColor: 'white', padding: 20, borderRadius: 10 }}
        >
          <Text variant={'titleLarge'}>{t('no_access_requests')}</Text>
        </View>
      )}
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
  },
  fab: {
    position: 'absolute',
    margin: 16,
    right: 0,
    bottom: 0
  }
});
