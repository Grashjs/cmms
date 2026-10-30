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
import type { OfflineRepository } from './OfflineRepository';
import type { SyncCoordinator } from './SyncCoordinator';
import type { PreparedWorkOrderSummary, TaskDraft } from './types';
import { OfflineError } from './types';
import { offlineErrorKey } from './errors';

interface OfflineContextValue {
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
    value: string
  ): Promise<TaskDraft>;
  submitDraft(taskId: number, revision: number): Promise<unknown>;
  resolveConflict(
    taskId: number,
    draftRevision?: number,
    serverVersion?: number
  ): Promise<unknown>;
  reportError(error: unknown): void;
}
const noop = async () => {};
const OfflineContext = createContext<OfflineContextValue>({
  enabled: false,
  initialized: true,
  readable: false,
  writable: false,
  networkAvailable: true,
  syncing: false,
  error: null,
  repository: null,
  prepared: [],
  sync: noop,
  prepare: noop,
  refresh: noop,
  removePreparation: noop,
  saveDraft: async () => {
    throw new OfflineError('OFFLINE_DISABLED');
  },
  submitDraft: noop,
  resolveConflict: noop,
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
  const network = useRef(false);
  const foreground = useRef(AppState.currentState === 'active');
  const canWrite = useRef(false);
  const writes = useRef(new Set<Promise<unknown>>());
  const writeLane = useRef<Promise<unknown>>(Promise.resolve());
  const failedWrites = useRef(new Set<number>());
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
      const sync = new Coordinator(repo, client, {
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
      });
      coordinator.current = sync;
      setRepository(repo);
      const acceptPrepared = (items: PreparedWorkOrderSummary[]) => {
        if (!isCurrent()) return;
        preparedIds.current = new Set(items.map((item) => item.id));
        setPrepared(items);
        const ids = items.map((item) => item.id);
        if (enabled) {
          dispatch(removePreparedTasks(ids));
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
          try {
            await Promise.all([...writes.current]);
            if (failedWrites.current.size)
              throw new OfflineError('STORAGE_FAILED');
          } catch (failure) {
            setTransitioning(false);
            throw failure;
          }
          sync.suspend();
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
    value: string
  ): Promise<TaskDraft> => {
    if (sessionChangeInProgress())
      return Promise.reject(new OfflineError('CONTEXT_BUSY'));
    const writing = persist(async () => {
      if (!repository || !canWrite.current || !(await checkAccess.current()))
        throw new OfflineError('OFFLINE_DISABLED');
      const task = await repository.getTask(taskId);
      if (
        task.serverTask?.taskBase.user &&
        task.serverTask.taskBase.user.id !== auth.user?.id
      )
        throw new OfflineError('EDIT_FORBIDDEN');
      return repository.saveDraft(workOrderId, taskId, value);
    });
    void writing.then(
      () => failedWrites.current.delete(taskId),
      () => failedWrites.current.add(taskId)
    );
    return writing;
  };
  const value: OfflineContextValue = {
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
    saveDraft,
    submitDraft: (taskId, revision) =>
      persist(async () => {
        if (!repository || !canWrite.current || !(await checkAccess.current()))
          throw new OfflineError('OFFLINE_DISABLED');
        const operation = await repository.submitDraft(taskId, revision);
        void coordinator.current?.trigger('submission').catch(reportError);
        return operation;
      }),
    resolveConflict: (taskId, revision, serverVersion) =>
      persist(async () => {
        if (!repository || !canWrite.current || !(await checkAccess.current()))
          throw new OfflineError('OFFLINE_DISABLED');
        const current = await repository.getTask(taskId);
        if (
          (current.draft?.revision ?? null) !== (revision ?? null) ||
          current.serverTask?.version !== serverVersion
        ) {
          throw new OfflineError('DRAFT_CHANGED');
        }
        if (
          current.operation &&
          ['CONFLICT', 'REJECTED'].includes(current.operation.status)
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
            serverVersion
          );
        throw new OfflineError('DRAFT_CHANGED');
      })
  };
  return (
    <OfflineContext.Provider value={value}>{children}</OfflineContext.Provider>
  );
}
