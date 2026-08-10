import Constants from 'expo-constants';
import AsyncStorage from '@react-native-async-storage/async-storage';

export const googleMapsConfig = {
  apiKey: process.env.GOOGLE_KEY
};

// Default API URL from Expo config (.env -> app.config.ts)
const defaultApiUrl = Constants.expoConfig?.extra?.API_URL as string | undefined;

// Website hosts are not API servers — requests return Next.js/HTML instead of JSON.
const WEBSITE_HOSTS = new Set([
  'atlas-cmms.com',
  'www.atlas-cmms.com',
  'app.atlas-cmms.com'
]);

export function normalizeApiUrl(url: string): string {
  if (!url) return url;
  const trimmed = url.trim().replace(/\/+$/, '');
  if (trimmed.endsWith('/api')) return trimmed;
  return `${trimmed}/api`;
}

function parseHost(url: string): string | null {
  try {
    const withProtocol = /^https?:\/\//i.test(url) ? url : `http://${url}`;
    return new URL(withProtocol).hostname.toLowerCase();
  } catch {
    return null;
  }
}

export function isWebsiteHost(url: string): boolean {
  const host = parseHost(url);
  return host !== null && WEBSITE_HOSTS.has(host);
}

async function resolveApiUrl(): Promise<string> {
  let customUrl = await AsyncStorage.getItem('customApiUrl');

  if (customUrl && isWebsiteHost(customUrl)) {
    await AsyncStorage.removeItem('customApiUrl');
    customUrl = null;
  }

  const rawApiUrl = normalizeApiUrl(customUrl || defaultApiUrl || '');
  if (!rawApiUrl) {
    throw new Error('API_URL is not configured. Set it in .env and restart Metro.');
  }

  return rawApiUrl.endsWith('/') ? rawApiUrl : `${rawApiUrl}/`;
}

// Function to get the API URL (either custom or default)
export const getApiUrl = async (): Promise<string> => {
  try {
    return await resolveApiUrl();
  } catch (error) {
    console.error('Failed to resolve API URL:', error);
    const fallback = normalizeApiUrl(defaultApiUrl || 'http://localhost:3001/api');
    return fallback.endsWith('/') ? fallback : `${fallback}/`;
  }
};

export const getDefaultApiUrl = (): string => defaultApiUrl || 'http://localhost:3001/api';

export const IS_LOCALHOST = (() => {
  const host = parseHost(defaultApiUrl || '');
  return host === 'localhost' || host === '127.0.0.1';
})();
