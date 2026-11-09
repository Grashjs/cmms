import AsyncStorage from '@react-native-async-storage/async-storage';
import * as SecureStore from 'expo-secure-store';
import * as Crypto from 'expo-crypto';
import { getApiUrl } from '../config';
import { canonicalizeServerUrl, installationId } from '../offline/accessPolicy';
import { Platform } from 'react-native';
import { assertSessionGeneration } from '../offline/sessionBridge';

export interface Credentials {
  accessToken: string;
  refreshToken: string | null;
  companyId?: number;
  userId?: number;
}
interface StoredCredentials extends Credentials {
  installationId: string;
}
const options = {
  keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY
};
const migrationPromises = new Map<string, Promise<void>>();
const writes = new Map<string, Promise<void>>();
async function storageKey(serverUrl: string): Promise<string> {
  return `atlas.session.${await Crypto.digestStringAsync(
    Crypto.CryptoDigestAlgorithm.SHA256,
    canonicalizeServerUrl(serverUrl)
  )}`;
}
async function migrateLegacy(serverUrl: string): Promise<void> {
  if (
    canonicalizeServerUrl(serverUrl) !==
    canonicalizeServerUrl(await getApiUrl())
  )
    return;
  const key = await storageKey(serverUrl);
  if (!migrationPromises.has(key))
    migrationPromises.set(
      key,
      (async () => {
        const existing = await SecureStore.getItemAsync(key);
        const [accessToken, refreshToken, companyId] = await Promise.all([
          AsyncStorage.getItem('accessToken'),
          AsyncStorage.getItem('refreshToken'),
          AsyncStorage.getItem('companyId')
        ]);
        if (accessToken && !existing) {
          await SecureStore.setItemAsync(
            key,
            JSON.stringify({
              accessToken,
              refreshToken,
              companyId: companyId ? Number(companyId) : undefined,
              installationId: await installationId()
            }),
            options
          );
        }
        // Only delete the old credentials after the secure write has succeeded.
        if (accessToken || refreshToken)
          await AsyncStorage.multiRemove([
            'accessToken',
            'refreshToken',
            'companyId'
          ]);
      })().catch((error) => {
        migrationPromises.delete(key);
        throw error;
      })
    );
  await migrationPromises.get(key);
}
export async function getCredentials(
  serverUrl?: string
): Promise<Credentials | null> {
  if (Platform.OS === 'web') {
    const accessToken = await AsyncStorage.getItem('accessToken');
    if (!accessToken) return null;
    return {
      accessToken,
      refreshToken: await AsyncStorage.getItem('refreshToken'),
      companyId: Number(await AsyncStorage.getItem('companyId')) || undefined
    };
  }
  const url = serverUrl || (await getApiUrl());
  await migrateLegacy(url);
  const encoded = await SecureStore.getItemAsync(await storageKey(url));
  if (!encoded) return null;
  const parsed: StoredCredentials = JSON.parse(encoded);
  if (parsed.installationId !== (await installationId())) return null;
  return parsed;
}
export async function setCredentials(
  credentials: Credentials | null,
  serverUrl?: string,
  expectedGeneration?: number
): Promise<void> {
  if (Platform.OS === 'web') {
    if (!credentials) {
      await AsyncStorage.multiRemove([
        'accessToken',
        'refreshToken',
        'companyId'
      ]);
      return;
    }
    await AsyncStorage.setItem('accessToken', credentials.accessToken);
    if (credentials.refreshToken)
      await AsyncStorage.setItem('refreshToken', credentials.refreshToken);
    else await AsyncStorage.removeItem('refreshToken');
    if (credentials.companyId)
      await AsyncStorage.setItem('companyId', String(credentials.companyId));
    return;
  }
  const url = serverUrl || (await getApiUrl());
  const key = await storageKey(url);
  const marker = credentials ? await installationId() : null;
  const write = (writes.get(key) || Promise.resolve())
    .catch(() => {})
    .then(async () => {
      if (expectedGeneration !== undefined)
        assertSessionGeneration(expectedGeneration);
      if (!credentials) await SecureStore.deleteItemAsync(key);
      else
        await SecureStore.setItemAsync(
          key,
          JSON.stringify({ ...credentials, installationId: marker }),
          options
        );
      await AsyncStorage.multiRemove([
        'accessToken',
        'refreshToken',
        'companyId'
      ]);
    });
  writes.set(key, write);
  await write;
}
