import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { PersistGate } from 'redux-persist/integration/react';
import useCachedResources from './hooks/useCachedResources';
import useColorScheme from './hooks/useColorScheme';
import Navigation from './navigation';
import { Provider } from 'react-redux';
import { Subscription } from 'expo-modules-core';
import store, { persistor } from './store';
import { CompanySettingsProvider } from './contexts/CompanySettingsContext';
import { CustomSnackbarProvider } from './contexts/CustomSnackBarContext';
import { AuthProvider } from './contexts/AuthContext';
import FlashMessage from 'react-native-flash-message';
import { URL } from 'react-native-url-polyfill';

import Constants from 'expo-constants';

import {
  MD3LightTheme as DefaultTheme,
  Provider as PaperProvider,
  useTheme
} from 'react-native-paper';
import { useEffect, useRef, useState } from 'react';
import { Alert, Linking, LogBox } from 'react-native';
import { SheetProvider } from 'react-native-actions-sheet';
import './components/actionSheets/sheets';
import * as Notifications from 'expo-notifications';
import { getNotificationUrl } from './utils/urlPaths';
import { NotificationType } from './models/notification';
import { navigate } from './navigation/RootNavigation';
import subscriptionPlan from './slices/subscriptionPlan';
import { isNumeric } from './utils/validators';
import { customTheme } from './custom-theme';

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: false
  })
});

export default function App() {
  const isLoadingComplete = useCachedResources();
  const colorScheme = useColorScheme();
  const [notification, setNotification] =
    useState<Notifications.Notification>(null);
  const notificationListener = useRef<Subscription>();
  const responseListener = useRef<Subscription>();

  useEffect(() => {
    LogBox.ignoreLogs([
      'Warning: Async Storage has been extracted from react-native core'
    ]);

    notificationListener.current =
      Notifications.addNotificationReceivedListener((notification) => {
        setNotification(notification);
        //TODO maybe showNotification alert
      });

    responseListener.current =
      Notifications.addNotificationResponseReceivedListener((response) => {
        const data = response.notification.request.content.data;
        const type = data.type as NotificationType;
        const id = data.id as number;
        let url = getNotificationUrl(type, id);
        if (url) {
          navigate(url.route, url.params);
        }
      });

    return () => {
      Notifications.removeNotificationSubscription(
        notificationListener.current
      );
      Notifications.removeNotificationSubscription(responseListener.current);
    };
  }, []);

  useEffect(() => {
    let subscription;
    const handleDeepLink = async () => {
      // Get the initial URL when the app is launched from the deep link
      const initialUrl = await Linking.getInitialURL();
      handleUrl(initialUrl);
      // Listen to incoming deep links while the app is open
      subscription = Linking.addEventListener('url', ({ url }) =>
        handleUrl(url)
      );
    };

    const handleUrl = (url) => {
      if (url) {
        const { pathname: path } = new URL(url);
        if (path.startsWith('/app/')) {
          const arr = path.split('/');
          if (arr[2] === 'work-orders') {
            if (isNumeric(arr[3]))
              navigate('WODetails', { id: Number(arr[3]) });
            else navigate('WorkOrders', {});
          } else {
            if (arr[2] === 'requests') {
              if (isNumeric(arr[3]))
                navigate('RequestDetails', { id: Number(arr[3]) });
              else navigate('Requests', {});
            }
          }
        }
      }
    };

    handleDeepLink();

    // Clean up event listeners
    return () => {
      if (subscription) subscription.remove();
    };
  }, []);

  if (!isLoadingComplete) {
    return null;
  } else {
    return (
      <SafeAreaProvider>
        <Provider store={store}>
          <PersistGate loading={null} persistor={persistor}>
            <AuthProvider>
              <CompanySettingsProvider>
                <PaperProvider theme={customTheme}>
                  <CustomSnackbarProvider>
                    <SheetProvider>
                      <Navigation colorScheme={colorScheme} />
                      <StatusBar />
                      <FlashMessage
                        position="top"
                        statusBarHeight={Constants.statusBarHeight}
                      />
                    </SheetProvider>
                  </CustomSnackbarProvider>
                </PaperProvider>
              </CompanySettingsProvider>
            </AuthProvider>
          </PersistGate>
        </Provider>
      </SafeAreaProvider>
    );
  }
}
