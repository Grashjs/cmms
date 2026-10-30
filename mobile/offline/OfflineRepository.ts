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
  TaskView
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
const draftId = (taskId: number) => `${taskId}:value`;

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
      createdAt: Number(raw(row, 'created_at')),
      updatedAt: Number(raw(row, 'updated_at'))
    };
  }
  private async unresolvedRows(taskId?: number, workOrderId?: number) {
    return this.collection(OPERATIONS)
      .query(
        Q.where('status', Q.oneOf(UNRESOLVED)),
        ...(taskId === undefined ? [] : [Q.where('task_id', String(taskId))]),
        ...(workOrderId === undefined
          ? []
          : [Q.where('work_order_id', String(workOrderId))]),
        Q.sortBy('created_at', Q.asc)
      )
      .fetch();
  }
  private async usableTask(
    workOrderId: number,
    taskId: number,
    editing = false
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
    return { workOrder, task, server: json<OfflineTask>(task) };
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
      const records: Model[] = [];
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

  async saveDraft(
    workOrderId: number,
    taskId: number,
    value: string
  ): Promise<TaskDraft> {
    this.assertStorage();
    if (typeof value !== 'string') throw new OfflineError('INVALID_VALUE');
    return this.database.write(async () => {
      const { server } = await this.usableTask(workOrderId, taskId, true);
      if (server.taskBase.taskType !== 'TEXT')
        throw new OfflineError('UNSUPPORTED_TASK');
      const existing = await this.find(DRAFTS, draftId(taskId));
      const original = existing ? json<TaskDraft>(existing) : null;
      const operations = await this.unresolvedRows(taskId);
      const predecessor = operations.length
        ? this.operation(operations[operations.length - 1])
        : null;
      const clock = await this.nextDraftRevision();
      const draft: TaskDraft = {
        id: draftId(taskId),
        workOrderId,
        taskId,
        revision: clock.revision,
        value,
        baseVersion:
          original?.baseVersion ??
          predecessor?.request.baseVersion ??
          server.version,
        baseValue:
          original?.baseValue ??
          predecessor?.request.value ??
          String(server.value ?? ''),
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
    expectedDraftRevision: number
  ): Promise<LocalOperation> {
    this.assertStorage();
    return this.database.write(async () => {
      const row = await this.find(DRAFTS, draftId(taskId));
      if (!row || Number(raw(row, 'revision')) !== expectedDraftRevision) {
        throw new OfflineError('DRAFT_CHANGED');
      }
      const draft = json<TaskDraft>(row);
      const { server } = await this.usableTask(draft.workOrderId, taskId, true);
      if (server.taskBase.taskType !== 'TEXT')
        throw new OfflineError('UNSUPPORTED_TASK');
      if (draft.value.length > 255) throw new OfflineError('TEXT_TOO_LONG');
      if (
        draft.basedOnOperationId ||
        draft.blockedReason ||
        (await this.unresolvedRows(taskId)).length
      ) {
        throw new OfflineError('PREVIOUS_OPERATION');
      }
      const now = this.now();
      const id = this.options.uuid();
      const context = JSON.parse(this.contextKey);
      if (
        !Array.isArray(context) ||
        !Number.isSafeInteger(context[1]) ||
        !Number.isSafeInteger(context[2])
      ) {
        throw new OfflineError('INVALID_CONTEXT');
      }
      const request: OperationRequest = {
        companyId: context[1],
        userId: context[2],
        operationId: id,
        type: 'SET_TASK_VALUE',
        workOrderId: draft.workOrderId,
        taskId,
        value: draft.value,
        baseVersion: draft.baseVersion,
        occurredAt: new Date(now).toISOString()
      };
      const operation = this.create(OPERATIONS, id, {
        work_order_id: String(draft.workOrderId),
        task_id: String(taskId),
        request: JSON.stringify(request),
        status: 'QUEUED',
        attempt_count: 0,
        receipt: null,
        created_at: now,
        updated_at: now
      });
      await this.commit([operation, row.prepareDestroyPermanently()]);
      return this.operation(operation);
    });
  }

  async abandonDraft(taskId: number, expectedRevision: number): Promise<void> {
    this.assertStorage();
    await this.database.write(async () => {
      const row = await this.find(DRAFTS, draftId(taskId));
      if (!row || Number(raw(row, 'revision')) !== expectedRevision) {
        throw new OfflineError('DRAFT_CHANGED');
      }
      await this.commit([row.prepareDestroyPermanently()]);
    });
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
      ) {
        throw new OfflineError('RESULT_UNKNOWN');
      }
      const draftRow = await this.find(
        DRAFTS,
        draftId(operation.request.taskId)
      );
      const draft = draftRow ? json<TaskDraft>(draftRow) : null;
      const records = [
        this.change(row, { status: 'ABANDONED', updated_at: this.now() })
      ];
      if (draft?.basedOnOperationId === operationId) {
        // The following draft was written on top of an unconfirmed value. It
        // requires explicit review, even when its predecessor is abandoned.
        records.push(
          this.change(draftRow!, {
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

  /** Explicit user decision to keep the displayed draft against a reviewed server revision. */
  async resolveDraftAgainstServer(
    taskId: number,
    expectedDraftRevision: number,
    expectedServerVersion: number
  ): Promise<TaskDraft> {
    this.assertStorage();
    return this.database.write(async () => {
      const row = await this.find(DRAFTS, draftId(taskId));
      if (!row || Number(raw(row, 'revision')) !== expectedDraftRevision)
        throw new OfflineError('DRAFT_CHANGED');
      const draft = json<TaskDraft>(row);
      const { server } = await this.usableTask(draft.workOrderId, taskId, true);
      if (server.version !== expectedServerVersion)
        throw new OfflineError('SERVER_CHANGED');
      const operations = await this.unresolvedRows(taskId);
      if (
        operations.some((operation) =>
          ACTIVE.includes(this.operation(operation).status)
        )
      ) {
        throw new OfflineError('RESULT_UNKNOWN');
      }
      const clock = await this.nextDraftRevision();
      const resolved: TaskDraft = {
        ...draft,
        revision: clock.revision,
        baseVersion: server.version,
        baseValue: String(server.value ?? ''),
        basedOnOperationId: undefined,
        blockedReason: undefined,
        updatedAt: this.now()
      };
      await this.commit([
        this.clockChange(clock),
        this.change(row, {
          payload: JSON.stringify(resolved),
          revision: resolved.revision
        }),
        ...operations.map((operation) =>
          this.change(operation, {
            status: 'ABANDONED',
            updated_at: this.now()
          })
        )
      ]);
      return resolved;
    });
  }

  /** Resolve a terminal operation, optionally with a newer draft explicitly reviewed by the user. */
  async resolveOperationAgainstServer(
    operationId: string,
    expectedDraftRevision: number | null,
    expectedServerVersion: number
  ): Promise<TaskDraft> {
    this.assertStorage();
    return this.database.write(async () => {
      const operationRow = await this.find(OPERATIONS, operationId);
      if (!operationRow) throw new OfflineError('OPERATION_MISSING');
      const operation = this.operation(operationRow);
      if (operation.status !== 'CONFLICT' && operation.status !== 'REJECTED') {
        throw new OfflineError('RESULT_UNKNOWN');
      }
      const { taskId, workOrderId } = operation.request;
      const { server } = await this.usableTask(workOrderId, taskId, true);
      if (server.version !== expectedServerVersion)
        throw new OfflineError('SERVER_CHANGED');
      const draftRow = await this.find(DRAFTS, draftId(taskId));
      const draft = draftRow ? json<TaskDraft>(draftRow) : null;
      if ((draft?.revision ?? null) !== expectedDraftRevision)
        throw new OfflineError('DRAFT_CHANGED');
      const operations = await this.unresolvedRows(taskId);
      if (operations.some((row) => row.id !== operationId))
        throw new OfflineError('PREVIOUS_OPERATION');
      const clock = await this.nextDraftRevision();
      const resolved: TaskDraft = {
        id: draftId(taskId),
        workOrderId,
        taskId,
        revision: clock.revision,
        value: draft?.value ?? operation.request.value,
        baseVersion: server.version,
        baseValue: String(server.value ?? ''),
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
        this.change(operationRow, {
          status: 'ABANDONED',
          updated_at: this.now()
        })
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
      if (!row) return; // A response for another isolated context cannot create data.
      const operation = this.operation(row);
      if (operation.receipt || operation.status === 'ABANDONED') return;
      if (
        receipt.task &&
        (receipt.task.id !== operation.request.taskId ||
          !Number.isSafeInteger(receipt.task.version))
      ) {
        throw new OfflineError('INVALID_RECEIPT');
      }
      const taskRow = await this.find(TASKS, String(operation.request.taskId));
      const draftRow = await this.find(
        DRAFTS,
        draftId(operation.request.taskId)
      );
      const draft = draftRow ? json<TaskDraft>(draftRow) : null;
      const workOrder = await this.find(
        WORK_ORDERS,
        String(operation.request.workOrderId)
      );
      const clock =
        draft && draft.basedOnOperationId === receipt.operationId
          ? await this.nextDraftRevision()
          : null;
      const records: Model[] = [
        this.change(row, {
          status: receipt.status,
          receipt: JSON.stringify(receipt),
          updated_at: this.now()
        })
      ];
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
      // Historical APPLIED/CONFLICT receipts lose their task payload when the
      // server can no longer disclose it. Preserve the historical outcome, but
      // hide the complete local target now, even if the next request loses the
      // network. A later authorized complete preparation can restore access.
      const withheldTask =
        !receipt.task &&
        (receipt.status === 'APPLIED' || receipt.status === 'CONFLICT');
      if ((receipt.code === 'ACCESS_REVOKED' || withheldTask) && workOrder) {
        records.push(
          this.change(workOrder, {
            available: false,
            unavailable_reason:
              receipt.code === 'ACCESS_REVOKED'
                ? 'ACCESS_REVOKED'
                : 'RECEIPT_WITHOUT_TASK',
            availability_revision:
              Number(raw(workOrder, 'availability_revision')) + 1
          })
        );
      }
      if (draft && draft.basedOnOperationId === receipt.operationId) {
        const next: TaskDraft =
          receipt.status === 'APPLIED' && receipt.task
            ? {
                ...draft,
                basedOnOperationId: undefined,
                blockedReason: undefined,
                baseVersion: receipt.task.version,
                baseValue: String(receipt.task.value ?? ''),
                revision: clock!.revision
              }
            : {
                ...draft,
                blockedReason: withheldTask
                  ? 'RECEIPT_WITHOUT_TASK'
                  : receipt.code || receipt.status,
                revision: clock!.revision
              };
        records.push(this.clockChange(clock!));
        records.push(
          this.change(draftRow!, {
            payload: JSON.stringify(next),
            revision: next.revision
          })
        );
      }
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

  async getTask(taskId: number): Promise<TaskView> {
    this.assertStorage();
    return this.database.read(async () => {
      const [taskRow, draftRow, operations] = await Promise.all([
        this.find(TASKS, String(taskId)),
        this.find(DRAFTS, draftId(taskId)),
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
      const lastApplied = operations.length
        ? []
        : await this.collection(OPERATIONS)
            .query(
              Q.where('task_id', String(taskId)),
              Q.where('status', 'APPLIED'),
              Q.sortBy('updated_at', Q.desc),
              Q.take(1)
            )
            .fetch();
      const operation = operations.length
        ? this.operation(operations[operations.length - 1])
        : lastApplied.length
        ? this.operation(lastApplied[0])
        : null;
      const draft = draftRow ? json<TaskDraft>(draftRow) : null;
      const serverTask = available ? json<OfflineTask>(taskRow!) : null;
      return {
        serverTask,
        task: serverTask
          ? {
              ...serverTask,
              value:
                draft?.value ??
                (operation && operation.status !== 'APPLIED'
                  ? operation.request.value
                  : serverTask.value)
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
        canEdit: !!(available && json<OfflineSnapshot>(workOrder!).canEdit)
      };
    });
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
        draftRows.map((draft) => [
          String(raw(draft, 'task_id')),
          json<TaskDraft>(draft)
        ])
      );
      const operations = new Map(
        operationRows.map((operation) => [
          String(raw(operation, 'task_id')),
          this.operation(operation)
        ])
      );
      const projectedTasks = tasks.map((task) => {
        const server = json<OfflineTask>(task);
        return {
          ...server,
          value:
            drafts.get(task.id)?.value ??
            operations.get(task.id)?.request.value ??
            server.value
        };
      });
      return {
        id: workOrderId,
        workOrder: snapshot?.workOrder || null,
        tasks: projectedTasks,
        comments: snapshot?.comments || [],
        history: snapshot?.history || [],
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
    const [drafts, operations] = await Promise.all([
      this.collection(DRAFTS)
        .query(
          ...(workOrderId === undefined
            ? []
            : [Q.where('work_order_id', String(workOrderId))])
        )
        .fetch(),
      this.unresolvedRows(undefined, workOrderId)
    ]);
    const items: PendingWork[] = [];
    for (const row of drafts) {
      const draft = json<TaskDraft>(row);
      const target = await this.getTask(draft.taskId);
      items.push({
        kind: 'DRAFT',
        id: draft.id,
        workOrderId: draft.workOrderId,
        taskId: draft.taskId,
        draftRevision: draft.revision,
        targetAvailable: target.available,
        reason: target.unavailableReason || draft.blockedReason || null,
        canAbandon: true,
        ...(target.available
          ? { label: target.task?.taskBase.label, value: draft.value }
          : {})
      });
    }
    for (const row of operations) {
      const operation = this.operation(row);
      const target = await this.getTask(operation.request.taskId);
      items.push({
        kind: 'OPERATION',
        id: operation.id,
        workOrderId: operation.request.workOrderId,
        taskId: operation.request.taskId,
        status: operation.status,
        attemptCount: operation.attemptCount,
        targetAvailable: target.available,
        reason: target.unavailableReason || operation.receipt?.code || null,
        canAbandon:
          (operation.status === 'QUEUED' && operation.attemptCount === 0) ||
          operation.status === 'REJECTED' ||
          operation.status === 'CONFLICT',
        ...(target.available
          ? {
              label: target.task?.taskBase.label,
              value: operation.request.value
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
        .observeWithColumns(['status', 'attempt_count', 'receipt'])
    ]);
  }
  observeWorkOrder(workOrderId: number) {
    return this.changes().pipe(
      switchMap(() => this.getWorkOrderProjection(workOrderId))
    );
  }
  observeTask(taskId: number) {
    return this.changes().pipe(switchMap(() => this.getTask(taskId)));
  }
  observePendingWork(workOrderId?: number) {
    return this.changes().pipe(
      switchMap(() => this.getPendingWork(workOrderId))
    );
  }
}
