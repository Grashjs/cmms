import {
  Linking,
  StyleSheet,
  TouchableOpacity,
  useWindowDimensions
} from 'react-native';
import { useTheme } from 'react-native-paper';

import { View } from '../../components/Themed';
import * as React from 'react';
import { useEffect, useState } from 'react';
import { RootStackScreenProps } from '../../types';
import { useTranslation } from 'react-i18next';
import { Text } from 'react-native-paper';
import { CameraView } from 'expo-camera';
import { ensureScannerCameraPermission } from '../../utils/mediaPermissions';

export default function SelectBarcodeModal({
  navigation,
  route
}: RootStackScreenProps<'SelectBarcode'>) {
  const { onChange } = route.params;
  const { t } = useTranslation();
  const theme = useTheme();
  const [scanned, setScanned] = useState<boolean>(false);
  const [hasPermission, setHasPermission] = useState<boolean | null>(null);
  const layout = useWindowDimensions();

  useEffect(() => {
    let mounted = true;

    const requestPermission = async () => {
      console.warn('[SelectBarcodeModal] Tap/screen open -> request camera permission');
      const granted = await ensureScannerCameraPermission('SelectBarcodeModal');
      if (mounted) {
        setHasPermission(granted);
      }
    };

    requestPermission();

    return () => {
      mounted = false;
    };
  }, []);

  const handleBarCodeScanned = ({
    type,
    data
  }: {
    type: string;
    data: string;
  }) => {
    if (!scanned) {
      console.warn('[SelectBarcodeModal] Barcode scanned', JSON.stringify({ type }));
      setScanned(true);
      navigation.goBack();
      onChange(data);
    }
  };

  if (!hasPermission) {
    return (
      <View
        style={{
          backgroundColor: theme.colors.surface,
          padding: 20,
          borderRadius: 10
        }}
      >
        <Text variant={'titleLarge'}>{t('no_access_to_camera')}</Text>
        <TouchableOpacity
          onPress={async () => {
            console.warn('[SelectBarcodeModal] Retry permission');
            const granted = await ensureScannerCameraPermission('SelectBarcodeModal');
            setHasPermission(granted);
          }}
          style={styles.permissionButton}
        >
          <Text variant="titleMedium">{t('camera')}</Text>
        </TouchableOpacity>
        <TouchableOpacity
          onPress={() => Linking.openSettings()}
          style={styles.permissionButton}
        >
          <Text variant="titleMedium">{t('open_settings')}</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <CameraView
        onBarcodeScanned={handleBarCodeScanned}
        style={{ width: layout.width, height: layout.height }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  },
  permissionButton: {
    alignSelf: 'flex-start',
    marginTop: 16
  }
});
