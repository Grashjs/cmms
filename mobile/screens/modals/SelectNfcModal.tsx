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

  // Pre-step, call this before any NFC operations
  async function initNfc() {
    await NfcManager.start();
  }

  function readNdef() {
    const cleanUp = () => {
      NfcManager.setEventListener(NfcEvents.DiscoverTag, null);
      NfcManager.setEventListener(NfcEvents.SessionClosed, null);
    };

    return new Promise<string>((resolve) => {
      let tagFound = null;

      NfcManager.setEventListener(NfcEvents.DiscoverTag, (tag) => {
        tagFound = tag;
        resolve(tagFound);
        NfcManager.setAlertMessageIOS('NDEF tag found');
        NfcManager.unregisterTagEvent().catch(() => 0);
      });

      NfcManager.setEventListener(NfcEvents.SessionClosed, () => {
        cleanUp();
        if (!tagFound) {
          resolve(null);
        }
      });

      NfcManager.registerTagEvent();
    });
  }

  useEffect(() => {
    if (!NfcManager || !NfcEvents) {
      navigation.goBack();
      return;
    }

    let cancelled = false;

    initNfc()
      .then(() => readNdef())
      .then((tag) => {
        if (cancelled) return;
        if (tag) onChange?.(tag);
        else {
          Alert.alert(t('error'), t('tag_not_found'), [
            { text: 'Ok', onPress: () => navigation.goBack() }
          ]);
        }
      })
      .catch((error) => {
        if (cancelled) return;
        Alert.alert(t('error'), t(error.message), [
          { text: 'Ok', onPress: () => navigation.goBack() }
        ]);
      });

    return () => {
      cancelled = true;
      NfcManager?.cancelTechnologyRequest().catch(() => {});
    };
  }, []);

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
