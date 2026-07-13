import { StyleSheet, useWindowDimensions } from 'react-native';

import { View } from '../../components/Themed';
import * as React from 'react';
import { useEffect, useState } from 'react';
import { RootStackScreenProps } from '../../types';
import { useTranslation } from 'react-i18next';
import { Text } from 'react-native-paper';
import { BarCodeScanner } from 'expo-barcode-scanner';

export default function SelectBarcodeModal({
  navigation,
  route
}: RootStackScreenProps<'SelectBarcode'>) {
  const { onChange } = route.params;
  const { t } = useTranslation();
  const [scanned, setScanned] = useState<boolean>(false);
  const [hasPermission, setHasPermission] = useState(null);
  const layout = useWindowDimensions();

  useEffect(() => {
    const getBarCodeScannerPermissions = async () => {
      const { status } = await BarCodeScanner.requestPermissionsAsync();
      setHasPermission(status === 'granted');
    };

    getBarCodeScannerPermissions();
  }, []);

  const handleBarCodeScanned = ({
    type,
    data
  }: {
    type: string;
    data: string;
  }) => {
    if (!scanned) {
      setScanned(true);
      onChange(data);
    }
  };
  return hasPermission ? (
    <View style={styles.container}>
      <BarCodeScanner
        onBarCodeScanned={handleBarCodeScanned}
        style={{ width: layout.width, height: layout.height }}
      />
    </View>
  ) : (
    <View
      style={{
        backgroundColor: 'white',
        padding: 20,
        borderRadius: 10
      }}
    >
      <Text variant={'titleLarge'}>{t('no_access_to_camera')}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1
  }
});
