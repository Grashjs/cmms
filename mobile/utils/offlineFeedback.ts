import { getErrorMessage, isOfflineQueuedError } from './errors';

type ShowSnackBar = (
  message: string,
  type: 'success' | 'error' | 'info'
) => void;

/** Maps mutation failures to either an offline-queued confirmation or an error. */
export function showMutationResult(
  error: unknown,
  showSnackBar: ShowSnackBar,
  t: (key: string) => string,
  failureKey: string
) {
  if (isOfflineQueuedError(error)) {
    showSnackBar(t('offline_queued_success'), 'info');
    return;
  }
  showSnackBar(getErrorMessage(error, t(failureKey)), 'error');
}
