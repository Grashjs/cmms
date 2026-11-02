import { Database, Model, Q } from '@nozbe/watermelondb';
import { combineLatest } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import type WorkOrder from '../models/workOrder';
import type Comment from '../models/comment';
import {
  assertSnapshot,
  LocalOperation,
  OfflineError,
  OfflineSnapshot,
  OfflineTask,
  OFFLINE_LIMITS,
  OperationReceipt,
  OperationRequest,
  OperationStatus,
  PendingWork,
  PreparedWorkOrderSummary,
  PreparedWorkOrderView,
  TaskDraft,
  TaskView,
  TaskField,
  CommentDraft,
  operationField,
  operationText,
  utf8ByteLength,
  isTaskOperation,
  transferIds,
  AttachmentDraft,
  FileTarget,
  LocalFile,
  OfflineAsset
} from './types';

const WORK_ORDERS = 'offline_work_orders';
const TASKS = 'offline_tasks';
const DRAFTS = 'offline_drafts';
const OPERATIONS = 'offline_operations';
const FILES = 'offline_files';
const ASSETS = 'offline_assets';
const attachmentDraftId = (workOrderId: number, taskId?: number) =>
  taskId == null
    ? `work-order:${workOrderId}:attachments`
    : `task:${taskId}:attachments`;
type AssetAccess = { observedAt: string; canRead: boolean; canScan: boolean };
type AssetCopy = {
  asset: OfflineAsset;
  refs: number[];
  observedAt: string;
  available: boolean;
};

const UNRESOLVED: OperationStatus[] = [
  'QUEUED',
  'SENDING',
  'UNKNOWN',
  'CONFLICT',
  'REJECTED'
];
const ACTIVE: OperationStatus[] = ['QUEUED', 'SENDING', 'UNKNOWN'];
const failedDatabases = new WeakSet<Database>();
const raw = (row: Model, key: string) => row._getRaw(key);
const json = <T>(row: Model, key = 'payload'): T =>
  JSON.parse(String(raw(row, key)));
const draftId = (taskId: number, field: TaskField = 'value') =>
  `${taskId}:${field}`;
const commentDraftId = (workOrderId: number) =>
  `work-order:${workOrderId}:comment`;

interface RepositoryOptions {
  uuid: () => string;
  now?: () => number;
}

export class OfflineRepository {
  private now: () => number;

  constructor(
    public readonly database: Database,
    public readonly contextKey: string,
    private readonly options: RepositoryOptions
  ) {
    this.now = options.now || Date.now;
  }

  private collection(name: string) {
    return this.database.get<Model>(name);
  }
  private async find(table: string, id: string): Promise<Model | null> {
    const rows = await this.collection(table).query(Q.where('id', id)).fetch();
    return rows[0] || null;
  }
  private assertStorage() {
    if (failedDatabases.has(this.database))
      throw new OfflineError('STORAGE_FAILED');
  }
  private async commit(records: Model[]) {
    try {
      await this.database.batch(records);
    } catch (error) {
      // A failed Watermelon batch may leave prepared in-memory models changed.
      // Never let that memory state be mistaken for a durable confirmation.
      failedDatabases.add(this.database);
      throw new OfflineError('STORAGE_FAILED');
    }
  }
  private create(
    table: string,
    id: string,
    fields: Record<string, string | number | boolean | null>
  ) {
    return this.collection(table).prepareCreate((record) => {
      record._raw.id = id;
      for (const [key, value] of Object.entries(fields))
        record._setRaw(key, value);
    });
  }
  private change(
    row: Model,
    fields: Record<string, string | number | boolean | null>
  ) {
    return row.prepareUpdate((record) => {
      for (const [key, value] of Object.entries(fields))
        record._setRaw(key, value);
    });
  }
  private async nextDraftRevision(): Promise<{
    revision: number;
    row: Model | null;
  }> {
    const clock = await this.find('offline_draft_clock', 'revision');
    const revision = clock ? Number(raw(clock, 'revision')) + 1 : 1;
    if (!Number.isSafeInteger(revision))
      throw new OfflineError('REVISION_LIMIT');
    return { revision, row: clock };
  }
  private clockChange(clock: { revision: number; row: Model | null }) {
    return clock.row
      ? this.change(clock.row, { revision: clock.revision })
      : this.create('offline_draft_clock', 'revision', {
          revision: clock.revision
        });
  }
  private operation(row: Model): LocalOperation {
    return {
      id: row.id,
      request: json<OperationRequest>(row, 'request'),
      status: raw(row, 'status') as OperationStatus,
      attemptCount: Number(raw(row, 'attempt_count')),
      receipt: raw(row, 'receipt')
        ? json<OperationReceipt>(row, 'receipt')
        : null,
      submittedRevision: raw(row, 'submitted_revision') as number | null,
      serverCommentId: raw(row, 'server_comment_id') as number | null,
      snapshotConfirmed: raw(row, 'snapshot_confirmed') === true,
      createdAt: Number(raw(row, 'created_at')),
      updatedAt: Number(raw(row, 'updated_at'))
    };
  }
  private async unresolvedRows(taskId?: number, workOrderId?: number) {
    const rows = await this.collection(OPERATIONS)
      .query(
        Q.where('status', Q.oneOf(UNRESOLVED)),
        ...(taskId === undefined ? [] : [Q.where('task_id', String(taskId))]),
        ...(workOrderId === undefined
          ? []
          : [Q.where('work_order_id', String(workOrderId))]),
        Q.sortBy('created_at', Q.asc)
      )
      .fetch();
    return rows
      .filter(
        (row) =>
          taskId === undefined || isTaskOperation(this.operation(row).request)
      )
      .sort((a, b) => {
        const ar = raw(a, 'submitted_revision'),
          br = raw(b, 'submitted_revision');
        return ar != null && br != null
          ? Number(ar) - Number(br)
          : Number(raw(a, 'created_at')) - Number(raw(b, 'created_at'));
      });
  }
  private async usableTask(
    workOrderId: number,
    taskId: number,
    editing = false,
    field: TaskField = 'value'
  ) {
    const [workOrder, task] = await Promise.all([
      this.find(WORK_ORDERS, String(workOrderId)),
      this.find(TASKS, String(taskId))
    ]);
    if (
      !workOrder ||
      !raw(workOrder, 'available') ||
      !task ||
      !raw(task, 'available') ||
      raw(task, 'work_order_id') !== String(workOrderId)
    ) {
      throw new OfflineError('TARGET_UNAVAILABLE');
    }
    if (editing && json<OfflineSnapshot>(workOrder).canEdit !== true) {
      throw new OfflineError('EDIT_FORBIDDEN');
    }
    const server = json<OfflineTask>(task);
    if (
      editing &&
      field === 'value' &&
      server.taskBase.user &&
      server.taskBase.user.id !== JSON.parse(this.contextKey)[2]
    )
      throw new OfflineError('EDIT_FORBIDDEN');
    return { workOrder, task, server };
  }

  async getAvailabilityRevision(workOrderId: number): Promise<number> {
    const row = await this.find(WORK_ORDERS, String(workOrderId));
    return row ? Number(raw(row, 'availability_revision')) : 0;
  }

  async beginPreparation(workOrderId: number): Promise<string | null> {
    this.assertStorage();
    return this.database.write(async () => {
      if (await this.find(WORK_ORDERS, String(workOrderId))) return null;
      if (await this.find('offline_cleanup', String(workOrderId))) {
        throw new OfflineError('REMOVAL_PENDING');
      }
      const token = this.options.uuid();
      await this.commit([
        this.create('offline_cleanup', String(workOrderId), {
          created_at: this.now(),
          kind: 'PREPARATION',
          token
        })
      ]);
      return token;
    });
  }

  async prepareSnapshot(
    snapshot: OfflineSnapshot,
    expectedAvailabilityRevision?: number,
    preparationToken?: string | null
  ): Promise<void> {
    assertSnapshot(snapshot);
    this.assertStorage();
    await this.database.write(async () => {
      const workOrderId = String(snapshot.workOrder.id);
      const [existing, count, oldTasks] = await Promise.all([
        this.find(WORK_ORDERS, workOrderId),
        this.collection(WORK_ORDERS).query().fetchCount(),
        this.collection(TASKS)
          .query(Q.where('work_order_id', workOrderId))
          .fetch()
      ]);
      if (!existing && count >= OFFLINE_LIMITS.preparedWorkOrders) {
        throw new OfflineError('PREPARED_LIMIT');
      }
      if (
        (expectedAvailabilityRevision !== undefined &&
          expectedAvailabilityRevision !==
            Number(existing ? raw(existing, 'availability_revision') : 0)) ||
        (expectedAvailabilityRevision === undefined &&
          existing &&
          !raw(existing, 'available'))
      ) {
        throw new OfflineError('STALE_PREPARATION');
      }
      const cleanup = await this.find('offline_cleanup', workOrderId);
      const ownsIntent =
        !!cleanup &&
        raw(cleanup, 'kind') === 'PREPARATION' &&
        raw(cleanup, 'token') === preparationToken;
      if ((cleanup && !ownsIntent) || (preparationToken && !ownsIntent)) {
        throw new OfflineError('REMOVAL_PENDING');
      }
      if (
        existing &&
        Date.parse(String(raw(existing, 'prepared_at'))) >
          Date.parse(snapshot.preparedAt)
      )
        return;
      const previous = new Map(oldTasks.map((task) => [task.id, task]));
      // Read/validate all identities before preparing any model changes.
      for (const task of snapshot.tasks) {
        const other =
          previous.get(String(task.id)) ||
          (await this.find(TASKS, String(task.id)));
        if (other && raw(other, 'work_order_id') !== workOrderId) {
          throw new OfflineError('TASK_IDENTITY');
        }
      }
      const associations: { row: Model; commentId: number }[] = [];
      const seenOperations = new Set<string>();
      for (const comment of snapshot.comments) {
        if (!comment.sourceOperationId) continue;
        const sourceKey = `${comment.user?.id}:${comment.sourceOperationId}`;
        if (seenOperations.has(sourceKey))
          throw new OfflineError('INVALID_SNAPSHOT');
        seenOperations.add(sourceKey);
        const row = await this.find(OPERATIONS, comment.sourceOperationId);
        if (!row) continue;
        const operation = this.operation(row);
        if (comment.user?.id !== operation.request.userId) continue;
        if (
          operation.request.type !== 'CREATE_COMMENT' ||
          operation.request.workOrderId !== snapshot.workOrder.id ||
          (operation.serverCommentId != null &&
            operation.serverCommentId !== comment.id) ||
          (operation.receipt?.commentId != null &&
            operation.receipt.commentId !== comment.id)
        )
          throw new OfflineError('INVALID_SNAPSHOT');
        if (operation.serverCommentId == null)
          associations.push({ row, commentId: comment.id });
      }
      const records: Model[] = [
        ...(await this.snapshotFileChanges(snapshot)),
        ...(await this.snapshotAssetChanges(snapshot))
      ];
      associations.forEach(({ row, commentId }) =>
        records.push(this.change(row, { server_comment_id: commentId }))
      );
      if (ownsIntent) records.push(cleanup!.prepareDestroyPermanently());
      // Tasks live in their own table; do not maintain a second mutable copy.
      const workOrderFields = {
        payload: JSON.stringify({ ...snapshot, tasks: [], assets: [] }),
        prepared_at: snapshot.preparedAt,
        available: true,
        unavailable_reason: null,
        availability_revision: Number(
          existing ? raw(existing, 'availability_revision') : 0
        )
      };
      records.push(
        existing
          ? this.change(existing, workOrderFields)
          : this.create(WORK_ORDERS, workOrderId, workOrderFields)
      );
      for (const task of snapshot.tasks) {
        const id = String(task.id);
        const prior = previous.get(id);
        const priorServer = prior ? json<OfflineTask>(prior) : null;
        // A late snapshot must not regress a task confirmed by an HTTP patch/receipt.
        const canonical =
          priorServer && priorServer.version > task.version
            ? priorServer
            : task;
        const fields = {
          work_order_id: workOrderId,
          payload: JSON.stringify(canonical),
          available: true,
          unavailable_reason: null
        };
        records.push(
          prior ? this.change(prior, fields) : this.create(TASKS, id, fields)
        );
        previous.delete(id);
      }
      for (const missing of previous.values()) {
        records.push(
          this.change(missing, {
            available: false,
            unavailable_reason: 'TASK_REMOVED'
          })
        );
      }
      // Drafts/operations retain their original base and identities, including
      // those attached to missing tasks. No matching by label is ever performed.
      await this.commit(records);
    });
  }

  async markUnavailable(workOrderId: number, reason: string): Promise<void> {
    this.assertStorage();
    await this.database.write(async () => {
      const workOrder = await this.find(WORK_ORDERS, String(workOrderId));
      if (!workOrder) return;
      const tasks = await this.collection(TASKS)
        .query(Q.where('work_order_id', String(workOrderId)))
        .fetch();
      await this.commit([
        this.change(workOrder, {
          available: false,
          unavailable_reason: reason,
          availability_revision:
            Number(raw(workOrder, 'availability_revision')) + 1
        }),
        ...tasks.map((task) =>
          this.change(task, { available: false, unavailable_reason: reason })
        )
      ]);
    });
  }

  async ingestServerTask(
    workOrderId: number,
    task: OfflineTask
  ): Promise<void> {
    this.assertStorage();
    if (!Number.isSafeInteger(task.version) || task.version < 0)
      throw new OfflineError('INVALID_TASK');
    await this.database.write(async () => {
      const existing = await this.find(TASKS, String(task.id));
      if (!existing || raw(existing, 'work_order_id') !== String(workOrderId))
        return;
      if (json<OfflineTask>(existing).version > task.version) return;
      // Never revive an unavailable target from a late response or rebase drafts.
      await this.commit([
        this.change(existing, { payload: JSON.stringify(task) })
      ]);
    });
  }

  private async updateCanonicalWorkOrder(
    workOrderId: number,
    update: (snapshot: OfflineSnapshot) => OfflineSnapshot | null
  ): Promise<void> {
    this.assertStorage();
    await this.database.write(async () => {
      const row = await this.find(WORK_ORDERS, String(workOrderId));
      if (!row || !raw(row, 'available')) return;
      const tasks = await this.collection(TASKS)
        .query(
          Q.where('work_order_id', String(workOrderId)),
          Q.where('available', true)
        )
        .fetch();
      const assets = (
        await this.collection(ASSETS)
          .query(Q.where('id', Q.notEq('access')))
          .fetch()
      )
        .map((asset) => json<AssetCopy>(asset))
        .filter((copy) => copy.available && copy.refs.includes(workOrderId))
        .map((copy) => copy.asset);
      const next = update({
        ...json<OfflineSnapshot>(row),
        assets,
        tasks: tasks.map((task) => json<OfflineTask>(task))
      });
      if (!next) return;
      // Keep the previous complete copy if a connected response exceeds offline
      // capacity. The caller must distinguish server success from local refresh.
      assertSnapshot(next);
      await this.commit([
        this.change(row, {
          payload: JSON.stringify({ ...next, tasks: [], assets: [] })
        })
      ]);
    });
  }

  async ingestServerWorkOrder(workOrder: WorkOrder): Promise<void> {
    return this.updateCanonicalWorkOrder(workOrder.id, (snapshot) => {
      const previous = snapshot.workOrder as WorkOrder & { version?: number };
      const next = workOrder as WorkOrder & { version?: number };
      if (
        typeof previous.version === 'number' &&
        typeof next.version === 'number' &&
        previous.version > next.version
      )
        return null;
      return { ...snapshot, workOrder };
    });
  }

  async ingestServerComment(
    workOrderId: number,
    comment: Comment
  ): Promise<void> {
    return this.updateCanonicalWorkOrder(workOrderId, (snapshot) => {
      const index = snapshot.comments.findIndex(
        (existing) => existing.id === comment.id
      );
      const comments = snapshot.comments.slice();
      if (index >= 0) comments[index] = comment;
      else comments.push(comment);
      return { ...snapshot, comments };
    });
  }

  async removeServerComment(
    workOrderId: number,
    commentId: number
  ): Promise<void> {
    return this.updateCanonicalWorkOrder(workOrderId, (snapshot) => ({
      ...snapshot,
      comments: snapshot.comments.filter((comment) => comment.id !== commentId)
    }));
  }

  private identity() {
    const context = JSON.parse(this.contextKey);
    if (
      !Array.isArray(context) ||
      !Number.isSafeInteger(context[1]) ||
      !Number.isSafeInteger(context[2])
    )
      throw new OfflineError('INVALID_CONTEXT');
    return { companyId: context[1] as number, userId: context[2] as number };
  }

  private validateTaskInput(
    server: OfflineTask,
    value: string,
    field: TaskField
  ) {
    if (value.length > 255)
      throw new OfflineError(
        field === 'notes' ? 'NOTES_TOO_LONG' : 'TEXT_TOO_LONG'
      );
    if (field === 'notes') return;
    switch (server.taskBase.taskType) {
      case 'TEXT':
        return;
      case 'NUMBER':
      case 'METER':
        if (
          !/^(?:\d+(?:\.\d*)?|\.\d+)$/.test(value) ||
          !Number.isFinite(Number(value))
        )
          throw new OfflineError('INVALID_NUMBER');
        return;
      case 'SUBTASK':
        if (!['OPEN', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETE'].includes(value))
          throw new OfflineError('INVALID_CHOICE');
        return;
      case 'INSPECTION':
        if (!['PASS', 'FLAG', 'FAIL'].includes(value))
          throw new OfflineError('INVALID_CHOICE');
        return;
      case 'MULTIPLE':
        if (!server.taskBase.options?.some((option) => option.label === value))
          throw new OfflineError('INVALID_CHOICE');
        return;
      default:
        throw new OfflineError('UNSUPPORTED_TASK');
    }
  }

  private async usableWorkOrder(workOrderId: number, editing = false) {
    const row = await this.find(WORK_ORDERS, String(workOrderId));
    if (!row || !raw(row, 'available'))
      throw new OfflineError('TARGET_UNAVAILABLE');
    if (editing && !json<OfflineSnapshot>(row).canEdit)
      throw new OfflineError('EDIT_FORBIDDEN');
    return row;
  }

  private createOperation(request: OperationRequest, revision: number) {
    if (utf8ByteLength(JSON.stringify(request)) > OFFLINE_LIMITS.snapshotBytes)
      throw new OfflineError('REQUEST_TOO_LARGE');
    const now = this.now();
    return this.create(OPERATIONS, request.operationId, {
      work_order_id: String(request.workOrderId),
      task_id:
        'taskId' in request && request.taskId != null
          ? String(request.taskId)
          : '',
      request: JSON.stringify(request),
      status: 'QUEUED',
      attempt_count: 0,
      receipt: null,
      created_at: now,
      updated_at: now,
      submitted_revision: revision,
      server_comment_id: null,
      snapshot_confirmed: false
    });
  }

  async saveDraft(
    workOrderId: number,
    taskId: number,
    value: string,
    field: TaskField = 'value'
  ): Promise<TaskDraft> {
    this.assertStorage();
    if (typeof value !== 'string') throw new OfflineError('INVALID_VALUE');
    return this.database.write(async () => {
      const { server } = await this.usableTask(
        workOrderId,
        taskId,
        true,
        field
      );
      const existing = await this.find(DRAFTS, draftId(taskId, field));
      const original = existing ? json<TaskDraft>(existing) : null;
      const operations = (await this.unresolvedRows(taskId)).map((row) =>
        this.operation(row)
      );
      const predecessor = operations[operations.length - 1];
      const request = predecessor?.request;
      const priorField = operations
        .filter((op) => operationField(op.request) === field)
        .pop();
      const clock = await this.nextDraftRevision();
      const draft: TaskDraft = {
        id: draftId(taskId, field),
        workOrderId,
        taskId,
        field,
        revision: clock.revision,
        value,
        baseVersion:
          original?.baseVersion ??
          (request && isTaskOperation(request)
            ? request.baseVersion
            : server.version),
        baseValue:
          original?.baseValue ??
          (priorField
            ? operationText(priorField.request)
            : String(server[field] ?? '')),
        basedOnOperationId: original?.basedOnOperationId || predecessor?.id,
        blockedReason:
          original?.blockedReason ||
          (predecessor && !ACTIVE.includes(predecessor.status)
            ? predecessor.receipt?.code || predecessor.status
            : undefined),
        updatedAt: this.now()
      };
      const fields = {
        work_order_id: String(workOrderId),
        task_id: String(taskId),
        payload: JSON.stringify(draft),
        revision: draft.revision
      };
      await this.commit([
        this.clockChange(clock),
        existing
          ? this.change(existing, fields)
          : this.create(DRAFTS, draft.id, fields)
      ]);
      return draft;
    });
  }

  async submitDraft(
    taskId: number,
    expectedDraftRevision: number,
    field: TaskField = 'value'
  ): Promise<LocalOperation> {
    this.assertStorage();
    return this.database.write(async () => {
      const row = await this.find(DRAFTS, draftId(taskId, field));
      if (!row || Number(raw(row, 'revision')) !== expectedDraftRevision)
        throw new OfflineError('DRAFT_CHANGED');
      const draft = json<TaskDraft>(row);
      const { server } = await this.usableTask(
        draft.workOrderId,
        taskId,
        true,
        field
      );
      this.validateTaskInput(server, draft.value, field);
      const unresolved = (await this.unresolvedRows(taskId)).map((item) =>
        this.operation(item)
      );
      if (
        draft.blockedReason ||
        unresolved.some(
          (op) =>
            operationField(op.request) === field || !ACTIVE.includes(op.status)
        )
      )
        throw new OfflineError('PREVIOUS_OPERATION');
      const predecessor = unresolved[unresolved.length - 1];
      if (
        predecessor &&
        (predecessor.submittedRevision == null ||
          !isTaskOperation(predecessor.request) ||
          predecessor.request.baseVersion !== draft.baseVersion)
      )
        throw new OfflineError('PREVIOUS_OPERATION');
      if (!predecessor && draft.basedOnOperationId)
        throw new OfflineError('PREVIOUS_OPERATION');
      const id = this.options.uuid();
      const common = {
        ...this.identity(),
        operationId: id,
        workOrderId: draft.workOrderId,
        taskId,
        baseVersion: draft.baseVersion,
        occurredAt: new Date(this.now()).toISOString(),
        ...(predecessor ? { predecessorOperationId: predecessor.id } : {})
      };
      const request: OperationRequest =
        field === 'notes'
          ? { ...common, type: 'SET_TASK_NOTES', notes: draft.value }
          : { ...common, type: 'SET_TASK_VALUE', value: draft.value };
      const other = await this.find(
        DRAFTS,
        draftId(taskId, field === 'value' ? 'notes' : 'value')
      );
      const otherDraft = other ? json<TaskDraft>(other) : null;
      // A field's own receipt is the only authority allowed to advance another local draft.
      const binding =
        otherDraft &&
        !otherDraft.blockedReason &&
        otherDraft.baseVersion === draft.baseVersion;
      const clock = binding ? await this.nextDraftRevision() : null;
      const operation = this.createOperation(request, draft.revision);
      const records = [operation, row.prepareDestroyPermanently()];
      if (binding)
        records.push(
          this.clockChange(clock!),
          this.change(other!, {
            payload: JSON.stringify({
              ...otherDraft,
              basedOnOperationId: id,
              revision: clock!.revision
            }),
            revision: clock!.revision
          })
        );
      await this.commit(records);
      return this.operation(operation);
    });
  }

  async saveCommentDraft(
    workOrderId: number,
    content: string,
    expectedRevision?: number | null,
    attachmentPending?: boolean
  ): Promise<CommentDraft> {
    this.assertStorage();
    return this.database.write(async () => {
      await this.usableWorkOrder(workOrderId, true);
      const row = await this.find(DRAFTS, commentDraftId(workOrderId));
      if (
        expectedRevision !== undefined &&
        (row ? Number(raw(row, 'revision')) : null) !== expectedRevision
      )
        throw new OfflineError('DRAFT_CHANGED');
      const clock = await this.nextDraftRevision();
      const draft: CommentDraft = {
        id: commentDraftId(workOrderId),
        kind: 'COMMENT',
        workOrderId,
        content,
        fileIds: row ? json<CommentDraft>(row).fileIds || [] : [],
        attachmentPending:
          attachmentPending ??
          (row ? !!json<CommentDraft>(row).attachmentPending : false),
        revision: clock.revision,
        updatedAt: this.now()
      };
      const fields = {
        work_order_id: String(workOrderId),
        task_id: '',
        payload: JSON.stringify(draft),
        revision: draft.revision
      };
      await this.commit([
        this.clockChange(clock),
        row ? this.change(row, fields) : this.create(DRAFTS, draft.id, fields)
      ]);
      return draft;
    });
  }

  async submitCommentDraft(
    workOrderId: number,
    expectedRevision: number
  ): Promise<LocalOperation> {
    this.assertStorage();
    return this.database.write(async () => {
      await this.usableWorkOrder(workOrderId, true);
      const row = await this.find(DRAFTS, commentDraftId(workOrderId));
      if (!row || Number(raw(row, 'revision')) !== expectedRevision)
        throw new OfflineError('DRAFT_CHANGED');
      const draft = json<CommentDraft>(row);
      if (draft.attachmentPending)
        throw new OfflineError('COMMENT_ATTACHMENT_PENDING');
      if (!draft.content.trim()) throw new OfflineError('COMMENT_EMPTY');
      const transfers = await this.prepareTransfers(
        workOrderId,
        { kind: 'COMMENT' },
        draft.fileIds || [],
        draft.revision
      );
      const request: OperationRequest = {
        ...this.identity(),
        operationId: this.options.uuid(),
        type: 'CREATE_COMMENT',
        workOrderId,
        content: draft.content,
        ...(transfers.ids.length ? { fileTransferIds: transfers.ids } : {}),
        occurredAt: new Date(this.now()).toISOString()
      };
      const operation = this.createOperation(request, draft.revision);
      await this.commit([
        ...transfers.records(),
        operation,
        row.prepareDestroyPermanently()
      ]);
      return this.operation(operation);
    });
  }

  private async abandonDraftRow(id: string, revision: number) {
    this.assertStorage();
    await this.database.write(async () => {
      const row = await this.find(DRAFTS, id);
      if (!row || Number(raw(row, 'revision')) !== revision)
        throw new OfflineError('DRAFT_CHANGED');
      await this.commit([row.prepareDestroyPermanently()]);
    });
  }
  abandonDraft(
    taskId: number,
    expectedRevision: number,
    field: TaskField = 'value'
  ): Promise<void> {
    return this.abandonDraftRow(draftId(taskId, field), expectedRevision);
  }
  abandonCommentDraft(
    workOrderId: number,
    expectedRevision: number
  ): Promise<void> {
    return this.abandonDraftRow(commentDraftId(workOrderId), expectedRevision);
  }

  private async blockedReason(
    operation: LocalOperation
  ): Promise<string | undefined> {
    const request = operation.request;
    for (const id of transferIds(request)) {
      const dependency = await this.getOperation(id);
      if (
        !dependency ||
        ['REJECTED', 'CONFLICT', 'ABANDONED'].includes(dependency.status) ||
        dependency.receipt?.visibility === 'FILE_DELETED' ||
        dependency.receipt?.visibility === 'TARGET_UNAVAILABLE'
      )
        return 'TRANSFER_FAILED';
      if (
        dependency.status === 'QUEUED' &&
        dependency.attemptCount === 0 &&
        (await this.getTransferFile(id))?.status === 'ERROR'
      )
        return 'TRANSFER_FAILED';
    }
    if (!isTaskOperation(request) || !request.predecessorOperationId)
      return undefined;
    const predecessor = await this.getOperation(request.predecessorOperationId);
    if (
      !predecessor ||
      ['CONFLICT', 'REJECTED', 'ABANDONED'].includes(predecessor.status)
    )
      return 'PREDECESSOR_FAILED';
    return undefined;
  }

  async abandonOperation(operationId: string): Promise<void> {
    this.assertStorage();
    await this.database.write(async () => {
      const row = await this.find(OPERATIONS, operationId);
      if (!row) return;
      const operation = this.operation(row);
      if (
        !(
          (operation.status === 'QUEUED' && operation.attemptCount === 0) ||
          operation.status === 'REJECTED' ||
          operation.status === 'CONFLICT'
        )
      )
        throw new OfflineError('RESULT_UNKNOWN');
      const drafts = !isTaskOperation(operation.request)
        ? []
        : await this.collection(DRAFTS)
            .query(Q.where('task_id', String(operation.request.taskId)))
            .fetch();
      const records = [
        this.change(row, { status: 'ABANDONED', updated_at: this.now() })
      ];
      const otherParents = (await this.unresolvedRows())
        .map((item) => this.operation(item))
        .filter((item) => item.id !== operationId);
      for (const id of transferIds(operation.request)) {
        if (
          otherParents.some((parent) =>
            transferIds(parent.request).includes(id)
          )
        )
          continue;
        const dependencyRow = await this.find(OPERATIONS, id);
        if (!dependencyRow) continue;
        const dependency = this.operation(dependencyRow);
        if (
          (dependency.status === 'QUEUED' && dependency.attemptCount === 0) ||
          ['REJECTED', 'CONFLICT'].includes(dependency.status)
        )
          records.push(
            this.change(dependencyRow, {
              status: 'ABANDONED',
              updated_at: this.now()
            })
          );
      }

      for (const draftRow of drafts) {
        const draft = json<TaskDraft>(draftRow);
        if (draft.basedOnOperationId === operationId)
          records.push(
            this.change(draftRow, {
              payload: JSON.stringify({
                ...draft,
                blockedReason: 'PREDECESSOR_ABANDONED'
              })
            })
          );
      }
      await this.commit(records);
    });
  }

  async discardTaskField(
    taskId: number,
    field: TaskField,
    expectedDraftRevision: number | null,
    operationId: string | null,
    expectedServerVersion: number
  ): Promise<void> {
    this.assertStorage();
    await this.database.write(async () => {
      const task = await this.find(TASKS, String(taskId));
      if (!task) throw new OfflineError('TARGET_UNAVAILABLE');
      const { server } = await this.usableTask(
        Number(raw(task, 'work_order_id')),
        taskId
      );
      if (server.version !== expectedServerVersion)
        throw new OfflineError('SERVER_CHANGED');
      const draftRow = await this.find(DRAFTS, draftId(taskId, field));
      if (
        (draftRow ? Number(raw(draftRow, 'revision')) : null) !==
        expectedDraftRevision
      )
        throw new OfflineError('DRAFT_CHANGED');
      const unresolved = (await this.unresolvedRows(taskId)).filter(
        (row) => operationField(this.operation(row).request) === field
      );
      if (unresolved.some((row) => row.id !== operationId))
        throw new OfflineError('DRAFT_CHANGED');
      const records: Model[] = [];
      for (const row of unresolved) {
        const operation = this.operation(row);
        if (
          !['CONFLICT', 'REJECTED'].includes(operation.status) &&
          !(operation.status === 'QUEUED' && operation.attemptCount === 0)
        )
          throw new OfflineError('RESULT_UNKNOWN');
        records.push(
          this.change(row, { status: 'ABANDONED', updated_at: this.now() })
        );
      }
      if (draftRow) records.push(draftRow.prepareDestroyPermanently());
      // Other-field drafts retain their predecessor and must explicitly review an abandoned chain.
      const other = await this.find(
        DRAFTS,
        draftId(taskId, field === 'value' ? 'notes' : 'value')
      );
      if (other) {
        const draft = json<TaskDraft>(other);
        if (unresolved.some((row) => row.id === draft.basedOnOperationId))
          records.push(
            this.change(other, {
              payload: JSON.stringify({
                ...draft,
                blockedReason: 'PREDECESSOR_ABANDONED'
              })
            })
          );
      }
      if (records.length) await this.commit(records);
    });
  }

  async resolveDraftAgainstServer(
    taskId: number,
    expectedDraftRevision: number,
    expectedServerVersion: number,
    field: TaskField = 'value'
  ): Promise<TaskDraft> {
    this.assertStorage();
    return this.database.write(async () => {
      const row = await this.find(DRAFTS, draftId(taskId, field));
      if (!row || Number(raw(row, 'revision')) !== expectedDraftRevision)
        throw new OfflineError('DRAFT_CHANGED');
      const draft = json<TaskDraft>(row);
      const { server } = await this.usableTask(
        draft.workOrderId,
        taskId,
        true,
        field
      );
      if (server.version !== expectedServerVersion)
        throw new OfflineError('SERVER_CHANGED');
      const operations = (await this.unresolvedRows(taskId)).filter(
        (row) => operationField(this.operation(row).request) === field
      );
      if (operations.length) throw new OfflineError('PREVIOUS_OPERATION');
      const clock = await this.nextDraftRevision();
      const resolved = {
        ...draft,
        revision: clock.revision,
        baseVersion: server.version,
        baseValue: String(server[field] ?? ''),
        basedOnOperationId: undefined,
        blockedReason: undefined,
        updatedAt: this.now()
      };
      await this.commit([
        this.clockChange(clock),
        this.change(row, {
          payload: JSON.stringify(resolved),
          revision: resolved.revision
        })
      ]);
      return resolved;
    });
  }

  async resolveOperationAgainstServer(
    operationId: string,
    expectedDraftRevision: number | null,
    expectedServerVersion: number
  ): Promise<TaskDraft> {
    this.assertStorage();
    return this.database.write(async () => {
      const row = await this.find(OPERATIONS, operationId);
      if (!row) throw new OfflineError('OPERATION_MISSING');
      const operation = this.operation(row);
      if (!isTaskOperation(operation.request))
        throw new OfflineError('INVALID_VALUE');
      if (
        !['CONFLICT', 'REJECTED'].includes(operation.status) &&
        !(
          operation.status === 'QUEUED' &&
          operation.attemptCount === 0 &&
          (await this.blockedReason(operation))
        )
      )
        throw new OfflineError('RESULT_UNKNOWN');
      const { taskId, workOrderId } = operation.request;
      const field = operationField(operation.request) as TaskField;
      const { server } = await this.usableTask(
        workOrderId,
        taskId,
        true,
        field
      );
      if (server.version !== expectedServerVersion)
        throw new OfflineError('SERVER_CHANGED');
      const draftRow = await this.find(DRAFTS, draftId(taskId, field));
      const draft = draftRow ? json<TaskDraft>(draftRow) : null;
      if ((draft?.revision ?? null) !== expectedDraftRevision)
        throw new OfflineError('DRAFT_CHANGED');
      const clock = await this.nextDraftRevision();
      const resolved: TaskDraft = {
        id: draftId(taskId, field),
        workOrderId,
        taskId,
        field,
        revision: clock.revision,
        value: draft?.value ?? operationText(operation.request),
        baseVersion: server.version,
        baseValue: String(server[field] ?? ''),
        updatedAt: this.now()
      };
      const fields = {
        work_order_id: String(workOrderId),
        task_id: String(taskId),
        payload: JSON.stringify(resolved),
        revision: resolved.revision
      };
      await this.commit([
        this.clockChange(clock),
        draftRow
          ? this.change(draftRow, fields)
          : this.create(DRAFTS, resolved.id, fields),
        this.change(row, { status: 'ABANDONED', updated_at: this.now() })
      ]);
      return resolved;
    });
  }

  async applyReceipt(receipt: OperationReceipt): Promise<void> {
    this.assertStorage();
    if (!['APPLIED', 'CONFLICT', 'REJECTED'].includes(receipt.status))
      throw new OfflineError('INVALID_RECEIPT');
    await this.database.write(async () => {
      const row = await this.find(OPERATIONS, receipt.operationId);
      if (!row) return;
      const operation = this.operation(row);
      if (operation.status === 'ABANDONED') return;
      // The result is final, but the server re-evaluates comment visibility on
      // receipt reads. This also resolves deletion before the first snapshot.
      if (
        operation.receipt &&
        !isTaskOperation(operation.request) &&
        operation.request.type !== 'CREATE_COMMENT'
      ) {
        if (
          receipt.status !== operation.receipt.status ||
          receipt.receivedAt !== operation.receipt.receivedAt ||
          receipt.fileId !== operation.receipt.fileId
        )
          throw new OfflineError('INVALID_RECEIPT');
        if (
          operation.receipt.visibility === 'FILE_DELETED' ||
          operation.receipt.visibility === receipt.visibility
        )
          return;
        await this.applyFileReceipt(row, operation, {
          ...operation.receipt,
          visibility: receipt.visibility
        });
        return;
      }
      if (operation.receipt) {
        if (
          operation.request.type !== 'CREATE_COMMENT' ||
          operation.status !== 'APPLIED'
        )
          return;
        if (
          receipt.status !== 'APPLIED' ||
          receipt.commentId !== operation.receipt.commentId ||
          receipt.receivedAt !== operation.receipt.receivedAt
        )
          throw new OfflineError('INVALID_RECEIPT');
        if (
          operation.receipt.visibility === 'COMMENT_DELETED' ||
          operation.receipt.visibility === receipt.visibility
        )
          return;
        // Retain the original result; only visibility is mutable.
        receipt = { ...operation.receipt, visibility: receipt.visibility };
      }
      const request = operation.request;
      if (!isTaskOperation(request) && request.type !== 'CREATE_COMMENT') {
        await this.applyFileReceipt(row, operation, receipt);
        return;
      }
      const workOrder = await this.find(
        WORK_ORDERS,
        String(request.workOrderId)
      );
      const isComment = request.type === 'CREATE_COMMENT';
      if (isComment && receipt.task) throw new OfflineError('INVALID_RECEIPT');
      if (!isComment && receipt.commentId != null)
        throw new OfflineError('INVALID_RECEIPT');
      if (
        isComment &&
        receipt.status === 'APPLIED' &&
        !['VISIBLE', 'COMMENT_DELETED', 'WORK_ORDER_UNAVAILABLE'].includes(
          receipt.visibility || ''
        )
      )
        throw new OfflineError('INVALID_RECEIPT');
      if (
        isTaskOperation(request) &&
        receipt.chainBaseVersion != null &&
        receipt.chainBaseVersion !== request.baseVersion
      )
        throw new OfflineError('INVALID_RECEIPT');
      if (
        isComment &&
        receipt.status === 'APPLIED' &&
        (!Number.isSafeInteger(receipt.commentId) || receipt.commentId! <= 0)
      )
        throw new OfflineError('INVALID_RECEIPT');
      if (
        isComment &&
        operation.serverCommentId != null &&
        receipt.commentId != null &&
        operation.serverCommentId !== receipt.commentId
      )
        throw new OfflineError('INVALID_RECEIPT');
      if (
        isTaskOperation(request) &&
        receipt.task &&
        (receipt.task.id !== request.taskId ||
          !Number.isSafeInteger(receipt.task.version))
      )
        throw new OfflineError('INVALID_RECEIPT');
      const records: Model[] = [
        this.change(row, {
          status: receipt.status,
          receipt: JSON.stringify(receipt),
          updated_at: this.now()
        })
      ];
      const withheld =
        !isComment &&
        !receipt.task &&
        ['APPLIED', 'CONFLICT'].includes(receipt.status);
      if (
        workOrder &&
        (receipt.code === 'ACCESS_REVOKED' ||
          (isComment && receipt.code === 'TARGET_NOT_FOUND') ||
          withheld ||
          receipt.visibility === 'WORK_ORDER_UNAVAILABLE')
      ) {
        records.push(
          this.change(workOrder, {
            available: false,
            unavailable_reason: receipt.code || 'ACCESS_REVOKED',
            availability_revision:
              Number(raw(workOrder, 'availability_revision')) + 1
          })
        );
      }
      if (isComment && workOrder && receipt.visibility === 'COMMENT_DELETED') {
        const snapshot = json<OfflineSnapshot>(workOrder);
        records.push(
          this.change(workOrder, {
            payload: JSON.stringify({
              ...snapshot,
              comments: snapshot.comments.filter(
                (comment) => comment.id !== receipt.commentId
              )
            })
          })
        );
      }
      if (isTaskOperation(request)) {
        const taskRow = await this.find(TASKS, String(request.taskId));
        if (taskRow) {
          const fields: Record<string, string | boolean | null> = {};
          if (
            receipt.task &&
            receipt.task.version >= json<OfflineTask>(taskRow).version
          )
            fields.payload = JSON.stringify(receipt.task);
          if (receipt.code === 'TARGET_NOT_FOUND') {
            fields.available = false;
            fields.unavailable_reason = receipt.code;
          }
          if (Object.keys(fields).length)
            records.push(this.change(taskRow, fields));
        }
        const drafts = await this.collection(DRAFTS)
          .query(Q.where('task_id', String(request.taskId)))
          .fetch();
        const pending = (await this.unresolvedRows(request.taskId))
          .map((item) => this.operation(item))
          .filter((op) => op.id !== operation.id);
        const tail = pending[pending.length - 1];
        const updates: { row: Model; draft: TaskDraft }[] = [];
        for (const draftRow of drafts) {
          const draft = json<TaskDraft>(draftRow);
          if (draft.basedOnOperationId !== operation.id) continue;
          const field = draft.field || 'value';
          let next: TaskDraft;
          if (receipt.status === 'APPLIED' && receipt.task) {
            if (
              tail &&
              isTaskOperation(tail.request) &&
              tail.request.baseVersion === draft.baseVersion
            ) {
              next = { ...draft, basedOnOperationId: tail.id };
            } else {
              next = {
                ...draft,
                basedOnOperationId: undefined,
                blockedReason: undefined,
                baseVersion: receipt.task.version,
                baseValue: String(receipt.task[field] ?? '')
              };
            }
          } else
            next = {
              ...draft,
              blockedReason: withheld
                ? 'RECEIPT_WITHOUT_TASK'
                : receipt.code || receipt.status
            };
          updates.push({ row: draftRow, draft: next });
        }
        if (updates.length) {
          const clock = await this.nextDraftRevision();
          updates.forEach((update, index) => {
            const revision = clock.revision + index;
            records.push(
              this.change(update.row, {
                payload: JSON.stringify({ ...update.draft, revision }),
                revision
              })
            );
          });
          clock.revision += updates.length - 1;
          records.push(this.clockChange(clock));
        }
      }
      if (isComment && receipt.status === 'APPLIED') {
        for (const id of transferIds(request)) {
          const fileRow = await this.transferFileRow(id);
          if (!fileRow) continue;
          const file = json<LocalFile>(fileRow);
          records.push(
            this.change(fileRow, {
              payload: JSON.stringify({
                ...file,
                workOrderIds: [
                  ...new Set([...file.workOrderIds, request.workOrderId])
                ]
              })
            })
          );
        }
      }
      // Comment acknowledgements are compact. Only a complete snapshot changes the canonical thread.
      await this.commit(records);
    });
  }

  async beginAttempt(operationId: string): Promise<LocalOperation> {
    this.assertStorage();
    return this.database.write(async () => {
      const row = await this.find(OPERATIONS, operationId);
      if (!row) throw new OfflineError('OPERATION_MISSING');
      const operation = this.operation(row);
      if (!ACTIVE.includes(operation.status))
        throw new OfflineError('OPERATION_RESOLVED');
      await this.commit([
        this.change(row, {
          status: 'SENDING',
          attempt_count: operation.attemptCount + 1,
          updated_at: this.now()
        })
      ]);
      return this.operation(row);
    });
  }

  async markUnknown(operationId: string): Promise<void> {
    this.assertStorage();
    await this.database.write(async () => {
      const row = await this.find(OPERATIONS, operationId);
      if (row && ACTIVE.includes(raw(row, 'status') as OperationStatus)) {
        await this.commit([
          this.change(row, { status: 'UNKNOWN', updated_at: this.now() })
        ]);
      }
    });
  }

  async getOperation(id: string): Promise<LocalOperation | null> {
    this.assertStorage();
    const row = await this.find(OPERATIONS, id);
    return row ? this.operation(row) : null;
  }

  async uploadHasParent(id: string): Promise<boolean> {
    return (await this.unresolvedRows()).some((row) =>
      transferIds(this.operation(row).request).includes(id)
    );
  }

  async getOperationsToSync(): Promise<LocalOperation[]> {
    this.assertStorage();
    return (await this.unresolvedRows())
      .map((row) => this.operation(row))
      .filter((operation) => ACTIVE.includes(operation.status));
  }

  async getFileReceiptsToRefresh(): Promise<LocalOperation[]> {
    const ids = new Set(
      (await this.unresolvedRows()).flatMap((row) =>
        transferIds(this.operation(row).request)
      )
    );
    const operations: LocalOperation[] = [];
    for (const id of ids) {
      const operation = await this.getOperation(id);
      if (
        operation?.status === 'APPLIED' &&
        operation.receipt?.visibility !== 'FILE_DELETED'
      )
        operations.push(operation);
    }
    return operations;
  }

  async getCommentReceiptsToRefresh(): Promise<LocalOperation[]> {
    this.assertStorage();
    const rows = await this.collection(OPERATIONS)
      .query(Q.where('task_id', ''), Q.where('status', 'APPLIED'))
      .fetch();
    // An association means the comment was already seen in a complete copy.
    // A confirmed publication with no association still needs either that copy
    // or an authoritative deletion receipt, never another creation request.
    return rows
      .map((row) => this.operation(row))
      .filter(
        (operation) =>
          operation.request.type === 'CREATE_COMMENT' &&
          operation.serverCommentId == null &&
          operation.receipt?.visibility === 'VISIBLE'
      );
  }

  async hasUnresolved(workOrderId?: number): Promise<boolean> {
    this.assertStorage();
    const [drafts, operations] = await Promise.all([
      this.collection(DRAFTS)
        .query(
          ...(workOrderId === undefined
            ? []
            : [Q.where('work_order_id', String(workOrderId))])
        )
        .fetchCount(),
      this.unresolvedRows(undefined, workOrderId)
    ]);
    return drafts > 0 || operations.length > 0;
  }

  async removePreparation(workOrderId: number): Promise<void> {
    this.assertStorage();
    await this.database.write(async () => {
      if (await this.hasUnresolved(workOrderId))
        throw new OfflineError('WORK_PENDING');
      const [workOrder, tasks, operations] = await Promise.all([
        this.find(WORK_ORDERS, String(workOrderId)),
        this.collection(TASKS)
          .query(Q.where('work_order_id', String(workOrderId)))
          .fetch(),
        this.collection(OPERATIONS)
          .query(Q.where('work_order_id', String(workOrderId)))
          .fetch()
      ]);
      const cleanup = await this.find('offline_cleanup', String(workOrderId));
      const references: Model[] = [];
      for (const row of await this.collection(FILES).query().fetch()) {
        const file = json<LocalFile>(row);
        if (file.workOrderIds.includes(workOrderId))
          references.push(
            this.change(row, {
              payload: JSON.stringify({
                ...file,
                workOrderIds: file.workOrderIds.filter(
                  (id) => id !== workOrderId
                )
              })
            })
          );
      }
      for (const row of await this.collection(ASSETS)
        .query(Q.where('id', Q.notEq('access')))
        .fetch()) {
        const copy = json<AssetCopy>(row);
        if (!copy.refs.includes(workOrderId)) continue;
        const refs = copy.refs.filter((id) => id !== workOrderId);
        references.push(
          refs.length
            ? this.change(row, { payload: JSON.stringify({ ...copy, refs }) })
            : row.prepareDestroyPermanently()
        );
      }
      await this.commit([
        ...references,
        ...[...(workOrder ? [workOrder] : []), ...tasks, ...operations].map(
          (row) => row.prepareDestroyPermanently()
        ),
        ...(!cleanup
          ? [
              this.create('offline_cleanup', String(workOrderId), {
                created_at: this.now(),
                kind: 'REMOVAL',
                token: null
              })
            ]
          : [])
      ]);
    });
  }

  async getPendingReleases(): Promise<number[]> {
    this.assertStorage();
    return (await this.collection('offline_cleanup').query().fetch()).map(
      (row) => Number(row.id)
    );
  }

  async acknowledgeRelease(workOrderId: number): Promise<void> {
    this.assertStorage();
    await this.database.write(async () => {
      const row = await this.find('offline_cleanup', String(workOrderId));
      if (row) await this.commit([row.prepareDestroyPermanently()]);
    });
  }

  async getTask(taskId: number, field: TaskField = 'value'): Promise<TaskView> {
    this.assertStorage();
    return this.database.read(async () => {
      const [taskRow, draftRow, allOperations] = await Promise.all([
        this.find(TASKS, String(taskId)),
        this.find(DRAFTS, draftId(taskId, field)),
        this.unresolvedRows(taskId)
      ]);
      const workOrder = taskRow
        ? await this.find(WORK_ORDERS, String(raw(taskRow, 'work_order_id')))
        : null;
      const available = !!(
        workOrder &&
        raw(workOrder, 'available') &&
        taskRow &&
        raw(taskRow, 'available')
      );
      const operations = allOperations
        .map((row) => this.operation(row))
        .filter((op) => operationField(op.request) === field);
      let operation = operations[operations.length - 1] || null;
      if (!operation) {
        const applied = await this.collection(OPERATIONS)
          .query(
            Q.where('task_id', String(taskId)),
            Q.where('status', 'APPLIED'),
            Q.sortBy('updated_at', Q.desc)
          )
          .fetch();
        operation =
          applied
            .map((row) => this.operation(row))
            .find((op) => operationField(op.request) === field) || null;
      }
      if (operation)
        operation = {
          ...operation,
          blockedReason: await this.blockedReason(operation)
        };
      let draft = draftRow ? json<TaskDraft>(draftRow) : null;
      if (draft?.basedOnOperationId && !draft.blockedReason) {
        const predecessor = await this.getOperation(draft.basedOnOperationId);
        if (
          !predecessor ||
          ['CONFLICT', 'REJECTED', 'ABANDONED'].includes(predecessor.status)
        )
          draft = { ...draft, blockedReason: 'PREDECESSOR_FAILED' };
        else {
          const reason = await this.blockedReason(predecessor);
          if (reason) draft = { ...draft, blockedReason: reason };
        }
      }
      const serverTask = available ? json<OfflineTask>(taskRow!) : null;
      const userId = this.identity().userId;
      return {
        serverTask,
        task: serverTask
          ? {
              ...serverTask,
              [field]:
                draft?.value ??
                (operation && operation.status !== 'APPLIED'
                  ? operationText(operation.request)
                  : serverTask[field])
            }
          : null,
        draft: available ? draft : null,
        operation: available ? operation : null,
        available,
        unavailableReason: available
          ? null
          : String(
              (workOrder && raw(workOrder, 'unavailable_reason')) ||
                (taskRow && raw(taskRow, 'unavailable_reason')) ||
                'TARGET_UNAVAILABLE'
            ),
        canEdit: !!(
          available &&
          json<OfflineSnapshot>(workOrder!).canEdit &&
          (field === 'notes' ||
            !serverTask?.taskBase.user ||
            serverTask.taskBase.user.id === userId)
        )
      };
    });
  }

  async getCommentDraft(workOrderId: number): Promise<CommentDraft | null> {
    this.assertStorage();
    const workOrder = await this.find(WORK_ORDERS, String(workOrderId));
    if (!workOrder || !raw(workOrder, 'available')) return null;
    const row = await this.find(DRAFTS, commentDraftId(workOrderId));
    return row ? json<CommentDraft>(row) : null;
  }

  async getCommentOperations(workOrderId: number): Promise<LocalOperation[]> {
    this.assertStorage();
    const workOrder = await this.find(WORK_ORDERS, String(workOrderId));
    if (!workOrder || !raw(workOrder, 'available')) return [];
    const rows = await this.collection(OPERATIONS)
      .query(
        Q.where('work_order_id', String(workOrderId)),
        Q.where('task_id', ''),
        Q.sortBy('created_at', Q.asc)
      )
      .fetch();
    return rows
      .map((row) => this.operation(row))
      .filter(
        (op) =>
          op.status !== 'ABANDONED' &&
          op.serverCommentId == null &&
          (op.status !== 'APPLIED' ||
            (op.serverCommentId == null &&
              op.receipt?.visibility === 'VISIBLE'))
      );
  }

  async getWorkOrderProjection(
    workOrderId: number
  ): Promise<PreparedWorkOrderView | null> {
    this.assertStorage();
    return this.database.read(async () => {
      const row = await this.find(WORK_ORDERS, String(workOrderId));
      if (!row) return null;
      const available = !!raw(row, 'available');
      const snapshot = available ? json<OfflineSnapshot>(row) : null;
      const [tasks, draftRows, operationRows] = available
        ? await Promise.all([
            this.collection(TASKS)
              .query(
                Q.where('work_order_id', String(workOrderId)),
                Q.where('available', true)
              )
              .fetch(),
            this.collection(DRAFTS)
              .query(Q.where('work_order_id', String(workOrderId)))
              .fetch(),
            this.unresolvedRows(undefined, workOrderId)
          ])
        : [[], [], []];
      const drafts = new Map(
        draftRows
          .filter((row) => raw(row, 'task_id') !== '' && !json<any>(row).kind)
          .map((row) => [row.id, json<TaskDraft>(row)])
      );
      const operations = new Map<string, LocalOperation>();
      operationRows.forEach((row) => {
        const operation = this.operation(row);
        if (isTaskOperation(operation.request))
          operations.set(
            draftId(
              operation.request.taskId,
              operationField(operation.request) as TaskField
            ),
            operation
          );
      });
      const projectedTasks = tasks.map((row) => {
        const server = json<OfflineTask>(row);
        const projected = { ...server };
        for (const field of ['value', 'notes'] as TaskField[]) {
          const id = draftId(server.id, field);
          const operation = operations.get(id);
          projected[field] =
            drafts.get(id)?.value ??
            (operation
              ? operationText(operation.request)
              : String(server[field] ?? ''));
        }
        return projected;
      });
      return {
        id: workOrderId,
        workOrder: snapshot?.workOrder || null,
        tasks: projectedTasks,
        comments: snapshot?.comments || [],
        history: snapshot?.history || [],
        canEdit: !!snapshot?.canEdit,
        canComment: !!(snapshot?.canComment ?? snapshot?.canEdit),
        commentDraft: available
          ? await this.getCommentDraft(workOrderId)
          : null,
        commentOperations: available
          ? await this.getCommentOperations(workOrderId)
          : [],
        preparedAt: String(raw(row, 'prepared_at')),
        available,
        unavailableReason: available
          ? null
          : String(raw(row, 'unavailable_reason') || 'TARGET_UNAVAILABLE')
      };
    });
  }

  private workOrderSummary(row: Model): PreparedWorkOrderSummary {
    const available = !!raw(row, 'available');
    return {
      id: Number(row.id),
      available,
      workOrder: available ? json<OfflineSnapshot>(row).workOrder : null,
      preparedAt: String(raw(row, 'prepared_at')),
      unavailableReason: available
        ? null
        : String(raw(row, 'unavailable_reason') || 'TARGET_UNAVAILABLE')
    };
  }

  async getPreparedWorkOrderSummaries(): Promise<PreparedWorkOrderSummary[]> {
    this.assertStorage();
    return (await this.collection(WORK_ORDERS).query().fetch()).map((row) =>
      this.workOrderSummary(row)
    );
  }

  observePreparedWorkOrderSummaries() {
    return this.collection(WORK_ORDERS)
      .query()
      .observeWithColumns([
        'payload',
        'available',
        'unavailable_reason',
        'prepared_at'
      ])
      .pipe(
        map((rows) => {
          this.assertStorage();
          return rows.map((row) => this.workOrderSummary(row));
        })
      );
  }

  async getPendingWork(workOrderId?: number): Promise<PendingWork[]> {
    this.assertStorage();
    const conditions =
      workOrderId === undefined
        ? []
        : [Q.where('work_order_id', String(workOrderId))];
    const [drafts, rows] = await Promise.all([
      this.collection(DRAFTS)
        .query(...conditions)
        .fetch(),
      this.collection(OPERATIONS)
        .query(...conditions, Q.sortBy('created_at', Q.asc))
        .fetch()
    ]);
    const items: PendingWork[] = [];
    for (const row of drafts) {
      const draft = json<TaskDraft | CommentDraft | AttachmentDraft>(row);
      const comment = 'kind' in draft && draft.kind === 'COMMENT';
      const attachments = 'kind' in draft && draft.kind === 'ATTACHMENTS';
      const field = comment
        ? 'comment'
        : attachments
        ? 'attachments'
        : (draft as TaskDraft).field || 'value';
      const target =
        attachments && (draft as AttachmentDraft).taskId != null
          ? await this.getTask((draft as AttachmentDraft).taskId!, 'notes')
          : comment || attachments
          ? await this.getWorkOrderProjection(draft.workOrderId)
          : await this.getTask((draft as TaskDraft).taskId, field as TaskField);
      items.push({
        kind: 'DRAFT',
        id: draft.id,
        workOrderId: draft.workOrderId,
        taskId: comment ? undefined : (draft as TaskDraft).taskId,
        field,
        draftRevision: draft.revision,
        targetAvailable: !!target?.available,
        reason:
          target?.unavailableReason ||
          (!comment && !attachments && (draft as TaskDraft).blockedReason) ||
          null,
        canAbandon: true,
        ...(target?.available
          ? {
              label:
                comment || attachments
                  ? undefined
                  : (target as TaskView).task?.taskBase.label,
              value: comment
                ? (draft as CommentDraft).content
                : attachments
                ? ''
                : (draft as TaskDraft).value
            }
          : {})
      });
    }
    for (const row of rows) {
      const operation = this.operation(row);
      const request = operation.request;
      const comment = request.type === 'CREATE_COMMENT';
      if (
        !UNRESOLVED.includes(operation.status) &&
        !(
          comment &&
          operation.status === 'APPLIED' &&
          operation.serverCommentId == null &&
          operation.receipt?.visibility === 'VISIBLE'
        ) &&
        !(
          operation.status === 'APPLIED' &&
          ['ADD_TASK_IMAGES', 'UPDATE_WORK_ORDER_ATTACHMENTS'].includes(
            request.type
          ) &&
          !operation.snapshotConfirmed
        )
      )
        continue;
      const field = operationField(request);
      const target =
        !isTaskOperation(request) &&
        'taskId' in request &&
        request.taskId != null
          ? await this.getTask(request.taskId, 'notes')
          : !isTaskOperation(request)
          ? await this.getWorkOrderProjection(request.workOrderId)
          : await this.getTask(request.taskId, field as TaskField);
      items.push({
        kind: 'OPERATION',
        operationType: request.type,
        id: operation.id,
        workOrderId: request.workOrderId,
        taskId: 'taskId' in request ? request.taskId : undefined,
        field,
        status: operation.status,
        attemptCount: operation.attemptCount,
        targetAvailable: !!target?.available,
        reason:
          target?.unavailableReason ||
          (await this.blockedReason(operation)) ||
          operation.receipt?.code ||
          null,
        canAbandon:
          (operation.status === 'QUEUED' && operation.attemptCount === 0) ||
          ['REJECTED', 'CONFLICT'].includes(operation.status),
        ...(target?.available
          ? {
              label: !isTaskOperation(request)
                ? undefined
                : (target as TaskView).task?.taskBase.label,
              value: operationText(request)
            }
          : {})
      });
    }
    return items;
  }

  /** Explicit review returns a resolved submission to a draft; never replays it. */
  async restoreFileOperation(
    operationId: string,
    expectedDraftRevision: number | null,
    expectedImageId?: number | null
  ): Promise<void> {
    this.assertStorage();
    await this.database.write(async () => {
      const row = await this.find(OPERATIONS, operationId);
      if (!row) throw new OfflineError('OPERATION_MISSING');
      const operation = this.operation(row),
        request = operation.request;
      if (
        ![
          'CREATE_COMMENT',
          'ADD_TASK_IMAGES',
          'UPDATE_WORK_ORDER_ATTACHMENTS'
        ].includes(request.type)
      )
        throw new OfflineError('INVALID_VALUE');
      if (
        !['REJECTED', 'CONFLICT'].includes(operation.status) &&
        !(
          operation.status === 'QUEUED' &&
          operation.attemptCount === 0 &&
          (await this.blockedReason(operation))
        )
      )
        throw new OfflineError('RESULT_UNKNOWN');
      const taskId =
        request.type === 'ADD_TASK_IMAGES' ? request.taskId : undefined;
      const workOrder = await this.fileTarget(request.workOrderId, {
        kind:
          request.type === 'CREATE_COMMENT'
            ? 'COMMENT'
            : taskId
            ? 'TASK'
            : 'WORK_ORDER',
        taskId
      });
      const id =
        request.type === 'CREATE_COMMENT'
          ? commentDraftId(request.workOrderId)
          : attachmentDraftId(request.workOrderId, taskId);
      const previous = await this.find(DRAFTS, id);
      // A newer composition is never overwritten or silently combined.
      if (
        (previous ? Number(raw(previous, 'revision')) : null) !==
          expectedDraftRevision ||
        previous
      )
        throw new OfflineError('DRAFT_CHANGED');
      const imageId =
        json<OfflineSnapshot>(workOrder).workOrder.image?.id ?? null;
      if (
        request.type === 'UPDATE_WORK_ORDER_ATTACHMENTS' &&
        request.imageChange &&
        (expectedImageId === undefined || expectedImageId !== imageId)
      )
        throw new OfflineError('SERVER_CHANGED');
      const fileIds: string[] = [],
        dependencyRows: Model[] = [];
      for (const transferId of transferIds(request)) {
        const transfer = await this.find(OPERATIONS, transferId);
        if (!transfer) throw new OfflineError('OPERATION_MISSING');
        const dependency = this.operation(transfer);
        if (
          dependency.attemptCount > 0 &&
          ['UNKNOWN', 'SENDING', 'QUEUED'].includes(dependency.status)
        )
          throw new OfflineError('RESULT_UNKNOWN');
        const fileRow = await this.transferFileRow(transferId);
        if (!fileRow || json<LocalFile>(fileRow).status === 'DELETING')
          throw new OfflineError('FILE_NOT_READY');
        fileIds.push(fileRow.id);
        if (
          dependency.status !== 'APPLIED' &&
          dependency.status !== 'ABANDONED'
        )
          dependencyRows.push(
            this.change(transfer, {
              status: 'ABANDONED',
              updated_at: this.now()
            })
          );
      }
      const clock = await this.nextDraftRevision();
      const common = {
        id,
        workOrderId: request.workOrderId,
        revision: clock.revision,
        updatedAt: this.now()
      };
      const draft: CommentDraft | AttachmentDraft =
        request.type === 'CREATE_COMMENT'
          ? { ...common, kind: 'COMMENT', content: request.content, fileIds }
          : {
              ...common,
              kind: 'ATTACHMENTS',
              taskId,
              fileIds:
                request.type === 'UPDATE_WORK_ORDER_ATTACHMENTS' &&
                request.imageChange
                  ? fileIds.slice(0, -1)
                  : fileIds,
              ...(request.type === 'UPDATE_WORK_ORDER_ATTACHMENTS' &&
              request.imageChange
                ? { imageFileId: fileIds[fileIds.length - 1] }
                : {}),
              baseImageId: imageId
            };
      await this.commit([
        this.clockChange(clock),
        ...dependencyRows,
        this.create(DRAFTS, id, {
          work_order_id: String(request.workOrderId),
          task_id: taskId ? String(taskId) : '',
          payload: JSON.stringify(draft),
          revision: clock.revision
        }),
        this.change(row, { status: 'ABANDONED', updated_at: this.now() })
      ]);
    });
  }

  private async fileTarget(workOrderId: number, target: FileTarget) {
    const workOrder = await this.usableWorkOrder(workOrderId, true);
    if (target.kind === 'TASK') {
      if (!target.taskId) throw new OfflineError('TARGET_UNAVAILABLE');
      await this.usableTask(workOrderId, target.taskId, true, 'notes');
    }
    return workOrder;
  }

  private fileDraftId(workOrderId: number, target: FileTarget) {
    return target.kind === 'COMMENT'
      ? commentDraftId(workOrderId)
      : attachmentDraftId(workOrderId, target.taskId);
  }

  async getAttachmentDraft(
    workOrderId: number,
    taskId?: number
  ): Promise<AttachmentDraft | null> {
    this.assertStorage();
    const workOrder = await this.find(WORK_ORDERS, String(workOrderId));
    if (!workOrder || !raw(workOrder, 'available')) return null;
    if (taskId != null) {
      const task = await this.find(TASKS, String(taskId));
      if (
        !task ||
        !raw(task, 'available') ||
        raw(task, 'work_order_id') !== String(workOrderId)
      )
        return null;
    }
    const row = await this.find(DRAFTS, attachmentDraftId(workOrderId, taskId));
    return row ? json<AttachmentDraft>(row) : null;
  }

  /** Reserve the selected file and its exact draft revision before copying bytes. */
  async createFileSelection(
    file: LocalFile,
    workOrderId: number,
    target: FileTarget
  ): Promise<void> {
    this.assertStorage();
    await this.database.write(async () => {
      const workOrder = await this.fileTarget(workOrderId, target);
      if (await this.find(FILES, file.id))
        throw new OfflineError('FILE_IDENTITY');
      const id = this.fileDraftId(workOrderId, target);
      const row = await this.find(DRAFTS, id);
      const existing = row ? json<CommentDraft | AttachmentDraft>(row) : null;
      const clock = await this.nextDraftRevision();
      const common = {
        id,
        workOrderId,
        revision: clock.revision,
        updatedAt: this.now()
      };
      const draft: CommentDraft | AttachmentDraft =
        target.kind === 'COMMENT'
          ? {
              ...common,
              kind: 'COMMENT',
              content: (existing as CommentDraft)?.content || '',
              fileIds: [...(existing?.fileIds || []), file.id],
              attachmentPending: false
            }
          : {
              ...common,
              kind: 'ATTACHMENTS',
              taskId: target.taskId,
              fileIds: target.image
                ? existing?.fileIds || []
                : [...(existing?.fileIds || []), file.id],
              imageFileId: target.image
                ? file.id
                : (existing as AttachmentDraft)?.imageFileId,
              baseImageId:
                existing?.kind === 'ATTACHMENTS'
                  ? existing.baseImageId
                  : json<OfflineSnapshot>(workOrder).workOrder.image?.id ?? null
            };
      const fields = {
        work_order_id: String(workOrderId),
        task_id: target.taskId ? String(target.taskId) : '',
        payload: JSON.stringify(draft),
        revision: clock.revision
      };
      await this.commit([
        this.clockChange(clock),
        row ? this.change(row, fields) : this.create(DRAFTS, id, fields),
        this.create(FILES, file.id, {
          payload: JSON.stringify({
            ...file,
            target,
            importWorkOrderId: workOrderId
          }),
          server_file_id: null
        })
      ]);
    });
  }

  async putFile(file: LocalFile): Promise<void> {
    this.assertStorage();
    await this.database.write(async () => {
      const row = await this.find(FILES, file.id);
      if (row && json<LocalFile>(row).status === 'DELETING')
        throw new OfflineError('FILE_REMOVED');
      // Filesystem completions may be older than a snapshot/receipt update.
      // Only the SQLite writer owns live references and transfer associations.
      const current = row ? json<LocalFile>(row) : null;
      const next = current
        ? {
            ...file,
            workOrderIds: current.workOrderIds,
            transferIds: current.transferIds,
            serverFileId: current.serverFileId ?? file.serverFileId
          }
        : file;
      const fields = {
        payload: JSON.stringify(next),
        server_file_id: next.serverFileId ?? null
      };
      await this.commit([
        row ? this.change(row, fields) : this.create(FILES, file.id, fields)
      ]);
    });
  }

  async allFiles(): Promise<LocalFile[]> {
    this.assertStorage();
    return (await this.collection(FILES).query().fetch()).map((row) =>
      json<LocalFile>(row)
    );
  }

  private async fileVisible(file: LocalFile): Promise<boolean> {
    if (file.target?.kind === 'TASK') {
      const task = await this.find(TASKS, String(file.target.taskId));
      if (!task || !raw(task, 'available')) return false;
    }
    const ids = new Set([
      ...file.workOrderIds,
      ...(file.importWorkOrderId ? [file.importWorkOrderId] : [])
    ]);
    for (const id of ids) {
      const row = await this.find(WORK_ORDERS, String(id));
      if (row && raw(row, 'available')) return true;
    }
    return false;
  }

  async getFiles(ids: string[]): Promise<LocalFile[]> {
    this.assertStorage();
    const files: LocalFile[] = [];
    for (const id of ids) {
      const row = await this.transferFileRow(id);
      if (!row) continue;
      const file = json<LocalFile>(row);
      if (file.status === 'DELETING' || !(await this.fileVisible(file)))
        continue;
      const transfer = await this.getOperation(
        file.transferIds?.[file.transferIds.length - 1] || id
      );
      files.push({ ...file, transferStatus: transfer?.status });
    }
    return files;
  }

  async getServerFile(
    workOrderId: number,
    serverFileId: number
  ): Promise<LocalFile | null> {
    const workOrder = await this.find(WORK_ORDERS, String(workOrderId));
    if (!workOrder || !raw(workOrder, 'available')) return null;
    const rows = await this.collection(FILES)
      .query(Q.where('server_file_id', serverFileId))
      .fetch();
    return (
      rows
        .map((row) => json<LocalFile>(row))
        .filter(
          (file) =>
            file.status !== 'DELETING' &&
            file.workOrderIds.includes(workOrderId)
        )
        .sort(
          (a, b) =>
            (a.status === 'READY' ? 0 : a.status === 'IMPORTING' ? 1 : 2) -
            (b.status === 'READY' ? 0 : b.status === 'IMPORTING' ? 1 : 2)
        )[0] || null
    );
  }

  async removeDraftFile(
    workOrderId: number,
    target: FileTarget,
    fileId: string,
    expectedRevision: number
  ): Promise<void> {
    this.assertStorage();
    await this.database.write(async () => {
      await this.fileTarget(workOrderId, target);
      const row = await this.find(
        DRAFTS,
        this.fileDraftId(workOrderId, target)
      );
      if (!row || Number(raw(row, 'revision')) !== expectedRevision)
        throw new OfflineError('DRAFT_CHANGED');
      const draft = json<CommentDraft | AttachmentDraft>(row);
      const clock = await this.nextDraftRevision();
      const next = {
        ...draft,
        fileIds: (draft.fileIds || []).filter((id) => id !== fileId),
        revision: clock.revision,
        updatedAt: this.now(),
        ...(draft.kind === 'ATTACHMENTS' && draft.imageFileId === fileId
          ? { imageFileId: undefined }
          : {})
      };
      const empty =
        next.kind === 'ATTACHMENTS' &&
        !next.fileIds.length &&
        !next.imageFileId;
      await this.commit([
        this.clockChange(clock),
        empty
          ? row.prepareDestroyPermanently()
          : this.change(row, {
              payload: JSON.stringify(next),
              revision: clock.revision
            })
      ]);
    });
  }

  private async transferFileRow(id: string): Promise<Model | null> {
    const direct = await this.find(FILES, id);
    if (direct) return direct;
    return (
      (await this.collection(FILES).query().fetch()).find((row) =>
        json<LocalFile>(row).transferIds?.includes(id)
      ) || null
    );
  }
  async getTransferFile(id: string): Promise<LocalFile | null> {
    this.assertStorage();
    const row = await this.transferFileRow(id);
    return row ? json<LocalFile>(row) : null;
  }

  private async prepareTransfers(
    workOrderId: number,
    target: FileTarget,
    ids: string[],
    revision: number
  ): Promise<{ records: () => Model[]; ids: string[] }> {
    if (new Set(ids).size !== ids.length)
      throw new OfflineError('FILE_IDENTITY');
    const requests: OperationRequest[] = [],
      files: { row: Model; file: LocalFile }[] = [],
      transfers: string[] = [];
    for (const id of ids) {
      const row = await this.find(FILES, id);
      const file = row ? json<LocalFile>(row) : null;
      if (
        !file ||
        file.status !== 'READY' ||
        !/^[a-f0-9]{64}$/.test(file.sha256)
      )
        throw new OfflineError('FILE_NOT_READY');
      if (
        file.importWorkOrderId !== workOrderId ||
        file.target?.kind !== target.kind ||
        file.target?.taskId !== target.taskId
      )
        throw new OfflineError('FILE_IDENTITY');
      const previousId =
        file.transferIds?.[file.transferIds.length - 1] || file.id;
      const previous = await this.getOperation(previousId);
      if (
        previous?.status === 'APPLIED' &&
        previous.receipt?.visibility !== 'FILE_DELETED'
      ) {
        transfers.push(previous.id);
        continue;
      }
      if (
        previous &&
        !['REJECTED', 'ABANDONED', 'CONFLICT'].includes(previous.status) &&
        previous.receipt?.visibility !== 'FILE_DELETED'
      )
        throw new OfflineError('RESULT_UNKNOWN');
      const transferId = previous ? this.options.uuid() : file.id;
      transfers.push(transferId);
      requests.push({
        ...this.identity(),
        operationId: transferId,
        workOrderId,
        type: 'UPLOAD_FILE',
        target: target.kind,
        ...(target.taskId ? { taskId: target.taskId } : {}),
        name: file.name,
        mimeType: file.mimeType,
        size: file.size,
        sha256: file.sha256,
        fileType: file.mimeType.startsWith('image/') ? 'IMAGE' : 'OTHER',
        occurredAt: new Date(this.now()).toISOString()
      });
      files.push({
        row: row!,
        file: {
          ...file,
          transferIds: [...new Set([...(file.transferIds || []), transferId])]
        }
      });
    }
    return {
      ids: transfers,
      records: () => [
        ...requests.map((request) => this.createOperation(request, revision)),
        ...files.map(({ row, file }) =>
          this.change(row, { payload: JSON.stringify(file) })
        )
      ]
    };
  }

  async submitAttachmentDraft(
    workOrderId: number,
    expectedRevision: number,
    taskId?: number
  ): Promise<LocalOperation> {
    this.assertStorage();
    return this.database.write(async () => {
      await this.fileTarget(workOrderId, {
        kind: taskId ? 'TASK' : 'WORK_ORDER',
        taskId
      });
      const row = await this.find(
        DRAFTS,
        attachmentDraftId(workOrderId, taskId)
      );
      if (!row || Number(raw(row, 'revision')) !== expectedRevision)
        throw new OfflineError('DRAFT_CHANGED');
      const draft = json<AttachmentDraft>(row);
      const ids = [
        ...draft.fileIds,
        ...(draft.imageFileId ? [draft.imageFileId] : [])
      ];
      if (!ids.length) throw new OfflineError('FILE_NOT_READY');
      const transfers = await this.prepareTransfers(
        workOrderId,
        { kind: taskId ? 'TASK' : 'WORK_ORDER', taskId },
        ids,
        expectedRevision
      );
      const common = {
        ...this.identity(),
        operationId: this.options.uuid(),
        workOrderId,
        occurredAt: new Date(this.now()).toISOString()
      };
      const request: OperationRequest = taskId
        ? {
            ...common,
            type: 'ADD_TASK_IMAGES',
            taskId,
            fileTransferIds: transfers.ids
          }
        : {
            ...common,
            type: 'UPDATE_WORK_ORDER_ATTACHMENTS',
            addTransferIds: transfers.ids.slice(0, draft.fileIds.length),
            ...(draft.imageFileId
              ? {
                  imageChange: {
                    transferId: transfers.ids[transfers.ids.length - 1],
                    baseImageId: draft.baseImageId
                  }
                }
              : {})
          };
      const operation = this.createOperation(request, expectedRevision);
      await this.commit([
        ...transfers.records(),
        operation,
        row.prepareDestroyPermanently()
      ]);
      return this.operation(operation);
    });
  }

  abandonAttachmentDraft(
    workOrderId: number,
    expectedRevision: number,
    taskId?: number
  ): Promise<void> {
    return this.abandonDraftRow(
      attachmentDraftId(workOrderId, taskId),
      expectedRevision
    );
  }

  private async applyFileReceipt(
    row: Model,
    operation: LocalOperation,
    receipt: OperationReceipt
  ): Promise<void> {
    const request = operation.request;
    if (receipt.task || receipt.commentId != null)
      throw new OfflineError('INVALID_RECEIPT');
    if (
      request.type === 'UPLOAD_FILE' &&
      receipt.status === 'APPLIED' &&
      (!Number.isSafeInteger(receipt.fileId) || receipt.fileId! <= 0)
    )
      throw new OfflineError('INVALID_RECEIPT');
    const records: Model[] = [];
    if (request.type === 'UPLOAD_FILE' && receipt.status === 'APPLIED') {
      const fileRow = await this.transferFileRow(operation.id);
      if (fileRow) {
        const file = json<LocalFile>(fileRow);
        records.push(
          this.change(fileRow, {
            payload: JSON.stringify({ ...file, serverFileId: receipt.fileId }),
            server_file_id: receipt.fileId!
          })
        );
      }
    } else if (receipt.status === 'APPLIED') {
      // Preserve the copy until the following complete snapshot acknowledges
      // the relation, even if that refresh is too large or the response is lost.
      for (const id of transferIds(request)) {
        const fileRow = await this.transferFileRow(id);
        if (!fileRow) continue;
        const file = json<LocalFile>(fileRow);
        records.push(
          this.change(fileRow, {
            payload: JSON.stringify({
              ...file,
              workOrderIds: [
                ...new Set([...file.workOrderIds, request.workOrderId])
              ]
            })
          })
        );
      }
    }
    const workOrder = await this.find(WORK_ORDERS, String(request.workOrderId));
    if (
      workOrder &&
      (receipt.code === 'ACCESS_REVOKED' ||
        receipt.visibility === 'WORK_ORDER_UNAVAILABLE')
    )
      records.push(
        this.change(workOrder, {
          available: false,
          unavailable_reason: 'ACCESS_REVOKED',
          availability_revision:
            Number(raw(workOrder, 'availability_revision')) + 1
        })
      );
    if (
      'taskId' in request &&
      request.taskId != null &&
      (receipt.code === 'TARGET_NOT_FOUND' ||
        receipt.visibility === 'TARGET_UNAVAILABLE')
    ) {
      const task = await this.find(TASKS, String(request.taskId));
      if (task)
        records.push(
          this.change(task, {
            available: false,
            unavailable_reason: 'TARGET_NOT_FOUND'
          })
        );
    }
    records.push(
      this.change(row, {
        status: receipt.status,
        receipt: JSON.stringify(receipt),
        updated_at: this.now()
      })
    );
    await this.commit(records);
  }

  private async retainedFileIds(): Promise<Set<string>> {
    const retained = new Set<string>();
    for (const row of await this.collection(DRAFTS).query().fetch()) {
      const draft = json<CommentDraft | AttachmentDraft>(row);
      (draft.fileIds || []).forEach((id) => retained.add(id));
      if (draft.kind === 'ATTACHMENTS' && draft.imageFileId)
        retained.add(draft.imageFileId);
    }
    for (const row of await this.unresolvedRows()) {
      const operation = this.operation(row);
      for (const id of operation.request.type === 'UPLOAD_FILE'
        ? [operation.id]
        : transferIds(operation.request)) {
        const file = await this.transferFileRow(id);
        if (file) retained.add(file.id);
      }
    }
    return retained;
  }

  /** Deletion intent is durable and reference-checked in the same SQLite writer. */
  async queueUnusedFiles(explicitId?: string): Promise<LocalFile[]> {
    this.assertStorage();
    return this.database.write(async () => {
      const retained = await this.retainedFileIds();
      const records: Model[] = [],
        deleting: LocalFile[] = [];
      for (const row of await this.collection(FILES).query().fetch()) {
        const file = json<LocalFile>(row);
        const explicit = row.id === explicitId;
        if (explicit && (retained.has(row.id) || file.workOrderIds.length > 1))
          throw new OfflineError('FILE_IN_USE');
        if (
          retained.has(row.id) ||
          (file.status !== 'DELETING' && !explicit && file.workOrderIds.length)
        )
          continue;
        const next: LocalFile = {
          ...file,
          status: 'DELETING',
          workOrderIds: []
        };
        if (file.status !== 'DELETING')
          records.push(this.change(row, { payload: JSON.stringify(next) }));
        deleting.push(next);
      }
      if (records.length) await this.commit(records);
      return deleting;
    });
  }

  async acknowledgeFileDeletion(id: string): Promise<void> {
    this.assertStorage();
    await this.database.write(async () => {
      const row = await this.find(FILES, id);
      if (row && json<LocalFile>(row).status === 'DELETING')
        await this.commit([row.prepareDestroyPermanently()]);
    });
  }

  private snapshotFileIds(snapshot: OfflineSnapshot): Set<number> {
    return new Set([
      ...(snapshot.workOrder.image ? [snapshot.workOrder.image.id] : []),
      ...(snapshot.workOrder.files || []).map((file) => file.id),
      ...snapshot.tasks.flatMap((task) =>
        (task.images || []).map((file) => file.id)
      ),
      ...snapshot.comments.flatMap((comment) =>
        (comment.files || []).map((file) => file.id)
      )
    ]);
  }

  private async snapshotFileChanges(
    snapshot: OfflineSnapshot
  ): Promise<Model[]> {
    const ids = this.snapshotFileIds(snapshot),
      records: Model[] = [];
    for (const row of await this.collection(FILES).query().fetch()) {
      const file = json<LocalFile>(row);
      if (file.status === 'DELETING') continue;
      const refs = file.workOrderIds.filter(
        (id) => id !== snapshot.workOrder.id
      );
      if (file.serverFileId && ids.has(file.serverFileId))
        refs.push(snapshot.workOrder.id);
      if (JSON.stringify(refs) !== JSON.stringify(file.workOrderIds))
        records.push(
          this.change(row, {
            payload: JSON.stringify({ ...file, workOrderIds: refs })
          })
        );
    }
    for (const row of await this.collection(OPERATIONS)
      .query(
        Q.where('work_order_id', String(snapshot.workOrder.id)),
        Q.where('status', 'APPLIED')
      )
      .fetch()) {
      const operation = this.operation(row);
      if (
        !operation.snapshotConfirmed &&
        ['ADD_TASK_IMAGES', 'UPDATE_WORK_ORDER_ATTACHMENTS'].includes(
          operation.request.type
        ) &&
        operation.receipt &&
        Date.parse(snapshot.preparedAt) >=
          Date.parse(operation.receipt.receivedAt)
      )
        records.push(this.change(row, { snapshot_confirmed: true }));
    }
    return records;
  }

  private async snapshotAssetChanges(
    snapshot: OfflineSnapshot
  ): Promise<Model[]> {
    const accessRow = await this.find(ASSETS, 'access');
    const previousAccess = accessRow ? json<AssetAccess>(accessRow) : null;
    const incoming = new Map(
      (snapshot.assets || []).map((asset) => [String(asset.id), asset])
    );
    const rows = await this.collection(ASSETS)
      .query(Q.where('id', Q.notEq('access')))
      .fetch();
    const records: Model[] = [];
    if (
      !previousAccess ||
      Date.parse(snapshot.preparedAt) >= Date.parse(previousAccess.observedAt)
    ) {
      const fields = {
        payload: JSON.stringify({
          observedAt: snapshot.preparedAt,
          canRead: snapshot.canReadAssets === true,
          canScan: snapshot.canScan === true
        } satisfies AssetAccess)
      };
      records.push(
        accessRow
          ? this.change(accessRow, fields)
          : this.create(ASSETS, 'access', fields)
      );
    }
    for (const row of rows) {
      const copy = json<AssetCopy>(row),
        asset = incoming.get(row.id);
      const refs = copy.refs.filter((id) => id !== snapshot.workOrder.id);
      if (asset) refs.push(snapshot.workOrder.id);
      // A newer authoritative omission cannot be undone by an older snapshot
      // stored for another preparation. Only a fresh server projection revives it.
      const newer =
        Date.parse(snapshot.preparedAt) >= Date.parse(copy.observedAt);
      const wasReferenced = copy.refs.includes(snapshot.workOrder.id);
      const next: AssetCopy = {
        asset: asset && newer ? asset : copy.asset,
        refs,
        observedAt:
          newer && (asset || wasReferenced)
            ? snapshot.preparedAt
            : copy.observedAt,
        available: newer && (asset || wasReferenced) ? !!asset : copy.available
      };
      if (!refs.length) records.push(row.prepareDestroyPermanently());
      else records.push(this.change(row, { payload: JSON.stringify(next) }));
      incoming.delete(row.id);
    }
    for (const [id, asset] of incoming)
      records.push(
        this.create(ASSETS, id, {
          payload: JSON.stringify({
            asset,
            refs: [snapshot.workOrder.id],
            observedAt: snapshot.preparedAt,
            available: true
          } satisfies AssetCopy)
        })
      );
    return records;
  }

  async getAsset(id: number): Promise<OfflineAsset | null> {
    this.assertStorage();
    const authorization = await this.find(ASSETS, 'access');
    if (!authorization || !json<AssetAccess>(authorization).canRead)
      return null;
    const row = await this.find(ASSETS, String(id));
    if (!row) return null;
    const copy = json<AssetCopy>(row);
    if (!copy.available) return null;
    for (const ref of copy.refs) {
      const workOrder = await this.find(WORK_ORDERS, String(ref));
      if (workOrder && raw(workOrder, 'available')) return copy.asset;
    }
    return null;
  }

  async canScan(): Promise<boolean> {
    this.assertStorage();
    const authorization = await this.find(ASSETS, 'access');
    return (
      !!authorization &&
      json<AssetAccess>(authorization).canRead &&
      json<AssetAccess>(authorization).canScan &&
      (await this.collection(WORK_ORDERS)
        .query(Q.where('available', true))
        .fetchCount()) > 0
    );
  }

  async findAssetByScan(
    value: string,
    kind: 'barcode' | 'nfc'
  ): Promise<OfflineAsset | null> {
    if (!(await this.canScan())) throw new OfflineError('SCAN_FORBIDDEN');
    const matches: OfflineAsset[] = [];
    for (const row of await this.collection(ASSETS)
      .query(Q.where('id', Q.notEq('access')))
      .fetch()) {
      const copy = json<AssetCopy>(row);
      if (copy.asset[kind === 'barcode' ? 'barCode' : 'nfcId'] !== value)
        continue;
      const asset = await this.getAsset(Number(row.id));
      if (!asset) continue;
      for (const id of copy.refs) {
        const workOrder = await this.find(WORK_ORDERS, String(id));
        if (
          workOrder &&
          raw(workOrder, 'available') &&
          json<OfflineSnapshot>(workOrder).canScan
        ) {
          matches.push(asset);
          break;
        }
      }
    }
    if (matches.length > 1) throw new OfflineError('SCAN_AMBIGUOUS');
    return matches[0] || null;
  }

  async getPreparedAssetWorkOrders(assetId: number): Promise<WorkOrder[]> {
    if (!(await this.getAsset(assetId))) return [];
    const row = await this.find(ASSETS, String(assetId));
    const workOrders: WorkOrder[] = [];
    for (const id of json<AssetCopy>(row!).refs) {
      const workOrder = await this.find(WORK_ORDERS, String(id));
      if (workOrder && raw(workOrder, 'available'))
        workOrders.push(json<OfflineSnapshot>(workOrder).workOrder);
    }
    return workOrders;
  }

  observeAsset(id: number) {
    return this.changes().pipe(switchMap(() => this.getAsset(id)));
  }
  observeAttachmentDraft(workOrderId: number, taskId?: number) {
    return this.changes().pipe(
      switchMap(() => this.getAttachmentDraft(workOrderId, taskId))
    );
  }
  observeFiles(ids: string[]) {
    return this.changes().pipe(switchMap(() => this.getFiles(ids)));
  }
  observeServerFile(workOrderId: number, serverFileId: number) {
    return this.changes().pipe(
      switchMap(() => this.getServerFile(workOrderId, serverFileId))
    );
  }

  private changes() {
    return combineLatest([
      this.collection(FILES)
        .query()
        .observeWithColumns(['payload', 'server_file_id']),
      this.collection(ASSETS).query().observeWithColumns(['payload']),
      this.collection(WORK_ORDERS)
        .query()
        .observeWithColumns([
          'payload',
          'available',
          'unavailable_reason',
          'prepared_at'
        ]),
      this.collection(TASKS)
        .query()
        .observeWithColumns(['payload', 'available', 'unavailable_reason']),
      this.collection(DRAFTS)
        .query()
        .observeWithColumns(['payload', 'revision']),
      this.collection(OPERATIONS)
        .query()
        .observeWithColumns([
          'status',
          'attempt_count',
          'receipt',
          'server_comment_id',
          'snapshot_confirmed'
        ])
    ]);
  }
  observeWorkOrder(workOrderId: number) {
    return this.changes().pipe(
      switchMap(() => this.getWorkOrderProjection(workOrderId))
    );
  }
  observeTask(taskId: number, field: TaskField = 'value') {
    return this.changes().pipe(switchMap(() => this.getTask(taskId, field)));
  }
  observePendingWork(workOrderId?: number) {
    return this.changes().pipe(
      switchMap(() => this.getPendingWork(workOrderId))
    );
  }
}
