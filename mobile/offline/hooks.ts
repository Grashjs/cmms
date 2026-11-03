import { useEffect, useState } from 'react';
import { useOffline } from './OfflineProvider';
import type {
  PreparedWorkOrderView,
  TaskView,
  PendingWork,
  TaskField
} from './types';
import { NO_TIME_PERMISSIONS } from './types';
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
  if (!offline.readable && value) {
    return {
      ...value,
      available: false,
      workOrder: null,
      tasks: [],
      comments: [],
      labors: [],
      timePermissions: NO_TIME_PERMISSIONS,
      timerStatusRevision: 0,
      commentDraft: null,
      commentOperations: [],
      canEdit: false,
      canComment: false,
      history: [],
      unavailableReason: 'OFFLINE_DISABLED'
    };
  }
  return value;
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
    const subscription = offline.repository
      .observePendingWork(workOrderId)
      .subscribe({
        next: setValue,
        error: () => setValue([])
      });
    return () => subscription.unsubscribe();
  }, [offline.repository, workOrderId]);
  return offline.readable
    ? value
    : value.map(({ label, value: text, ...item }) => item);
}
