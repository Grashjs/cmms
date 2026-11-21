import { useEffect, useState } from 'react';
import { useOffline } from './OfflineProvider';
import type { OfflineAsset } from './types';
import type { OfflineRepository } from './OfflineRepository';
import { sessionGeneration, sessionChangeInProgress } from './sessionBridge';

/** Never show the previous account's asset while a new subscription is starting. */
export function useOfflineAsset(id: number) {
  const offline = useOffline();
  const [state, setState] = useState<{
    repository: OfflineRepository;
    id: number;
    value: OfflineAsset | null;
  } | null>(null);
  useEffect(() => {
    setState(null);
    const repository = offline.repository;
    if (!repository || !offline.readable) return;
    const generation = sessionGeneration();
    const subscription = repository.observeAsset(id).subscribe({
      next: (value) => {
        if (generation === sessionGeneration() && !sessionChangeInProgress())
          setState({ repository, id, value });
      },
      error: () => setState({ repository, id, value: null })
    });
    return () => subscription.unsubscribe();
  }, [offline.repository, offline.readable, id]);
  const current =
    offline.readable &&
    state?.repository === offline.repository &&
    state?.id === id;
  return {
    asset: current ? state.value : null,
    loading: offline.readable && !!offline.repository && !current
  };
}
