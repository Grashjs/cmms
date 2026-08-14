/**
 * Errors surfaced by {@link ../utils/api.ts} and helpers for showing them in UI.
 *
 * Previously every failure was `throw new Error(JSON.stringify(body))`, which
 * forced callers to JSON.parse the message string to learn anything useful.
 */

export type ApiErrorBody = {
  success?: boolean;
  message?: string;
  [key: string]: unknown;
};

export class ApiError extends Error {
  readonly status?: number;

  readonly body?: ApiErrorBody;

  readonly isNetworkError: boolean;

  constructor(
    message: string,
    options: {
      status?: number;
      body?: ApiErrorBody;
      isNetworkError?: boolean;
      cause?: unknown;
    } = {}
  ) {
    super(message, options.cause ? { cause: options.cause } : undefined);
    this.name = 'ApiError';
    this.status = options.status;
    this.body = options.body;
    this.isNetworkError = options.isNetworkError ?? false;
  }
}

/** Thrown after a field mutation was written to the offline queue. */
export class OfflineQueuedError extends Error {
  constructor() {
    super('offline_queued');
    this.name = 'OfflineQueuedError';
  }
}

export const isApiError = (error: unknown): error is ApiError =>
  error instanceof ApiError;

export const isOfflineQueuedError = (error: unknown): error is OfflineQueuedError =>
  error instanceof OfflineQueuedError;

export const isNetworkError = (error: unknown): boolean => {
  if (isApiError(error)) return error.isNetworkError;
  if (error instanceof TypeError) return true;
  const message =
    error instanceof Error ? error.message.toLowerCase() : String(error);
  return (
    message.includes('network request failed') ||
    message.includes('failed to fetch') ||
    message.includes('network error')
  );
};

/**
 * User-facing text for snackbars and empty states. Accepts legacy `Error`
 * objects whose message is still a JSON string from before ApiError existed.
 */
export const getErrorMessage = (
  error: unknown,
  defaultMessage?: string
): string => {
  if (isOfflineQueuedError(error)) return defaultMessage ?? error.message;

  if (isApiError(error)) {
    if (error.isNetworkError) {
      return defaultMessage ?? error.message;
    }
    return error.body?.message ?? error.message ?? defaultMessage ?? 'error';
  }

  if (error instanceof Error) {
    try {
      const parsed = JSON.parse(error.message) as ApiErrorBody;
      return parsed?.message ?? error.message ?? defaultMessage ?? 'error';
    } catch {
      if (isNetworkError(error)) {
        return defaultMessage ?? error.message;
      }
      return error.message ?? defaultMessage ?? 'error';
    }
  }

  return defaultMessage ?? 'error';
};
