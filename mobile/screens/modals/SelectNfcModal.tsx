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
const NfcTech = nfcModule?.NfcTech;

export default function SelectNfcModal({
  navigation,
  route
}: RootStackScreenProps<'SelectNfc'>) {
  const { onChange } = route.params;
  const { t } = useTranslation();

  async function readNdef() {
    try {
      await NfcManager.requestTechnology(NfcTech.Ndef);
      const tag = await NfcManager.getTag();
      return tag?.id || null;
    } catch (ex) {
      console.warn('NFC Error:', ex);
      throw ex;
    } finally {
      // Always clean up
      NfcManager.cancelTechnologyRequest().catch(() => {});
    }
  }

  useEffect(() => {
    if (!NfcManager || !NfcTech) {
      navigation.goBack();
      return;
    }

    let cancelled = false;

    // Initialize NFC
    NfcManager.start()
      .then(() => readNdef())
      .then((tagId) => {
        if (cancelled) return;
        if (tagId) {
          onChange(tagId);
        } else {
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
