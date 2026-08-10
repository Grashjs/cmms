import { StyleSheet } from 'react-native';
import { View } from '../components/Themed';
import Notification, { NotificationType } from '../models/notification';
import {
  editNotification,
  getMoreNotifications,
  getNotifications,
  readAllNotifications
} from '../slices/notification';
import { RootStackParamList, RootStackScreenProps } from '../types';
import { useDispatch, useSelector } from '../store';
import { getNotificationUrl } from '../utils/urlPaths';
import { Text, useTheme } from 'react-native-paper';
import * as React from 'react';
import { useCallback, useContext, useEffect, useState } from 'react';
import { CompanySettingsContext } from '../contexts/CompanySettingsContext';
import { useTranslation } from 'react-i18next';
import { SearchCriteria } from '../models/page';
import { TouchableOpacity } from 'react-native';
import { IconSource } from 'react-native-paper/lib/typescript/components/Icon';
import {
  EmptyState,
  EntityListCard,
  PaginatedEntityList
} from '../components/ui';

const notificationIcons: Record<NotificationType, IconSource> = {
  ASSET: 'package-variant-closed',
  LOCATION: 'map-marker-outline',
  METER: 'gauge',
  PART: 'archive-outline',
  REQUEST: 'inbox-arrow-down-outline',
  TEAM: 'account-outline',
  WORK_ORDER: 'clipboard-text-outline',
  INFO: 'information',
  PURCHASE_ORDER: 'comma-circle-outline'
};

export default function NotificationsScreen({
  navigation
}: RootStackScreenProps<'Notifications'>) {
  const dispatch = useDispatch();
  const { notifications, loadingGet, lastPage, currentPageNum } = useSelector(
    (state) => state.notifications
  );
  const criteria: SearchCriteria = {
    filterFields: [],
    pageSize: 15,
    pageNum: 0,
    direction: 'DESC'
  };
  const theme = useTheme();
  const { t } = useTranslation();
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    if (!loadingGet) setRefreshing(false);
  }, [loadingGet]);

  useEffect(() => {
    if (notifications.content.some((notification) => !notification.seen))
      navigation.setOptions({
        headerRight: () => (
          <TouchableOpacity
            accessibilityRole="button"
            accessibilityLabel={t('mark_all_as_seen')}
            onPress={() => dispatch(readAllNotifications())}
          >
            <Text style={{ color: theme.colors.primary }} variant="titleMedium">
              {t('mark_all_as_seen')}
            </Text>
          </TouchableOpacity>
        )
      });
  }, [notifications, dispatch, navigation, t, theme.colors.primary]);

  const onReadNotification = (notification: Notification) => {
    const url: { route: keyof RootStackParamList; params: {} } | null =
      getNotificationUrl(notification.notificationType, notification.resourceId);
    if (notification.seen) {
      if (url) {
        // @ts-ignore
        navigation.navigate(url.route, url.params);
      }
      return;
    }
    dispatch(editNotification(notification.id, { seen: true })).then(() => {
      if (url) {
        // @ts-ignore
        navigation.navigate(url.route, url.params);
      }
    });
  };

  const renderItem = useCallback(
    ({ item: notification }: { item: Notification }) => (
      <EntityListCard
        title={notification.message}
        subtitle={getFormattedDate(notification.createdAt)}
        icon={notificationIcons[notification.notificationType]}
        onPress={() => onReadNotification(notification)}
        style={
          notification.seen
            ? undefined
            : { borderLeftWidth: 3, borderLeftColor: theme.colors.primary }
        }
      />
    ),
    [getFormattedDate, theme.colors.primary]
  );

  return (
    <View style={styles.container}>
      <PaginatedEntityList
        data={notifications.content}
        keyExtractor={(notification) => notification.id.toString()}
        renderItem={renderItem}
        loading={loadingGet}
        refreshing={refreshing}
        onRefresh={() => {
          setRefreshing(true);
          dispatch(getNotifications(criteria));
        }}
        onEndReached={() => {
          if (!loadingGet && !lastPage)
            dispatch(getMoreNotifications(criteria, currentPageNum + 1));
        }}
        ListEmptyComponent={
          <EmptyState
            icon="bell-outline"
            title={t('no_notification')}
            description={t('no_notification_message')}
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
