/**
 * Creates a cancellable API request wrapper using AbortController.
 * Returns a function that manages the abort controller lifecycle.
 */
import { AppThunk } from '../store';

interface CancellableRequest {
  abort: () => void;
  signal: AbortSignal | null;
}

/**
 * Creates a new cancellable request, aborting any previous request.
 * @returns CancellableRequest object with abort function and signal
 */
const controllers = new Map<string, AbortController>();

function createCancellableRequest(key: string): CancellableRequest {
  controllers.get(key)?.abort();

  const controller = new AbortController();
  controllers.set(key, controller);

  return {
    abort: () => controller.abort(),
    signal: controller.signal
  };
}
/**
 * Checks if an error is an AbortError (request was cancelled).
 */
function isAbortError(error: any): boolean {
  return error?.name === 'AbortError';
}

/**
 * Wraps a cancellable API request with loading state management.
 * Sets loading to true before the request and false after completion,
 * unless the request was aborted (replaced by a newer request).
 *
 * @param key - Unique key for the request group (used to cancel previous requests)
 * @param dispatch - Redux dispatch function
 * @param apiCall - Function that performs the API request with the given AbortSignal
 * @param onSuccess - Callback with the response data
 * @param setLoading - Optional function to set loading state (called with true/false)
 */
export async function cancellableFetch<T>(
  dispatch: AppThunk,
  key: string,
  apiCall: (signal: AbortSignal) => Promise<T>,
  onSuccess: (data: T) => void,
  setLoading?: (loading: boolean) => void
): Promise<void> {
  const { signal } = createCancellableRequest(key);
  let isCancelled = false;
  try {
    setLoading?.(true);
    const data = await apiCall(signal);
    onSuccess(data);
  } catch (error) {
    if (isAbortError(error)) {
      isCancelled = true;
      return;
    }
    throw error;
  } finally {
    if (!isCancelled) {
      setLoading?.(false);
    }
  }
}
