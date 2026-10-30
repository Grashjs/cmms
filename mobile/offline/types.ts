import type WorkOrder from '../models/workOrder';
import type { Task } from '../models/tasks';
import type Comment from '../models/comment';

export const OFFLINE_LIMITS = Object.freeze({
  preparedWorkOrders: 25,
  tasks: 500,
  comments: 500,
  history: 500,
  snapshotItems: 2000,
  snapshotBytes: 2 * 1024 * 1024,
  receiptIds: 100
});

export type OfflineTask = Task & { version: number };
export interface OfflineSnapshot {
  workOrder: WorkOrder;
  tasks: OfflineTask[];
  comments: Comment[];
  history: unknown[];
  preparedAt: string;
  complete: true;
  canEdit: boolean;
}

export interface OfflineEligibility {
  enabled: boolean;
  eligible: boolean;
  companyId: number;
  userId: number;
  serverTime: string;
  authorizedUntil?: string;
  limits: typeof OFFLINE_LIMITS;
}

export interface OperationRequest {
  operationId: string;
  companyId: number;
  userId: number;
  workOrderId: number;
  taskId: number;
  type: 'SET_TASK_VALUE';
  baseVersion: number;
  value: string;
  occurredAt: string;
}

export interface OperationReceipt {
  operationId: string;
  status: 'APPLIED' | 'CONFLICT' | 'REJECTED';
  code?: string;
  task?: OfflineTask;
  receivedAt: string;
}

export type OperationStatus =
  | 'QUEUED'
  | 'SENDING'
  | 'UNKNOWN'
  | 'APPLIED'
  | 'CONFLICT'
  | 'REJECTED'
  | 'ABANDONED';

export interface LocalOperation {
  id: string;
  request: OperationRequest;
  status: OperationStatus;
  attemptCount: number;
  receipt: OperationReceipt | null;
  createdAt: number;
  updatedAt: number;
}

export interface TaskDraft {
  id: string;
  workOrderId: number;
  taskId: number;
  revision: number;
  value: string;
  baseValue: string;
  baseVersion: number;
  basedOnOperationId?: string;
  blockedReason?: string;
  updatedAt: number;
}

export interface TaskView {
  serverTask: OfflineTask | null;
  task: OfflineTask | null;
  draft: TaskDraft | null;
  operation: LocalOperation | null;
  available: boolean;
  unavailableReason: string | null;
  canEdit: boolean;
}

export interface PreparedWorkOrderView {
  id: number;
  workOrder: WorkOrder | null;
  tasks: OfflineTask[];
  comments: Comment[];
  history: unknown[];
  preparedAt: string;
  available: boolean;
  unavailableReason: string | null;
}

export type PreparedWorkOrderSummary = Omit<
  PreparedWorkOrderView,
  'tasks' | 'comments' | 'history'
>;

export interface PendingWork {
  kind: 'DRAFT' | 'OPERATION';
  id: string;
  workOrderId: number;
  taskId: number;
  draftRevision?: number;
  status?: OperationStatus;
  attemptCount?: number;
  targetAvailable: boolean;
  reason: string | null;
  canAbandon: boolean;
  // Never expose a withdrawn target's title, server data or local value.
  label?: string;
  value?: string;
}

export class OfflineError extends Error {
  constructor(public readonly code: string) {
    super(code);
    this.name = 'OfflineError';
  }
}

export function utf8ByteLength(value: string): number {
  let bytes = 0;
  for (let i = 0; i < value.length; i++) {
    const code = value.charCodeAt(i);
    if (code < 0x80) bytes++;
    else if (code < 0x800) bytes += 2;
    else if (
      code >= 0xd800 &&
      code <= 0xdbff &&
      i + 1 < value.length &&
      value.charCodeAt(i + 1) >= 0xdc00 &&
      value.charCodeAt(i + 1) <= 0xdfff
    ) {
      bytes += 4;
      i++;
    } else bytes += 3;
  }
  return bytes;
}

export function assertSnapshot(snapshot: OfflineSnapshot): void {
  if (
    !snapshot ||
    snapshot.complete !== true ||
    !snapshot.workOrder ||
    !Number.isSafeInteger(snapshot.workOrder.id) ||
    snapshot.workOrder.id <= 0 ||
    !Array.isArray(snapshot.tasks) ||
    !Array.isArray(snapshot.comments) ||
    !Array.isArray(snapshot.history) ||
    typeof snapshot.canEdit !== 'boolean' ||
    !Number.isFinite(Date.parse(snapshot.preparedAt))
  ) {
    throw new OfflineError('INCOMPLETE_SNAPSHOT');
  }
  if (
    snapshot.tasks.length > OFFLINE_LIMITS.tasks ||
    snapshot.comments.length > OFFLINE_LIMITS.comments ||
    snapshot.history.length > OFFLINE_LIMITS.history ||
    snapshotItemCount(snapshot) > OFFLINE_LIMITS.snapshotItems ||
    utf8ByteLength(JSON.stringify(snapshot)) > OFFLINE_LIMITS.snapshotBytes
  ) {
    throw new OfflineError('SNAPSHOT_LIMIT');
  }
  const ids = new Set<number>();
  for (const task of snapshot.tasks) {
    if (
      !task ||
      !Number.isSafeInteger(task.id) ||
      task.id <= 0 ||
      ids.has(task.id) ||
      !Number.isSafeInteger(task.version) ||
      task.version < 0 ||
      !task.taskBase
    ) {
      throw new OfflineError('INVALID_SNAPSHOT');
    }
    ids.add(task.id);
  }
}

/** Mirror the bounded entity collections included by the server snapshot. */
export function snapshotItemCount(snapshot: OfflineSnapshot): number {
  const workOrder = snapshot.workOrder as any;
  const size = (value: unknown) => (Array.isArray(value) ? value.length : 0);
  return (
    1 +
    snapshot.tasks.length * 2 +
    snapshot.comments.length +
    snapshot.history.length +
    size(workOrder.assignedTo) +
    size(workOrder.team?.userIds) +
    size(workOrder.customers) +
    size(workOrder.files) +
    size(workOrder.customFieldValues) +
    (Array.isArray(workOrder.customFieldValues)
      ? workOrder.customFieldValues
      : []
    ).reduce(
      (n: number, field: any) => n + size(field?.customField?.options),
      0
    ) +
    snapshot.tasks.reduce(
      (n, task) => n + size(task.taskBase?.options) + size(task.images),
      0
    ) +
    snapshot.comments.reduce((n, comment) => n + size(comment.files), 0)
  );
}
