import { useEffect, useRef, useState } from 'react';
import { View } from 'react-native';
import { Button, Switch, Text } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { RootStackParamList } from '../../types';
import type Labor from '../../models/labor';
import CustomDateTimePicker from '../../components/CustomDateTimePicker';
import { useOffline } from '../OfflineProvider';
import { offlineErrorKey } from '../errors';
import { getHMSString } from '../../utils/formatters';
import { isTimeOperation } from '../types';
import type { LocalOperation, OfflineTimeView, TimerDraft } from '../types';
import { assertSessionGeneration, sessionGeneration } from '../sessionBridge';

/** Present one total. Keep only actionable time conflicts below the timer. */
export function OfflineTimePanel({
  workOrderId,
  view,
  navigation,
  statusKey
}: {
  workOrderId: number;
  view: OfflineTimeView;
  navigation: NativeStackNavigationProp<RootStackParamList>;
  statusKey: string | null;
}) {
  const offline = useOffline();
  const { t } = useTranslation();
  const [busy, setBusy] = useState(false);
  const [review, setReview] = useState<{
    operationId: string;
    labor: Labor | null;
    status: string;
    statusRevision: number;
  } | null>(null);
  const [preserveStatus, setPreserveStatus] = useState(false);
  const generation = sessionGeneration();
  const mounted = useRef(true);
  useEffect(
    () => () => {
      mounted.current = false;
    },
    []
  );
  useEffect(() => {
    setReview(null);
    setBusy(false);
  }, [offline.repository, workOrderId]);
  const run = async (action: () => Promise<unknown>) => {
    if (busy) return;
    setBusy(true);
    try {
      await action();
    } catch (error) {
      if (mounted.current && generation === sessionGeneration())
        offline.reportError(error);
    } finally {
      if (mounted.current && generation === sessionGeneration()) setBusy(false);
    }
  };
  const examine = (operation: LocalOperation) =>
    run(async () => {
      await offline.refresh(workOrderId);
      assertSessionGeneration(generation);
      const current = await offline.repository.getTimeView(workOrderId);
      const projection = await offline.repository.getWorkOrderProjection(
        workOrderId
      );
      assertSessionGeneration(generation);
      if (!mounted.current || !isTimeOperation(operation.request)) return;
      const request = operation.request;
      const laborId =
        operation.receipt?.timeResult?.laborId ||
        request.time.laborId ||
        (request.time.startOperation &&
          current.operations.find(
            (item) => item.id === request.time.startOperation.operationId
          )?.receipt?.timeResult?.laborId);
      setReview({
        operationId: operation.id,
        labor:
          current.labors.find((item) => item.id === laborId) ||
          (request.type !== 'CREATE_MANUAL_TIME' &&
            current.labors.find(
              (item) => item.logged && item.assignedTo?.id === request.userId
            )) ||
          null,
        status: projection?.workOrder?.status || '',
        statusRevision: current.timerStatusRevision
      });
      setPreserveStatus(false);
    });
  const operations = view.operations.filter(
    (operation) =>
      isTimeOperation(operation.request) &&
      (operation.canReviewTime ||
        operation.status === 'CONFLICT' ||
        operation.status === 'REJECTED')
  );
  const activeStart = view.operations.find(
    (operation) => operation.id === view.activePeriod?.startOperationId
  );
  const canEditStart =
    !view.activePeriod?.timerRunId &&
    (!view.activePeriod?.startOperationId ||
      (!!activeStart &&
        ((activeStart.status === 'QUEUED' && activeStart.attemptCount === 0) ||
          ['CONFLICT', 'REJECTED'].includes(activeStart.status))));
  const confirmed = view.labors
    .filter((labor) => labor.includeToTotalTime)
    .reduce((sum, labor) => sum + labor.duration, 0);
  const pendingSeconds = view.operations.reduce((sum, op) => {
    if (!isTimeOperation(op.request) || !op.request.time.endedAt) return sum;
    // An attempted command with a lost receipt may already be in the snapshot.
    // Add only definitely unsent work, or a receipt newer than the snapshot.
    const receipt = op.receipt?.timeResult;
    if (op.status === 'APPLIED' && receipt && !op.snapshotConfirmed) {
      const labor = view.labors.find((item) => item.id === receipt.laborId);
      return !labor ||
        (labor.includeToTotalTime && labor.version < receipt.laborVersion)
        ? sum + receipt.durationAdded
        : sum;
    }
    if (op.status !== 'QUEUED' || op.attemptCount || op.blockedReason)
      return sum;
    const laborId = op.request.time.laborId;
    const source = view.labors.find((item) => item.id === laborId);
    if (source && !source.includeToTotalTime) return sum;
    return (
      sum +
      (Date.parse(op.request.time.endedAt) -
        Date.parse(op.request.time.startedAt)) /
        1000
    );
  }, 0);
  return (
    <View style={{ margin: 10, gap: 8 }}>
      <Text>
        {t('duration')} · {getHMSString(confirmed + pendingSeconds)}
      </Text>
      {statusKey && <Text>{t(statusKey)}</Text>}
      {view.activePeriod?.endedAt && (
        <TimeReviewDraft
          key={view.activePeriod.periodId}
          draft={view.activePeriod}
          canEditStart={canEditStart}
        />
      )}
      {view.manualDraft && (
        <Button
          onPress={() =>
            navigation.navigate('AddAdditionalTime', { workOrderId })
          }
        >
          {t('offline_time_personal')}
        </Button>
      )}
      {operations.map((operation) => {
        if (!isTimeOperation(operation.request)) return null;
        const time = operation.request.time;
        const status =
          operation.status === 'APPLIED'
            ? operation.snapshotConfirmed
              ? 'offline_applied'
              : 'offline_time_saved_refresh_pending'
            : operation.status === 'SENDING'
            ? 'offline_sending'
            : operation.status === 'UNKNOWN'
            ? 'offline_unknown'
            : operation.status === 'CONFLICT'
            ? 'offline_conflict'
            : operation.status === 'REJECTED'
            ? 'offline_rejected'
            : operation.request.type === 'STOP_TIMER'
            ? 'offline_time_stop_pending'
            : 'offline_queued';
        const terminal = ['CONFLICT', 'REJECTED'].includes(operation.status);
        const examined = review?.operationId === operation.id ? review : null;
        return (
          <View
            key={operation.id}
            style={{ borderTopWidth: 0.5, paddingVertical: 10, gap: 6 }}
          >
            <Text>
              {new Date(time.startedAt).toLocaleString()}
              {time.endedAt
                ? ` — ${new Date(time.endedAt).toLocaleString()}`
                : ''}
            </Text>
            <Text>{t(status)}</Text>
            {(operation.blockedReason ||
              (terminal && operation.receipt?.code)) && (
              <Text>
                {t(
                  offlineErrorKey({
                    code: operation.blockedReason || operation.receipt?.code
                  })
                )}
              </Text>
            )}
            {operation.canReviewTime &&
              !(
                view.activePeriod?.periodId === time.periodId &&
                !view.activePeriod.endedAt
              ) && (
                <Button
                  disabled={!offline.networkAvailable || busy}
                  onPress={() => void examine(operation)}
                >
                  {t('offline_time_review')}
                </Button>
              )}
            {examined && (
              <View style={{ gap: 8 }}>
                <Text>
                  {t('status')}: {t(examined.status)}
                </Text>
                <Text>
                  {t('labor_server_duration')}:{' '}
                  {examined.labor
                    ? `${examined.labor.duration / 60} ${t('minutes')}`
                    : '—'}
                </Text>
                {examined.labor && (
                  <Text>
                    {t(
                      examined.labor.status === 'RUNNING'
                        ? 'timer_running'
                        : 'offline_time_stopped'
                    )}{' '}
                    · {new Date(examined.labor.startedAt).toLocaleString()}
                  </Text>
                )}
                {operation.request.type === 'STOP_TIMER' && time.endedAt && (
                  <View
                    style={{
                      flexDirection: 'row',
                      alignItems: 'center',
                      flexWrap: 'wrap'
                    }}
                  >
                    <Switch
                      value={preserveStatus}
                      onValueChange={setPreserveStatus}
                      disabled={busy}
                    />
                    <Text>{t('offline_time_preserve_status')}</Text>
                  </View>
                )}
                <Button
                  disabled={!offline.writable || busy}
                  onPress={() =>
                    void run(async () => {
                      const draft = await offline.reviewTimeOperation(
                        operation.id,
                        examined.labor?.version ?? null,
                        preserveStatus,
                        examined.statusRevision
                      );
                      assertSessionGeneration(generation);
                      if (!mounted.current) return;
                      setReview(null);
                      if (draft.kind === 'MANUAL_TIME')
                        navigation.navigate('AddAdditionalTime', {
                          workOrderId
                        });
                    })
                  }
                >
                  {t('labor_review_confirm')}
                </Button>
                {examined.labor && offline.networkAvailable && (
                  <Button
                    onPress={() =>
                      navigation.navigate('EditAdditionalTime', {
                        workOrderId,
                        labor: examined.labor
                      })
                    }
                  >
                    {t('offline_time_correct_connected')}
                  </Button>
                )}
              </View>
            )}
          </View>
        );
      })}
    </View>
  );
}

function TimeReviewDraft({
  draft,
  canEditStart
}: {
  draft: TimerDraft;
  canEditStart: boolean;
}) {
  const offline = useOffline();
  const { t } = useTranslation();
  const [dates, setDates] = useState({
    startedAt: draft.startedAt,
    endedAt: draft.endedAt
  });
  const [busy, setBusy] = useState(false);
  const revision = useRef(draft.revision);
  const busyRef = useRef(false);
  const generation = useRef(sessionGeneration());
  const mounted = useRef(true);
  useEffect(
    () => () => {
      mounted.current = false;
    },
    []
  );
  const current = () =>
    mounted.current && generation.current === sessionGeneration();
  const change = (patch: Partial<typeof dates>) => {
    if (!offline.writable || busyRef.current) return;
    setDates((previous) => ({ ...previous, ...patch }));
  };
  const valid =
    Number.isFinite(Date.parse(dates.startedAt)) &&
    Date.parse(dates.endedAt) > Date.parse(dates.startedAt);
  return (
    <View style={{ gap: 10 }}>
      <View
        pointerEvents={offline.writable && !busy ? 'auto' : 'none'}
        style={{ gap: 10 }}
      >
        <View pointerEvents={canEditStart ? 'auto' : 'none'}>
          <CustomDateTimePicker
            label={t('work_started_at')}
            value={new Date(dates.startedAt)}
            onChange={(date) => change({ startedAt: date.toISOString() })}
          />
        </View>
        <CustomDateTimePicker
          label={t('end')}
          value={new Date(dates.endedAt)}
          onChange={(date) => change({ endedAt: date.toISOString() })}
        />
      </View>
      <Button
        disabled={!offline.writable || busy || !valid}
        loading={busy}
        onPress={async () => {
          if (busyRef.current) return;
          busyRef.current = true;
          setBusy(true);
          try {
            const saved = await offline.saveTimerReview(
              draft.workOrderId,
              dates,
              revision.current
            );
            revision.current = saved.revision;
            assertSessionGeneration(generation.current);
            await offline.submitReviewedTimer(
              draft.workOrderId,
              revision.current
            );
          } catch (error) {
            if (current()) offline.reportError(error);
          } finally {
            if (current()) {
              busyRef.current = false;
              setBusy(false);
            }
          }
        }}
      >
        {t('offline_time_save')}
      </Button>
    </View>
  );
}
