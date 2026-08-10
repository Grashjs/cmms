import { getApiUrl } from '../config';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Platform } from 'react-native';
import { ApiError, ApiErrorBody, getErrorMessage } from './errors';

type Options = RequestInit & { raw?: boolean; headers?: HeadersInit };

async function parseJsonResponse(response: Response) {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    throw new ApiError(
      `Expected JSON but received: ${text.slice(0, 80).replace(/\s+/g, ' ')}`,
      { status: response.status }
    );
  }
}

async function api<T>(url: string, options: Options): Promise<T> {
  try {
    const response = await fetch(url, {
      headers: await authHeader(false),
      ...options
    });
    if (!response.ok) {
      const body = (await parseJsonResponse(response)) as ApiErrorBody | null;
      throw new ApiError(body?.message ?? response.statusText, {
        status: response.status,
        body: body ?? undefined
      });
    }
    return parseJsonResponse(response) as Promise<T>;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    throw new ApiError(
      error instanceof Error ? error.message : 'Network request failed',
      { isNetworkError: true, cause: error }
    );
  }
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
export { getErrorMessage } from './errors';

export default { get, patch, post, deletes, getErrorMessage };
