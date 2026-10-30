import { getApiUrl } from '../config';
import { getCredentials, setCredentials } from './credentialStore';
import {
  assertSessionGeneration,
  sessionGeneration
} from '../offline/sessionBridge';
import { Platform } from 'react-native';
import i18next from 'i18next';
import { OfflineError } from '../offline/types';
import { offlineErrorKey } from '../offline/errors';

export type Options = RequestInit & {
  raw?: boolean;
  headers?: HeadersInit;
  publicRoute?: boolean;
  timeoutMs?: number;
};
export const AUTH_REQUEST_TIMEOUT_MS = 10_000;
type RequestContext = { serverUrl: string; generation: number };

function assertNotAborted(signal?: AbortSignal | null): void {
  if (signal?.aborted) {
    const error = new Error('Request aborted');
    error.name = 'AbortError';
    throw error;
  }
}

// Only callers opting into a deadline are bounded. In particular, file uploads keep
// their existing behavior. Race the whole read as some native fetch implementations
// do not reject promptly on abort, or receive headers before a stalled JSON body.
async function withNetworkDeadline<T>(
  options: Pick<Options, 'signal' | 'timeoutMs'>,
  request: (signal?: AbortSignal | null) => Promise<T>
): Promise<T> {
  if (!options?.timeoutMs && !options?.signal) return request(undefined);
  assertNotAborted(options.signal);
  const controller = new AbortController();
  let timer: ReturnType<typeof setTimeout> | undefined;
  let rejectDeadline: (reason: Error) => void;
  const deadline = new Promise<never>((_resolve, reject) => {
    rejectDeadline = reject;
  });
  const abort = (timedOut: boolean) => {
    const error = new Error(timedOut ? 'Request timed out' : 'Request aborted');
    error.name = timedOut ? 'TimeoutError' : 'AbortError';
    rejectDeadline(error);
    controller.abort();
  };
  const onAbort = () => abort(false);
  options.signal?.addEventListener('abort', onAbort);
  if (options.timeoutMs)
    timer = setTimeout(() => abort(true), options.timeoutMs);
  try {
    return await Promise.race([request(controller.signal), deadline]);
  } finally {
    if (timer) clearTimeout(timer);
    options.signal?.removeEventListener('abort', onAbort);
  }
}
async function captureContext(): Promise<RequestContext> {
  const generation = sessionGeneration();
  assertSessionGeneration(generation);
  const serverUrl = await getApiUrl();
  assertSessionGeneration(generation);
  return { serverUrl, generation };
}

const refreshPromises = new Map<string, Promise<boolean>>();

async function performRefresh(
  serverUrl: string,
  generation: number
): Promise<boolean> {
  const credentials = await getCredentials(serverUrl);
  if (!credentials?.refreshToken) return false;
  try {
    const { response, data } = await withNetworkDeadline(
      { timeoutMs: AUTH_REQUEST_TIMEOUT_MS },
      async (signal) => {
        const response = await fetch(serverUrl + 'auth/refresh', {
          method: 'POST',
          headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ refreshToken: credentials.refreshToken }),
          signal
        });
        assertNotAborted(signal);
        const data = response.ok ? await response.json() : null;
        assertNotAborted(signal);
        return { response, data };
      }
    );
    assertSessionGeneration(generation);
    const current = await getCredentials(serverUrl);
    if (current?.refreshToken !== credentials.refreshToken) return false;
    // A timeout, 5xx, malformed response or unavailable network is not a logout.
    if (!response.ok) {
      if (response.status === 401 || response.status === 403)
        await setCredentials(null, serverUrl, generation);
      return false;
    }
    assertSessionGeneration(generation);
    if (!data?.accessToken) return false;
    await setCredentials(
      {
        ...credentials,
        accessToken: data.accessToken,
        refreshToken: data.refreshToken || credentials.refreshToken
      },
      serverUrl,
      generation
    );
    return true;
  } catch {
    return false;
  }
}

export async function refreshAccessToken(serverUrl?: string): Promise<boolean> {
  const url = serverUrl || (await getApiUrl());
  const generation = sessionGeneration();
  const key = `${generation}:${url}`;
  if (!refreshPromises.has(key))
    refreshPromises.set(
      key,
      performRefresh(url, generation).finally(() => {
        refreshPromises.delete(key);
      })
    );
  return refreshPromises.get(key);
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
  retried: boolean,
  generation: number,
  serverUrl: string
): Promise<T> {
  assertSessionGeneration(generation);
  const credentials = options?.publicRoute
    ? null
    : await getCredentials(serverUrl);
  assertSessionGeneration(generation);
  const headers = new Headers(options?.headers);
  if (!headers.has('Accept')) headers.set('Accept', 'application/json');
  if (!headers.has('Content-Type') && !(options?.body instanceof FormData))
    headers.set('Content-Type', 'application/json');
  headers.set('X-Platform', Platform.OS);
  headers.delete('Authorization');
  if (credentials?.accessToken)
    headers.set('Authorization', 'Bearer ' + credentials.accessToken);
  const { publicRoute, raw, timeoutMs, ...requestOptions } = options || {};
  return withNetworkDeadline(options, async (signal) => {
    const response = await fetch(url, { ...requestOptions, headers, signal });
    assertNotAborted(signal);
    assertSessionGeneration(generation);
    if (!response.ok) {
      if (
        response.status === 401 &&
        !publicRoute &&
        !retried &&
        !isRefreshRequest(url)
      ) {
        const refreshed = await refreshAccessToken(serverUrl);
        assertNotAborted(signal);
        assertSessionGeneration(generation);
        if (refreshed) {
          return doFetch<T>(
            url,
            { ...options, signal },
            true,
            generation,
            serverUrl
          );
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
      assertNotAborted(signal);
      assertSessionGeneration(generation);
      const err = new Error(JSON.stringify(body));
      (err as any).status = response.status;
      throw err;
    }
    if (raw) return response as unknown as T;
    const data = await response.json();
    assertNotAborted(signal);
    assertSessionGeneration(generation);
    return data as T;
  });
}

async function api<T>(
  path: string,
  options: Options,
  context: RequestContext
): Promise<T> {
  return doFetch<T>(
    context.serverUrl + path,
    options,
    false,
    context.generation,
    context.serverUrl
  );
}

async function get<T>(url: string, options?: Options) {
  const context = await captureContext();
  return api<T>(url, options, context);
}

async function post<T>(
  url: string,
  data: object | any,
  options?: Options,
  withoutCompany?: boolean,
  isNotJson?: boolean
) {
  const context = await captureContext();
  const companyId = options?.publicRoute
    ? undefined
    : (await getCredentials(context.serverUrl))?.companyId;
  assertSessionGeneration(context.generation);
  return api<T>(
    url,
    {
      ...options,
      method: 'POST',
      body: isNotJson
        ? data
        : JSON.stringify(
            withoutCompany ? data : { ...data, company: { id: companyId } }
          )
    },
    context
  );
}

async function patch<T>(
  url: string,
  data: object,
  options?: Options,
  withoutCompany?: boolean
) {
  const context = await captureContext();
  const companyId = (await getCredentials(context.serverUrl))?.companyId;
  assertSessionGeneration(context.generation);
  return api<T>(
    url,
    {
      ...options,
      method: 'PATCH',
      body: JSON.stringify(
        withoutCompany ? data : { ...data, company: { id: companyId } }
      )
    },
    context
  );
}

async function deletes<T>(url: string, options?: Options) {
  return api<T>(url, { ...options, method: 'DELETE' }, await captureContext());
}

export async function authHeader(publicRoute: boolean, serverUrl?: string) {
  // return authorization header with jwt token
  const accessToken = publicRoute
    ? null
    : (await getCredentials(serverUrl))?.accessToken;
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
  if (error instanceof OfflineError)
    return String(i18next.t(offlineErrorKey(error)));
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
  if (error.name === 'TimeoutError' || error.name === 'AbortError') return true;
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
