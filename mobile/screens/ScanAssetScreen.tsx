import { Alert, Platform, StyleSheet } from 'react-native';
import { View } from '../components/Themed';
import { Divider, List, useTheme } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { RootStackScreenProps } from '../types';
import { getAssetByBarcode, getAssetByNfc } from '../slices/asset';
import { useDispatch } from '../store';
import { useContext, useEffect } from 'react';
import { getLicenseValidity } from '../slices/license';
import { useLicenseEntitlement } from '../hooks/useLicenseEntitlement';
import { CustomSnackBarContext } from '../contexts/CustomSnackBarContext';

export default function ScanAssetScreen({
  navigation
}: RootStackScreenProps<'ScanAsset'>) {
  const theme = useTheme();
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const hasBarcodeNfcEntitlement = useLicenseEntitlement('NFC_BARCODE');
  // NFC scanning must remain disabled on iOS; keep barcode only there.
  const isNfcEnabled = Platform.select({ ios: false, default: true });
  const { showSnackBar } = useContext(CustomSnackBarContext);

  const showLicenseError = () => {
    showSnackBar(t('you_need_a_license'), 'error');
  };
  useEffect(() => {
    dispatch(getLicenseValidity());
  }, []);

  return (
    <View style={{ flex: 1, backgroundColor: theme.colors.background }}>
      <View>
        {isNfcEnabled && (
          <>
            <List.Item
              title={t('NFC')}
              onPress={() => {
                if (hasBarcodeNfcEntitlement)
                  navigation.navigate('SelectNfc', {
                    onChange: (nfcId) =>
                      dispatch(getAssetByNfc(nfcId))
                        .then((assetId: number) =>
                          navigation.replace('AssetDetails', { id: assetId })
                        )
                        .catch((err) =>
                          Alert.alert(t('error'), t('no_asset_found_nfc'), [
                            {
                              text: t('no'),
                              onPress: () => navigation.goBack()
                            },
                            {
                              text: t('yes'),
                              onPress: () =>
                                navigation.replace('AddAsset', { nfcId })
                            }
                          ])
                        )
                  });
                else showLicenseError();
              }}
            />
            <Divider />
          </>
        )}
        <List.Item
          title={t('barcode')}
          onPress={() => {
            if (hasBarcodeNfcEntitlement)
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
              });
            else showLicenseError();
          }}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({});
