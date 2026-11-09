import { useEffect, useState } from 'react';
import { useOffline } from './OfflineProvider';
import type {
  PreparedWorkOrderView,
  TaskView,
  PendingWork,
  TaskField
} from './types';
import type WorkOrder from '../models/workOrder';

/** Related connected lists retain IDs; prepared entities are projected from SQLite. */
export function useProjectedWorkOrders(
  connected: WorkOrder[],
  preparedIds: number[]
) {
  const offline = useOffline();
  return [
    ...connected,
    ...(offline.readable
      ? offline.prepared
          .filter(
            (item) =>
              preparedIds.includes(item.id) && item.available && item.workOrder
          )
          .map((item) => item.workOrder)
      : [])
  ];
}

export function useOfflineWorkOrder(id: number) {
  const offline = useOffline();
  const [value, setValue] = useState<PreparedWorkOrderView | null>(null);
  useEffect(() => {
    setValue(null);
    if (!offline.repository) return;
    const subscription = offline.repository.observeWorkOrder(id).subscribe({
      next: setValue,
      error: () => setValue(null)
    });
    return () => subscription.unsubscribe();
  }, [offline.repository, id]);
  return offline.readable ? value : null;
}

export function useOfflineTask(id: number, field: TaskField = 'value') {
  const offline = useOffline();
  const [value, setValue] = useState<TaskView | null>(null);
  useEffect(() => {
    setValue(null);
    if (!offline.repository) return;
    const subscription = offline.repository.observeTask(id, field).subscribe({
      next: setValue,
      error: () => setValue(null)
    });
    return () => subscription.unsubscribe();
  }, [offline.repository, id, field]);
  return offline.readable ? value : null;
}

export function usePendingWork(workOrderId?: number): PendingWork[] {
  const offline = useOffline();
  const [value, setValue] = useState<PendingWork[]>([]);
  useEffect(() => {
    setValue([]);
    if (!offline.repository) return;
    try {
      const subscription = offline.repository
        .observePendingWork(workOrderId)
        .subscribe({
          next: setValue,
          error: () => setValue([])
        });
      return () => subscription.unsubscribe();
    } catch (failure) {
      // The preference can close the repository between render and this effect.
      if ((failure as { code?: string })?.code !== 'OFFLINE_DISABLED')
        throw failure;
    }
  }, [offline.repository, workOrderId]);
  return offline.readable ? value : [];
}
