import { NativeModules, Platform } from 'react-native';
import mime from 'mime';
import { OfflineError } from './types';

/** The native bridge only accepts files inside Atlas' private offline directory. */
export async function openOfflineDocument(
  uri: string,
  name: string,
  mimeType?: string
): Promise<void> {
  const storage = NativeModules.AtlasOfflineStorage;
  if (Platform.OS === 'web' || !storage?.openDocument)
    throw new OfflineError('OFFLINE_NATIVE_REQUIRED');
  try {
    await storage.openDocument(
      uri,
      name,
      mimeType || mime.getType(name) || 'application/octet-stream'
    );
  } catch {
    throw new OfflineError('FILE_OPEN_FAILED');
  }
}

/** Files imported after prepareDirectory need their own iOS protection attributes. */
export async function protectOfflineFile(uri: string): Promise<void> {
  if (Platform.OS !== 'ios') return;
  const storage = NativeModules.AtlasOfflineStorage;
  if (!storage?.protectFile) throw new OfflineError('OFFLINE_NATIVE_REQUIRED');
  try {
    await storage.protectFile(uri);
  } catch {
    throw new OfflineError('STORAGE_UNAVAILABLE');
  }
}

/** Dismiss the iOS preview / revoke Android read grants when leaving the account. */
export async function closeOfflineDocument(): Promise<void> {
  await NativeModules.AtlasOfflineStorage?.closeDocument?.();
}
