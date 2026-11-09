import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Crypto from 'expo-crypto';
import { serverContextKey } from './accessPolicy';
import type { OfflineContext } from './accessPolicy';
import { OfflineError } from './types';

async function preferenceKey(context: OfflineContext): Promise<string> {
  const id = await Crypto.digestStringAsync(
    Crypto.CryptoDigestAlgorithm.SHA256,
    serverContextKey(context)
  );
  return `atlas.offline.preference.${id}`;
}

/** A missing value is OFF. This setting is local. */
export async function readOfflinePreference(
  context: OfflineContext
): Promise<boolean> {
  const stored = await AsyncStorage.getItem(await preferenceKey(context));
  if (stored === null || stored === 'false') return false;
  if (stored === 'true') return true;
  throw new OfflineError('OFFLINE_PREFERENCE_READ_FAILED');
}

export async function writeOfflinePreference(
  context: OfflineContext,
  enabled: boolean
): Promise<void> {
  const key = await preferenceKey(context);
  if (enabled) await AsyncStorage.setItem(key, 'true');
  else await AsyncStorage.removeItem(key);
}
