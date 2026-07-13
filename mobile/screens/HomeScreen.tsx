import {
  RefreshControl,
  ScrollView,
  StyleSheet,
  TouchableOpacity
} from 'react-native';
import { View } from '../components/Themed';
import { RootTabScreenProps } from '../types';
import { Badge, IconButton, Switch, Text, useTheme } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { ExtendedWorkOrderStatus, getStatusColor } from '../utils/overall';
import { FilterField, SearchCriteria } from '../models/page';
import useAuth from '../hooks/useAuth';
import * as React from 'react';
import { useContext, useEffect, useState } from 'react';
import { getMobileOverviewStats } from '../slices/analytics/workOrder';
import { useDispatch, useSelector } from '../store';
import { getNotifications } from '../slices/notification';
import { useNetInfo } from '@react-native-community/netinfo';
import { CustomSnackBarContext } from '../contexts/CustomSnackBarContext';
import { PermissionEntity } from '../models/role';
import { useAppTheme } from '../custom-theme';

export default function HomeScreen({ navigation }: RootTabScreenProps<'Home'>) {
  const theme = useAppTheme();
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const netInfo = useNetInfo();
  const {
    userSettings,
    fetchUserSettings,
    hasViewPermission,
    hasViewOtherPermission,
    patchUserSettings,
    user
  } = useAuth();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { notifications } = useSelector((state) => state.notifications);
  const { mobileOverview, loading } = useSelector((state) => state.woAnalytics);
  const iconButtonStyle = {
    ...styles.iconButton,
    backgroundColor: theme.colors.background
  };
  const [assignedToMe, setAssignedToMe] = useState<boolean>(
    userSettings?.statsForAssignedWorkOrders
  );
  const notificationsCriteria: SearchCriteria = {
    filterFields: [],
    pageSize: 15,
    pageNum: 0,
    direction: 'DESC'
  };
  const getTodayDates = () => {
    const date1 = new Date();
    const date2 = new Date();
    date1.setHours(0, 0, 0, 0);
    date2.setHours(24, 0, 0, 0);
    return [date1, date2];
  };

  useEffect(() => {
    fetchUserSettings();
    dispatch(getNotifications(notificationsCriteria));
  }, []);

  useEffect(() => {
    if (userSettings?.statsForAssignedWorkOrders !== undefined) {
      dispatch(getMobileOverviewStats(userSettings.statsForAssignedWorkOrders));
      setAssignedToMe(userSettings.statsForAssignedWorkOrders);
    }
  }, [userSettings]);

  const onRefresh = () => {
    if (userSettings)
      dispatch(getMobileOverviewStats(userSettings.statsForAssignedWorkOrders));
  };
  const stats: {
    label: ExtendedWorkOrderStatus;
    value: number;
    filterFields: FilterField[];
  }[] = [
    {
      label: 'OPEN',
      value: mobileOverview.open,
      filterFields: [
        {
          field: 'status',
          operation: 'in',
          value: '',
          values: ['OPEN'],
          enumName: 'STATUS'
        }
      ]
    },
    {
      label: 'ON_HOLD',
      value: mobileOverview.onHold,
      filterFields: [
        {
          field: 'status',
          operation: 'in',
          value: '',
          values: ['ON_HOLD'],
          enumName: 'STATUS'
        }
      ]
    },
    {
      label: 'IN_PROGRESS',
      value: mobileOverview.inProgress,
      filterFields: [
        {
          field: 'status',
          operation: 'in',
          value: '',
          values: ['IN_PROGRESS'],
          enumName: 'STATUS'
        }
      ]
    },
    {
      label: 'COMPLETE',
      value: mobileOverview.complete,
      filterFields: [
        {
          field: 'status',
          operation: 'in',
          value: '',
          values: ['COMPLETE'],
          enumName: 'STATUS'
        }
      ]
    },
    // {
    //   label: 'LATE_WO', value: 3,
    //   filterField: {
    //     field: 'dueDate',
    //     operation: 'ge',
    //     value: 'ON_HOLD'
    //   }
    // },
    {
      label: 'TODAY_WO',
      value: mobileOverview.today,
      filterFields: [
        {
          field: 'dueDate',
          operation: 'ge',
          value: getTodayDates()[0],
          enumName: 'JS_DATE'
        },
        {
          field: 'dueDate',
          operation: 'le',
          value: getTodayDates()[1],
          enumName: 'JS_DATE'
        }
      ]
    },
    {
      label: 'HIGH_WO',
      value: mobileOverview.high,
      filterFields: [
        {
          field: 'priority',
          operation: 'in',
          value: '',
          values: ['HIGH'],
          enumName: 'PRIORITY'
        }
      ]
    }
  ];
  return (
    <ScrollView
      style={{ ...styles.container, backgroundColor: theme.colors.background }}
      refreshControl={
        <RefreshControl
          refreshing={loading.mobileOverview}
          colors={[theme.colors.primary]}
          onRefresh={onRefresh}
        />
      }
    >
      <View
        style={{
          display: 'flex',
          flexDirection: 'row',
          justifyContent: 'space-between'
        }}
      >
        {hasViewPermission(PermissionEntity.ASSETS) && (
          <IconButton
            style={iconButtonStyle}
            icon={'magnify-scan'}
            onPress={() => {
              if (netInfo.isInternetReachable) {
                navigation.navigate('ScanAsset');
              } else {
                showSnackBar(t('no_internet_connection'), 'error');
              }
            }}
          />
        )}
        <IconButton
          style={iconButtonStyle}
          icon={'poll'}
          onPress={() => {
            navigation.navigate('WorkOrderStats');
          }}
        />
        <View style={{ ...iconButtonStyle, position: 'relative' }}>
          <IconButton
            icon={'bell-outline'}
            onPress={() => navigation.navigate('Notifications')}
          />
          <Badge
            style={{
              position: 'absolute',
              bottom: 0,
              right: 0,
              backgroundColor: theme.colors.error
            }}
            visible={
              notifications.content.filter((notification) => !notification.seen)
                .length > 0
            }
          >
            {
              notifications.content.filter((notification) => !notification.seen)
                .length
            }
          </Badge>
        </View>
        {hasViewPermission(PermissionEntity.ASSETS) && (
          <IconButton
            style={iconButtonStyle}
            icon={'package-variant-closed'}
            onPress={() => navigation.navigate('Assets')}
          />
        )}
      </View>
      {hasViewOtherPermission(PermissionEntity.WORK_ORDERS) && (
        <View
          style={{
            marginHorizontal: 10,
            marginTop: 20,
            paddingHorizontal: 10,
            paddingVertical: 5,
            flexDirection: 'row',
            justifyContent: 'space-between',
            borderRadius: 10,
            alignItems: 'center'
          }}
        >
          <Text style={{ color: theme.colors.grey }}>
            {t('only_assigned_to_me')}
          </Text>
          <Switch
            value={assignedToMe}
            onValueChange={(value) => {
              patchUserSettings({
                ...userSettings,
                statsForAssignedWorkOrders: value
              });
              setAssignedToMe(value);
            }}
          />
        </View>
      )}
      {stats.map((stat) => (
        <View
          key={stat.label}
          style={{
            marginHorizontal: 10,
            marginTop: 20,
            paddingHorizontal: 10,
            paddingVertical: 5,
            borderRadius: 10
          }}
        >
          <TouchableOpacity
            style={{
              display: 'flex',
              flexDirection: 'row',
              justifyContent: 'space-between',
              alignItems: 'center',
              width: '100%'
            }}
            onPress={() => {
              if (userSettings) {
                const filterFields = stat.filterFields;
                if (userSettings.statsForAssignedWorkOrders) {
                  filterFields.push({
                    field: 'assignedToUser',
                    operation: 'eq',
                    value: user.id
                  });
                }
                navigation.navigate('WorkOrders', {
                  filterFields,
                  fromHome: true
                });
              }
            }}
          >
            <View
              style={{
                display: 'flex',
                flexDirection: 'row',
                justifyContent: 'flex-start',
                alignItems: 'center'
              }}
            >
              <View
                style={{
                  width: 2,
                  height: 30,
                  backgroundColor: getStatusColor(stat.label, theme)
                }}
              >
                {null}
              </View>
              <Text
                variant={'titleSmall'}
                style={{ fontWeight: 'bold', marginLeft: 10 }}
              >
                {t(stat.label)}
              </Text>
            </View>
            <View
              style={{
                display: 'flex',
                flexDirection: 'row',
                alignItems: 'center',
                justifyContent: 'flex-start'
              }}
            >
              <Text style={{ color: theme.colors.grey }}>{stat.value}</Text>
              <IconButton
                icon={'chevron-double-right'}
                iconColor={theme.colors.grey}
              />
            </View>
          </TouchableOpacity>
        </View>
      ))}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  iconButton: { width: 50, height: 50, borderRadius: 25 }
});
