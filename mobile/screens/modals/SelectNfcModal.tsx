import { Alert, Platform, StyleSheet } from 'react-native';

import { View } from '../../components/Themed';
import * as React from 'react';
import { useEffect } from 'react';
import { RootStackScreenProps } from '../../types';
import { useTranslation } from 'react-i18next';
import { ActivityIndicator, Text } from 'react-native-paper';

type NfcModule = typeof import('react-native-nfc-manager');

const nfcModule: NfcModule | null =
  Platform.OS === 'ios'
    ? null
    : (require('react-native-nfc-manager') as NfcModule);

const NfcManager = nfcModule?.default;
const NfcEvents = nfcModule?.NfcEvents;

export default function SelectNfcModal({
  navigation,
  route
}: RootStackScreenProps<'SelectNfc'>) {
  const { onChange } = route.params;
  const { t } = useTranslation();

  useEffect(() => {
    if (!NfcManager || !NfcEvents) {
      navigation.goBack();
      return;
    }

    const manager = NfcManager;
    const events = NfcEvents;

    const initNfc = async () => {
      await manager.start();
    };

    const readNdef = () => {
      const cleanUp = () => {
        manager.setEventListener(events.DiscoverTag, null);
        manager.setEventListener(events.SessionClosed, null);
      };

      return new Promise<string>((resolve) => {
        let tagFound = null;

        manager.setEventListener(events.DiscoverTag, (tag) => {
          tagFound = tag;
          resolve(tagFound);
          manager.setAlertMessageIOS('NDEF tag found');
          manager.unregisterTagEvent().catch(() => 0);
        });

        manager.setEventListener(events.SessionClosed, () => {
          cleanUp();
          if (!tagFound) {
            resolve(null);
          }
        });

        manager.registerTagEvent();
      });
    };

    initNfc()
      .then(() =>
        readNdef().then((tag) => {
          if (tag) onChange(tag);
          else
            Alert.alert(t('error'), t('tag_not_found'), [
              { text: 'Ok', onPress: () => navigation.goBack() }
            ]);
        })
      )
      .catch((error) =>
        Alert.alert(t('error'), t(error.message), [
          { text: 'Ok', onPress: () => navigation.goBack() }
        ])
      );
  }, [NfcManager, NfcEvents, navigation, onChange, t]);

  return (
    <View style={styles.container}>
      <Text style={{ marginBottom: 20 }} variant={'titleLarge'}>
        {t('scanning')}
      </Text>
      <ActivityIndicator size={'large'} />
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
