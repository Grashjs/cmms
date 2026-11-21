import { Platform } from 'react-native';
import * as LocalAuthentication from 'expo-local-authentication';

export type DeviceOwnerResult =
  | 'verified'
  | 'cancelled'
  | 'unavailable'
  | 'failed';

export async function canVerifyDeviceOwner(): Promise<boolean> {
  try {
    return (
      (await LocalAuthentication.getEnrolledLevelAsync()) >=
      LocalAuthentication.SecurityLevel.SECRET
    );
  } catch {
    return false;
  }
}

/** Checks the phone owner, without creating or renewing an Atlas session. */
export async function verifyDeviceOwner(labels: {
  prompt: string;
  cancel: string;
  fallback: string;
}): Promise<DeviceOwnerResult> {
  try {
    if (!(await canVerifyDeviceOwner())) return 'unavailable';

    const result = await LocalAuthentication.authenticateAsync({
      promptMessage: labels.prompt,
      cancelLabel: labels.cancel,
      fallbackLabel: labels.fallback,
      disableDeviceFallback: false,
      ...(Platform.OS === 'android'
        ? { biometricsSecurityLevel: 'strong' as const }
        : {})
    });
    if (result.success) return 'verified';
    if (!('error' in result)) return 'failed';
    if (
      result.error === 'user_cancel' ||
      result.error === 'app_cancel' ||
      result.error === 'system_cancel'
    )
      return 'cancelled';
    if (
      result.error === 'not_available' ||
      result.error === 'not_enrolled' ||
      result.error === 'passcode_not_set'
    )
      return 'unavailable';
    return 'failed';
  } catch {
    return 'unavailable';
  }
}
