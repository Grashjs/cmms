import {
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  View
} from 'react-native';
import { RootTabScreenProps } from '../types';
import { Badge, IconButton, Switch, Text } from 'react-native-paper';
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
import { bucketFilters, getMyDay, MyDayBucket } from '../slices/myDay';
import WorkOrderCard from './workOrders/components/WorkOrderCard';
import { EmptyState, ListSkeleton } from '../components/ui';
import { fontWeight, radius, spacing, touchTarget } from '../theme/tokens';
import { raisedSurface } from '../theme/surface';

const greetingKey = () => {
  const hour = new Date().getHours();
  if (hour < 12) return 'good_morning';
  if (hour < 18) return 'good_afternoon';
  return 'good_evening';
};

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
  const { buckets, counts, loading: loadingFeed, error } = useSelector(
    (state) => state.myDay
  );
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

  useEffect(() => {
    fetchUserSettings();
    dispatch(getNotifications(notificationsCriteria));
  }, []);

  useEffect(() => {
    if (userSettings?.statsForAssignedWorkOrders !== undefined) {
      const onlyMine = userSettings.statsForAssignedWorkOrders;
      dispatch(getMobileOverviewStats(onlyMine));
      dispatch(getMyDay(onlyMine ? user.id : undefined));
      setAssignedToMe(onlyMine);
    }
  }, [userSettings]);

  const onRefresh = () => {
    if (userSettings) {
      const onlyMine = userSettings.statsForAssignedWorkOrders;
      dispatch(getMobileOverviewStats(onlyMine));
      dispatch(getMyDay(onlyMine ? user.id : undefined));
    }
  };

  const openBucket = (bucket: MyDayBucket) => {
    navigation.navigate('WorkOrders', {
      filterFields: bucketFilters(bucket, assignedToMe ? user.id : undefined),
      fromHome: true
    });
  };

  const openStat = (filterFields: FilterField[]) => {
    navigation.navigate('WorkOrders', {
      // Copied rather than appended in place: these arrays are rebuilt each
      // render, so mutating one would compound across presses.
      filterFields: assignedToMe
        ? [
            ...filterFields,
            { field: 'assignedToUser', operation: 'eq', value: user.id }
          ]
        : filterFields,
      fromHome: true
    });
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
    }
  ];

  const feed: { bucket: MyDayBucket; title: string; accent: string }[] = [
    { bucket: 'overdue', title: t('overdue'), accent: theme.colors.error },
    { bucket: 'today', title: t('due_today'), accent: theme.colors.primary },
    {
      bucket: 'inProgress',
      title: t('IN_PROGRESS'),
      accent: theme.colors.success
    }
  ];

  const isFeedEmpty = feed.every(({ bucket }) => !buckets[bucket].length);

  return (
    <ScrollView
      contentContainerStyle={{ paddingBottom: 100 }}
      style={{ ...styles.container, backgroundColor: theme.colors.background }}
      refreshControl={
        <RefreshControl
          refreshing={loading.mobileOverview || loadingFeed}
          colors={[theme.colors.primary]}
          tintColor={theme.colors.primary}
          onRefresh={onRefresh}
        />
      }
    >
      <View style={styles.quickActions}>
        <IconButton
          style={iconButtonStyle}
          icon={'magnify'}
          accessibilityLabel={t('search_everything')}
          onPress={() => navigation.navigate('GlobalSearch')}
        />
        {hasViewPermission(PermissionEntity.ASSETS) && (
          <IconButton
            style={iconButtonStyle}
            icon={'magnify-scan'}
            accessibilityLabel={t('scan_asset')}
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
          accessibilityLabel={t('statistics')}
          onPress={() => navigation.navigate('WorkOrderStats')}
        />
        <View style={{ ...iconButtonStyle, position: 'relative' }}>
          <IconButton
            icon={'bell-outline'}
            accessibilityLabel={t('notifications')}
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
            accessibilityLabel={t('assets')}
            onPress={() => navigation.navigate('Assets')}
          />
        )}
      </View>

      <View style={styles.greeting}>
        <Text variant="headlineSmall" style={{ color: theme.colors.text }}>
          {t(greetingKey(), { name: user.firstName })}
        </Text>
        <Text variant="bodyMedium" style={{ color: theme.colors.grey }}>
          {new Date().toLocaleDateString(undefined, {
            weekday: 'long',
            month: 'long',
            day: 'numeric'
          })}
        </Text>
      </View>

      {hasViewOtherPermission(PermissionEntity.WORK_ORDERS) && (
        <View style={styles.toggleRow}>
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

      {/* Counts stay available as shortcuts, but compressed into one row so
          that the actual work occupies the screen instead of six tallies. */}
      <View style={styles.statRow}>
        {stats.map((stat) => (
          <Pressable
            key={stat.label}
            accessibilityRole="button"
            accessibilityLabel={`${t(stat.label)}, ${stat.value}`}
            onPress={() => openStat(stat.filterFields)}
            style={[
              styles.statPill,
              raisedSurface(theme),
              {
                borderLeftWidth: 3,
                borderLeftColor: getStatusColor(stat.label, theme)
              }
            ]}
          >
            <Text
              variant="titleMedium"
              style={{ color: theme.colors.text, fontWeight: fontWeight.bold }}
            >
              {stat.value}
            </Text>
            <Text
              variant="bodySmall"
              numberOfLines={2}
              style={{ color: theme.colors.grey }}
            >
              {t(stat.label)}
            </Text>
          </Pressable>
        ))}
      </View>

      {loadingFeed && isFeedEmpty ? (
        <ListSkeleton count={3} />
      ) : error ? (
        <EmptyState
          variant="error"
          title={t('my_day_load_failed')}
          description={t('my_day_load_failed_description')}
          action={{ label: t('retry'), onPress: onRefresh }}
        />
      ) : isFeedEmpty ? (
        <EmptyState
          icon="check-circle-outline"
          title={t('my_day_all_clear')}
          description={t('my_day_all_clear_description')}
        />
      ) : (
        feed.map(({ bucket, title, accent }) =>
          buckets[bucket].length ? (
            <View key={bucket} style={styles.section}>
              <View style={styles.sectionHeader}>
                <View style={[styles.accent, { backgroundColor: accent }]} />
                <Text
                  variant="titleMedium"
                  style={{
                    color: theme.colors.text,
                    fontWeight: fontWeight.semibold
                  }}
                >
                  {title}
                </Text>
                <Text variant="bodyMedium" style={{ color: theme.colors.grey }}>
                  {counts[bucket]}
                </Text>
                <View style={{ flex: 1 }} />
                {counts[bucket] > buckets[bucket].length && (
                  <Pressable
                    accessibilityRole="button"
                    onPress={() => openBucket(bucket)}
                    hitSlop={8}
                  >
                    <Text
                      variant="bodyMedium"
                      style={{ color: theme.colors.primary }}
                    >
                      {t('see_all')}
                    </Text>
                  </Pressable>
                )}
              </View>
              {buckets[bucket].map((workOrder) => (
                <WorkOrderCard
                  key={workOrder.id}
                  workOrder={workOrder}
                  onPress={() =>
                    navigation.push('WODetails', {
                      id: workOrder.id,
                      workOrderProp: workOrder
                    })
                  }
                />
              ))}
            </View>
          ) : null
        )
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  iconButton: { width: 50, height: 50, borderRadius: radius.pill },
  quickActions: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: spacing.sm
  },
  greeting: {
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.sm,
    paddingBottom: spacing.md
  },
  toggleRow: {
    marginHorizontal: spacing.lg,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    minHeight: touchTarget.min
  },
  statRow: {
    flexDirection: 'row',
    paddingHorizontal: spacing.lg,
    gap: spacing.sm,
    marginBottom: spacing.lg
  },
  statPill: {
    flex: 1,
    borderRadius: radius.md,
    paddingVertical: spacing.sm,
    paddingHorizontal: spacing.sm,
    minHeight: touchTarget.min,
    justifyContent: 'center'
  },
  section: {
    marginBottom: spacing.lg
  },
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    marginHorizontal: spacing.lg,
    marginBottom: spacing.sm,
    minHeight: touchTarget.min
  },
  accent: {
    width: 4,
    height: 20,
    borderRadius: radius.sm
  }
});
