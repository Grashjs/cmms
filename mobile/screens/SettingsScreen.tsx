import { StyleSheet } from 'react-native';
import { View } from '../components/Themed';
import {
  ActivityIndicator,
  Avatar,
  Button,
  Dialog,
  IconButton,
  List,
  Portal,
  Switch,
  Text,
  useTheme
} from 'react-native-paper';
import useAuth from '../hooks/useAuth';
import { useTranslation } from 'react-i18next';
import { getUserInitials } from '../utils/displayers';
import * as React from 'react';
import { useContext, useEffect, useState } from 'react';
import { RootStackScreenProps } from '../types';
import Constants from 'expo-constants';
import * as Updates from 'expo-updates';
import { CustomSnackBarContext } from '../contexts/CustomSnackBarContext';
import { getErrorMessage } from '../utils/api';
import { useOffline } from '../offline/OfflineProvider';
import { offlineErrorKey } from '../offline/errors';

export default function SettingsScreen({
  navigation
}: RootStackScreenProps<'Settings'>) {
  const theme = useTheme();
  const { user, switchAccount, logout } = useAuth();
  const offline = useOffline();
  const [switchingAccount, setSwitchingAccount] = useState<boolean>(false);
  const [updatingOffline, setUpdatingOffline] = useState(false);
  const { t } = useTranslation();
  const [versionPressCount, setVersionPressCount] = useState<number>(0);
  const [openLogout, setOpenLogout] = useState<boolean>(false);
  const [openDevInfo, setOpenDevInfo] = useState<boolean>(false);
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [devMode, setDevMode] = useState<boolean>(false);
  const changeOffline = async (next: boolean) => {
    if (updatingOffline || offline.preferenceChanging) return;
    setUpdatingOffline(true);
    try {
      await offline.setPreference(next);
    } catch (failure) {
      const code = (failure as { code?: string })?.code;
      const errorKey = offlineErrorKey(failure);
      showSnackBar(
        t(
          code === 'WORK_PENDING'
            ? 'offline_preference_pending'
            : code === 'OFFLINE_NOT_AUTHORIZED' || code === 'OFFLINE_DISABLED'
            ? 'offline_preference_denied'
            : next &&
              (code === 'INVALID_AUTHORIZATION' ||
                code === 'STORAGE_FAILED' ||
                code === 'STORAGE_UNAVAILABLE' ||
                errorKey === 'offline_action_failed')
            ? 'offline_preference_activation_failed'
            : errorKey
        ),
        'error'
      );
    } finally {
      setUpdatingOffline(false);
    }
  };
  useEffect(() => {
    if (versionPressCount > 2 && versionPressCount < 6) {
      showSnackBar(`Dev mode in ${6 - versionPressCount}`, 'info');
    } else if (versionPressCount === 6) {
      setOpenDevInfo(true);
      setDevMode(true);
      setVersionPressCount(0);
    }
  }, [versionPressCount]);
  const renderConfirmLogout = () => {
    return (
      <Portal theme={theme}>
        <Dialog visible={openLogout} onDismiss={() => setOpenLogout(false)}>
          <Dialog.Title>{t('confirmation')}</Dialog.Title>
          <Dialog.Content>
            <Text variant="bodyMedium">{t('confirm_logout')}</Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setOpenLogout(false)}>{t('cancel')}</Button>
            <Button
              onPress={() =>
                logout().catch((error) =>
                  showSnackBar(getErrorMessage(error), 'error')
                )
              }
            >
              {t('Sign out')}
            </Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    );
  };
  const renderDevInfo = () => {
    return (
      <Portal theme={theme}>
        <Dialog visible={openDevInfo} onDismiss={() => setOpenDevInfo(false)}>
          <Dialog.Title>{t('Dev Info')}</Dialog.Title>
          <Dialog.Content>
            <Text variant="titleMedium">{t('Build ID')}</Text>
            <Text variant="bodyMedium">{Updates.updateId}</Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setOpenDevInfo(false)}>{t('cancel')}</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    );
  };
  return (
    <View style={{ flex: 1, backgroundColor: theme.colors.background }}>
      {renderConfirmLogout()}
      {renderDevInfo()}
      <View>
        <List.Item
          style={{ paddingHorizontal: 20 }}
          left={(props) =>
            user.image ? (
              <Avatar.Image source={{ uri: user.image.url }} />
            ) : (
              <Avatar.Text size={50} label={getUserInitials(user)} />
            )
          }
          title={user.email}
          description={t('update_profile')}
          onPress={() => navigation.navigate('UserProfile')}
        />
        {offline.availableInBuild && (
          <List.Item
            style={{ paddingHorizontal: 20 }}
            title={t('offline_preference_title')}
            description={t('offline_preference_description')}
            right={() =>
              updatingOffline ||
              offline.preferenceChanging ||
              !offline.initialized ? (
                <ActivityIndicator />
              ) : (
                <Switch
                  value={offline.enabled}
                  onValueChange={(next) => void changeOffline(next)}
                />
              )
            }
          />
        )}
        {user.parentSuperAccount && (
          <List.Item
            style={{ paddingHorizontal: 20 }}
            left={(props) => <IconButton icon={'swap-horizontal'} />}
            title={t('switch_to_super_user')}
            right={(props) => switchingAccount && <ActivityIndicator />}
            onPress={() => {
              setSwitchingAccount(true);
              switchAccount(user.parentSuperAccount.superUserId)
                .catch((error) => showSnackBar(getErrorMessage(error), 'error'))
                .finally(() => setSwitchingAccount(false));
            }}
          />
        )}
        <List.Item
          style={{ paddingHorizontal: 20 }}
          left={(props) => (
            <IconButton iconColor={theme.colors.error} icon={'logout'} />
          )}
          title={t('Sign out')}
          titleStyle={{ color: theme.colors.error }}
          onPress={() => setOpenLogout(true)}
        />
        <List.Item
          onPress={() => {
            if (devMode) {
              setOpenDevInfo(true);
            } else {
              setVersionPressCount((state) => state + 1);
            }
          }}
          style={{ paddingHorizontal: 20 }}
          left={(props) => <IconButton icon={'information-outline'} />}
          title={t('Version')}
          description={Constants.expoConfig.version}
        />
      </View>
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
  separator: {
    marginVertical: 30,
    height: 1,
    width: '80%'
  }
});
