import { OfflineError } from './types';
import * as FileSystem from 'expo-file-system';
import { NativeModules, Platform } from 'react-native';

/** Private, durable storage. Never use cacheDirectory for unsent work. */
export async function offlineStorageDirectory(): Promise<string> {
  try {
    if (Platform.OS === 'web') throw new OfflineError('OFFLINE_DISABLED');
    if (Platform.OS === 'ios') {
      const nativeStorage = NativeModules.AtlasOfflineStorage;
      if (!nativeStorage) throw new OfflineError('OFFLINE_NATIVE_REQUIRED');
      return await nativeStorage.prepareDirectory();
    }
    if (!FileSystem.documentDirectory)
      throw new OfflineError('STORAGE_UNAVAILABLE');
    const directory = `${FileSystem.documentDirectory}atlas-offline/`;
    await FileSystem.makeDirectoryAsync(directory, { intermediates: true });
    return directory;
  } catch (error) {
    if (error instanceof OfflineError) throw error;
    throw new OfflineError('STORAGE_UNAVAILABLE');
  }
}
