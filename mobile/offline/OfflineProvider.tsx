import React, {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState
} from 'react';
import { Alert, AppState, Modal, Platform, StyleSheet, View } from 'react-native';
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
  ObservedServerTimer,
  StatusDraft,
  StatusInput
} from './types';
import { OfflineError } from './types';
import { offlineErrorKey } from './errors';
import { canVerifyDeviceOwner, verifyDeviceOwner } from './deviceOwner';
import { OfflineLockScreen } from './OfflineLockScreen';
import { readOfflinePreference, writeOfflinePreference } from './preference';

type AccessGate = 'checking' | 'locked' | 'unlocked' | 'open';
type LockIssue =
  | 'none'
  | 'cancelled'
  | 'failed'
  | 'unavailable'
  | 'authorization'
  | 'denied'
  | 'session'
  | 'preference'
  | 'storage';

function waitForActiveApp(): Promise<boolean> {
  if (AppState.currentState === 'active') return Promise.resolve(true);
  return new Promise((resolve) => {
    let finished = false;
    const finish = (active: boolean) => {
      if (finished) return;
      finished = true;
      clearTimeout(timeout);
      subscription.remove();
      resolve(active);
    };
    const subscription = AppState.addEventListener('change', (state) => {
      if (state !== 'inactive') finish(state === 'active');
    });
    const timeout = setTimeout(() => finish(false), 10_000);
  });
}

interface OfflineContextValue {
  saveStatusDraft(
    workOrderId: number,
    input: StatusInput,
    expectedRevision?: number | null
  ): Promise<StatusDraft>;
  submitStatusDraft(
    workOrderId: number,
    revision: number
  ): Promise<LocalOperation>;
  abandonStatusDraft(workOrderId: number, revision: number): Promise<void>;
  reviewStatusOperation(
    operationId: string,
    expectedDraftRevision: number | null,
    expectedServerRevision: number,
    expectedActiveTimerRuns?: { laborId: number; timerRunId: string }[]
  ): Promise<StatusDraft>;
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
  availableInBuild: boolean;
  preferenceChanging: boolean;
  setPreference(enabled: boolean): Promise<void>;
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
  saveStatusDraft: async () => {
    throw new OfflineError('OFFLINE_DISABLED');
  },
  submitStatusDraft: async () => {
    throw new OfflineError('OFFLINE_DISABLED');
  },
  abandonStatusDraft: noop,
  reviewStatusOperation: async () => {
    throw new OfflineError('OFFLINE_DISABLED');
  },
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
  availableInBuild: false,
  preferenceChanging: false,
  setPreference: async () => {
    throw new OfflineError('OFFLINE_DISABLED');
  },
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
  const availableInBuild = Platform.OS !== 'web' && OFFLINE_ENABLED;
  const [initialized, setInitialized] = useState(!availableInBuild);
  const [preferenceOn, setPreferenceOn] = useState(false);
  const [preferenceOwner, setPreferenceOwner] = useState<string | null>(null);
  const [preferenceChanging, setPreferenceChanging] = useState(false);
  const [repository, setRepository] = useState<OfflineRepository | null>(null);
  const [prepared, setPrepared] = useState<PreparedWorkOrderSummary[]>([]);
  const [readable, setReadable] = useState(false);
  const [networkAvailable, setNetworkAvailable] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [transitioning, setTransitioning] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [gate, setGate] = useState<AccessGate>(
    availableInBuild ? 'checking' : 'open'
  );
  const [gateOwner, setGateOwner] = useState<string | null>(null);
  const [lockIssue, setLockIssue] = useState<LockIssue>('none');
  const [unlocking, setUnlocking] = useState(false);
  const [renewing, setRenewing] = useState(false);
  const [setupRetry, setSetupRetry] = useState(0);
  const coordinator = useRef<SyncCoordinator | null>(null);
  const fileStore = useRef<OfflineFileStore | null>(null);
  const network = useRef(false);
  const foreground = useRef(AppState.currentState === 'active');
  const accessGate = useRef<AccessGate>(availableInBuild ? 'checking' : 'open');
  const localChoice = useRef(false);
  const requestEligibleSync = useRef<(reason: string) => Promise<void>>(noop);
  const changingChoice = useRef(false);
  const changePreference = useRef<(value: boolean) => Promise<void>>(
    async () => {
      throw new OfflineError('OFFLINE_DISABLED');
    }
  );
  const protectedWork = useRef(false);
  const gateEpoch = useRef(0);
  const unlockingNow = useRef(false);
  const renewingNow = useRef(false);
  const stopPreparedObservation = useRef<() => void>(noop);
  const startPreparedObservation = useRef<() => void>(noop);
  const requestUnlock = useRef<() => Promise<void>>(noop);
  const requestRenewal = useRef<() => Promise<void>>(noop);
  const canWrite = useRef(false);
  const writes = useRef(new Set<Promise<unknown>>());
  const writeLane = useRef<Promise<unknown>>(Promise.resolve());
  const failedWrites = useRef(new Set<string>());
  const preparedIds = useRef(new Set<number>());
  const sequence = useRef(0);
  const checkAccess = useRef<() => Promise<boolean>>(async () => false);
  const revokeAccess = useRef<() => Promise<void>>(noop);
  const ownerKey = `${authGeneration}:${auth.user?.companyId ?? ''}:${auth.user?.id ?? ''}`;
  const enabled =
    availableInBuild && preferenceOwner === ownerKey && preferenceOn;
  const gateForOwner = gateOwner === ownerKey ? gate : 'checking';
  const canStartOfflineAction = () =>
    accessGate.current === 'unlocked' &&
    foreground.current &&
    !sessionChangeInProgress();
  const canUseOfflineFeature = () =>
    canStartOfflineAction() && localChoice.current && !changingChoice.current;
  const changeGate = (next: AccessGate) => {
    accessGate.current = next;
    setGate(next);
  };
  const lockAccess = (issue: LockIssue = 'none') => {
    gateEpoch.current += 1;
    stopPreparedObservation.current();
    stopPreparedObservation.current = noop;
    if (protectedWork.current || accessGate.current === 'checking')
      changeGate('locked');
    else changeGate('open');
    setReadable(false);
    setLockIssue(issue);
    void closeOfflineDocument().catch(() => undefined);
  };
  const reportError = (failure: unknown) => {
    const code = (failure as { code?: string })?.code || '';
    if (
      code === 'OFFLINE_NOT_AUTHORIZED' ||
      code === 'OFFLINE_CONTEXT_MISMATCH'
    ) {
      canWrite.current = false;
      setReadable(false);
      void revokeAccess.current();
      lockAccess('authorization');
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
        if (usable && canStartOfflineAction())
          void requestEligibleSync.current('network').catch(reportError);
      }),
    []
  );

  useEffect(() => {
    const listener = AppState.addEventListener('change', (state) => {
      foreground.current = state === 'active';
      if (state !== 'active') {
        coordinator.current?.cancelTransfers();
        return;
      }
      if (accessGate.current === 'locked') {
        if (localChoice.current && protectedWork.current)
          void requestUnlock.current();
        return;
      }
      if (accessGate.current === 'unlocked')
        void checkAccess
          .current()
          .then((allowed) => {
            if (allowed && canUseOfflineFeature())
              void requestEligibleSync.current('foreground').catch(reportError);
          })
          .catch(reportError);
    });
    return () => listener.remove();
  }, []);

  useEffect(() => {
    const currentSequence = ++sequence.current;
    let active = true;
    let unregister = () => {};
    let expiryTimer: ReturnType<typeof setTimeout> | undefined;
    gateEpoch.current += 1;
    protectedWork.current = false;
    localChoice.current = false;
    requestEligibleSync.current = noop;
    changingChoice.current = false;
    changePreference.current = async () => {
      throw new OfflineError('OFFLINE_DISABLED');
    };
    unlockingNow.current = false;
    renewingNow.current = false;
    requestUnlock.current = noop;
    requestRenewal.current = noop;
    stopPreparedObservation.current();
    stopPreparedObservation.current = noop;
    startPreparedObservation.current = noop;
    changeGate(availableInBuild ? 'checking' : 'open');
    setGateOwner(ownerKey);
    setPreferenceOn(false);
    setPreferenceOwner(null);
    setPreferenceChanging(false);
    setLockIssue('none');
    setUnlocking(false);
    setRenewing(false);
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
      let selected = false;
      if (availableInBuild) {
        try {
          selected = await readOfflinePreference(context);
        } catch {
          throw new OfflineError('OFFLINE_PREFERENCE_READ_FAILED');
        }
      }
      if (!isCurrent()) return;
      localChoice.current = selected;
      setPreferenceOn(selected);
      setPreferenceOwner(ownerKey);
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
            if (protectedWork.current) lockAccess('authorization');
            else void closeOfflineDocument().catch(() => undefined);
          }, Math.max(0, expires - Date.now()));
      };
      checkAccess.current = async () => {
        // A context change drains already accepted local writes before changing its generation.
        if (!availableInBuild || !contextIsCurrent()) return false;
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
          if (!allowed && protectedWork.current) lockAccess('authorization');
          else if (!allowed) void closeOfflineDocument().catch(() => undefined);
        }
        return allowed;
      };
      const snapshot =
        (availableInBuild ? await auth.getServerSession() : null) ||
        (await getOnlineIdentity(serverUrl));
      // Disabled fresh installations do not create or open an offline database.
      if (!snapshot || serverContextKey(snapshot.context) !== key) {
        if (isCurrent()) changeGate('open');
        return;
      }
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
        if (!isCurrent() || !canStartOfflineAction()) return false;
        const allowed = await checkAccess.current();
        return allowed && isCurrent() && canStartOfflineAction();
      });
      fileStore.current = files;
      let lastRefresh = 0;
      let serverDenied = false;
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
      const valid = availableInBuild
        ? await restoreLocalAuthorization(serverUrl)
        : null;
      const localAllowed = !!valid && serverContextKey(valid.context) === key;
      setReadable(false);
      canWrite.current = localAllowed && availableInBuild;
      await scheduleExpiry();
      const sync = new Coordinator(
        repo,
        client,
        {
          enabled: () => availableInBuild,
          online: () =>
            network.current && canStartOfflineAction() && localChoice.current,
          sessionIsCurrent: isCurrent,
          onEligibility: async (eligibility) => {
            if (!isCurrent()) return;
            if (
              eligibility.companyId !== context.companyId ||
              eligibility.userId !== context.userId
            )
              throw new OfflineError('OFFLINE_CONTEXT_MISMATCH');
            if (
              availableInBuild &&
              eligibility.enabled &&
              eligibility.eligible
            ) {
              await authorizeLocalAccess(context, eligibility, snapshot);
              if (!isCurrent()) return;
              serverDenied = false;
              setReadable(true);
              canWrite.current = true;
              setLockIssue('none');
              await scheduleExpiry();
            } else {
              serverDenied = true;
              setReadable(false);
              canWrite.current = false;
              if (protectedWork.current) lockAccess('authorization');
              await revokeLocalAccess(context);
              await closeOfflineDocument();
              if (!isCurrent()) return;
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
      const hasProtectedWork = async (items: PreparedWorkOrderSummary[]) =>
        items.length > 0 ||
        (await repo.hasUnresolved()) ||
        (await repo.getPendingWork()).length > 0 ||
        (await repo.getPendingReleases()).length > 0 ||
        (await repo.allFiles()).length > 0;
      requestEligibleSync.current = async (reason) => {
        if (
          !isCurrent() ||
          coordinator.current !== sync ||
          !network.current ||
          !canUseOfflineFeature()
        )
          return;
        const hadRelease = (await repo.getPendingReleases()).length > 0;
        try {
          await sync.trigger(reason);
        } finally {
          if (hadRelease && isCurrent() && coordinator.current === sync) {
            const items = await repo.getPreparedWorkOrderSummaries();
            const needed = await hasProtectedWork(items);
            if (isCurrent() && coordinator.current === sync) {
              protectedWork.current = needed;
              if (!needed) {
                changeGate('open');
                setReadable(false);
                setLockIssue('none');
                if (foreground.current) startPreparedObservation.current();
              }
            }
          }
        }
      };
      setRepository(repo);
      let recovered = false;
      const recoverFiles = async () => {
        if (recovered) return;
        await files.recover();
        recovered = true;
      };
      const acceptPrepared = (items: PreparedWorkOrderSummary[]) => {
        if (!isCurrent()) return;
        preparedIds.current = new Set(items.map((item) => item.id));
        setPrepared(items);
        if (items.length) {
          protectedWork.current = true;
          if (accessGate.current === 'open') lockAccess();
        }
        else
          void hasProtectedWork(items).then((needed) => {
            if (!isCurrent()) return;
            protectedWork.current = needed;
            if (!needed && accessGate.current === 'unlocked')
              changeGate('open');
          }).catch(reportError);
        const ids = items.map((item) => item.id);
        if (availableInBuild && localChoice.current) {
          dispatch(removePreparedTasks(ids));
          dispatch(removePreparedLabors(ids));
          dispatch(removePreparedWorkOrders(ids));
          dispatch(removePreparedComments(ids));
        }
      };
      const observePrepared = () => {
        stopPreparedObservation.current();
        const observed = repo
          .observePreparedWorkOrderSummaries()
          .subscribe({ next: acceptPrepared, error: reportError });
        stopPreparedObservation.current = () => observed.unsubscribe();
      };
      startPreparedObservation.current = observePrepared;
      requestUnlock.current = async () => {
        if (
          unlockingNow.current ||
          !isCurrent() ||
          !foreground.current ||
          accessGate.current !== 'locked'
        )
          return;
        unlockingNow.current = true;
        setUnlocking(true);
        const expectedEpoch = gateEpoch.current;
        const stillWaiting = () =>
          isCurrent() &&
          gateEpoch.current === expectedEpoch &&
          accessGate.current === 'locked';
        try {
          if (serverDenied) {
            if (stillWaiting()) setLockIssue('denied');
            return;
          }
          if (!(await canVerifyDeviceOwner())) {
            if (stillWaiting()) setLockIssue('unavailable');
            return;
          }
          const before = await restoreLocalAuthorization(serverUrl).catch(
            () => null
          );
          if (!stillWaiting()) return;
          if (!before || serverContextKey(before.context) !== key) {
            setLockIssue('authorization');
            return;
          }
          const result = await verifyDeviceOwner({
            prompt: t('offline_unlock_system_prompt'),
            cancel: t('cancel'),
            fallback: t('offline_unlock_use_code')
          });
          if (!stillWaiting()) return;
          if (result !== 'verified') {
            setLockIssue(result);
            return;
          }
          if (!(await waitForActiveApp()) || !stillWaiting()) return;
          await recoverFiles();
          const items = await repo.getPreparedWorkOrderSummaries();
          const needed = await hasProtectedWork(items);
          const after = await restoreLocalAuthorization(serverUrl).catch(
            () => null
          );
          if (!stillWaiting() || !foreground.current) return;
          if (!after || serverContextKey(after.context) !== key) {
            setLockIssue('authorization');
            return;
          }
          protectedWork.current = needed;
          acceptPrepared(items);
          changeGate(needed ? 'unlocked' : 'open');
          canWrite.current = true;
          setReadable(needed);
          setLockIssue('none');
          observePrepared();
          if (needed && network.current)
            void requestEligibleSync.current('unlock').catch(reportError);
        } catch (failure) {
          if (stillWaiting()) {
            setLockIssue('storage');
            reportError(failure);
          }
        } finally {
          if (currentSequence === sequence.current) unlockingNow.current = false;
          if (isCurrent()) setUnlocking(false);
        }
      };
      requestRenewal.current = async () => {
        if (
          renewingNow.current ||
          unlockingNow.current ||
          !isCurrent() ||
          !foreground.current ||
          accessGate.current !== 'locked'
        )
          return;
        renewingNow.current = true;
        setRenewing(true);
        const expectedEpoch = gateEpoch.current;
        const stillLocked = () =>
          isCurrent() &&
          gateEpoch.current === expectedEpoch &&
          accessGate.current === 'locked' &&
          foreground.current &&
          coordinator.current === sync;
        let renewed = false;
        try {
          if (!network.current) {
            setLockIssue('authorization');
            return;
          }
          // A local session or stored credentials alone cannot renew a grant.
          // The eligibility response must come from this authenticated server.
          const before = await auth.getServerSession();
          if (!stillLocked()) return;
          if (!before || serverContextKey(before.context) !== key) {
            setLockIssue('session');
            return;
          }
          const eligibility = await sync.checkEligibilityOnly();
          if (!stillLocked()) return;
          if (!network.current) {
            setLockIssue('authorization');
            return;
          }
          if (!eligibility.enabled || !eligibility.eligible) {
            serverDenied = true;
            await revokeLocalAccess(context);
            if (stillLocked()) setLockIssue('denied');
            return;
          }
          if (
            eligibility.companyId !== context.companyId ||
            eligibility.userId !== context.userId
          ) {
            setLockIssue('session');
            return;
          }
          const after = await auth.getServerSession();
          if (!stillLocked()) return;
          if (!after || serverContextKey(after.context) !== key) {
            setLockIssue('session');
            return;
          }
          await authorizeLocalAccess(context, eligibility, after);
          if (!stillLocked()) return;
          serverDenied = false;
          await scheduleExpiry();
          if (!stillLocked()) return;
          setLockIssue('none');
          renewed = true;
        } catch (failure) {
          if (stillLocked()) {
            const status = (failure as { status?: number })?.status;
            if (status === 403) {
              serverDenied = true;
              await revokeLocalAccess(context).catch(() => undefined);
            }
            if (!stillLocked()) return;
            setLockIssue(
              status === 401
                ? 'session'
                : status === 403
                ? 'denied'
                : 'authorization'
            );
          }
        } finally {
          if (currentSequence === sequence.current) renewingNow.current = false;
          if (isCurrent()) setRenewing(false);
        }
        if (renewed && stillLocked()) void requestUnlock.current();
      };
      changePreference.current = async (next) => {
        const expectedEpoch = gateEpoch.current;
        const canChange = () =>
          availableInBuild &&
          isCurrent() &&
          foreground.current &&
          gateEpoch.current === expectedEpoch &&
          (accessGate.current === 'open' ||
            accessGate.current === 'unlocked' ||
            (next && !localChoice.current && accessGate.current === 'locked'));
        if (!canChange()) throw new OfflineError('OFFLINE_DISABLED');
        if (changingChoice.current) throw new OfflineError('CONTEXT_BUSY');
        if (localChoice.current === next) return;
        changingChoice.current = true;
        setPreferenceChanging(true);
        try {
          let preparedToProject: number[] = [];
          if (next) {
            if (!network.current) throw new OfflineError('NETWORK_UNAVAILABLE');
            if (!(await canVerifyDeviceOwner()))
              throw new OfflineError('DEVICE_LOCK_REQUIRED');
            if (!canChange()) throw new OfflineError('CONTEXT_CHANGED');
            const session = await auth.getServerSession();
            if (!canChange()) throw new OfflineError('CONTEXT_CHANGED');
            if (!session || serverContextKey(session.context) !== key)
              throw new OfflineError('AUTHENTICATION_REQUIRED');
            const eligibility = await sync.checkEligibilityOnly();
            if (!canChange()) throw new OfflineError('CONTEXT_CHANGED');
            if (!network.current) throw new OfflineError('NETWORK_UNAVAILABLE');
            if (
              eligibility.companyId !== context.companyId ||
              eligibility.userId !== context.userId
            )
              throw new OfflineError('OFFLINE_CONTEXT_MISMATCH');
            if (!eligibility.enabled || !eligibility.eligible)
              throw new OfflineError('OFFLINE_NOT_AUTHORIZED');
            const confirmedSession = await auth.getServerSession();
            if (!canChange()) throw new OfflineError('CONTEXT_CHANGED');
            if (
              !confirmedSession ||
              serverContextKey(confirmedSession.context) !== key
            )
              throw new OfflineError('AUTHENTICATION_REQUIRED');
            await authorizeLocalAccess(context, eligibility, confirmedSession);
            if (!canChange()) throw new OfflineError('CONTEXT_CHANGED');
            const grant = await restoreLocalAuthorization(serverUrl);
            if (!canChange()) throw new OfflineError('CONTEXT_CHANGED');
            if (!grant || serverContextKey(grant.context) !== key)
              throw new OfflineError('INVALID_AUTHORIZATION');
            await scheduleExpiry();
            if (!canChange()) throw new OfflineError('CONTEXT_CHANGED');
            preparedToProject = (
              await repo.getPreparedWorkOrderSummaries()
            ).map((item) => item.id);
          } else {
            // Stop new local submissions, then inspect every accepted write.
            await Promise.all([...writes.current]);
            if (!canChange()) throw new OfflineError('CONTEXT_CHANGED');
            if (failedWrites.current.size)
              throw new OfflineError('STORAGE_FAILED');
            if (
              (await repo.hasUnresolved()) ||
              (await repo.getPendingWork()).length > 0 ||
              (await repo.getPendingReleases()).length > 0
            )
              throw new OfflineError('WORK_PENDING');
          }
          if (!canChange()) throw new OfflineError('CONTEXT_CHANGED');
          await writeOfflinePreference(context, next);
          // A write accepted in the foreground may finish after backgrounding.
          // Keep memory consistent with the durable choice for this context.
          if (!isCurrent()) throw new OfflineError('CONTEXT_CHANGED');
          if (next) {
            dispatch(removePreparedTasks(preparedToProject));
            dispatch(removePreparedLabors(preparedToProject));
            dispatch(removePreparedWorkOrders(preparedToProject));
            dispatch(removePreparedComments(preparedToProject));
          }
          localChoice.current = next;
          setPreferenceOn(next);
          if (next && protectedWork.current) {
            serverDenied = false;
            if (accessGate.current !== 'locked') lockAccess();
            setLockIssue('none');
            void requestUnlock.current();
          }
        } finally {
          if (currentSequence === sequence.current)
            changingChoice.current = false;
          if (isCurrent()) setPreferenceChanging(false);
        }
      };
      const initialItems = await repo.getPreparedWorkOrderSummaries();
      const needsLock = await hasProtectedWork(initialItems);
      if (!isCurrent()) return;
      protectedWork.current = needsLock;
      acceptPrepared(initialItems);
      changeGate(needsLock ? 'locked' : 'open');
      setLockIssue(needsLock && !localAllowed ? 'authorization' : 'none');
      if (!needsLock) observePrepared();
      setPreparedRuntime({
        hasPreparation: (id) =>
          availableInBuild &&
          localChoice.current &&
          isCurrent() &&
          preparedIds.current.has(id),
        refresh: (id) =>
          canUseOfflineFeature()
            ? sync.refresh(id)
            : Promise.reject(new OfflineError('OFFLINE_DISABLED')),
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
              !canUseOfflineFeature() ||
              !(await checkAccess.current()) ||
              !canUseOfflineFeature() ||
              !network.current
            )
              throw new OfflineError('NETWORK_UNAVAILABLE');
            const target = await repo.getWorkOrderProjection(workOrderId);
            if (!target?.available) throw new OfflineError('EDIT_FORBIDDEN');
            if (!canUseOfflineFeature())
              throw new OfflineError('NETWORK_UNAVAILABLE');
            return action();
          }),
        runConnectedLabor: (workOrderId, action) =>
          sync.runConnected(async () => {
            if (
              !isCurrent() ||
              !canUseOfflineFeature() ||
              !(await checkAccess.current()) ||
              !canUseOfflineFeature() ||
              !network.current
            )
              throw new OfflineError('NETWORK_UNAVAILABLE');
            const target = await repo.getWorkOrderProjection(workOrderId);
            if (!target?.available || !target.canEdit)
              throw new OfflineError('EDIT_FORBIDDEN');
            if (!canUseOfflineFeature())
              throw new OfflineError('NETWORK_UNAVAILABLE');
            return action();
          }),
        markUnavailable: (id, reason) => repo.markUnavailable(id, reason),
        patchTask: async (workOrderId, taskId, patch) => {
          if (
            !isCurrent() ||
            !canUseOfflineFeature() ||
            !(await checkAccess.current()) ||
            !canUseOfflineFeature() ||
            !network.current
          )
            throw new OfflineError('NETWORK_UNAVAILABLE');
          return sync.patchTask(workOrderId, taskId, patch);
        }
      });
      unregister = registerOfflineLifecycle({
        beforeContextChange: async () => {
          setTransitioning(true);
          lockAccess();
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
          if (
            (await repo.hasUnresolved()) ||
            (await repo.getPendingReleases()).length > 0
          )
            Alert.alert(t('offline_pending'), t('offline_leave_context'));
          setPreparedRuntime(null);
          setReadable(false);
          setPrepared([]);
          setRepository(null);
        }
      });
      if (isCurrent()) {
        setInitialized(true);
        if (needsLock && foreground.current && localChoice.current)
          void requestUnlock.current();
        if (!needsLock && foreground.current && network.current)
          void requestEligibleSync.current('start').catch(reportError);
      }
    };
    void setup()
      .catch((failure) => {
        if (
          active &&
          currentSequence === sequence.current &&
          authGeneration === sessionGeneration()
        ) {
          lockAccess(
            (failure as { code?: string })?.code ===
              'OFFLINE_PREFERENCE_READ_FAILED'
              ? 'preference'
              : 'storage'
          );
          reportError(failure);
        }
      })
      .finally(() => {
        if (
          active &&
          currentSequence === sequence.current &&
          authGeneration === sessionGeneration()
        )
          setInitialized(true);
      });
    return () => {
      active = false;
      stopPreparedObservation.current();
      stopPreparedObservation.current = noop;
      startPreparedObservation.current = noop;
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
    availableInBuild,
    authGeneration,
    setupRetry
  ]);

  const persist = <T,>(
    action: (acceptedAccess: () => Promise<boolean>) => Promise<T>
  ): Promise<T> => {
    if (sessionChangeInProgress())
      return Promise.reject(new OfflineError('CONTEXT_BUSY'));
    // Admission is synchronous. A write already accepted may finish while
    // the app is in the background; new actions still require the foreground.
    if (!canUseOfflineFeature() || !canWrite.current)
      return Promise.reject(new OfflineError('OFFLINE_DISABLED'));
    const generation = sessionGeneration();
    const acceptedAccess = () => {
      if (generation !== sessionGeneration()) return Promise.resolve(false);
      // A write admitted while unlocked may finish after backgrounding, when
      // SecureStore can no longer be read until the phone is unlocked again.
      if (!foreground.current) return Promise.resolve(true);
      return checkAccess.current();
    };
    // Reserve the lane before any SecureStore/database await so keystrokes cannot overtake one another.
    const writing = writeLane.current.then(() => {
      if (generation !== sessionGeneration())
        throw new OfflineError('OFFLINE_DISABLED');
      return action(acceptedAccess);
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
    const writing = persist(async (acceptedAccess) => {
      if (!repository || !(await acceptedAccess()))
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
    const writing = persist(async (acceptedAccess) => {
      if (!repository || !(await acceptedAccess()))
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
    persist(async (acceptedAccess) => {
      if (!repository || !(await acceptedAccess()))
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
  const projectionsOpen =
    enabled &&
    !preferenceChanging &&
    gateForOwner === 'unlocked' &&
    readable &&
    !transitioning;
  // A mounted handler may retain an older context value. Check again at the
  // method boundary so it cannot start a new repository read after locking.
  const exposedRepository = useMemo(
    () =>
      repository &&
      new Proxy(repository, {
        get(target, property) {
          const member = Reflect.get(target, property, target);
          if (typeof member !== 'function') return member;
          return (...args: unknown[]) => {
            if (!canUseOfflineFeature())
              throw new OfflineError('OFFLINE_DISABLED');
            return member.apply(target, args);
          };
        }
      }),
    [repository]
  );
  const lockVisible =
    availableInBuild &&
    auth.isAuthenticated &&
    (lockIssue === 'preference' ||
      (preferenceOwner !== ownerKey && gateForOwner === 'checking') ||
      (enabled && (gateForOwner === 'checking' || gateForOwner === 'locked')));
  const value: OfflineContextValue = {
    saveStatusDraft: (id, input, revision) => {
      const writing = persistTime((repo) =>
        repo.saveStatusDraft(id, input, revision)
      );
      const key = `work-order:${id}:status`;
      void writing.then(
        () => failedWrites.current.delete(key),
        () => failedWrites.current.add(key)
      );
      return writing;
    },
    submitStatusDraft: (id, revision) =>
      persistTime((repo) => {
        if (failedWrites.current.has(`work-order:${id}:status`))
          throw new OfflineError('STORAGE_FAILED');
        return repo.submitStatusDraft(id, revision);
      }, true),
    abandonStatusDraft: (id, revision) =>
      persist(async () => {
        if (!repository) throw new OfflineError('OFFLINE_DISABLED');
        await repository.abandonStatusDraft(id, revision);
        failedWrites.current.delete(`work-order:${id}:status`);
      }),
    reviewStatusOperation: (
      id,
      draftRevision,
      serverRevision,
      activeTimerRuns
    ) =>
      persistTime((repo) =>
        repo.reviewStatusOperation(
          id,
          draftRevision,
          serverRevision,
          activeTimerRuns
        )
      ),
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
    availableInBuild,
    preferenceChanging,
    setPreference: (next) =>
      authGeneration === sessionGeneration() && !sessionChangeInProgress()
        ? changePreference.current(next)
        : Promise.reject(new OfflineError('CONTEXT_CHANGED')),
    initialized,
    readable: projectionsOpen,
    writable: projectionsOpen,
    networkAvailable,
    syncing,
    error,
    repository: projectionsOpen ? exposedRepository : null,
    prepared: projectionsOpen ? prepared : [],
    reportError,
    sync: async () => {
      if (!canUseOfflineFeature()) return;
      setError(null);
      try {
        await coordinator.current?.trigger('manual');
      } catch (failure) {
        reportError(failure);
      }
    },
    prepare: async (id) => {
      setError(null);
      if (
        !coordinator.current ||
        !availableInBuild ||
        !localChoice.current ||
        changingChoice.current
      )
        throw new OfflineError('OFFLINE_DISABLED');
      if (!foreground.current || sessionChangeInProgress())
        throw new OfflineError('OFFLINE_DISABLED');
      if (accessGate.current === 'open') {
        const generation = sessionGeneration();
        const expectedCoordinator = coordinator.current;
        const expectedEpoch = ++gateEpoch.current;
        changeGate('checking');
        try {
          const result = await verifyDeviceOwner({
            prompt: t('offline_unlock_system_prompt'),
            cancel: t('cancel'),
            fallback: t('offline_unlock_use_code')
          });
          if (result !== 'unavailable' && !(await waitForActiveApp()))
            throw new OfflineError('CONTEXT_CHANGED');
          if (
            generation !== sessionGeneration() ||
            expectedCoordinator !== coordinator.current ||
            expectedEpoch !== gateEpoch.current ||
            !foreground.current
          )
            throw new OfflineError('CONTEXT_CHANGED');
          if (result !== 'verified')
            throw new OfflineError(
              result === 'unavailable'
                ? 'DEVICE_LOCK_REQUIRED'
                : result === 'cancelled'
                ? 'DEVICE_UNLOCK_CANCELLED'
                : 'DEVICE_UNLOCK_FAILED'
            );
          changeGate('unlocked');
          await expectedCoordinator.prepare(id);
        } finally {
          if (
            expectedCoordinator === coordinator.current &&
            generation === sessionGeneration()
          ) {
            const repo = expectedCoordinator.repository;
            const items = await repo.getPreparedWorkOrderSummaries();
            protectedWork.current =
              items.length > 0 ||
              (await repo.hasUnresolved()) ||
              (await repo.getPendingWork()).length > 0 ||
              (await repo.getPendingReleases()).length > 0 ||
              (await repo.allFiles()).length > 0;
            if (!protectedWork.current) changeGate('open');
            if (accessGate.current === 'open')
              startPreparedObservation.current();
          }
        }
      } else {
        if (!canUseOfflineFeature())
          throw new OfflineError('OFFLINE_DISABLED');
        await coordinator.current.prepare(id);
      }
    },
    refresh: async (id) => {
      if (!canUseOfflineFeature())
        throw new OfflineError('OFFLINE_DISABLED');
      if (coordinator.current) await coordinator.current.refresh(id);
    },
    removePreparation: async (id) => {
      if (!canUseOfflineFeature())
        throw new OfflineError('OFFLINE_DISABLED');
      if (coordinator.current) await coordinator.current.removePreparation(id);
    },
    removeServerAttachment: async (workOrderId, fileId, image) => {
      const sync = coordinator.current;
      const generation = sessionGeneration();
      if (
        !sync ||
        !canUseOfflineFeature() ||
        !canWrite.current ||
        !(await checkAccess.current()) ||
        !canUseOfflineFeature()
      )
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
      persist(async (acceptedAccess) => {
        if (!fileStore.current || !(await acceptedAccess()))
          throw new OfflineError('OFFLINE_DISABLED');
        await fileStore.current.importFiles(
          workOrderId,
          target,
          files,
          maxBytes
        );
      }),
    retryFileImport: (id) =>
      persist(async (acceptedAccess) => {
        if (!fileStore.current || !(await acceptedAccess()))
          throw new OfflineError('OFFLINE_DISABLED');
        await fileStore.current.retryFileImport(id);
      }),
    removeDraftFile: (workOrderId, target, id, revision) =>
      persist(async (acceptedAccess) => {
        if (!repository || !(await acceptedAccess()))
          throw new OfflineError('OFFLINE_DISABLED');
        await repository.removeDraftFile(workOrderId, target, id, revision);
        await fileStore.current?.cleanup();
      }),
    submitAttachmentDraft: (workOrderId, revision, taskId) =>
      persist(async (acceptedAccess) => {
        if (!repository || !(await acceptedAccess()))
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
      persist(async (acceptedAccess) => {
        if (!repository || !(await acceptedAccess()))
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
      if (
        !coordinator.current ||
        !canUseOfflineFeature() ||
        !(await checkAccess.current()) ||
        !canUseOfflineFeature()
      )
        throw new OfflineError('OFFLINE_DISABLED');
      await coordinator.current.downloadFile(workOrderId, id);
    },
    openFile: async (id) => {
      if (
        !fileStore.current ||
        !canUseOfflineFeature() ||
        !(await checkAccess.current()) ||
        !canUseOfflineFeature()
      )
        throw new OfflineError('OFFLINE_DISABLED');
      await fileStore.current.openFile(id);
    },
    removeLocalCopy: (id) =>
      persist(async (acceptedAccess) => {
        if (!fileStore.current || !(await acceptedAccess()))
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
      const writing = persist(async (acceptedAccess) => {
        if (!repository || !(await acceptedAccess()))
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
      persist(async (acceptedAccess) => {
        if (!repository || !(await acceptedAccess()))
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
      persist(async (acceptedAccess) => {
        if (!repository || !(await acceptedAccess()))
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
      persist(async (acceptedAccess) => {
        if (!repository || !(await acceptedAccess()))
          throw new OfflineError('OFFLINE_DISABLED');
        const operation = await repository.submitDraft(taskId, revision, field);
        void coordinator.current?.trigger('submission').catch(reportError);
        return operation;
      }),
    resolveConflict: (taskId, revision, serverVersion, field = 'value') =>
      persist(async (acceptedAccess) => {
        if (!repository || !(await acceptedAccess()))
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
    <OfflineContext.Provider value={value}>
      <View style={styles.container}>
        {children}
        {lockVisible && (
          <View style={styles.immediateMask} pointerEvents="auto" />
        )}
      </View>
      <Modal
        visible={lockVisible}
        animationType="none"
        presentationStyle="fullScreen"
        statusBarTranslucent
        navigationBarTranslucent
        onRequestClose={() => undefined}
      >
        <OfflineLockScreen
          waiting={gateForOwner === 'checking'}
          unlocking={unlocking || renewing}
          issue={lockIssue}
          onRetry={() => {
            if (lockIssue === 'authorization' || lockIssue === 'denied')
              void requestRenewal.current();
            else if (requestUnlock.current === noop)
              setSetupRetry((value) => value + 1);
            else void requestUnlock.current();
          }}
          onLogout={() => void auth.logout().catch(reportError)}
        />
      </Modal>
    </OfflineContext.Provider>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  immediateMask: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: '#ffffff',
    zIndex: 1000,
    elevation: 1000
  }
});
