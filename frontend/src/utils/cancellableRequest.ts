/**
 * Creates a cancellable API request wrapper using AbortController.
 * Returns a function that manages the abort controller lifecycle.
 */

interface CancellableRequest {
  abort: () => void;
  signal: AbortSignal | null;
}

let currentController: AbortController | null = null;

/**
 * Creates a new cancellable request, aborting any previous request.
 * @returns CancellableRequest object with abort function and signal
 */
export function createCancellableRequest(): CancellableRequest {
  // Abort previous request if it exists
  if (currentController) {
    currentController.abort();
  }

  // Create new controller
  currentController = new AbortController();

  return {
    abort: () => currentController?.abort(),
    signal: currentController.signal
  };
}

/**
 * Checks if an error is an AbortError (request was cancelled).
 */
export function isAbortError(error: any): boolean {
  return error?.name === 'AbortError';
}
