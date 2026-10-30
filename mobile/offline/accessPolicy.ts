import { OfflineError } from './types';
import * as SecureStore from 'expo-secure-store';
import * as Crypto from 'expo-crypto';
import * as FileSystem from 'expo-file-system';
import { URL } from 'react-native-url-polyfill';
import { offlineStorageDirectory } from './storage';
import { assertSessionGeneration, sessionGeneration } from './sessionBridge';
import type { UserResponseDTO } from '../models/user';
import type { Company } from '../models/company';
import type CompanySettings from '../models/companySettings';
import type UserSettings from '../models/userSettings';

export const LOCAL_ACCESS_DURATION_MS = 7 * 24 * 60 * 60 * 1000;
const CLOCK_TOLERANCE_MS = 2 * 60 * 1000;
const secureOptions = {
  keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY
};

export interface OfflineContext {
  serverUrl: string;
  companyId: number;
  userId: number;
}
export interface BootstrapSnapshot {
  context: OfflineContext;
  user: UserResponseDTO;
  company: Company;
  companySettings: CompanySettings;
  userSettings: UserSettings | null;
}
interface LocalEligibility {
  enabled?: boolean;
  eligible: boolean;
  serverTime?: string;
  authorizedUntil?: string;
}
interface Grant {
  contextKey: string;
  installationId: string;
  authorizedAt: number;
  expiresAt: number;
  lastSeenAt: number;
  snapshotHash: string;
}

export function canonicalizeServerUrl(raw: string): string {
  const url = new URL(raw.trim());
  url.hostname = url.hostname.toLowerCase();
  if (
    !['https:', 'http:'].includes(url.protocol) ||
    url.username ||
    url.password ||
    url.search ||
    url.hash
  ) {
    throw new OfflineError('INVALID_SERVER_URL');
  }
  url.pathname = `${url.pathname.replace(/\/+$/, '')}/`;
  return url.toString();
}
export function serverContextKey(context: OfflineContext): string {
  if (
    !Number.isSafeInteger(context.companyId) ||
    context.companyId <= 0 ||
    !Number.isSafeInteger(context.userId) ||
    context.userId <= 0
  )
    throw new OfflineError('INVALID_CONTEXT');
  return JSON.stringify([
    canonicalizeServerUrl(context.serverUrl),
    context.companyId,
    context.userId
  ]);
}
async function contextStorageId(context: OfflineContext): Promise<string> {
  return Crypto.digestStringAsync(
    Crypto.CryptoDigestAlgorithm.SHA256,
    serverContextKey(context)
  );
}
async function serverId(serverUrl: string): Promise<string> {
  return Crypto.digestStringAsync(
    Crypto.CryptoDigestAlgorithm.SHA256,
    canonicalizeServerUrl(serverUrl)
  );
}
let installationPromise: Promise<string> | null = null;
export function installationId(): Promise<string> {
  if (!installationPromise)
    installationPromise = (async () => {
      // This marker is not a credential. It detects Keychain values surviving reinstallation.
      // Native backup rules exclude this directory, including when offline is disabled.
      if (!FileSystem.documentDirectory)
        throw new OfflineError('STORAGE_UNAVAILABLE');
      const directory = `${FileSystem.documentDirectory}atlas-offline/`;
      await FileSystem.makeDirectoryAsync(directory, { intermediates: true });
      const uri = `${directory}installation-id`;
      if ((await FileSystem.getInfoAsync(uri)).exists)
        return FileSystem.readAsStringAsync(uri);
      const id = Crypto.randomUUID();
      await FileSystem.writeAsStringAsync(uri, id);
      return id;
    })().finally(() => {
      installationPromise = null;
    });
  return installationPromise;
}
async function snapshotPath(
  context: OfflineContext,
  grantHash?: string
): Promise<string> {
  return `${await offlineStorageDirectory()}auth-${await contextStorageId(
    context
  )}${grantHash ? `-grant-${grantHash}` : ''}.json`;
}
const grantWrites = new Map<string, Promise<unknown>>();
function withGrantLock<T>(
  context: OfflineContext,
  action: () => Promise<T>
): Promise<T> {
  const key = serverContextKey(context);
  const result = (grantWrites.get(key) || Promise.resolve())
    .catch(() => {})
    .then(action);
  grantWrites.set(key, result);
  return result;
}

/** Online identity is not an offline authorization. */
export async function rememberOnlineIdentity(
  snapshot: BootstrapSnapshot
): Promise<void> {
  const context = {
    ...snapshot.context,
    serverUrl: canonicalizeServerUrl(snapshot.context.serverUrl)
  };
  const serialized = JSON.stringify({ ...snapshot, context });
  await FileSystem.writeAsStringAsync(await snapshotPath(context), serialized);
  await SecureStore.setItemAsync(
    `atlas.identity.${await serverId(context.serverUrl)}`,
    JSON.stringify({ context, installationId: await installationId() }),
    secureOptions
  );
}
export async function getOnlineIdentity(
  serverUrl: string
): Promise<BootstrapSnapshot | null> {
  const encoded = await SecureStore.getItemAsync(
    `atlas.identity.${await serverId(serverUrl)}`
  );
  if (!encoded) return null;
  try {
    const identity = JSON.parse(encoded);
    if (
      identity.installationId !== (await installationId()) ||
      canonicalizeServerUrl(identity.context.serverUrl) !==
        canonicalizeServerUrl(serverUrl)
    )
      return null;
    const result = JSON.parse(
      await FileSystem.readAsStringAsync(await snapshotPath(identity.context))
    );
    if (
      serverContextKey(result.context) !== serverContextKey(identity.context) ||
      result.user.id !== result.context.userId ||
      result.user.companyId !== result.context.companyId
    )
      return null;
    return result;
  } catch {
    return null;
  }
}
export function authorizeLocalAccess(
  context: OfflineContext,
  eligibility: LocalEligibility,
  snapshot: BootstrapSnapshot
): Promise<void> {
  const generation = sessionGeneration();
  if (!eligibility.eligible || eligibility.enabled === false)
    return revokeLocalAccess(context);
  return withGrantLock(context, async () => {
    assertSessionGeneration(generation);
    if (!canonicalizeServerUrl(context.serverUrl).startsWith('https://')) {
      throw new OfflineError('OFFLINE_HTTPS_REQUIRED');
    }
    if (
      serverContextKey(context) !== serverContextKey(snapshot.context) ||
      snapshot.user.id !== context.userId ||
      snapshot.user.companyId !== context.companyId ||
      Number(snapshot.company.id) !== context.companyId
    )
      throw new OfflineError('CONTEXT_MISMATCH');
    const now = Date.now();
    const serverTime = eligibility.serverTime
      ? Date.parse(eligibility.serverTime)
      : now;
    const expiry = eligibility.authorizedUntil;
    const serverExpiry = expiry
      ? Date.parse(expiry)
      : serverTime + LOCAL_ACCESS_DURATION_MS;
    const duration = Math.min(
      LOCAL_ACCESS_DURATION_MS,
      serverExpiry - serverTime
    );
    if (!Number.isFinite(duration) || duration <= 0)
      throw new OfflineError('INVALID_AUTHORIZATION');
    // The authorized snapshot is immutable. An ordinary online refresh must not
    // invalidate the previous grant if connectivity fails before renewal.
    const canonicalSnapshot = {
      ...snapshot,
      context: {
        ...context,
        serverUrl: canonicalizeServerUrl(context.serverUrl)
      }
    };
    const serialized = JSON.stringify(canonicalSnapshot);
    const hash = await Crypto.digestStringAsync(
      Crypto.CryptoDigestAlgorithm.SHA256,
      serialized
    );
    await FileSystem.writeAsStringAsync(
      await snapshotPath(context, hash),
      serialized
    );
    await rememberOnlineIdentity(canonicalSnapshot);
    const grant: Grant = {
      contextKey: serverContextKey(context),
      installationId: await installationId(),
      authorizedAt: now,
      expiresAt: now + duration,
      lastSeenAt: now,
      snapshotHash: hash
    };
    assertSessionGeneration(generation);
    await SecureStore.setItemAsync(
      `atlas.grant.${await contextStorageId(context)}`,
      JSON.stringify(grant),
      secureOptions
    );
  });
}
export async function restoreLocalAuthorization(
  serverUrl: string
): Promise<BootstrapSnapshot | null> {
  // The secure identity pointer can survive online bootstrap changes; its
  // authorized snapshot is loaded from the hash protected by SecureStore.
  const identityEncoded = await SecureStore.getItemAsync(
    `atlas.identity.${await serverId(serverUrl)}`
  );
  if (!identityEncoded) return null;
  let context: OfflineContext;
  try {
    const identity = JSON.parse(identityEncoded);
    if (
      identity.installationId !== (await installationId()) ||
      canonicalizeServerUrl(identity.context.serverUrl) !==
        canonicalizeServerUrl(serverUrl)
    )
      return null;
    context = identity.context;
  } catch {
    return null;
  }
  return withGrantLock(context, async () => {
    const key = `atlas.grant.${await contextStorageId(context)}`;
    const encoded = await SecureStore.getItemAsync(key);
    if (!encoded) return null;
    try {
      const grant: Grant = JSON.parse(encoded);
      const now = Date.now();
      if (
        grant.contextKey !== serverContextKey(context) ||
        grant.installationId !== (await installationId()) ||
        !Number.isFinite(grant.authorizedAt) ||
        !Number.isFinite(grant.lastSeenAt) ||
        !Number.isFinite(grant.expiresAt) ||
        now >= grant.expiresAt ||
        grant.expiresAt > grant.authorizedAt + LOCAL_ACCESS_DURATION_MS ||
        now + CLOCK_TOLERANCE_MS < grant.lastSeenAt ||
        now + CLOCK_TOLERANCE_MS < grant.authorizedAt ||
        !/^[0-9a-f]{64}$/.test(grant.snapshotHash)
      )
        return null;
      const serialized = await FileSystem.readAsStringAsync(
        await snapshotPath(context, grant.snapshotHash)
      );
      const hash = await Crypto.digestStringAsync(
        Crypto.CryptoDigestAlgorithm.SHA256,
        serialized
      );
      if (hash !== grant.snapshotHash) return null;
      const snapshot: BootstrapSnapshot = JSON.parse(serialized);
      if (
        serverContextKey(snapshot.context) !== serverContextKey(context) ||
        snapshot.user.id !== context.userId ||
        snapshot.user.companyId !== context.companyId ||
        Number(snapshot.company.id) !== context.companyId
      )
        return null;
      await SecureStore.setItemAsync(
        key,
        JSON.stringify({
          ...grant,
          lastSeenAt: Math.max(now, grant.lastSeenAt)
        }),
        secureOptions
      );
      return snapshot;
    } catch {
      return null;
    }
  });
}
export function revokeLocalAccess(context: OfflineContext): Promise<void> {
  return withGrantLock(context, async () => {
    await SecureStore.deleteItemAsync(
      `atlas.grant.${await contextStorageId(context)}`
    );
  });
}

/** Timer scheduling only; callers must still validate the authorization before access. */
export function localAuthorizationExpiresAt(
  context: OfflineContext
): Promise<number | null> {
  return withGrantLock(context, async () => {
    const encoded = await SecureStore.getItemAsync(
      `atlas.grant.${await contextStorageId(context)}`
    );
    if (!encoded) return null;
    try {
      const grant: Grant = JSON.parse(encoded);
      if (
        grant.contextKey !== serverContextKey(context) ||
        grant.installationId !== (await installationId()) ||
        !Number.isFinite(grant.expiresAt) ||
        grant.expiresAt > grant.authorizedAt + LOCAL_ACCESS_DURATION_MS
      )
        return null;
      return grant.expiresAt;
    } catch {
      return null;
    }
  });
}
