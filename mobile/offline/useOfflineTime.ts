import { useEffect, useRef, useState } from 'react';
import { useOffline } from './OfflineProvider';
import type { OfflineTimeView } from './types';
import { assertSessionGeneration, sessionGeneration } from './sessionBridge';

/** The clock only paints persisted timestamps; it never accumulates worked time. */
export function useOfflineTime(workOrderId: number, enabled = true) {
  const offline = useOffline();
  const [observed, setObserved] = useState<{
    repository: typeof offline.repository;
    workOrderId: number;
    view: OfflineTimeView;
  } | null>(null);
  const [busy, setBusy] = useState(false);
  const [now, setNow] = useState(Date.now());
  const busyRef = useRef(false);
  const context = useRef({ workOrderId, repository: offline.repository });
  context.current = { workOrderId, repository: offline.repository };
  useEffect(() => {
    setObserved(null);
    setBusy(false);
    busyRef.current = false;
    if (!enabled || !offline.readable || !offline.repository) return;
    const subscription = offline.repository.observeTime(workOrderId).subscribe({
      next: (view) =>
        setObserved({ repository: offline.repository, workOrderId, view }),
      error: offline.reportError
    });
    return () => subscription.unsubscribe();
  }, [enabled, offline.readable, offline.repository, workOrderId]);
  const visible =
    enabled &&
    offline.readable &&
    observed?.repository === offline.repository &&
    observed?.workOrderId === workOrderId
      ? observed.view
      : null;
  const local = visible?.activePeriod;
  const running = !!(
    (local && !local.endedAt) ||
    (!local && visible?.serverTimer)
  );
  const startedAt = local?.startedAt || visible?.serverTimer?.startedAt;
  useEffect(() => {
    setNow(Date.now());
    if (!running) return;
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [running, startedAt]);
  const startOperation = visible?.operations.find(
    (operation) => operation.id === local?.startOperationId
  );
  const statusKey = local?.endedAt
    ? 'offline_draft_saved'
    : running
    ? !local || startOperation?.status === 'APPLIED'
      ? 'offline_time_running_server'
      : startOperation?.status === 'UNKNOWN' ||
        startOperation?.status === 'SENDING'
      ? 'offline_time_start_pending'
      : 'offline_time_running_local'
    : null;
  const lastTimerOperation = visible?.operations
    .filter(
      (operation) =>
        operation.request.type === 'START_TIMER' ||
        operation.request.type === 'STOP_TIMER'
    )
    .slice(-1)[0];
  const resumeWithoutSnapshot =
    lastTimerOperation?.status === 'APPLIED' &&
    !lastTimerOperation.snapshotConfirmed &&
    !!lastTimerOperation.receipt?.timeResult &&
    lastTimerOperation.receipt.visibility !== 'LABOR_DELETED';
  const control = async () => {
    if (busyRef.current || !visible?.available || !offline.writable) return;
    const generation = sessionGeneration();
    const current = context.current;
    const isCurrent = () =>
      generation === sessionGeneration() &&
      current.workOrderId === context.current.workOrderId &&
      current.repository === context.current.repository;
    busyRef.current = true;
    setBusy(true);
    try {
      if (running)
        await offline.stopTimer(
          workOrderId,
          local?.revision,
          !local && visible.serverTimer
            ? {
                laborId: visible.serverTimer.id,
                laborVersion: visible.serverTimer.version,
                timerRunId: visible.serverTimer.timerRunId,
                startedAt: visible.serverTimer.startedAt
              }
            : undefined
        );
      else await offline.startTimer(workOrderId);
      assertSessionGeneration(generation);
    } catch (error) {
      if (isCurrent()) offline.reportError(error);
    } finally {
      if (isCurrent()) {
        busyRef.current = false;
        setBusy(false);
      }
    }
  };
  return {
    view: visible,
    busy,
    running,
    statusKey,
    resumeWithoutSnapshot,
    elapsedSeconds: startedAt
      ? Math.max(0, Math.floor((now - Date.parse(startedAt)) / 1000))
      : 0,
    control
  };
}
