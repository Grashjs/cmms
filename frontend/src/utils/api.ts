import { apiUrl } from '../config';

type Options = RequestInit & { raw?: boolean; headers?: HeadersInit };

/** Carries the HTTP status so callers can distinguish a rejection from an outage. */
export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

/**
 * True when the request never got a verdict from the application: the server was not
 * reachable (fetch rejects without a status) or answered 5xx. A wrong password is a 4xx.
 */
export const isServerUnavailable = (error: any): boolean =>
  typeof error?.status !== 'number' || error.status >= 500;
function api<T>(url: string, options: Options): Promise<T> {
  return fetch(url, { headers: authHeader(false), ...options }).then(
    async (response) => {
      if (!response.ok) {
        // The status used to be dropped here, so callers could not tell a rejection from an
        // unreachable server. A non-JSON body — nginx's 502/503 page while the api boots —
        // additionally made response.json() throw, replacing the error with a SyntaxError.
        // The message keeps its old shape for JSON bodies so getErrorMessage() is unaffected.
        let body: any = null;
        try {
          body = await response.json();
        } catch {
          body = null;
        }
        throw new ApiError(
          body ? JSON.stringify(body) : `HTTP ${response.status}`,
          response.status
        );
      }
      if (options?.raw) return response as unknown as Promise<T>;
      return response.json() as Promise<T>;
    }
  );
}

function get<T>(url, options?: Options) {
  return api<T>(apiUrl + url, options);
}

function post<T>(url, data, options?: Options, isNotJson?: boolean) {
  return api<T>(apiUrl + url, {
    ...options,
    method: 'POST',
    body: isNotJson ? data : JSON.stringify(data)
  });
}

function patch<T>(url, data, options?: Options) {
  return api<T>(apiUrl + url, {
    ...options,
    method: 'PATCH',
    body: JSON.stringify(data)
  });
}

function deletes<T>(url, options?: Options) {
  return api<T>(apiUrl + url, { ...options, method: 'DELETE' });
}

export function authHeader(publicRoute: boolean): HeadersInit {
  // return authorization header with jwt token
  let accessToken = localStorage.getItem('accessToken');

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

export default { get, patch, post, deletes };
