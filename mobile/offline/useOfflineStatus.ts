import { useEffect, useRef, useState } from 'react';
import { useOffline } from './OfflineProvider';
import { OfflineError } from './types';
import type { OfflineStatusView, StatusDraft, StatusInput } from './types';
import type { WorkOrderStatus } from '../models/workOrder';
import { assertSessionGeneration, sessionGeneration } from './sessionBridge';

/** Prepared status actions share the durable account lane, including while online. */
export function useOfflineStatus(workOrderId: number, enabled = true) {
  const offline = useOffline();
  const [observed, setObserved] = useState<{
    repository: typeof offline.repository;
    id: number;
    value: OfflineStatusView;
  } | null>(null);
  const [saving, setSaving] = useState(false);
  const [busy, setBusy] = useState(false);
  const [saveError, setSaveError] = useState(false);
  const context = useRef({ repository: offline.repository, workOrderId });
  context.current = { repository: offline.repository, workOrderId };
  const draft = useRef<StatusDraft | null>(null);
  const writing = useRef<Promise<unknown>>(Promise.resolve());
  const writeCount = useRef(0);
  const submitting = useRef(false);
  const requesting = useRef(false);
  useEffect(() => {
    setObserved(null);
    draft.current = null;
    setSaving(false);
    setBusy(false);
    setSaveError(false);
    writeCount.current = 0;
    submitting.current = false;
    requesting.current = false;
    writing.current = Promise.resolve();
    if (!enabled || !offline.readable || !offline.repository) return;
    const repo = offline.repository;
    const subscription = repo.observeStatus(workOrderId).subscribe({
      next: (value) => {
        if (!writeCount.current) draft.current = value.draft;
        setObserved({ repository: repo, id: workOrderId, value });
      },
      error: offline.reportError
    });
    return () => subscription.unsubscribe();
  }, [enabled, offline.readable, offline.repository, workOrderId]);
  const view =
    enabled &&
    offline.readable &&
    observed?.repository === offline.repository &&
    observed?.id === workOrderId
      ? observed.value
      : null;
  const guard = () => {
    const current = context.current;
    const generation = sessionGeneration();
    return () => {
      assertSessionGeneration(generation);
      if (
        current.repository !== context.current.repository ||
        current.workOrderId !== context.current.workOrderId
      )
        throw new OfflineError('CONTEXT_CHANGED');
    };
  };
  const save = (input: StatusInput): Promise<StatusDraft> => {
    const check = guard();
    ++writeCount.current;
    setSaving(true);
    // Reserve the provider lane synchronously: account changes must await every
    // keystroke already accepted by this hook, not only its first queued write.
    const pending = offline
      .saveStatusDraft(workOrderId, input)
      .then((result) => {
        check();
        draft.current = result;
        setSaveError(false);
        return result;
      });
    writing.current = pending;
    void pending
      .then(
        () => {},
        (error) => {
          try {
            check();
            setSaveError(true);
            offline.reportError(error);
          } catch {}
        }
      )
      .finally(() => {
        try {
          check();
          --writeCount.current;
          setSaving(writeCount.current > 0);
        } catch {}
      });
    return pending;
  };
  const submit = async (revision?: number) => {
    if (submitting.current) throw new OfflineError('PREVIOUS_OPERATION');
    const check = guard();
    submitting.current = true;
    setBusy(true);
    try {
      await writing.current;
      check();
      const exact = revision ?? draft.current?.revision;
      if (exact == null) throw new OfflineError('DRAFT_CHANGED');
      const result = await offline.submitStatusDraft(workOrderId, exact);
      check();
      if (draft.current?.revision === exact) draft.current = null;
      return result;
    } finally {
      try {
        check();
        submitting.current = false;
        setBusy(false);
      } catch {}
    }
  };
  const request = async (status: WorkOrderStatus) => {
    if (requesting.current || submitting.current)
      throw new OfflineError('PREVIOUS_OPERATION');
    const check = guard();
    requesting.current = true;
    setBusy(true);
    try {
      if (!offline.repository) throw new OfflineError('OFFLINE_DISABLED');
      const current = await offline.repository.getStatusView(workOrderId);
      check();
      if (current.draft) throw new OfflineError('STATUS_WORK_PENDING');
      const saved = await save({ status });
      check();
      return await submit(saved.revision);
    } finally {
      try {
        check();
        requesting.current = false;
        setBusy(false);
      } catch {}
    }
  };
  return { view, saving, saveError, busy, save, submit, request };
}
