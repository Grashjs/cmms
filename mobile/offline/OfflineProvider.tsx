import React, {
  createContext,
  useContext,
  useEffect,
  useRef,
  useState
} from 'react';
import { Alert, AppState, Platform } from 'react-native';
import NetInfo from '@react-native-community/netinfo';
import * as Crypto from 'expo-crypto';
import { useTranslation } from 'react-i18next';
import useAuth from '../hooks/useAuth';
import { getApiUrl, OFFLINE_ENABLED } from '../config';
import { getCredentials } from '../utils/credentialStore';
import { refreshAccessToken } from '../utils/api';
import { useDispatch } from '../store';
import { removePreparedTasks } from '../slices/task';
import { removePreparedLabors } from '../slices/labor';
import { removePreparedComments } from '../slices/comment';
import { removePreparedWorkOrders } from '../slices/workOrder';
import { setPreparedRuntime } from './runtime';
import {
  registerOfflineLifecycle,
  sessionGeneration,
  sessionChangeInProgress
} from './sessionBridge';
import {
  authorizeLocalAccess,
  getOnlineIdentity,
  installationId,
  restoreLocalAuthorization,
  localAuthorizationExpiresAt,
  revokeLocalAccess,
  serverContextKey
} from './accessPolicy';
import type { IFile } from '../models/file';
import type { OfflineFileStore } from './OfflineFileStore';
import { closeOfflineDocument } from './documentViewer';
import type { OfflineRepository } from './OfflineRepository';
import type { SyncCoordinator } from './SyncCoordinator';
import type {
  CommentDraft,
  PreparedWorkOrderSummary,
  TaskDraft,
  TaskField,
  FileTarget,
  TimerDraft,
  ManualTimeDraft,
  ManualTimeInput,
  LocalOperation,
  ObservedServerTimer
} from './types';
import { OfflineError } from './types';
import { offlineErrorKey } from './errors';

interface OfflineContextValue {
  startTimer(workOrderId: number): Promise<TimerDraft>;
  stopTimer(
    workOrderId: number,
    expectedRevision?: number,
    expectedServerTimer?: ObservedServerTimer
  ): Promise<LocalOperation>;
  saveManualTimeDraft(
    workOrderId: number,
    input: ManualTimeInput,
    expectedRevision?: number | null
  ): Promise<ManualTimeDraft>;
  submitManualTimeDraft(
    workOrderId: number,
    revision: number
  ): Promise<LocalOperation>;
  abandonManualTimeDraft(workOrderId: number, revision: number): Promise<void>;
  reviewTimeOperation(
    operationId: string,
    expectedLaborVersion: number | null,
    preserveStatus?: boolean,
    expectedTimerStatusRevision?: number
  ): Promise<TimerDraft | ManualTimeDraft>;
  saveTimerReview(
    workOrderId: number,
    input: { startedAt: string; endedAt: string },
    revision: number
  ): Promise<TimerDraft>;
  submitReviewedTimer(
    workOrderId: number,
    revision: number
  ): Promise<LocalOperation>;
  abandonTimeDraft(workOrderId: number, revision: number): Promise<void>;
  importFiles(
    workOrderId: number,
    target: FileTarget,
    files: IFile[],
    maxBytes?: number
  ): Promise<void>;
  retryFileImport(id: string): Promise<void>;
  removeDraftFile(
    workOrderId: number,
    target: FileTarget,
    id: string,
    revision: number
  ): Promise<void>;
  submitAttachmentDraft(
    workOrderId: number,
    revision: number,
    taskId?: number
  ): Promise<unknown>;
  abandonAttachmentDraft(
    workOrderId: number,
    revision: number,
    taskId?: number
  ): Promise<void>;
  abandonOperation(operationId: string): Promise<void>;
  restoreFileOperation(
    operationId: string,
    expectedDraftRevision: number | null,
    expectedImageId?: number | null
  ): Promise<void>;
  downloadFile(workOrderId: number, fileId: number): Promise<void>;
  openFile(localId: string): Promise<void>;
  removeLocalCopy(localId: string): Promise<void>;
  removeServerAttachment(
    workOrderId: number,
    fileId: number,
    image: boolean
  ): Promise<{ localUpdateError?: unknown }>;
  enabled: boolean;
  initialized: boolean;
  readable: boolean;
  writable: boolean;
  networkAvailable: boolean;
  syncing: boolean;
  error: string | null;
  repository: OfflineRepository | null;
  prepared: PreparedWorkOrderSummary[];
  sync(): Promise<void>;
  prepare(id: number): Promise<void>;
  refresh(id: number): Promise<void>;
  removePreparation(id: number): Promise<void>;
  saveDraft(
    workOrderId: number,
    taskId: number,
    value: string,
    field?: TaskField
  ): Promise<TaskDraft>;
  submitDraft(
    taskId: number,
    revision: number,
    field?: TaskField
  ): Promise<unknown>;
  saveCommentDraft(
    workOrderId: number,
    content: string,
    expectedRevision?: number | null,
    attachmentPending?: boolean
  ): Promise<CommentDraft>;
  preserveConnectedCommentDraft(
    workOrderId: number,
    content: string,
    attachmentPending?: boolean
  ): Promise<CommentDraft>;
  submitCommentDraft(workOrderId: number, revision: number): Promise<unknown>;
  abandonCommentDraft(workOrderId: number, revision: number): Promise<void>;
  resolveConflict(
    taskId: number,
    draftRevision?: number,
    serverVersion?: number,
    field?: TaskField
  ): Promise<unknown>;
  discardTaskField(
    taskId: number,
    field: TaskField,
    draftRevision: number | null,
    operationId: string | null,
    serverVersion: number
  ): Promise<void>;
  reportError(error: unknown): void;
}
const noop = async () => {};
const OfflineContext = createContext<OfflineContextValue>({
  startTimer: async () => {
    throw new OfflineError('OFFLINE_DISABLED');
  },
  stopTimer: async () => {
    throw new OfflineError('OFFLINE_DISABLED');
  },
  saveManualTimeDraft: async () => {
    throw new OfflineError('OFFLINE_DISABLED');
  },
  submitManualTimeDraft: async () => {
    throw new OfflineError('OFFLINE_DISABLED');
  },
  abandonManualTimeDraft: noop,
  reviewTimeOperation: async () => {
    throw new OfflineError('OFFLINE_DISABLED');
  },
  saveTimerReview: async () => {
    throw new OfflineError('OFFLINE_DISABLED');
  },
  submitReviewedTimer: async () => {
    throw new OfflineError('OFFLINE_DISABLED');
  },
  abandonTimeDraft: noop,
  enabled: false,
  initialized: true,
  readable: false,
  writable: false,
  networkAvailable: true,
  syncing: false,
  error: null,
  repository: null,
  prepared: [],
  importFiles: noop,
  retryFileImport: noop,
  removeDraftFile: noop,
  submitAttachmentDraft: noop,
  abandonAttachmentDraft: noop,
  abandonOperation: noop,
  restoreFileOperation: noop,
  downloadFile: noop,
  openFile: noop,
  removeLocalCopy: noop,
  removeServerAttachment: async () => {
    throw new OfflineError('OFFLINE_DISABLED');
  },
  sync: noop,
  prepare: noop,
  refresh: noop,
  removePreparation: noop,
  saveDraft: async () => {
    throw new OfflineError('OFFLINE_DISABLED');
  },
  submitDraft: noop,
  saveCommentDraft: async () => {
    throw new OfflineError('OFFLINE_DISABLED');
  },
  preserveConnectedCommentDraft: async () => {
    throw new OfflineError('OFFLINE_DISABLED');
  },
  submitCommentDraft: noop,
  abandonCommentDraft: noop,
  resolveConflict: noop,
  discardTaskField: noop,
  reportError: () => {}
});
export const useOffline = () => useContext(OfflineContext);

export function OfflineProvider({ children }: { children: React.ReactNode }) {
  const auth = useAuth();
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const authGeneration = sessionGeneration();
  const enabled = Platform.OS !== 'web' && OFFLINE_ENABLED;
  const [initialized, setInitialized] = useState(!enabled);
  const [repository, setRepository] = useState<OfflineRepository | null>(null);
  const [prepared, setPrepared] = useState<PreparedWorkOrderSummary[]>([]);
  const [readable, setReadable] = useState(false);
  const [networkAvailable, setNetworkAvailable] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [transitioning, setTransitioning] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const coordinator = useRef<SyncCoordinator | null>(null);
  const fileStore = useRef<OfflineFileStore | null>(null);
  const network = useRef(false);
  const foreground = useRef(AppState.currentState === 'active');
  const canWrite = useRef(false);
  const writes = useRef(new Set<Promise<unknown>>());
  const writeLane = useRef<Promise<unknown>>(Promise.resolve());
  const failedWrites = useRef(new Set<string>());
  const preparedIds = useRef(new Set<number>());
  const sequence = useRef(0);
  const checkAccess = useRef<() => Promise<boolean>>(async () => false);
  const revokeAccess = useRef<() => Promise<void>>(noop);
  const reportError = (failure: unknown) => {
    const code = (failure as { code?: string })?.code || '';
    if (
      code === 'OFFLINE_NOT_AUTHORIZED' ||
      code === 'OFFLINE_CONTEXT_MISMATCH'
    ) {
      canWrite.current = false;
      setReadable(false);
      void revokeAccess.current();
      void closeOfflineDocument().catch(() => undefined);
    }
    setError(String(t(offlineErrorKey(failure))));
  };

  useEffect(
    () =>
      NetInfo.addEventListener((state) => {
        const usable =
          state.isConnected === true && state.isInternetReachable !== false;
        network.current = usable;
        setNetworkAvailable(usable);
        if (usable && foreground.current)
          void coordinator.current?.trigger('network').catch(reportError);
      }),
    []
  );

  useEffect(() => {
    const listener = AppState.addEventListener('change', (state) => {
      foreground.current = state === 'active';
      if (!foreground.current) coordinator.current?.cancelTransfers();
      if (foreground.current) {
        canWrite.current = false;
        setReadable(false);
        void checkAccess
          .current()
          .then(() => coordinator.current?.trigger('foreground'))
          .catch(reportError);
      }
    });
    return () => listener.remove();
  }, []);

  useEffect(() => {
    const currentSequence = ++sequence.current;
    let active = true;
    let unsubscribe = () => {};
    let unregister = () => {};
    let expiryTimer: ReturnType<typeof setTimeout> | undefined;
    setPreparedRuntime(null);
    setRepository(null);
    setPrepared([]);
    setReadable(false);
    canWrite.current = false;
    preparedIds.current = new Set();
    coordinator.current?.suspend();
    fileStore.current = null;
    void closeOfflineDocument().catch(() => undefined);
    coordinator.current = null;
    setError(null);
    setTransitioning(false);
    if (Platform.OS === 'web' || !auth.isAuthenticated || !auth.user) {
      setInitialized(true);
      return;
    }
    setInitialized(false);
    const setup = async () => {
      const serverUrl = await getApiUrl();
      const context = {
        serverUrl,
        companyId: auth.user.companyId,
        userId: auth.user.id
      };
      const key = serverContextKey(context);
      const generation = sessionGeneration();
      const contextIsCurrent = () =>
        active &&
        currentSequence === sequence.current &&
        generation === sessionGeneration();
      const isCurrent = () => contextIsCurrent() && !sessionChangeInProgress();
      revokeAccess.current = () => revokeLocalAccess(context);
      const scheduleExpiry = async () => {
        if (expiryTimer) clearTimeout(expiryTimer);
        const expires = await localAuthorizationExpiresAt(context);
        if (expires && isCurrent())
          expiryTimer = setTimeout(() => {
            if (!isCurrent()) return;
            setReadable(false);
            canWrite.current = false;
            setError(t('offline_expired'));
            void closeOfflineDocument().catch(() => undefined);
          }, Math.max(0, expires - Date.now()));
      };
      checkAccess.current = async () => {
        // A context change drains already accepted local writes before changing its generation.
        if (!enabled || !contextIsCurrent()) return false;
        const grant = await restoreLocalAuthorization(serverUrl).catch(
          () => null
        );
        const allowed =
          contextIsCurrent() &&
          !!grant &&
          serverContextKey(grant.context) === key;
        if (contextIsCurrent()) {
          setReadable(allowed);
          canWrite.current = allowed;
          if (!allowed) void closeOfflineDocument().catch(() => undefined);
        }
        return allowed;
      };
      const snapshot =
        (enabled ? await auth.getServerSession() : null) ||
        (await getOnlineIdentity(serverUrl));
      // Disabled fresh installations do not create or open an offline database.
      if (!snapshot || serverContextKey(snapshot.context) !== key) return;
      const [
        { openOfflineDatabase },
        { OfflineRepository: Repository },
        { OfflineApi: Api },
        { SyncCoordinator: Coordinator }
      ] = await Promise.all([
        import('./database'),
        import('./OfflineRepository'),
        import('./OfflineApi'),
        import('./SyncCoordinator')
      ]);
      const database = await openOfflineDatabase(key);
      if (!isCurrent()) return;
      const repo = new Repository(database, key, { uuid: Crypto.randomUUID });
      const { OfflineFileStore: FileStore } = await import(
        './OfflineFileStore'
      );
      const files = new FileStore(repo, contextIsCurrent, async () => {
        if (!isCurrent() || sessionChangeInProgress()) return false;
        const allowed = await checkAccess.current();
        return allowed && isCurrent() && !sessionChangeInProgress();
      });
      await files.recover();
      if (!isCurrent()) return;
      fileStore.current = files;
      let lastRefresh = 0;
      const client = new Api({
        ...context,
        deviceId: await installationId(),
        isCurrent,
        getToken: async () => {
          if (!isCurrent()) return null;
          let credentials = await getCredentials(serverUrl);
          if (
            !credentials ||
            credentials.companyId !== context.companyId ||
            credentials.userId !== context.userId
          )
            return null;
          // Refresh can fail offline; it never removes local work or changes this identity.
          if (
            credentials.refreshToken &&
            network.current &&
            Date.now() - lastRefresh > 60_000
          ) {
            lastRefresh = Date.now();
            await refreshAccessToken(serverUrl);
            if (!isCurrent()) return null;
            credentials = await getCredentials(serverUrl);
          }
          return credentials?.accessToken || null;
        }
      });
      const valid = enabled ? await restoreLocalAuthorization(serverUrl) : null;
      const localAllowed = !!valid && serverContextKey(valid.context) === key;
      setReadable(localAllowed);
      canWrite.current = localAllowed && enabled;
      await scheduleExpiry();
      const sync = new Coordinator(
        repo,
        client,
        {
          enabled: () => enabled,
          online: () => network.current && foreground.current,
          sessionIsCurrent: isCurrent,
          onEligibility: async (eligibility) => {
            if (!isCurrent()) return;
            if (
              eligibility.companyId !== context.companyId ||
              eligibility.userId !== context.userId
            )
              throw new OfflineError('OFFLINE_CONTEXT_MISMATCH');
            if (enabled && eligibility.enabled && eligibility.eligible) {
              await authorizeLocalAccess(context, eligibility, snapshot);
              if (!isCurrent()) return;
              setReadable(true);
              canWrite.current = true;
              await scheduleExpiry();
            } else {
              await revokeLocalAccess(context);
              await closeOfflineDocument();
              if (!isCurrent()) return;
              setReadable(false);
              canWrite.current = false;
            }
          },
          onStateChanged: (state) => {
            if (!isCurrent()) return;
            setSyncing(state.status === 'syncing');
          },
          onError: (failure) => {
            if (isCurrent()) reportError(failure);
          }
        },
        undefined,
        files
      );
      coordinator.current = sync;
      setRepository(repo);
      const acceptPrepared = (items: PreparedWorkOrderSummary[]) => {
        if (!isCurrent()) return;
        preparedIds.current = new Set(items.map((item) => item.id));
        setPrepared(items);
        const ids = items.map((item) => item.id);
        if (enabled) {
          dispatch(removePreparedTasks(ids));
          dispatch(removePreparedLabors(ids));
          dispatch(removePreparedWorkOrders(ids));
          dispatch(removePreparedComments(ids));
        }
      };
      acceptPrepared(await repo.getPreparedWorkOrderSummaries());
      if (!isCurrent()) return;
      const observed = repo
        .observePreparedWorkOrderSummaries()
        .subscribe({ next: acceptPrepared, error: reportError });
      unsubscribe = () => observed.unsubscribe();
      setPreparedRuntime({
        hasPreparation: (id) =>
          enabled && isCurrent() && preparedIds.current.has(id),
        refresh: (id) => sync.refresh(id),
        ingestTask: async (id, task) => {
          if (isCurrent()) await repo.ingestServerTask(id, task as any);
        },
        ingestWorkOrder: async (workOrder) => {
          if (isCurrent()) await repo.ingestServerWorkOrder(workOrder);
        },
        ingestComment: async (id, comment) => {
          if (isCurrent()) await repo.ingestServerComment(id, comment);
        },
        removeComment: async (id, commentId) => {
          if (isCurrent()) await repo.removeServerComment(id, commentId);
        },
        runConnectedComment: (workOrderId, action) =>
          sync.runConnected(async () => {
            if (
              !isCurrent() ||
              !(await checkAccess.current()) ||
              !network.current
            )
              throw new OfflineError('NETWORK_UNAVAILABLE');
            const target = await repo.getWorkOrderProjection(workOrderId);
            if (!target?.available) throw new OfflineError('EDIT_FORBIDDEN');
            return action();
          }),
        runConnectedLabor: (workOrderId, action) =>
          sync.runConnected(async () => {
            if (
              !isCurrent() ||
              !(await checkAccess.current()) ||
              !network.current
            )
              throw new OfflineError('NETWORK_UNAVAILABLE');
            const target = await repo.getWorkOrderProjection(workOrderId);
            if (!target?.available || !target.canEdit)
              throw new OfflineError('EDIT_FORBIDDEN');
            return action();
          }),
        markUnavailable: (id, reason) => repo.markUnavailable(id, reason),
        patchTask: async (workOrderId, taskId, patch) => {
          if (
            !isCurrent() ||
            !(await checkAccess.current()) ||
            !network.current
          )
            throw new OfflineError('NETWORK_UNAVAILABLE');
          return sync.patchTask(workOrderId, taskId, patch);
        }
      });
      unregister = registerOfflineLifecycle({
        beforeContextChange: async () => {
          setTransitioning(true);
          sync.cancelTransfers();
          try {
            await Promise.all([...writes.current]);
            if (failedWrites.current.size)
              throw new OfflineError('STORAGE_FAILED');
          } catch (failure) {
            setTransitioning(false);
            throw failure;
          }
          sync.suspend();
          await closeOfflineDocument();
          canWrite.current = false;
          await database.write(async () => {});
          if (await repo.hasUnresolved())
            Alert.alert(t('offline_pending'), t('offline_leave_context'));
          setPreparedRuntime(null);
          setReadable(false);
          setPrepared([]);
          setRepository(null);
        }
      });
      if (isCurrent()) {
        setInitialized(true);
        if (foreground.current && network.current)
          void sync.trigger('start').catch(reportError);
      }
    };
    void setup()
      .catch((failure) => {
        if (active) reportError(failure);
      })
      .finally(() => {
        if (active) setInitialized(true);
      });
    return () => {
      active = false;
      unsubscribe();
      unregister();
      if (expiryTimer) clearTimeout(expiryTimer);
      coordinator.current?.suspend();
      setPreparedRuntime(null);
      failedWrites.current.clear();
    };
  }, [
    auth.isAuthenticated,
    auth.user?.id,
    auth.user?.companyId,
    enabled,
    authGeneration
  ]);

  const persist = <T,>(action: () => Promise<T>): Promise<T> => {
    if (sessionChangeInProgress())
      return Promise.reject(new OfflineError('CONTEXT_BUSY'));
    const generation = sessionGeneration();
    // Reserve the lane before any SecureStore/database await so keystrokes cannot overtake one another.
    const writing = writeLane.current.then(() => {
      if (generation !== sessionGeneration())
        throw new OfflineError('OFFLINE_DISABLED');
      return action();
    });
    writeLane.current = writing.catch(() => {});
    writes.current.add(writing);
    void writing.then(
      () => writes.current.delete(writing),
      () => writes.current.delete(writing)
    );
    return writing;
  };
  const saveDraft = (
    workOrderId: number,
    taskId: number,
    value: string,
    field: TaskField = 'value'
  ): Promise<TaskDraft> => {
    if (sessionChangeInProgress())
      return Promise.reject(new OfflineError('CONTEXT_BUSY'));
    const writing = persist(async () => {
      if (!repository || !canWrite.current || !(await checkAccess.current()))
        throw new OfflineError('OFFLINE_DISABLED');
      const task = await repository.getTask(taskId, field);
      if (
        field === 'value' &&
        task.serverTask?.taskBase.user &&
        task.serverTask.taskBase.user.id !== auth.user?.id
      )
        throw new OfflineError('EDIT_FORBIDDEN');
      return repository.saveDraft(workOrderId, taskId, value, field);
    });
    void writing.then(
      () => failedWrites.current.delete(`${taskId}:${field}`),
      () => failedWrites.current.add(`${taskId}:${field}`)
    );
    return writing;
  };
  const saveCommentDraft = (
    workOrderId: number,
    content: string,
    expectedRevision?: number | null,
    attachmentPending?: boolean
  ): Promise<CommentDraft> => {
    if (sessionChangeInProgress())
      return Promise.reject(new OfflineError('CONTEXT_BUSY'));
    const writing = persist(async () => {
      if (!repository || !canWrite.current || !(await checkAccess.current()))
        throw new OfflineError('OFFLINE_DISABLED');
      return repository.saveCommentDraft(
        workOrderId,
        content,
        expectedRevision,
        attachmentPending
      );
    });
    const key = `work-order:${workOrderId}:comment`;
    void writing.then(
      () => failedWrites.current.delete(key),
      () => failedWrites.current.add(key)
    );
    return writing;
  };
  const persistTime = <T,>(
    action: (repo: OfflineRepository) => Promise<T>,
    submit = false
  ): Promise<T> =>
    persist(async () => {
      if (!repository || !canWrite.current || !(await checkAccess.current()))
        throw new OfflineError('OFFLINE_DISABLED');
      const result = await action(repository);
      if (submit)
        void coordinator.current?.trigger('submission').catch(reportError);
      return result;
    });
  const saveManualTimeDraft = (
    workOrderId: number,
    input: ManualTimeInput,
    revision?: number | null
  ) => {
    const key = `work-order:${workOrderId}:manual-time`;
    const writing = persistTime((repo) =>
      repo.saveManualTimeDraft(workOrderId, input, revision)
    );
    void writing.then(
      () => failedWrites.current.delete(key),
      () => failedWrites.current.add(key)
    );
    return writing;
  };
  const value: OfflineContextValue = {
    startTimer: (id) => persistTime((repo) => repo.startTimer(id), true),
    stopTimer: (id, revision, expectedServerTimer) =>
      persistTime(
        (repo) => repo.stopTimer(id, revision, expectedServerTimer),
        true
      ),
    saveManualTimeDraft,
    submitManualTimeDraft: (id, revision) =>
      persistTime((repo) => {
        if (failedWrites.current.has(`work-order:${id}:manual-time`))
          throw new OfflineError('STORAGE_FAILED');
        return repo.submitManualTimeDraft(id, revision);
      }, true),
    abandonManualTimeDraft: (id, revision) =>
      persist(async () => {
        if (!repository) throw new OfflineError('OFFLINE_DISABLED');
        await repository.abandonManualTimeDraft(id, revision);
        failedWrites.current.delete(`work-order:${id}:manual-time`);
      }),
    reviewTimeOperation: (id, version, preserve, statusRevision) =>
      persistTime((repo) =>
        repo.reviewTimeOperation(id, version, preserve, statusRevision)
      ),
    saveTimerReview: (id, input, revision) =>
      persistTime((repo) => repo.saveTimerReview(id, input, revision)),
    submitReviewedTimer: (id, revision) =>
      persistTime((repo) => repo.submitReviewedTimer(id, revision), true),
    abandonTimeDraft: (id, revision) =>
      persist(async () => {
        if (!repository) throw new OfflineError('OFFLINE_DISABLED');
        await repository.abandonTimeDraft(id, revision);
      }),
    enabled,
    initialized,
    readable,
    writable: enabled && readable && !transitioning,
    networkAvailable,
    syncing,
    error,
    repository,
    prepared,
    reportError,
    sync: async () => {
      setError(null);
      try {
        await coordinator.current?.trigger('manual');
      } catch (failure) {
        reportError(failure);
      }
    },
    prepare: async (id) => {
      setError(null);
      if (!coordinator.current || !enabled)
        throw new OfflineError('OFFLINE_DISABLED');
      await coordinator.current.prepare(id);
    },
    refresh: async (id) => {
      if (coordinator.current) await coordinator.current.refresh(id);
    },
    removePreparation: async (id) => {
      if (coordinator.current) await coordinator.current.removePreparation(id);
    },
    removeServerAttachment: async (workOrderId, fileId, image) => {
      const sync = coordinator.current;
      const generation = sessionGeneration();
      if (!sync || !canWrite.current || !(await checkAccess.current()))
        throw new OfflineError('OFFLINE_DISABLED');
      if (
        sessionChangeInProgress() ||
        generation !== sessionGeneration() ||
        sync !== coordinator.current
      )
        throw new OfflineError('CONTEXT_CHANGED');
      return sync.removeServerAttachment(workOrderId, fileId, image);
    },
    importFiles: (workOrderId, target, files, maxBytes) =>
      persist(async () => {
        if (
          !fileStore.current ||
          !canWrite.current ||
          !(await checkAccess.current())
        )
          throw new OfflineError('OFFLINE_DISABLED');
        await fileStore.current.importFiles(
          workOrderId,
          target,
          files,
          maxBytes
        );
      }),
    retryFileImport: (id) =>
      persist(async () => {
        if (
          !fileStore.current ||
          !canWrite.current ||
          !(await checkAccess.current())
        )
          throw new OfflineError('OFFLINE_DISABLED');
        await fileStore.current.retryFileImport(id);
      }),
    removeDraftFile: (workOrderId, target, id, revision) =>
      persist(async () => {
        if (!repository || !canWrite.current || !(await checkAccess.current()))
          throw new OfflineError('OFFLINE_DISABLED');
        await repository.removeDraftFile(workOrderId, target, id, revision);
        await fileStore.current?.cleanup();
      }),
    submitAttachmentDraft: (workOrderId, revision, taskId) =>
      persist(async () => {
        if (!repository || !canWrite.current || !(await checkAccess.current()))
          throw new OfflineError('OFFLINE_DISABLED');
        const operation = await repository.submitAttachmentDraft(
          workOrderId,
          revision,
          taskId
        );
        void coordinator.current?.trigger('submission').catch(reportError);
        return operation;
      }),
    abandonAttachmentDraft: (workOrderId, revision, taskId) =>
      persist(async () => {
        if (!repository) throw new OfflineError('OFFLINE_DISABLED');
        await repository.abandonAttachmentDraft(workOrderId, revision, taskId);
        await fileStore.current?.cleanup();
      }),
    restoreFileOperation: (id, revision, imageId) =>
      persist(async () => {
        if (!repository || !canWrite.current || !(await checkAccess.current()))
          throw new OfflineError('OFFLINE_DISABLED');
        await repository.restoreFileOperation(id, revision, imageId);
      }),
    abandonOperation: (id) =>
      persist(async () => {
        if (!repository) throw new OfflineError('OFFLINE_DISABLED');
        await repository.abandonOperation(id);
        await fileStore.current?.cleanup();
      }),
    downloadFile: async (workOrderId, id) => {
      if (!coordinator.current || !(await checkAccess.current()))
        throw new OfflineError('OFFLINE_DISABLED');
      await coordinator.current.downloadFile(workOrderId, id);
    },
    openFile: async (id) => {
      if (
        !fileStore.current ||
        !(await checkAccess.current()) ||
        sessionChangeInProgress()
      )
        throw new OfflineError('OFFLINE_DISABLED');
      await fileStore.current.openFile(id);
    },
    removeLocalCopy: (id) =>
      persist(async () => {
        if (!fileStore.current || !(await checkAccess.current()))
          throw new OfflineError('OFFLINE_DISABLED');
        await fileStore.current.removeLocalCopy(id);
      }),
    saveDraft,
    saveCommentDraft,
    preserveConnectedCommentDraft: (
      workOrderId,
      content,
      attachmentPending = false
    ) => {
      if (sessionChangeInProgress())
        return Promise.reject(new OfflineError('CONTEXT_BUSY'));
      const key = `work-order:${workOrderId}:comment`;
      const writing = persist(async () => {
        if (!repository || !canWrite.current || !(await checkAccess.current()))
          throw new OfflineError('OFFLINE_DISABLED');
        const existing = await repository.getCommentDraft(workOrderId);
        if (existing) {
          if (
            existing.content !== content ||
            !!existing.attachmentPending !== attachmentPending
          )
            throw new OfflineError('DRAFT_CHANGED');
          return existing;
        }
        return repository.saveCommentDraft(
          workOrderId,
          content,
          null,
          attachmentPending
        );
      });
      void writing.then(
        () => failedWrites.current.delete(key),
        () => failedWrites.current.add(key)
      );
      return writing;
    },
    discardTaskField: (
      taskId,
      field,
      draftRevision,
      operationId,
      serverVersion
    ) =>
      persist(async () => {
        if (!repository || !canWrite.current || !(await checkAccess.current()))
          throw new OfflineError('OFFLINE_DISABLED');
        await repository.discardTaskField(
          taskId,
          field,
          draftRevision,
          operationId,
          serverVersion
        );
      }),
    submitCommentDraft: (workOrderId, revision) =>
      persist(async () => {
        if (!repository || !canWrite.current || !(await checkAccess.current()))
          throw new OfflineError('OFFLINE_DISABLED');
        const operation = await repository.submitCommentDraft(
          workOrderId,
          revision
        );
        void coordinator.current?.trigger('submission').catch(reportError);
        return operation;
      }),
    abandonCommentDraft: (workOrderId, revision) =>
      persist(async () => {
        if (!repository) throw new OfflineError('OFFLINE_DISABLED');
        await repository.abandonCommentDraft(workOrderId, revision);
        await fileStore.current?.cleanup();
      }),
    submitDraft: (taskId, revision, field = 'value') =>
      persist(async () => {
        if (!repository || !canWrite.current || !(await checkAccess.current()))
          throw new OfflineError('OFFLINE_DISABLED');
        const operation = await repository.submitDraft(taskId, revision, field);
        void coordinator.current?.trigger('submission').catch(reportError);
        return operation;
      }),
    resolveConflict: (taskId, revision, serverVersion, field = 'value') =>
      persist(async () => {
        if (!repository || !canWrite.current || !(await checkAccess.current()))
          throw new OfflineError('OFFLINE_DISABLED');
        const current = await repository.getTask(taskId, field);
        if (
          (current.draft?.revision ?? null) !== (revision ?? null) ||
          current.serverTask?.version !== serverVersion
        ) {
          throw new OfflineError('DRAFT_CHANGED');
        }
        if (
          current.operation &&
          (['CONFLICT', 'REJECTED'].includes(current.operation.status) ||
            (current.operation.status === 'QUEUED' &&
              current.operation.attemptCount === 0 &&
              current.operation.blockedReason))
        ) {
          return repository.resolveOperationAgainstServer(
            current.operation.id,
            revision ?? null,
            serverVersion
          );
        }
        if (current.draft?.blockedReason)
          return repository.resolveDraftAgainstServer(
            taskId,
            revision,
            serverVersion,
            field
          );
        throw new OfflineError('DRAFT_CHANGED');
      })
  };
  return (
    <OfflineContext.Provider value={value}>{children}</OfflineContext.Provider>
  );
}
