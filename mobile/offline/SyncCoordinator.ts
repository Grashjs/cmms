import { OfflineApi, OfflineApiError } from './OfflineApi';
import { OfflineRepository } from './OfflineRepository';
import type { OfflineFileStore } from './OfflineFileStore';
import {
  OfflineEligibility,
  OfflineError,
  OfflineTask,
  operationField,
  TaskField,
  OFFLINE_LIMITS,
  isTaskOperation,
  isTimeOperation,
  isStatusOperation,
  transferIds,
  PreparationRequest
} from './types';

interface SyncState {
  status: 'idle' | 'syncing' | 'paused' | 'authentication_required' | 'error';
}
interface SyncPolicy {
  enabled: () => boolean;
  online: () => boolean;
  sessionIsCurrent: () => boolean;
  onEligibility?: (eligibility: OfflineEligibility) => void | Promise<void>;
  onStateChanged?: (state: SyncState) => void;
  onError?: (error: unknown) => void;
  canPrepare?: () => boolean;
  canRemoveCopy?: (id: number) => boolean;
  onPreparationQueueChanged?: (requests: PreparationRequest[]) => void;
}
interface CoordinatorTimers {
  setTimeout: (
    callback: () => void,
    delay: number
  ) => ReturnType<typeof setTimeout>;
  clearTimeout: (timer: ReturnType<typeof setTimeout>) => void;
}

/** All foreground network work for an immutable account context shares this lane. */
export class SyncCoordinator {
  private tail: Promise<void> = Promise.resolve();
  private suspended = false;
  private removing = new Set<number>();
  private requested = false;
  private syncPromise: Promise<void> | null = null;
  private state: SyncState = { status: 'idle' };
  private retryTimer: ReturnType<typeof setTimeout> | null = null;
  private retryIndex = 0;
  private permissionConfirmed = false;
  private lastEligibility: OfflineEligibility | null = null;

  constructor(
    public readonly repository: OfflineRepository,
    public readonly api: OfflineApi,
    private readonly policy: SyncPolicy,
    private readonly timers: CoordinatorTimers = { setTimeout, clearTimeout },
    public readonly files?: OfflineFileStore
  ) {}

  private clearRetry() {
    if (this.retryTimer !== null) this.timers.clearTimeout(this.retryTimer);
    this.retryTimer = null;
  }
  private retryAfter(error: unknown) {
    if (
      !(error instanceof OfflineApiError) ||
      !this.connected() ||
      !(
        error.code === 'NETWORK_UNAVAILABLE' ||
        error.status === 429 ||
        error.status >= 500
      )
    )
      return;
    this.clearRetry();
    const delays = [5000, 15000, 60000, 300000];
    const delay = delays[Math.min(this.retryIndex++, delays.length - 1)];
    this.retryTimer = this.timers.setTimeout(() => {
      this.retryTimer = null;
      if (this.connected()) void this.trigger('retry').catch(() => undefined);
    }, delay);
    // Node tests should not be kept alive by a mobile foreground retry timer.
    (this.retryTimer as any)?.unref?.();
  }

  private current() {
    return !this.suspended && this.policy.sessionIsCurrent();
  }
  private connected() {
    return this.current() && this.policy.online();
  }
  private assertConnected() {
    if (!this.current()) throw new OfflineError('CONTEXT_CHANGED');
    if (!this.policy.online()) throw new OfflineError('NETWORK_UNAVAILABLE');
  }
  private emit(state: SyncState) {
    this.state = { ...this.state, ...state };
    this.policy.onStateChanged?.(this.state);
  }
  private enqueue<T>(action: () => Promise<T>): Promise<T> {
    const operation = this.tail.then(action);
    this.tail = operation.then(
      () => undefined,
      () => undefined
    );
    return operation;
  }
  /** Check server permission while the local gate is locked, without syncing work. */
  checkEligibilityOnly(): Promise<OfflineEligibility> {
    return this.enqueue(async () => {
      if (!this.current()) throw new OfflineError('CONTEXT_CHANGED');
      if (!this.policy.enabled()) throw new OfflineError('OFFLINE_DISABLED');
      const eligibility = await this.api.eligibility();
      if (!this.current()) throw new OfflineError('CONTEXT_CHANGED');
      return eligibility;
    });
  }
  private async authorize(): Promise<boolean> {
    if (!this.connected() || !this.policy.enabled()) return false;
    const eligibility = await this.api.eligibility();
    if (!this.current()) return false;
    await this.policy.onEligibility?.(eligibility);
    this.lastEligibility = eligibility;
    this.permissionConfirmed = eligibility.enabled && eligibility.eligible;
    return (
      this.connected() && this.policy.enabled() && this.permissionConfirmed
    );
  }
  private async authorizationRevoked() {
    this.permissionConfirmed = false;
    if (this.lastEligibility && this.current()) {
      this.lastEligibility = {
        ...this.lastEligibility,
        eligible: false,
        authorizedUntil: undefined
      };
      await this.policy.onEligibility?.(this.lastEligibility);
    }
  }
  private async releases() {
    for (const id of await this.repository.getPendingReleases()) {
      if (!this.connected()) return;
      await this.api.release(id);
      if (!this.current()) return;
      await this.repository.acknowledgeRelease(id);
    }
  }
  private async refreshOne(
    workOrderId: number,
    downloadFiles = true,
    request?: PreparationRequest
  ) {
    if (this.removing.has(workOrderId)) return;
    let snapshot;
    try {
      const revision = await this.repository.getAvailabilityRevision(
        workOrderId
      );
      // Reserve a durable compensating release before a new server reservation
      // can be committed without a locally acknowledged snapshot.
      const preparationToken = await this.repository.beginPreparation(
        workOrderId
      );
      this.assertConnected();
      if (this.removing.has(workOrderId)) return;
      snapshot = await this.api.prepare(workOrderId);
      if (
        request &&
        request.source !== 'MANUAL' &&
        (snapshot.workOrder.archived ||
          snapshot.workOrder.status === 'COMPLETE')
      )
        throw new OfflineError('PREPARATION_NOT_NEEDED');
      if (this.current() && !this.removing.has(workOrderId)) {
        await this.repository.prepareSnapshot(
          snapshot,
          revision,
          preparationToken,
          request && {
            manual: request.source === 'MANUAL',
            allowed: () =>
              this.canPrepareCopy() && !this.removing.has(workOrderId)
          }
        );
      } else return;
    } catch (error) {
      // Only an authoritative target response is a durable unavailability.
      // Timeouts, 5xx, over-limit and invalid/partial snapshots keep the old copy.
      if (
        this.current() &&
        error instanceof OfflineApiError &&
        error.code === 'OFFLINE_NOT_AUTHORIZED'
      ) {
        await this.authorizationRevoked();
      } else if (
        this.current() &&
        error instanceof OfflineApiError &&
        (error.status === 403 || error.status === 404)
      ) {
        await this.repository.markUnavailable(
          workOrderId,
          error.status === 403 ? 'ACCESS_REVOKED' : 'TARGET_NOT_FOUND'
        );
      }
      throw error;
    }
    if (snapshot && downloadFiles) await this.downloadPreparedFiles(workOrderId);
    return snapshot;
  }

  private canPrepareCopy() {
    return this.connected() && (this.policy.canPrepare?.() ?? true);
  }

  private async settlePreparation(
    request: PreparationRequest,
    state: PreparationRequest['state'] | null
  ) {
    if (!this.current()) return;
    const queue = await this.repository.settlePreparationRequest(
      request.id,
      state
    );
    if (this.current()) this.policy.onPreparationQueueChanged?.(queue);
  }

  private async retireCopy(id: number, mode: 'closure' | 'capacity') {
    const allowed = () =>
      this.connected() &&
      !this.removing.has(id) &&
      (this.policy.canRemoveCopy?.(id) ?? true);
    if (
      !allowed() ||
      !(await this.repository.removePreparation(id, mode, allowed))
    )
      return false;
    // The SQLite removal and release intent are durable before a network request.
    if (!this.connected()) return true;
    await this.releases();
    if (this.connected()) await this.files?.cleanup();
    return true;
  }

  private async processPreparations(checked: Set<number>): Promise<void> {
    if (!this.connected()) return;
    for (const id of await this.repository.getRetirementCandidates('closure')) {
      if (checked.has(id)) await this.retireCopy(id, 'closure');
    }
    if (!this.canPrepareCopy()) return;
    const requests = (await this.repository.getPreparationRequests())
      .filter((request) => request.state !== 'FAILED')
      .sort(
        (a, b) => Number(b.source === 'MANUAL') - Number(a.source === 'MANUAL')
      );
    if (!requests.length) return;
    const prepared = new Set(
      (await this.repository.getPreparedWorkOrderSummaries()).map(
        (item) => item.id
      )
    );
    for (const request of requests) {
      if (!this.canPrepareCopy()) return;
      const id = request.workOrderId;
      if (prepared.has(id) || this.removing.has(id)) {
        await this.settlePreparation(request, null);
        continue;
      }
      try {
        // Verify that the target is still accessible before freeing a place.
        const target = await this.api.workOrderForPreparation(id);
        if (!this.canPrepareCopy()) return;
        if (
          request.source !== 'MANUAL' &&
          (target.archived || target.status === 'COMPLETE')
        ) {
          await this.settlePreparation(request, null);
          continue;
        }
        if (prepared.size >= OFFLINE_LIMITS.preparedWorkOrders) {
          for (const candidate of await this.repository.getRetirementCandidates(
            'capacity'
          )) {
            if (!this.canPrepareCopy()) return;
            if (
              checked.has(candidate) &&
              (await this.retireCopy(candidate, 'capacity'))
            ) {
              prepared.delete(candidate);
              break;
            }
          }
        }
        if (prepared.size >= OFFLINE_LIMITS.preparedWorkOrders) {
          await this.settlePreparation(request, 'FULL');
          if (request.state !== 'FULL')
            this.policy.onError?.(new OfflineError('PREPARATION_QUEUE_FULL'));
          return;
        }
        if (!this.canPrepareCopy()) return;
        if (await this.refreshOne(id, false, request)) {
          prepared.add(id);
          await this.settlePreparation(request, null);
        }
      } catch (error) {
        if (!this.current()) return;
        const code = (error as { code?: string })?.code;
        if (code === 'PREPARATION_NOT_NEEDED') {
          await this.settlePreparation(request, null);
        } else if (code === 'PREPARATION_PAUSED') {
          return;
        } else if (
          code === 'PREPARED_LIMIT_EXCEEDED' ||
          code === 'PREPARED_LIMIT'
        ) {
          await this.settlePreparation(request, 'FULL');
          if (request.state !== 'FULL')
            this.policy.onError?.(new OfflineError('PREPARATION_QUEUE_FULL'));
          await this.releases();
          return;
        } else if (request.source === 'NOTIFICATION') {
          // A failed push preparation remains an optional manual action.
          await this.settlePreparation(request, 'FAILED');
        } else if (code === 'NETWORK_UNAVAILABLE') {
          throw error;
        } else {
          await this.settlePreparation(request, 'FAILED');
          if (request.source === 'MANUAL') this.policy.onError?.(error);
        }
        // Settle any durable reservation left by a failed import before the next request.
        await this.releases();
      }
    }
  }

  private async downloadPreparedFiles(workOrderId: number) {
    if (!this.files) return;
    for (const fileId of await this.repository.getPreparedFileIds(workOrderId)) {
      this.assertConnected();
      if (this.removing.has(workOrderId)) return;
      if ((await this.repository.getServerFile(workOrderId, fileId))?.status === 'READY')
        continue;
      const descriptor = await this.api.fileDescriptor(workOrderId, fileId);
      this.assertConnected();
      if (descriptor.fileId !== fileId)
        throw new OfflineError('TARGET_MISMATCH');
      await this.files.downloadFile(workOrderId, descriptor);
    }
  }

  trigger(_reason = 'manual'): Promise<void> {
    if (!this.current()) return Promise.resolve();
    this.clearRetry();
    this.requested = true;
    if (this.syncPromise) return this.syncPromise;
    this.syncPromise = this.enqueue(async () => {
      try {
        while (this.requested && this.current()) {
          this.requested = false;
          if (!this.connected()) {
            this.emit({ status: 'paused' });
            return;
          }
          this.emit({ status: 'syncing' });
          await this.syncPass();
        }
        if (this.current()) {
          this.retryIndex = 0;
          this.emit({ status: 'idle' });
        }
      } catch (error) {
        this.requested = false;
        if (this.current()) {
          this.emit({
            status:
              error instanceof OfflineApiError && error.status === 401
                ? 'authentication_required'
                : 'error'
          });
          this.policy.onError?.(error);
          this.retryAfter(error);
        }
        throw error;
      } finally {
        this.syncPromise = null;
      }
    });
    return this.syncPromise;
  }

  private async syncPass() {
    const attempted = [
      ...(await this.repository.getOperationsToSync()).filter(
        (operation) => operation.attemptCount > 0
      ),
      ...(await this.repository.getCommentReceiptsToRefresh()),
      ...(await this.repository.getFileReceiptsToRefresh()),
      ...(await this.repository.getTimeReceiptsToRefresh()),
      ...(await this.repository.getStatusReceiptsToRefresh())
    ];
    // Receipts are read even while the feature is disabled. Missing is NOT an
    // assurance of non-application, and never makes an operation abandonable.
    for (
      let start = 0;
      start < attempted.length;
      start += OFFLINE_LIMITS.receiptIds
    ) {
      if (!this.connected()) return;
      const batch = attempted.slice(start, start + OFFLINE_LIMITS.receiptIds);
      const results = await this.api.receipts(
        batch.map((operation) => operation.id)
      );
      if (!this.current()) return;
      for (const receipt of results.results)
        await this.repository.applyReceipt(receipt);
      for (const id of results.missingOperationIds) {
        // Missing does not undo a durable success, nor permit its replay.
        if (
          batch.some(
            (operation) => operation.id === id && operation.status !== 'APPLIED'
          )
        )
          await this.repository.markUnknown(id);
      }
    }
    await this.releases();
    if (!(await this.authorize())) return;
    let firstRefreshError: unknown;
    const checkedPreparations = new Set<number>();
    for (const workOrder of await this.repository.getPreparedWorkOrderSummaries()) {
      if (!this.connected() || !this.policy.enabled()) return;
      try {
        if (await this.refreshOne(workOrder.id, false))
          checkedPreparations.add(workOrder.id);
      } catch (error) {
        if (
          error instanceof OfflineApiError &&
          (error.status === 0 ||
            error.status === 401 ||
            error.code === 'OFFLINE_NOT_AUTHORIZED')
        )
          throw error;
        firstRefreshError ||= error;
        // An oversized/inaccessible intervention must not block independent work.
      }
    }
    const commentsToRefresh = new Set<number>();
    for (const operation of await this.repository.getOperationsToSync()) {
      if (
        !this.connected() ||
        !this.policy.enabled() ||
        !this.permissionConfirmed
      )
        return;
      const request = operation.request;
      if (this.removing.has(request.workOrderId)) continue;
      if (
        isStatusOperation(request) &&
        !(await this.repository.canSendStatusOperation(operation))
      )
        continue;
      if (
        isTimeOperation(request) &&
        !(await this.repository.canSendTimeOperation(operation))
      )
        continue;
      if (
        request.type === 'UPLOAD_FILE' &&
        operation.attemptCount === 0 &&
        !(await this.repository.uploadHasParent(operation.id))
      )
        continue;
      if (
        isTaskOperation(request) &&
        request.predecessorOperationId &&
        operation.attemptCount === 0
      ) {
        const predecessor = await this.repository.getOperation(
          request.predecessorOperationId
        );
        if (predecessor?.status !== 'APPLIED') continue;
      }
      if (
        operation.attemptCount === 0 &&
        (
          await Promise.all(
            transferIds(request).map((id) => this.repository.getOperation(id))
          )
        ).some(
          (dependency) =>
            dependency?.status !== 'APPLIED' ||
            [
              'FILE_DELETED',
              'TARGET_UNAVAILABLE',
              'WORK_ORDER_UNAVAILABLE'
            ].includes(dependency.receipt?.visibility || '')
        )
      )
        continue;
      const target =
        !isTaskOperation(request) &&
        'taskId' in request &&
        request.taskId != null
          ? await this.repository.getTask(request.taskId, 'notes')
          : !isTaskOperation(request)
          ? await this.repository.getWorkOrderProjection(request.workOrderId)
          : await this.repository.getTask(
              request.taskId,
              operationField(request) as TaskField
            );
      // A missing receipt cannot prove that a previous attempt had no effect.
      // Replay that immutable request so current server rights can produce a
      // terminal receipt, even when the local target was removed or is read-only.
      if (
        operation.attemptCount === 0 &&
        !isTimeOperation(request) &&
        (!target?.available ||
          !(request.type === 'CREATE_COMMENT'
            ? 'canComment' in target && target.canComment
            : request.type === 'UPDATE_COMMENT' || request.type === 'DELETE_COMMENT'
            ? true
            : target.canEdit))
      )
        continue;
      let localFile =
        request.type === 'UPLOAD_FILE'
          ? await this.repository.getTransferFile(operation.id)
          : null;
      if (request.type === 'UPLOAD_FILE') {
        try {
          if (!localFile || !this.files)
            throw new OfflineError('FILE_NOT_READY');
          await this.files.verify(localFile);
        } catch (error) {
          if (localFile)
            await this.repository.putFile({
              ...localFile,
              status: 'ERROR',
              error: 'FILE_INTEGRITY'
            });
          firstRefreshError ||= error;
          continue;
        }
      }
      if (!this.connected()) return;
      let attempt;
      try {
        attempt = await this.repository.beginAttempt(operation.id);
      } catch (error) {
        // A stop can commit after this pass selected its START candidate.
        if (
          error instanceof OfflineError &&
          ['OPERATION_RESOLVED', 'OPERATION_BLOCKED'].includes(error.code)
        )
          continue;
        throw error;
      }
      try {
        // The foreground gate can close while beginAttempt persists its UUID.
        // Do not start a new network mutation after that transition.
        this.assertConnected();
        if (this.removing.has(request.workOrderId)) continue;
        const result =
          attempt.request.type === 'UPLOAD_FILE'
            ? await this.api.uploadFile(attempt.request, localFile!)
            : await this.api.sendOperation(attempt.request);
        if (!this.current()) return; // reconciled through its receipt next login
        await this.repository.applyReceipt(result);
        if (
          request.type !== 'UPLOAD_FILE' &&
          !isTaskOperation(request) &&
          result.status === 'APPLIED'
        )
          commentsToRefresh.add(request.workOrderId);
      } catch (error) {
        await this.repository.markUnknown(operation.id);
        if (
          error instanceof OfflineApiError &&
          error.code === 'OFFLINE_NOT_AUTHORIZED'
        )
          await this.authorizationRevoked();
        throw error;
      }
    }
    // A refresh failure must never turn a durable creation receipt into UNKNOWN.
    for (const workOrderId of commentsToRefresh) {
      if (!this.connected() || !this.policy.enabled()) return;
      try {
        checkedPreparations.delete(workOrderId);
        if (await this.refreshOne(workOrderId, false))
          checkedPreparations.add(workOrderId);
      } catch (error) {
        firstRefreshError ||= error;
      }
    }
    await this.processPreparations(checkedPreparations);
    for (const workOrder of await this.repository.getPreparedWorkOrderSummaries()) {
      if (!this.connected() || !this.policy.enabled()) return;
      try {
        await this.downloadPreparedFiles(workOrder.id);
      } catch (error) {
        firstRefreshError ||= error;
      }
    }
    await this.files?.cleanup();
    if (firstRefreshError) throw firstRefreshError;
  }

  prepare(workOrderId: number): Promise<void> {
    return this.enqueue(async () => {
      this.assertConnected();
      if (!(await this.authorize())) throw new OfflineError('OFFLINE_DISABLED');
      await this.releases();
      await this.refreshOne(workOrderId);
    });
  }
  refresh(workOrderId: number): Promise<void> {
    return this.prepare(workOrderId);
  }

  runConnected<T>(action: () => Promise<T>): Promise<T> {
    return this.enqueue(async () => {
      this.assertConnected();
      return action();
    });
  }

  removeServerAttachment(
    workOrderId: number,
    fileId: number,
    image: boolean
  ): Promise<{ localUpdateError?: unknown }> {
    return this.enqueue(async () => {
      this.assertConnected();
      if (!this.policy.enabled()) throw new OfflineError('OFFLINE_DISABLED');
      const view = await this.repository.getWorkOrderProjection(workOrderId);
      if (!view?.available || !view.canEdit)
        throw new OfflineError('EDIT_FORBIDDEN');
      if (
        image
          ? view.workOrder?.image?.id !== fileId
          : !view.workOrder?.files.some((file) => file.id === fileId)
      )
        throw new OfflineError('SERVER_CHANGED');
      this.assertConnected();
      await this.api.removeWorkOrderAttachment(workOrderId, fileId, image);
      if (!this.current()) throw new OfflineError('CONTEXT_CHANGED');
      // Server success is final. A failed snapshot can only be refreshed; it
      // must not make this connected mutation look eligible for a retry.
      try {
        this.assertConnected();
        await this.refreshOne(workOrderId);
        if (!this.current()) throw new OfflineError('CONTEXT_CHANGED');
        return {};
      } catch (localUpdateError) {
        if (!this.current()) throw new OfflineError('CONTEXT_CHANGED');
        return { localUpdateError };
      }
    });
  }

  patchTask(
    workOrderId: number,
    taskId: number,
    patch: { notes?: string; value?: string | number }
  ): Promise<OfflineTask> {
    return this.enqueue(async () => {
      this.assertConnected();
      const view = await this.repository.getTask(taskId);
      const workOrder = await this.repository.getWorkOrderProjection(
        workOrderId
      );
      if (
        !view.serverTask ||
        !view.available ||
        !view.canEdit ||
        !workOrder?.tasks.some((task) => task.id === taskId)
      ) {
        throw new OfflineError('TARGET_UNAVAILABLE');
      }
      this.assertConnected();
      const canonical = await this.api.patchTask(taskId, {
        ...patch,
        version: view.serverTask.version
      });
      if (this.current())
        await this.repository.ingestServerTask(workOrderId, canonical);
      return canonical;
    });
  }

  removePreparation(workOrderId: number, clean = false): Promise<void> {
    this.removing.add(workOrderId);
    return this.enqueue(async () => {
      if (!this.current()) throw new OfflineError('CONTEXT_CHANGED');
      if (
        !(await this.repository.removePreparation(
          workOrderId,
          clean ? 'clean' : 'explicit',
          () =>
            this.current() &&
            (!clean || (this.policy.canRemoveCopy?.(workOrderId) ?? true))
        ))
      )
        throw new OfflineError('WORK_PENDING');
      if (this.policy.onPreparationQueueChanged)
        this.policy.onPreparationQueueChanged(
          await this.repository.getPreparationRequests()
        );
      await this.files?.cleanup();
      // Release is a durable housekeeping action; its failure does not undo an
      // explicit, safe local withdrawal. It is retried before the next prepare.
      if (this.connected()) {
        try {
          await this.releases();
        } catch (error) {
          this.policy.onError?.(error);
        }
      }
    }).finally(() => this.removing.delete(workOrderId));
  }

  downloadFile(workOrderId: number, fileId: number): Promise<void> {
    return this.enqueue(async () => {
      this.assertConnected();
      if (!(await this.authorize()) || !this.files)
        throw new OfflineError('OFFLINE_DISABLED');
      const descriptor = await this.api.fileDescriptor(workOrderId, fileId);
      this.assertConnected();
      if (descriptor.fileId !== fileId)
        throw new OfflineError('TARGET_MISMATCH');
      await this.files.downloadFile(workOrderId, descriptor);
    });
  }

  cancelTransfers(): void {
    this.api.cancelTransfers();
    this.files?.cancelTransfers();
  }

  suspend(): void {
    this.cancelTransfers();
    this.clearRetry();
    this.suspended = true;
    this.requested = false;
    this.emit({ status: 'paused' });
  }
}
