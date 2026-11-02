import { Alert, Platform, StyleSheet } from 'react-native';
import { View } from '../components/Themed';
import { Divider, List, useTheme } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { RootStackScreenProps } from '../types';
import { getAssetByBarcode, getAssetByNfc } from '../slices/asset';
import { useDispatch } from '../store';
import { useContext, useEffect, useRef } from 'react';
import { getLicenseValidity } from '../slices/license';
import { useLicenseEntitlement } from '../hooks/useLicenseEntitlement';
import { CustomSnackBarContext } from '../contexts/CustomSnackBarContext';
import { AssetDTO, AssetMiniDTO } from '../models/asset';
import useAuth from '../hooks/useAuth';
import { PermissionEntity } from '../models/role';
import { useOffline } from '../offline/OfflineProvider';
import {
  assertSessionGeneration,
  sessionGeneration
} from '../offline/sessionBridge';

export default function ScanAssetScreen({
  navigation,
  route
}: RootStackScreenProps<'ScanAsset'>) {
  const theme = useTheme();
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const offline = useOffline();
  const currentOffline = useRef(offline);
  currentOffline.current = offline;
  const localOnly = offline.enabled && !offline.networkAvailable;
  const { onChange } = route.params || {};
  const { hasCreatePermission } = useAuth();
  const hasBarcodeNfcEntitlement = useLicenseEntitlement('NFC_BARCODE');
  // NFC scanning must remain disabled on iOS; keep barcode only there.
  const isNfcEnabled = Platform.OS === 'android';
  const { showSnackBar } = useContext(CustomSnackBarContext);

  const showLicenseError = () => {
    showSnackBar(t('you_need_a_license'), 'error');
  };

  const handleAssetFound = (asset: AssetMiniDTO) => {
    if (onChange) {
      onChange(asset);
      navigation.goBack();
    } else {
      navigation.replace('AssetDetails', { id: asset.id });
    }
  };

  const getCreateAssetParams = (identifier: {
    nfcId?: string;
    barCode?: string;
  }) => ({
    ...identifier,
    openedFromSelector: !!onChange,
    onSuccess: onChange
  });

  const scanLocally = async (kind: 'barcode' | 'nfc') => {
    const repository = offline.repository;
    const generation = sessionGeneration();
    if (onChange || !offline.readable || !repository) {
      showSnackBar(t('offline_connected_required'), 'error');
      return;
    }
    try {
      const canScan = await repository.canScan();
      assertSessionGeneration(generation);
      if (!canScan || !currentOffline.current.readable) {
        showSnackBar(t('offline_scan_unavailable'), 'error');
        return;
      }
      const onScan = async (value: string) => {
        try {
          assertSessionGeneration(generation);
          if (
            !currentOffline.current.readable ||
            currentOffline.current.repository !== repository
          )
            return;
          const asset = await repository.findAssetByScan(value, kind);
          assertSessionGeneration(generation);
          if (
            !currentOffline.current.readable ||
            currentOffline.current.repository !== repository
          )
            return;
          if (asset) navigation.replace('AssetDetails', { id: asset.id });
          else showSnackBar(t('offline_asset_not_available'), 'error');
        } catch (error) {
          if (generation !== sessionGeneration()) return;
          const code = (error as { code?: string })?.code;
          showSnackBar(
            t(
              code === 'SCAN_AMBIGUOUS'
                ? 'offline_scan_ambiguous'
                : code === 'SCAN_FORBIDDEN'
                ? 'offline_scan_unavailable'
                : 'offline_asset_not_available'
            ),
            'error'
          );
        }
      };
      if (kind === 'nfc')
        navigation.navigate('SelectNfc', { onChange: onScan });
      else navigation.navigate('SelectBarcode', { onChange: onScan });
    } catch {
      if (generation === sessionGeneration())
        showSnackBar(t('offline_scan_unavailable'), 'error');
    }
  };

  useEffect(() => {
    if (!localOnly) dispatch(getLicenseValidity());
  }, [localOnly]);

  return (
    <View style={{ flex: 1, backgroundColor: theme.colors.background }}>
      <View>
        {isNfcEnabled && (
          <>
            <List.Item
              title={t('NFC')}
              onPress={() => {
                if (localOnly) {
                  void scanLocally('nfc');
                  return;
                }
                if (hasBarcodeNfcEntitlement)
                  navigation.navigate('SelectNfc', {
                    onChange: (nfcId) =>
                      dispatch(getAssetByNfc(nfcId))
                        .then((asset: AssetMiniDTO) => handleAssetFound(asset))
                        .catch((err) =>
                          currentOffline.current.enabled &&
                          !currentOffline.current.networkAvailable
                            ? showSnackBar(
                                t('offline_connected_required'),
                                'error'
                              )
                            : hasCreatePermission(PermissionEntity.WORK_ORDERS)
                            ? Alert.alert(t('error'), t('no_asset_found_nfc'), [
                                {
                                  text: t('no'),
                                  onPress: () => navigation.goBack()
                                },
                                {
                                  text: t('yes'),
                                  onPress: () =>
                                    navigation.replace(
                                      'AddAsset',
                                      getCreateAssetParams({ nfcId })
                                    )
                                }
                              ])
                            : Alert.alert(t('error'), t('asset_not_found'))
                        )
                  });
                else showLicenseError();
              }}
            />
            <Divider />
          </>
        )}
        <List.Item
          title={t('barcode') + '/QR code'}
          onPress={() => {
            if (localOnly) {
              void scanLocally('barcode');
              return;
            }
            if (hasBarcodeNfcEntitlement)
              navigation.navigate('SelectBarcode', {
                onChange: (barCode) => {
                  dispatch(getAssetByBarcode(barCode))
                    .then((asset: AssetMiniDTO) => handleAssetFound(asset))
                    .catch((err) =>
                      currentOffline.current.enabled &&
                      !currentOffline.current.networkAvailable
                        ? showSnackBar(t('offline_connected_required'), 'error')
                        : hasCreatePermission(PermissionEntity.WORK_ORDERS)
                        ? Alert.alert(t('error'), t('no_asset_found_barcode'), [
                            {
                              text: t('no'),
                              onPress: () => navigation.goBack()
                            },
                            {
                              text: t('yes'),
                              onPress: () =>
                                navigation.replace(
                                  'AddAsset',
                                  getCreateAssetParams({ barCode })
                                )
                            }
                          ])
                        : Alert.alert(t('error'), t('asset_not_found'))
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
