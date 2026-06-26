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
  RadioButton,
  Text,
  useTheme
} from 'react-native-paper';
import useAuth from '../hooks/useAuth';
import { useThemeMode } from '../theme';
import { useTranslation } from 'react-i18next';
import { getUserInitials } from '../utils/displayers';
import * as React from 'react';
import { useContext, useEffect, useState } from 'react';
import { RootStackScreenProps } from '../types';
import Constants from 'expo-constants';
import * as Updates from 'expo-updates';
import { showMessage } from 'react-native-flash-message';
import { CustomSnackBarContext } from '../contexts/CustomSnackBarContext';
import tr from '../i18n/translations/tr';

export default function SettingsScreen({
                                         navigation
                                       }: RootStackScreenProps<'Settings'>) {
  const theme = useTheme();
  const { themeMode, setThemeMode } = useThemeMode();
  const { user, switchAccount, logout } = useAuth();
  const [switchingAccount, setSwitchingAccount] = useState<boolean>(false);
  const { t } = useTranslation();
  const [versionPressCount, setVersionPressCount] = useState<number>(0);
  const [openLogout, setOpenLogout] = useState<boolean>(false);
  const [openDevInfo, setOpenDevInfo] = useState<boolean>(false);
  const [openThemeDialog, setOpenThemeDialog] = useState<boolean>(false);
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [devMode, setDevMode] = useState<boolean>(false);
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
            <Text variant='bodyMedium'>{t('confirm_logout')}</Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setOpenLogout(false)}>{t('cancel')}</Button>
            <Button onPress={logout}>{t('Sign out')}</Button>
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
            <Text variant='titleMedium'>{t('Build ID')}</Text>
            <Text variant='bodyMedium'>{Updates.updateId}</Text>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setOpenDevInfo(false)}>{t('cancel')}</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    );
  };

  const renderThemeDialog = () => {
    return (
      <Portal theme={theme}>
        <Dialog visible={openThemeDialog} onDismiss={() => setOpenThemeDialog(false)}>
          <Dialog.Title>{t('theme_selection')}</Dialog.Title>
          <Dialog.Content>
            <RadioButton.Group
              onValueChange={(value) => setThemeMode(value as 'system' | 'light' | 'dark')}
              value={themeMode}
            >
              <RadioButton.Item
                label={t('theme_system')}
                value="system"
              />
              <RadioButton.Item
                label={t('theme_light')}
                value="light"
              />
              <RadioButton.Item
                label={t('theme_dark')}
                value="dark"
              />
            </RadioButton.Group>
          </Dialog.Content>
          <Dialog.Actions>
            <Button onPress={() => setOpenThemeDialog(false)}>{t('close')}</Button>
          </Dialog.Actions>
        </Dialog>
      </Portal>
    );
  };

  return (
    <View style={{ flex: 1, backgroundColor: theme.colors.background }}>
      {renderConfirmLogout()}
      {renderDevInfo()}
      {renderThemeDialog()}
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
        <List.Item
          style={{ paddingHorizontal: 20 }}
          left={(props) => <IconButton icon={'theme-light-dark'} />}
          title={t('theme_selection')}
          description={
            themeMode === 'system'
              ? t('theme_system')
              : themeMode === 'light'
              ? t('theme_light')
              : t('theme_dark')
          }
          onPress={() => setOpenThemeDialog(true)}
        />
        {user.parentSuperAccount && <List.Item
          style={{ paddingHorizontal: 20 }}
          left={(props) => <IconButton icon={'swap-horizontal'} />}
          title={t('switch_to_super_user')}
          right={(props) => switchingAccount && <ActivityIndicator />}
          onPress={() => {
            setSwitchingAccount(true);
            switchAccount(user.parentSuperAccount.superUserId)
              .finally(() => setSwitchingAccount(false));
          }}
        />}
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
              setVersionPressCount(state => state + 1);
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
