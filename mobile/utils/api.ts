import { getApiUrl } from '../config';
import { AsyncStorage } from 'react-native';

async function api<T>(url: string, options): Promise<T> {
  return fetch(url, { headers: await authHeader(false), ...options }).then(
    (response) => {
      if (!response.ok) {
        if (response.status === 403) {
          //TODO
          // AsyncStorage.clear();
        }
        throw new Error(response.statusText);
      }
      return response.json() as Promise<T>;
    }
  );
}

async function get<T>(url, options?) {
  const currentApiUrl = await getApiUrl();
  return api<T>(currentApiUrl + url, options);
}

async function post<T>(
  url,
  data,
  options?,
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

async function patch<T>(url, data, options?, withoutCompany?: boolean) {
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

async function deletes<T>(url, options?) {
  const currentApiUrl = await getApiUrl();
  return api<T>(currentApiUrl + url, { ...options, method: 'DELETE' });
}

export async function authHeader(publicRoute) {
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

export default { get, patch, post, deletes };
