import { Alert, Linking, AppState, Platform } from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { Camera } from 'expo-camera';
import i18n from '../i18n/i18n';

type PermissionResponseLike = {
  granted: boolean;
  canAskAgain: boolean;
  status?: string;
};

interface PermissionFlowOptions {
  flow: string;
  permissionNameKey: 'camera' | 'media_library';
  requestPermission: () => Promise<PermissionResponseLike>;
  getPermission: () => Promise<PermissionResponseLike>;
}

export const ensurePermission = async ({
  flow,
  permissionNameKey,
  requestPermission,
  getPermission
}: PermissionFlowOptions): Promise<boolean> => {
  const permissionName = i18n.t(permissionNameKey);

  console.warn(`[${flow}] Requesting ${permissionName} permission`);

  try {
    const result = await requestPermission();
    console.warn(
      `[${flow}] ${permissionName} permission result`,
      JSON.stringify({
        granted: result.granted,
        canAskAgain: result.canAskAgain,
        status: result.status
      })
    );

    if (result.granted) {
      return true;
    }

    const latest = await getPermission();

    if (!latest.canAskAgain) {
      Alert.alert(
        i18n.t('permission_required_title'),
        i18n.t('permission_required_message', { permission: permissionName }),
        [
          { text: i18n.t('cancel'), style: 'cancel' },
          {
            text: i18n.t('open_settings'),
            onPress: () => Linking.openSettings()
          }
        ]
      );
    } else {
      Alert.alert(
        i18n.t('permission_denied_title'),
        i18n.t('permission_denied_message', { permission: permissionName }),
        [{ text: i18n.t('ok') }]
      );
    }

    return false;
  } catch (error) {
    console.warn(`[${flow}] Failed while requesting ${permissionName}`, error);
    Alert.alert(
      i18n.t('error'),
      i18n.t('unable_to_request_permission', { permission: permissionName })
    );
    return false;
  }
};

/**
 * On Android 12+, the permission dialog dismissal puts the activity into a
 * transient pause. Launching the camera immediately after causes Android to
 * silently drop the intent. We wait until the activity is fully active again.
 */
const waitForAppActive = (): Promise<void> => {
  return new Promise((resolve) => {
    if (AppState.currentState === 'active') {
      resolve();
      return;
    }

    const sub = AppState.addEventListener('change', (state) => {
      if (state === 'active') {
        sub.remove();
        // Small buffer for the activity window to fully settle
        setTimeout(resolve, 100);
      }
    });
  });
};

export const openCameraWithPermission = async (
  flow: string,
  options: ImagePicker.ImagePickerOptions
): Promise<ImagePicker.ImagePickerResult | null> => {
  const hasPermission = await ensurePermission({
    flow,
    permissionNameKey: 'camera',
    requestPermission: ImagePicker.requestCameraPermissionsAsync,
    getPermission: ImagePicker.getCameraPermissionsAsync
  });

  if (!hasPermission) {
    console.warn(`[${flow}] Camera permission denied, picker will not open`);
    return null;
  }

  // Wait for activity to fully resume before launching camera.
  // Only needed on Android — iOS handles this natively.
  if (Platform.OS === 'android') {
    await waitForAppActive();
  }

  console.warn(`[${flow}] Opening camera picker`);
  const result = await ImagePicker.launchCameraAsync(options);
  console.warn(
    `[${flow}] Camera picker completed`,
    JSON.stringify({ canceled: result.canceled })
  );
  return result;
};

export const openLibraryWithPermission = async (
  flow: string,
  options: ImagePicker.ImagePickerOptions
): Promise<ImagePicker.ImagePickerResult | null> => {
  const hasPermission = await ensurePermission({
    flow,
    permissionNameKey: 'media_library',
    requestPermission: ImagePicker.requestMediaLibraryPermissionsAsync,
    getPermission: ImagePicker.getMediaLibraryPermissionsAsync
  });

  if (!hasPermission) {
    console.warn(
      `[${flow}] Media library permission denied, picker will not open`
    );
    return null;
  }

  console.warn(`[${flow}] Opening media library picker`);
  const result = await ImagePicker.launchImageLibraryAsync(options);
  console.warn(
    `[${flow}] Library picker completed`,
    JSON.stringify({ canceled: result.canceled })
  );
  return result;
};

export const ensureScannerCameraPermission = async (
  flow: string
): Promise<boolean> => {
  return ensurePermission({
    flow,
    permissionNameKey: 'camera',
    requestPermission: Camera.requestCameraPermissionsAsync,
    getPermission: Camera.getCameraPermissionsAsync
  });
};
