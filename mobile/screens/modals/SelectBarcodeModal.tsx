import { StyleSheet, TouchableOpacity, useWindowDimensions } from 'react-native';

import { View } from '../../components/Themed';
import * as React from 'react';
import { useState } from 'react';
import { RootStackScreenProps } from '../../types';
import { useTranslation } from 'react-i18next';
import { Text } from 'react-native-paper';
import { CameraView, useCameraPermissions } from 'expo-camera';

export default function SelectBarcodeModal({
  navigation,
  route
}: RootStackScreenProps<'SelectBarcode'>) {
  const { onChange } = route.params;
  const { t } = useTranslation();
  const [scanned, setScanned] = useState<boolean>(false);
  const [permission, requestPermission] = useCameraPermissions();
  const layout = useWindowDimensions();

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

  if (!permission) {
    return null;
  }

  if (!permission.granted) {
    return (
      <View
        style={{
          backgroundColor: 'white',
          padding: 20,
          borderRadius: 10
        }}
      >
        <Text variant={'titleLarge'}>{t('no_access_to_camera')}</Text>
        <TouchableOpacity
          onPress={requestPermission}
          style={styles.permissionButton}
        >
          <Text variant="titleMedium">{t('camera')}</Text>
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
