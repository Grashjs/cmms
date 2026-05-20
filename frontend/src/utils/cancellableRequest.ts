/**
 * Creates a cancellable API request wrapper using AbortController.
 * Returns a function that manages the abort controller lifecycle.
 */

interface CancellableRequest {
  abort: () => void;
  signal: AbortSignal | null;
}

/**
 * Creates a new cancellable request, aborting any previous request.
 * @returns CancellableRequest object with abort function and signal
 */
const controllers = new Map<string, AbortController>();

export function createCancellableRequest(key: string): CancellableRequest {
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
export function isAbortError(error: any): boolean {
  return error?.name === 'AbortError';
}
