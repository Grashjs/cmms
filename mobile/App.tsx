import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { PersistGate } from 'redux-persist/integration/react';
import useCachedResources from './hooks/useCachedResources';
import useColorScheme from './hooks/useColorScheme';
import Navigation from './navigation';
import { Provider } from 'react-redux';
import store, { persistor } from './store';
import { CompanySettingsProvider } from './contexts/CompanySettingsContext';
import { CustomSnackbarProvider } from './contexts/CustomSnackBarContext';
import { AuthProvider } from './contexts/AuthContext';
import FlashMessage from 'react-native-flash-message';
import { URL } from 'react-native-url-polyfill';
import 'text-encoding';

import Constants from 'expo-constants';

import { Provider as PaperProvider } from 'react-native-paper';
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
import { customTheme, darkTheme, getNavigationTheme } from './custom-theme';
import { RootLayout } from './components/RootLayout';
import { ReviewModal } from './components/ReviewModal';
import { Subscription } from 'expo-notifications';
import { useSelector } from './store';

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
    shouldShowBanner: true,
    shouldShowList: true
  })
});

/**
 * Rendered inside the redux <Provider> so it can read the persisted theme
 * preference, which App itself cannot reach from above the store.
 */
function ThemedApp({ colorScheme }: { colorScheme: 'light' | 'dark' }) {
  const mode = useSelector((state) => state.themeMode.mode);
  const isDark = mode === 'system' ? colorScheme === 'dark' : mode === 'dark';

  return (
    <PaperProvider theme={isDark ? darkTheme : customTheme}>
      <CustomSnackbarProvider>
        <SheetProvider>
          <RootLayout>
            <FlashMessage
              position="top"
              statusBarHeight={Constants.statusBarHeight}
            />
            <Navigation theme={getNavigationTheme(isDark)} />
            <ReviewModal />
          </RootLayout>
          <StatusBar style={isDark ? 'light' : 'dark'} />
        </SheetProvider>
      </CustomSnackbarProvider>
    </PaperProvider>
  );
}

export default function App() {
  const isLoadingComplete = useCachedResources();
  const colorScheme = useColorScheme();
  const [notification, setNotification] =
    useState<Notifications.Notification>(null);
  const notificationListener = useRef<Subscription>(undefined);
  const responseListener = useRef<Subscription>(undefined);

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

    const handleUrl = (url: string) => {
      if (url) {
        const { pathname: path } = new URL(url);
        if (path.startsWith('/app/')) {
          const arr = path.split('/');
          if (arr[2] === 'work-orders') {
            if (isNumeric(arr[3]))
              navigate('WODetails', { id: Number(arr[3]) });
            else navigate('WorkOrders', { filterFields: [] });
          } else {
            if (arr[2] === 'requests') {
              if (isNumeric(arr[3]))
                navigate('RequestDetails', { id: Number(arr[3]) });
              else navigate('Requests');
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
                <ThemedApp colorScheme={colorScheme} />
              </CompanySettingsProvider>
            </AuthProvider>
          </PersistGate>
        </Provider>
      </SafeAreaProvider>
    );
  }
}
