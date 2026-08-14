import type { AppDispatch } from '../store';
import {
  OfflineMutation,
  OfflineMutationInput,
  applyOptimisticMutation,
  createMutationId,
  enqueueOfflineMutation
} from './offlineQueue';
import { isNetworkError, OfflineQueuedError } from './errors';

/**
 * Runs an API mutation or, on a network failure, persists it for later sync.
 * Callers should treat {@link OfflineQueuedError} as success-with-deferral.
 */
export async function runOrQueue<T>(
  dispatch: AppDispatch,
  execute: () => Promise<T>,
  mutation: OfflineMutationInput
): Promise<T> {
  try {
    return await execute();
  } catch (error) {
    if (!isNetworkError(error)) throw error;
    const item = { ...mutation, id: createMutationId() } as OfflineMutation;
    applyOptimisticMutation(item, dispatch);
    await enqueueOfflineMutation(item, dispatch);
    throw new OfflineQueuedError();
  }
}
