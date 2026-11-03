import type WorkOrder from '../models/workOrder';
import type { Task } from '../models/tasks';
import type Comment from '../models/comment';
import type { AssetDTO } from '../models/asset';
import type Labor from '../models/labor';

export const OFFLINE_LIMITS = Object.freeze({
  preparedWorkOrders: 25,
  tasks: 500,
  comments: 500,
  history: 500,
  snapshotItems: 2000,
  snapshotBytes: 2 * 1024 * 1024,
  receiptIds: 100,
  fileBytes: 35 * 1024 * 1024,
  localFileBytes: 500 * 1024 * 1024,
  freeDiskReserve: 100 * 1024 * 1024
});

export type OfflineTask = Task & { version: number };
export type OfflineAsset = Pick<
  AssetDTO,
  | 'id'
  | 'name'
  | 'customId'
  | 'description'
  | 'status'
  | 'model'
  | 'serialNumber'
  | 'manufacturer'
  | 'power'
  | 'area'
> & { barCode?: string; nfcId?: string };
export type OfflineLabor = Labor & {
  version: number;
  timerRunId?: string | null;
};
export interface TimePermissions {
  createTimer: boolean;
  resumeTimer: boolean;
  stopTimer: boolean;
  createManualTime: boolean;
}
export const NO_TIME_PERMISSIONS: TimePermissions = Object.freeze({
  createTimer: false,
  resumeTimer: false,
  stopTimer: false,
  createManualTime: false
});
export interface OfflineSnapshot {
  workOrder: WorkOrder;
  tasks: OfflineTask[];
  comments: Comment[];
  history: unknown[];
  preparedAt: string;
  complete: true;
  canEdit: boolean;
  canComment?: boolean;
  assets?: OfflineAsset[];
  canScan?: boolean;
  canReadAssets?: boolean;
  labors?: OfflineLabor[];
  timePermissions?: TimePermissions;
  timerStatusRevision?: number;
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

interface OperationIdentity {
  operationId: string;
  companyId: number;
  userId: number;
  workOrderId: number;
  occurredAt: string;
}

export type TaskField = 'value' | 'notes';
type TaskOperationIdentity = OperationIdentity & {
  taskId: number;
  baseVersion: number;
  predecessorOperationId?: string;
};
export interface TimePayload {
  periodId: string;
  startedAt: string;
  endedAt?: string;
  laborId?: number;
  laborVersion?: number;
  timerRunId?: string;
  timerStatusRevision?: number;
  preserveStatus?: boolean;
  startOperation?: TimerStartOperation;
}
export type TimerStartOperation = OperationIdentity & {
  type: 'START_TIMER';
  time: TimePayload;
  predecessorOperationId?: string;
};
export type TimeOperation =
  | TimerStartOperation
  | (OperationIdentity & {
      type: 'STOP_TIMER' | 'CREATE_MANUAL_TIME';
      time: TimePayload;
      predecessorOperationId?: string;
    });
export type OperationRequest =
  | TimeOperation
  | (TaskOperationIdentity & { type: 'SET_TASK_VALUE'; value: string })
  | (TaskOperationIdentity & { type: 'SET_TASK_NOTES'; notes: string })
  | (OperationIdentity & {
      type: 'CREATE_COMMENT';
      content: string;
      fileTransferIds?: string[];
    })
  | (OperationIdentity & {
      type: 'UPLOAD_FILE';
      target: 'WORK_ORDER' | 'TASK' | 'COMMENT';
      taskId?: number;
      name: string;
      mimeType: string;
      size: number;
      sha256: string;
      fileType: 'IMAGE' | 'OTHER';
    })
  | (OperationIdentity & {
      type: 'ADD_TASK_IMAGES';
      taskId: number;
      fileTransferIds: string[];
    })
  | (OperationIdentity & {
      type: 'UPDATE_WORK_ORDER_ATTACHMENTS';
      addTransferIds: string[];
      imageChange?: { transferId: string; baseImageId: number | null };
    });

export function isTaskOperation(
  request: OperationRequest
): request is Extract<
  OperationRequest,
  { type: 'SET_TASK_VALUE' | 'SET_TASK_NOTES' }
> {
  return request.type === 'SET_TASK_VALUE' || request.type === 'SET_TASK_NOTES';
}
export function isTimeOperation(
  request: OperationRequest
): request is TimeOperation {
  return ['START_TIMER', 'STOP_TIMER', 'CREATE_MANUAL_TIME'].includes(
    request.type
  );
}
export function transferIds(request: OperationRequest): string[] {
  if (request.type === 'CREATE_COMMENT' || request.type === 'ADD_TASK_IMAGES')
    return request.fileTransferIds || [];
  if (request.type === 'UPDATE_WORK_ORDER_ATTACHMENTS')
    return [
      ...request.addTransferIds,
      ...(request.imageChange ? [request.imageChange.transferId] : [])
    ];
  return [];
}

export function operationField(
  request: OperationRequest
): TaskField | 'comment' | 'attachments' | 'time' {
  return isTimeOperation(request)
    ? 'time'
    : request.type === 'CREATE_COMMENT'
    ? 'comment'
    : request.type === 'SET_TASK_NOTES'
    ? 'notes'
    : request.type === 'SET_TASK_VALUE'
    ? 'value'
    : 'attachments';
}
export function operationText(request: OperationRequest): string {
  return isTimeOperation(request)
    ? [request.time.startedAt, request.time.endedAt].filter(Boolean).join(' – ')
    : request.type === 'CREATE_COMMENT'
    ? request.content
    : request.type === 'SET_TASK_NOTES'
    ? request.notes
    : request.type === 'SET_TASK_VALUE'
    ? request.value
    : request.type === 'UPLOAD_FILE'
    ? request.name
    : '';
}

export interface TimeResult {
  periodId: string;
  laborId: number;
  laborVersion: number;
  timerRunId?: string | null;
  startedAt: string;
  endedAt?: string;
  durationAdded: number;
  closedWithoutActivation?: boolean;
}
export interface OperationReceipt {
  operationId: string;
  status: 'APPLIED' | 'CONFLICT' | 'REJECTED';
  code?: string;
  task?: OfflineTask;
  chainBaseVersion?: number;
  commentId?: number;
  fileId?: number;
  fileIds?: number[];
  imageId?: number | null;
  timeResult?: TimeResult;
  startReceipt?: OperationReceipt;
  visibility?:
    | 'VISIBLE'
    | 'COMMENT_DELETED'
    | 'WORK_ORDER_UNAVAILABLE'
    | 'FILE_DELETED'
    | 'LABOR_DELETED'
    | 'TARGET_UNAVAILABLE';
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
  submittedRevision: number | null;
  serverCommentId: number | null;
  snapshotConfirmed?: boolean;
  blockedReason?: string;
  // Read-only projection; never persisted in the immutable request.
  canReviewTime?: boolean;
}

export interface TaskDraft {
  id: string;
  workOrderId: number;
  taskId: number;
  field?: TaskField;
  revision: number;
  value: string;
  baseValue: string;
  baseVersion: number;
  basedOnOperationId?: string;
  blockedReason?: string;
  updatedAt: number;
}

export interface CommentDraft {
  id: string;
  kind: 'COMMENT';
  workOrderId: number;
  revision: number;
  content: string;
  fileIds?: string[];
  // Retain legacy attachment intent until the user selects its actual files.
  attachmentPending?: boolean;
  updatedAt: number;
}

export interface ObservedServerTimer {
  laborId: number;
  laborVersion: number;
  timerRunId: string;
  startedAt: string;
}

export interface TimerDraft {
  id: string;
  kind: 'TIMER';
  workOrderId: number;
  periodId: string;
  revision: number;
  startedAt: string;
  endedAt?: string;
  startOperationId?: string;
  laborId?: number;
  laborVersion?: number;
  timerRunId?: string;
  updatedAt: number;
  reviewOperationId?: string;
  predecessorOperationId?: string;
  timerStatusRevision?: number;
  preserveStatus?: boolean;
}
export interface ManualTimeDraft {
  id: string;
  kind: 'MANUAL_TIME';
  periodId: string;
  workOrderId: number;
  revision: number;
  startedAt: string;
  hours: string;
  minutes: string;
  updatedAt: number;
}
export type ManualTimeInput = Pick<
  ManualTimeDraft,
  'startedAt' | 'hours' | 'minutes'
>;
export interface OfflineTimeView {
  available: boolean;
  labors: OfflineLabor[];
  permissions: TimePermissions;
  timerStatusRevision: number;
  activePeriod: TimerDraft | null;
  serverTimer: OfflineLabor | null;
  manualDraft: ManualTimeDraft | null;
  operations: LocalOperation[];
}

export interface FileTarget {
  kind: 'COMMENT' | 'TASK' | 'WORK_ORDER';
  taskId?: number;
  image?: boolean;
}
export interface LocalFile {
  id: string;
  name: string;
  mimeType: string;
  uri: string;
  size: number;
  sha256: string;
  status: 'IMPORTING' | 'READY' | 'ERROR' | 'DELETING';
  error?: string;
  serverFileId?: number;
  transferStatus?: OperationStatus;
  transferIds?: string[];
  workOrderIds: number[];
  // The import intent is durable before touching the filesystem. Its identity
  // is never reused for another account, destination or byte content.
  target?: FileTarget;
  importWorkOrderId?: number;
  sourceUri?: string;
  temporaryUri?: string;
}
export interface AttachmentDraft {
  id: string;
  kind: 'ATTACHMENTS';
  workOrderId: number;
  taskId?: number;
  revision: number;
  fileIds: string[];
  imageFileId?: string;
  baseImageId: number | null;
  updatedAt: number;
}
export interface DownloadDescriptor {
  fileId: number;
  name: string;
  mimeType: string;
  size: number;
  url: string;
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
  commentDraft: CommentDraft | null;
  commentOperations: LocalOperation[];
  canEdit: boolean;
  canComment: boolean;
  labors: OfflineLabor[];
  timePermissions: TimePermissions;
  timerStatusRevision: number;
  history: unknown[];
  preparedAt: string;
  available: boolean;
  unavailableReason: string | null;
}

export type PreparedWorkOrderSummary = Omit<
  PreparedWorkOrderView,
  | 'tasks'
  | 'comments'
  | 'history'
  | 'commentDraft'
  | 'commentOperations'
  | 'canEdit'
  | 'canComment'
  | 'labors'
  | 'timePermissions'
  | 'timerStatusRevision'
>;

export interface PendingWork {
  operationType?: OperationRequest['type'];
  kind: 'DRAFT' | 'OPERATION';
  id: string;
  workOrderId: number;
  taskId?: number;
  field: TaskField | 'comment' | 'attachments' | 'time';
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
  if (snapshot.labors !== undefined) {
    if (
      !Array.isArray(snapshot.labors) ||
      !Number.isSafeInteger(snapshot.timerStatusRevision) ||
      snapshot.timerStatusRevision! < 0 ||
      !snapshot.timePermissions
    )
      throw new OfflineError('INVALID_SNAPSHOT');
    const laborIds = new Set<number>();
    for (const labor of snapshot.labors) {
      if (
        !labor ||
        !Number.isSafeInteger(labor.id) ||
        labor.id <= 0 ||
        laborIds.has(labor.id) ||
        !Number.isSafeInteger(labor.version) ||
        labor.version < 0 ||
        !Number.isFinite(labor.duration) ||
        !Number.isFinite(Date.parse(labor.startedAt)) ||
        (labor.status === 'RUNNING' && !labor.timerRunId)
      )
        throw new OfflineError('INVALID_SNAPSHOT');
      laborIds.add(labor.id);
    }
    if (
      Object.keys(NO_TIME_PERMISSIONS).some(
        (key) => typeof snapshot.timePermissions![key] !== 'boolean'
      )
    )
      throw new OfflineError('INVALID_SNAPSHOT');
  }
  const commentIds = new Set<number>();
  for (const comment of snapshot.comments) {
    if (
      !comment ||
      !Number.isSafeInteger(comment.id) ||
      comment.id <= 0 ||
      commentIds.has(comment.id)
    )
      throw new OfflineError('INVALID_SNAPSHOT');
    commentIds.add(comment.id);
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
    size(snapshot.assets) +
    2 * size(snapshot.labors) +
    (snapshot.labors || []).filter((labor) => !!labor.timeCategory).length +
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
