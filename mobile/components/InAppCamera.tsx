import React, { useRef, useState } from 'react';
import {
  Modal,
  StyleSheet,
  TouchableOpacity,
  View,
  SafeAreaView,
  Text
} from 'react-native';
import { CameraView, CameraType, useCameraPermissions } from 'expo-camera';
import { IconButton } from 'react-native-paper';

interface Props {
  visible: boolean;
  onCapture: (uri: string) => void;
  onClose: () => void;
}

export default function InAppCamera({ visible, onCapture, onClose }: Props) {
  const cameraRef = useRef<CameraView>(null);
  const [facing, setFacing] = useState<CameraType>('back');
  const [permission, requestPermission] = useCameraPermissions();

  React.useEffect(() => {
    if (visible && permission && !permission.granted && permission.canAskAgain) {
      requestPermission();
    }
  }, [visible, permission?.granted]);

  const handleCapture = async () => {
    if (!cameraRef.current) return;
    try {
      const photo = await cameraRef.current.takePictureAsync({ quality: 1 });
      if (photo?.uri) {
        onCapture(photo.uri);
      }
    } catch (e) {
      console.error('[InAppCamera] takePictureAsync error:', e);
    }
  };

  return (
    <Modal visible={visible} animationType="slide" statusBarTranslucent>
      {permission?.granted ? (
        <CameraView ref={cameraRef} style={styles.camera} facing={facing}>
          <SafeAreaView style={styles.overlay}>
            <View style={styles.topBar}>
              <IconButton
                icon="close"
                iconColor="white"
                size={30}
                onPress={onClose}
              />
              <IconButton
                icon="camera-flip-outline"
                iconColor="white"
                size={30}
                onPress={() =>
                  setFacing((f) => (f === 'back' ? 'front' : 'back'))
                }
              />
            </View>
            <View style={styles.captureRow}>
              <TouchableOpacity
                style={styles.captureButton}
                onPress={handleCapture}
              />
            </View>
          </SafeAreaView>
        </CameraView>
      ) : (
        <View style={styles.noAccess}>
          <Text style={styles.noAccessText}>Camera permission required.</Text>
          <TouchableOpacity onPress={onClose} style={{ marginTop: 20 }}>
            <Text style={{ color: 'white' }}>Go back</Text>
          </TouchableOpacity>
        </View>
      )}
    </Modal>
  );
}

const styles = StyleSheet.create({
  camera: { flex: 1 },
  overlay: {
    flex: 1,
    justifyContent: 'space-between'
  },
  topBar: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingHorizontal: 10,
    paddingTop: 10,
    backgroundColor: 'rgba(0,0,0,0.35)'
  },
  captureRow: {
    alignItems: 'center',
    paddingBottom: 50
  },
  captureButton: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: 'white',
    borderWidth: 4,
    borderColor: 'rgba(255,255,255,0.5)'
  },
  noAccess: {
    flex: 1,
    backgroundColor: 'black',
    alignItems: 'center',
    justifyContent: 'center'
  },
  noAccessText: {
    color: 'white',
    fontSize: 16
  }
});
