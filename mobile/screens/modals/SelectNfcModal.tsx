import { Alert, StyleSheet } from 'react-native';

import { View } from '../../components/Themed';
import * as React from 'react';
import { useEffect } from 'react';
import { RootStackScreenProps } from '../../types';
import { useTranslation } from 'react-i18next';
import NfcManager, { NfcEvents } from 'react-native-nfc-manager';
import { ActivityIndicator, Text } from 'react-native-paper';

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
