import { Pressable, RefreshControl, ScrollView, StyleSheet, TouchableOpacity } from 'react-native';
import { View } from '../components/Themed';
import { IconSource } from 'react-native-paper/src/components/Icon';
import Notification, { NotificationType } from '../models/notification';
import { editNotification, getMoreNotifications, readAllNotifications } from '../slices/notification';
import { RootStackParamList, RootStackScreenProps } from '../types';
import { useDispatch, useSelector } from '../store';
import { getNotificationUrl } from '../utils/urlPaths';
import { List, Text, useTheme } from 'react-native-paper';
import * as React from 'react';
import { useContext, useEffect } from 'react';
import { CompanySettingsContext } from '../contexts/CompanySettingsContext';
import { useTranslation } from 'react-i18next';
import { SearchCriteria } from '../models/page';

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

  useEffect(() => {
    if (notifications.content.some(notification => !notification.seen))
      navigation.setOptions({
        headerRight: () => (
          <TouchableOpacity
            onPress={() => {
              dispatch(readAllNotifications());
            }}
          >
            <Text style={{ color: theme.colors.primary }} variant='titleMedium'>{t('mark_all_as_seen')}</Text>
          </TouchableOpacity>
        )
      });
  }, [notifications]);

  const onReadNotification = (notification: Notification) => {
    let url: { route: keyof RootStackParamList; params: {} };
    const id = notification.resourceId;
    url = getNotificationUrl(notification.notificationType, id);
    if (notification.seen) {
      if (url) {
        // @ts-ignore
        navigation.navigate(url.route, url.params);
      }
    } else
      dispatch(editNotification(notification.id, { seen: true })).then(() => {
        if (url) {
          // @ts-ignore
          navigation.navigate(url.route, url.params);
        }
      });
  };
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
  return (
    <ScrollView
      style={styles.container}
      refreshControl={
        <RefreshControl
          refreshing={loadingGet}
          colors={[theme.colors.primary]}
        />
      }
      onScroll={({ nativeEvent }) => {
        if (isCloseToBottom(nativeEvent)) {
          if (!loadingGet && !lastPage)
            dispatch(getMoreNotifications(criteria, currentPageNum + 1));
        }
      }}
    >
      {Boolean(notifications.content.length) ? (
        <List.Section>
          {notifications.content.map((notification) => (
            // @ts-ignore
            <List.Item
              title={notification.message}
              titleNumberOfLines={2}
              description={getFormattedDate(notification.createdAt)}
              left={(props) => (
                <List.Icon
                  {...props}
                  icon={notificationIcons[notification.notificationType]}
                  color={notification.seen ? 'black' : theme.colors.primary}
                />
              )}
              style={{
                backgroundColor: notification.seen
                  ? 'white'
                  : theme.colors.background
              }}
              key={notification.id}
              onPress={() => onReadNotification(notification)}
            ></List.Item>
          ))}
        </List.Section>
      ) : (
        <View
          style={{
            backgroundColor: 'white',
            padding: 20,
            alignItems: 'center',
            borderRadius: 10
          }}
        >
          <Text variant={'titleMedium'} style={{ fontWeight: 'bold' }}>
            {' '}
            {t('no_notification')}
          </Text>
          <Text variant={'bodyMedium'}>{t('no_notification_message')}</Text>
        </View>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold'
  },
  separator: {
    marginVertical: 30,
    height: 1,
    width: '80%'
  }
});
