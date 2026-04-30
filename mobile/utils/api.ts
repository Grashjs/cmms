import { getApiUrl } from '../config';
import AsyncStorage from '@react-native-async-storage/async-storage';

type Options = RequestInit & { raw?: boolean; headers?: HeadersInit };

async function api<T>(url: string, options: Options): Promise<T> {
  return fetch(url, { headers: await authHeader(false), ...options }).then(
    async (response) => {
      if (!response.ok) {
        if (response.status === 403) {
          //TODO
          // AsyncStorage.clear();
        }
        throw new Error(JSON.stringify(await response.json()));
      }
      return response.json() as Promise<T>;
    }
  );
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
  if (!publicRoute && accessToken) {
    return {
      Authorization: 'Bearer ' + accessToken,
      Accept: 'application/json',
      'Content-Type': 'application/json'
    };
  } else {
    return {
      Accept: 'application/json',
      'Content-Type': 'application/json'
    };
  }
}
export const getErrorMessage = (
  error: any,
  defaultMessage?: string
): string => {
  try {
    const parsed = JSON.parse(error.message);
    return parsed?.message ?? error.message ?? defaultMessage;
  } catch {
    return error.message ?? defaultMessage;
  }
};

export default { get, patch, post, deletes, getErrorMessage };
