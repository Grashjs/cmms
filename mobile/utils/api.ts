import { getApiUrl } from '../config';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Platform } from 'react-native';

type Options = RequestInit & { raw?: boolean; headers?: HeadersInit };

let refreshPromise: Promise<boolean> | null = null;

async function clearTokens(): Promise<void> {
  await AsyncStorage.removeItem('accessToken');
  await AsyncStorage.removeItem('refreshToken');
}

async function performRefresh(): Promise<boolean> {
  const refreshToken = await AsyncStorage.getItem('refreshToken');
  if (!refreshToken) {
    return false;
  }
  try {
    const currentApiUrl = await getApiUrl();
    const response = await fetch(currentApiUrl + 'auth/refresh', {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ refreshToken })
    });
    if (!response.ok) {
      await clearTokens();
      return false;
    }
    const data = await response.json();
    if (!data?.accessToken) {
      await clearTokens();
      return false;
    }
    await AsyncStorage.setItem('accessToken', data.accessToken);
    if (data.refreshToken) {
      await AsyncStorage.setItem('refreshToken', data.refreshToken);
    }
    return true;
  } catch {
    await clearTokens();
    return false;
  }
}

export function refreshAccessToken(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = performRefresh().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

function isRefreshRequest(url: string): boolean {
  return url.replace(/\/+$/, '').endsWith('/auth/refresh');
}

let onConflictError: (() => void) | null = null;

export function setConflictErrorHandler(handler: () => void): () => void {
  onConflictError = handler;
  return () => {
    onConflictError = null;
  };
}

async function doFetch<T>(
  url: string,
  options: Options,
  retried: boolean
): Promise<T> {
  const response = await fetch(url, {
    headers: await authHeader(false),
    ...options
  });
  if (!response.ok) {
    if (response.status === 401 && !retried && !isRefreshRequest(url)) {
      const refreshed = await refreshAccessToken();
      if (refreshed) {
        return doFetch<T>(url, options, true);
      }
    }
    if (response.status === 409) {
      if (onConflictError) onConflictError();
      throw new Error('conflict_error');
    }
    let body: any = null;
    try {
      body = await response.json();
    } catch {
      body = null;
    }
    const err = new Error(JSON.stringify(body));
    (err as any).status = response.status;
    throw err;
  }
  if (options?.raw) return response as unknown as Promise<T>;
  return response.json() as Promise<T>;
}

async function api<T>(url: string, options: Options): Promise<T> {
  return doFetch<T>(url, options, false);
}

async function get<T>(url: string, options?: Options) {
  const currentApiUrl = await getApiUrl();
  return api<T>(currentApiUrl + url, options);
}

async function post<T>(
  url: string,
  data: object | any,
  options?: Options,
  withoutCompany?: boolean,
  isNotJson?: boolean
) {
  const companyId = await AsyncStorage.getItem('companyId');
  const currentApiUrl = await getApiUrl();
  return api<T>(currentApiUrl + url, {
    ...options,
    method: 'POST',
    body: isNotJson
      ? data
      : JSON.stringify(
          withoutCompany ? data : { ...data, company: { id: companyId } }
        )
  });
}

async function patch<T>(
  url: string,
  data: object,
  options?: Options,
  withoutCompany?: boolean
) {
  const companyId = await AsyncStorage.getItem('companyId');
  const currentApiUrl = await getApiUrl();
  return api<T>(currentApiUrl + url, {
    ...options,
    method: 'PATCH',
    body: JSON.stringify(
      withoutCompany ? data : { ...data, company: { id: companyId } }
    )
  });
}

async function deletes<T>(url, options?: Options) {
  const currentApiUrl = await getApiUrl();
  return api<T>(currentApiUrl + url, { ...options, method: 'DELETE' });
}

export async function authHeader(publicRoute: boolean) {
  // return authorization header with jwt token
  let accessToken = await AsyncStorage.getItem('accessToken');
  const commonHeaders = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    'X-Platform': Platform.OS
  };
  if (!publicRoute && accessToken) {
    return {
      Authorization: 'Bearer ' + accessToken,
      ...commonHeaders
    };
  } else {
    return commonHeaders;
  }
}
export const getErrorMessage = (
  error: any,
  defaultMessage: string = 'An error occurred'
): string => {
  try {
    const parsed = JSON.parse(error.message);
    return parsed?.message ?? error.message ?? defaultMessage;
  } catch {
    return error.message ?? defaultMessage;
  }
};

export const isNetworkError = (error: any): boolean => {
  if (!error) return false;
  if (error instanceof TypeError) return true;
  if (error?.status === 502) return true;
  const message = String(error?.message ?? '').toLowerCase();
  return (
    message.includes('failed to fetch') ||
    message.includes('networkerror') ||
    message.includes('network error') ||
    message.includes('load failed') ||
    message.includes('connection refused') ||
    message.includes('err_connection_refused')
  );
};

export default { get, patch, post, deletes, getErrorMessage, isNetworkError };
