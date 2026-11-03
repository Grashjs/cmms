import type { Task } from '../models/tasks';
import type WorkOrder from '../models/workOrder';
import type Comment from '../models/comment';

/** Narrow bridge for existing connected thunks. Never exposes a draft as a payload. */
interface PreparedRuntime {
  hasPreparation: (workOrderId: number) => boolean;
  refresh: (workOrderId: number) => Promise<void>;
  patchTask: (
    workOrderId: number,
    taskId: number,
    patch: {
      value?: string | number;
      notes?: string;
    }
  ) => Promise<Task>;
  ingestTask: (workOrderId: number, task: Task) => Promise<void>;
  ingestWorkOrder: (workOrder: WorkOrder) => Promise<void>;
  ingestComment: (workOrderId: number, comment: Comment) => Promise<void>;
  removeComment: (workOrderId: number, commentId: number) => Promise<void>;
  runConnectedComment: <T>(
    workOrderId: number,
    action: () => Promise<T>
  ) => Promise<T>;
  runConnectedLabor: <T>(
    workOrderId: number,
    action: () => Promise<T>
  ) => Promise<T>;
  markUnavailable: (workOrderId: number, reason: string) => Promise<void>;
}

let activeRuntime: PreparedRuntime | null = null;

export function setPreparedRuntime(runtime: PreparedRuntime | null) {
  activeRuntime = runtime;
}

export function getPreparedRuntime(
  workOrderId?: number
): PreparedRuntime | null {
  return activeRuntime &&
    (workOrderId === undefined || activeRuntime.hasPreparation(workOrderId))
    ? activeRuntime
    : null;
}

/** Explicit allowlist: display projections must never become PATCH requests. */
export function taskPatch(
  field: 'value' | 'notes',
  value: string | number,
  version?: number
) {
  return { [field]: value, ...(version === undefined ? {} : { version }) };
}
