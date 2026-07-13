import { Alert, StyleSheet } from 'react-native';
import { View } from '../components/Themed';
import { Divider, List, useTheme } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { RootStackScreenProps } from '../types';
import { getAssetByBarcode, getAssetByNfc } from '../slices/asset';
import { useDispatch } from '../store';

export default function ScanAssetScreen({
  navigation
}: RootStackScreenProps<'ScanAsset'>) {
  const theme = useTheme();
  const { t } = useTranslation();
  const dispatch = useDispatch();
  return (
    <View style={{ flex: 1, backgroundColor: theme.colors.background }}>
      <View>
        <List.Item
          title={t('NFC')}
          onPress={() =>
            navigation.navigate('SelectNfc', {
              onChange: (nfcId) =>
                dispatch(getAssetByNfc(nfcId))
                  .then((assetId: number) =>
                    navigation.replace('AssetDetails', { id: assetId })
                  )
                  .catch((err) =>
                    Alert.alert(t('error'), t('no_asset_found_nfc'), [
                      { text: t('no'), onPress: () => navigation.goBack() },
                      {
                        text: t('yes'),
                        onPress: () => navigation.replace('AddAsset', { nfcId })
                      }
                    ])
                  )
            })
          }
        />
        <Divider />
        <List.Item
          title={t('barcode')}
          onPress={() =>
            navigation.navigate('SelectBarcode', {
              onChange: (barCode) => {
                dispatch(getAssetByBarcode(barCode))
                  .then((assetId: number) =>
                    navigation.replace('AssetDetails', { id: assetId })
                  )
                  .catch((err) =>
                    Alert.alert(t('error'), t('no_asset_found_barcode'), [
                      { text: t('no'), onPress: () => navigation.goBack() },
                      {
                        text: t('yes'),
                        onPress: () =>
                          navigation.replace('AddAsset', { barCode })
                      }
                    ])
                  );
              }
            })
          }
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({});
