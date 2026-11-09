import { OfflineError } from './types';
type SessionChangeReason = 'logout' | 'account' | 'server' | 'login';
interface OfflineLifecycle {
  beforeContextChange(reason: SessionChangeReason): Promise<void>;
}

const listeners = new Set<OfflineLifecycle>();
let generation = 0;
let changing = false;

export const sessionGeneration = () => generation;
export const sessionChangeInProgress = () => changing;

export function registerOfflineLifecycle(
  lifecycle: OfflineLifecycle
): () => void {
  listeners.add(lifecycle);
  return () => {
    listeners.delete(lifecycle);
  };
}

/** Flush durable drafts and freeze sync before changing any identity or URL. */
export async function beforeSessionChange(
  reason: SessionChangeReason
): Promise<void> {
  if (changing) throw new OfflineError('CONTEXT_BUSY');
  changing = true;
  try {
    for (const listener of listeners)
      await listener.beforeContextChange(reason);
    generation += 1;
  } catch (error) {
    changing = false;
    throw error;
  }
}

export function finishSessionChange(): void {
  changing = false;
}

export function assertSessionGeneration(expected: number): void {
  if (expected !== generation || changing)
    throw new OfflineError('CONTEXT_CHANGED');
}
