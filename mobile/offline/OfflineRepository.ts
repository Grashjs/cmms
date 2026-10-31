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
  utf8ByteLength
} from './types';

const WORK_ORDERS = 'offline_work_orders';
const TASKS = 'offline_tasks';
const DRAFTS = 'offline_drafts';
const OPERATIONS = 'offline_operations';
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
    return rows.sort((a, b) => {
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
      const records: Model[] = [];
      associations.forEach(({ row, commentId }) =>
        records.push(this.change(row, { server_comment_id: commentId }))
      );
      if (ownsIntent) records.push(cleanup!.prepareDestroyPermanently());
      // Tasks live in their own table; do not maintain a second mutable copy.
      const workOrderFields = {
        payload: JSON.stringify({ ...snapshot, tasks: [] }),
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
      const next = update({
        ...json<OfflineSnapshot>(row),
        tasks: tasks.map((task) => json<OfflineTask>(task))
      });
      if (!next) return;
      // Keep the previous complete copy if a connected response exceeds offline
      // capacity. The caller must distinguish server success from local refresh.
      assertSnapshot(next);
      await this.commit([
        this.change(row, { payload: JSON.stringify({ ...next, tasks: [] }) })
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
      task_id: request.type === 'CREATE_COMMENT' ? '' : String(request.taskId),
      request: JSON.stringify(request),
      status: 'QUEUED',
      attempt_count: 0,
      receipt: null,
      created_at: now,
      updated_at: now,
      submitted_revision: revision,
      server_comment_id: null
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
          (request && request.type !== 'CREATE_COMMENT'
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
          predecessor.request.type === 'CREATE_COMMENT' ||
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
      const request: OperationRequest = {
        ...this.identity(),
        operationId: this.options.uuid(),
        type: 'CREATE_COMMENT',
        workOrderId,
        content: draft.content,
        occurredAt: new Date(this.now()).toISOString()
      };
      const operation = this.createOperation(request, draft.revision);
      await this.commit([operation, row.prepareDestroyPermanently()]);
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
    if (request.type === 'CREATE_COMMENT' || !request.predecessorOperationId)
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
      const drafts =
        operation.request.type === 'CREATE_COMMENT'
          ? []
          : await this.collection(DRAFTS)
              .query(Q.where('task_id', String(operation.request.taskId)))
              .fetch();
      const records = [
        this.change(row, { status: 'ABANDONED', updated_at: this.now() })
      ];
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
      if (operation.request.type === 'CREATE_COMMENT')
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
        !isComment &&
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
        !isComment &&
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
      if (!isComment) {
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
              tail.request.type !== 'CREATE_COMMENT' &&
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

  async getOperationsToSync(): Promise<LocalOperation[]> {
    this.assertStorage();
    return (await this.unresolvedRows())
      .map((row) => this.operation(row))
      .filter((operation) => ACTIVE.includes(operation.status));
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
      await this.commit([
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
          .filter((row) => raw(row, 'task_id') !== '')
          .map((row) => [row.id, json<TaskDraft>(row)])
      );
      const operations = new Map<string, LocalOperation>();
      operationRows.forEach((row) => {
        const operation = this.operation(row);
        if (operation.request.type !== 'CREATE_COMMENT')
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
      const draft = json<TaskDraft | CommentDraft>(row);
      const comment = 'kind' in draft && draft.kind === 'COMMENT';
      const field = comment ? 'comment' : (draft as TaskDraft).field || 'value';
      const target = comment
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
          (!comment && (draft as TaskDraft).blockedReason) ||
          null,
        canAbandon: true,
        ...(target?.available
          ? {
              label: comment
                ? undefined
                : (target as TaskView).task?.taskBase.label,
              value: comment
                ? (draft as CommentDraft).content
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
        )
      )
        continue;
      const field = operationField(request);
      const target =
        request.type === 'CREATE_COMMENT'
          ? await this.getWorkOrderProjection(request.workOrderId)
          : await this.getTask(request.taskId, field as TaskField);
      items.push({
        kind: 'OPERATION',
        id: operation.id,
        workOrderId: request.workOrderId,
        taskId: request.type === 'CREATE_COMMENT' ? undefined : request.taskId,
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
              label: comment
                ? undefined
                : (target as TaskView).task?.taskBase.label,
              value: operationText(request)
            }
          : {})
      });
    }
    return items;
  }

  private changes() {
    return combineLatest([
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
          'server_comment_id'
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
