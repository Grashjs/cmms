import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  FormControl,
  IconButton,
  InputLabel,
  LinearProgress,
  MenuItem,
  OutlinedInput,
  Select,
  Stack,
  Tooltip,
  Typography
} from '@mui/material';
import {
  addDays,
  addWeeks,
  endOfWeek,
  format,
  startOfWeek,
  subWeeks
} from 'date-fns';
import { useTranslation } from 'react-i18next';
import { useDispatch, useSelector } from 'src/store';
import {
  getOverview,
  getUnscheduled,
  scheduleWorkOrder,
  unscheduleWorkOrder,
  removeFromUnscheduled,
  addToUnscheduled
} from 'src/slices/workload';
import type {
  WorkloadDayDTO,
  WorkloadOverviewDTO,
  WorkloadUserDayDTO,
  WorkloadWorkOrderDTO
} from 'src/models/owns/workload';
import ArrowForwardTwoToneIcon from '@mui/icons-material/ArrowForwardTwoTone';
import ArrowBackTwoToneIcon from '@mui/icons-material/ArrowBackTwoTone';
import TodayTwoToneIcon from '@mui/icons-material/TodayTwoTone';
import useDateLocale from '../../../../hooks/useDateLocale';

interface WorkloadViewProps {
  handleOpenDetails: (id: number, type: string) => void;
}

const GRID_LABEL_WIDTH = 150;

const barColor = (percent: number) =>
  percent > 100 ? 'error' : percent > 80 ? 'warning' : 'success';

const pctTextColor = (percent: number) =>
  percent > 100 ? 'error' : percent > 80 ? 'warning' : 'success';

function WorkloadView({ handleOpenDetails }: WorkloadViewProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const dateLocale = useDateLocale();
  const { overview, unscheduled, loadingOverview, loadingUnscheduled } =
    useSelector((state) => state.workload);
  const allUsers = useSelector((state) => state.users.usersMini);
  const [selectedUserIds, setSelectedUserIds] = useState<number[]>([]);
  const [currentDate, setCurrentDate] = useState<Date>(new Date());
  const [expandedStatus, setExpandedStatus] = useState<string | null>(null);
  const [draggedWO, setDraggedWO] = useState<WorkloadWorkOrderDTO | null>(null);

  const weekStart = useMemo(
    () => startOfWeek(currentDate, { weekStartsOn: 1 }),
    [currentDate]
  );
  const weekEnd = useMemo(
    () => endOfWeek(currentDate, { weekStartsOn: 1 }),
    [currentDate]
  );

  const formatDate = useCallback(
    (date: Date) => format(date, 'yyyy-MM-dd'),
    []
  );

  const weekDays = useMemo(
    () => Array.from({ length: 7 }, (_, i) => addDays(weekStart, i)),
    [weekStart]
  );

  const loadOverview = useCallback(() => {
    dispatch(
      getOverview(
        formatDate(weekStart),
        formatDate(weekEnd),
        selectedUserIds.length ? selectedUserIds : undefined
      )
    );
  }, [weekStart, weekEnd, selectedUserIds, dispatch, formatDate]);

  useEffect(() => {
    loadOverview();
  }, [loadOverview]);

  useEffect(() => {
    dispatch(getUnscheduled());
  }, [dispatch]);

  const handlePrevWeek = () => setCurrentDate((d) => subWeeks(d, 1));
  const handleNextWeek = () => setCurrentDate((d) => addWeeks(d, 1));
  const handleThisWeek = () => setCurrentDate(new Date());

  const getDayData = (
    overviewData: WorkloadOverviewDTO | null,
    date: Date
  ): WorkloadDayDTO | undefined => {
    const dateStr = formatDate(date);
    return overviewData?.days?.find((d) => d.date === dateStr);
  };

  const allUniqueUsers = useMemo(() => {
    if (!overview?.days) return [];
    const userMap = new Map<number, string>();
    for (const day of overview.days) {
      for (const u of day.users) {
        if (!userMap.has(u.userId)) {
          userMap.set(u.userId, u.fullName);
        }
      }
    }
    return Array.from(userMap.entries()).map(([userId, fullName]) => ({
      userId,
      fullName
    }));
  }, [overview]);

  const pct = (allocated: number, capacity: number) =>
    capacity > 0 ? Math.round((allocated / capacity) * 100) : 0;

  const renderCapacityBar = (allocated: number, capacity: number) => {
    const percent = pct(allocated, capacity);
    return (
      <Tooltip title={`${Math.round(allocated)}/${capacity} ${t('minutes')}`}>
        <LinearProgress
          variant="determinate"
          value={Math.min(percent, 100)}
          color={barColor(percent)}
          sx={{ height: 10, borderRadius: 1, width: '100%' }}
        />
      </Tooltip>
    );
  };

  const renderCapacityCell = (
    allocated: number,
    capacity: number,
    label?: string
  ) => {
    const percent = pct(allocated, capacity);
    return (
      <Box sx={{ p: 0.5 }}>
        {label && (
          <Typography variant="body2" noWrap sx={{ mb: 0.5 }}>
            {label}
          </Typography>
        )}
        {renderCapacityBar(allocated, capacity)}
        <Typography variant="caption" color={pctTextColor(percent)}>
          {percent}%
        </Typography>
      </Box>
    );
  };

  const handleSchedule = async (
    workOrder: WorkloadWorkOrderDTO,
    date: Date,
    userId: number
  ) => {
    const dateStr = format(date, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    await dispatch(
      scheduleWorkOrder(workOrder.id, {
        estimatedStartDate: dateStr,
        estimatedDuration: workOrder.estimatedDuration || null,
        primaryUserId: userId
      })
    );
    loadOverview();
  };

  const handleUnschedule = async (workOrder: WorkloadWorkOrderDTO) => {
    await dispatch(unscheduleWorkOrder(workOrder.id, workOrder));
    loadOverview();
  };

  const onDragStart = (
    e: React.DragEvent,
    wo: WorkloadWorkOrderDTO,
    source: 'unscheduled' | 'user'
  ) => {
    e.dataTransfer.setData('text/plain', JSON.stringify({ id: wo.id, source }));
    e.dataTransfer.effectAllowed = 'move';
    setDraggedWO(wo);
  };

  const onDragOverUser = (e: React.DragEvent) => {
    if (e.dataTransfer.types.includes('text/plain')) {
      e.preventDefault();
      e.dataTransfer.dropEffect = 'move';
    }
  };

  const onDropOnUser = (
    e: React.DragEvent,
    targetDate: Date,
    targetUserId: number
  ) => {
    e.preventDefault();
    setDraggedWO(null);
    try {
      const data = JSON.parse(e.dataTransfer.getData('text/plain'));
      if (data.source === 'unscheduled') {
        const wo = unscheduled?.workOrders.find((w) => w.id === data.id);
        if (wo) {
          handleSchedule(wo, targetDate, targetUserId);
        }
      }
    } catch {}
  };

  const onDragOverUnscheduled = (e: React.DragEvent) => {
    if (e.dataTransfer.types.includes('text/plain')) {
      e.preventDefault();
      e.dataTransfer.dropEffect = 'move';
    }
  };

  const onDropOnUnscheduled = (e: React.DragEvent) => {
    e.preventDefault();
    setDraggedWO(null);
    try {
      const data = JSON.parse(e.dataTransfer.getData('text/plain'));
      if (data.source === 'user') {
        const allUserDays = overview?.days?.flatMap((d) => d.users) ?? [];
        for (const ud of allUserDays) {
          const wo = ud.workOrders.find((w) => w.id === data.id);
          if (wo) {
            handleUnschedule(wo);
            return;
          }
        }
      }
    } catch {}
  };

  const renderGridHeader = (title: string) => (
    <>
      <Box
        sx={{
          p: 1,
          borderBottom: 1,
          borderColor: 'divider',
          fontWeight: 'bold'
        }}
      >
        <Typography variant="body2" fontWeight="bold">
          {title}
        </Typography>
      </Box>
      {weekDays.map((day) => (
        <Box
          key={day.toISOString()}
          sx={{
            p: 1,
            borderBottom: 1,
            borderLeft: 1,
            borderColor: 'divider',
            textAlign: 'center',
            fontWeight: 'bold'
          }}
        >
          <Typography variant="caption">
            {format(day, 'EEE', { locale: dateLocale })}
          </Typography>
          <Typography variant="caption" display="block" color="text.secondary">
            {format(day, 'M/d')}
          </Typography>
        </Box>
      ))}
    </>
  );

  const renderWOChip = (
    wo: WorkloadWorkOrderDTO,
    source: 'unscheduled' | 'user'
  ) => (
    <Chip
      key={wo.id}
      label={`#${wo.customId ?? ''} ${wo.title}`}
      size="small"
      variant="outlined"
      draggable
      onDragStart={(e) => onDragStart(e, wo, source)}
      onClick={() => handleOpenDetails(wo.id, 'WORK_ORDER')}
      sx={{ mt: 0.3, mr: 0.3, maxWidth: 130, fontSize: 10 }}
    />
  );

  const overdueWOs = useMemo(() => {
    if (!unscheduled) return [];
    const now = new Date();
    return unscheduled.workOrders.filter(
      (wo) => wo.dueDate && new Date(wo.dueDate) < now
    );
  }, [unscheduled]);

  const dueSoonWOs = useMemo(() => {
    if (!unscheduled) return [];
    const now = new Date();
    const soon = new Date(now.getTime() + 2 * 24 * 3600 * 1000);
    return unscheduled.workOrders.filter(
      (wo) =>
        wo.dueDate && new Date(wo.dueDate) >= now && new Date(wo.dueDate) < soon
    );
  }, [unscheduled]);

  return (
    <Box sx={{ p: 2 }}>
      <Stack
        direction="row"
        alignItems="center"
        justifyContent="space-between"
        sx={{ mb: 2 }}
      >
        <Stack direction="row" alignItems="center" spacing={1}>
          <IconButton onClick={handlePrevWeek}>
            <ArrowBackTwoToneIcon />
          </IconButton>
          <Button
            size="small"
            variant="outlined"
            startIcon={<TodayTwoToneIcon />}
            onClick={handleThisWeek}
          >
            {t('this_week')}
          </Button>
          <IconButton onClick={handleNextWeek}>
            <ArrowForwardTwoToneIcon />
          </IconButton>
          <Typography variant="h6" sx={{ ml: 1 }}>
            {format(weekStart, 'MMM d', { locale: dateLocale })} -{' '}
            {format(weekEnd, 'MMM d, yyyy', { locale: dateLocale })}
          </Typography>
        </Stack>
        <FormControl size="small" sx={{ minWidth: 240 }}>
          <InputLabel>{t('users')}</InputLabel>
          <Select
            multiple
            value={selectedUserIds}
            onChange={(e) => setSelectedUserIds(e.target.value as number[])}
            input={<OutlinedInput label={t('users')} />}
            renderValue={(selected) => {
              const names = selected
                .map(
                  (id) =>
                    allUsers.find((u) => u.id === id)?.firstName ||
                    allUsers.find((u) => u.id === id)?.lastName ||
                    String(id)
                )
                .join(', ');
              return names || t('all_users');
            }}
          >
            {allUsers.map((user) => (
              <MenuItem key={user.id} value={user.id}>
                {user.firstName} {user.lastName}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </Stack>

      <Card sx={{ p: 2, mb: 2 }}>
        {loadingOverview ? (
          <CircularProgress size={24} />
        ) : (
          <Box sx={{ overflowX: 'auto' }}>
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: `${GRID_LABEL_WIDTH}px repeat(7, 1fr)`,
                width: '100%'
              }}
            >
              {renderGridHeader(t('total_resource_capacity'))}
              {renderCapacityCell(
                overview?.teamAllocatedMinutes ?? 0,
                overview?.teamCapacityMinutes ?? 0
              )}
              {overview?.days?.map((day) =>
                renderCapacityCell(
                  day.teamAllocatedMinutes,
                  day.teamCapacityMinutes
                )
              )}
            </Box>
          </Box>
        )}
      </Card>

      <Card
        sx={{ p: 2, mb: 2 }}
        onDragOver={onDragOverUnscheduled}
        onDrop={onDropOnUnscheduled}
      >
        <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
          {t('unscheduled_work_orders')}
        </Typography>
        {loadingUnscheduled ? (
          <CircularProgress size={24} />
        ) : (
          <Stack direction="row" spacing={1} flexWrap="wrap">
            {unscheduled?.statusCounts &&
              Object.entries(unscheduled.statusCounts).map(
                ([status, count]) => (
                  <Chip
                    key={status}
                    label={`${t(status)}: ${count}`}
                    color="primary"
                    variant="outlined"
                    onClick={() =>
                      setExpandedStatus(
                        expandedStatus === status ? null : status
                      )
                    }
                  />
                )
              )}
            <Chip
              label={`${t('overdue')}: ${overdueWOs.length}`}
              color="error"
              variant={expandedStatus === '__overdue__' ? 'filled' : 'outlined'}
              onClick={() =>
                setExpandedStatus(
                  expandedStatus === '__overdue__' ? null : '__overdue__'
                )
              }
            />
            <Chip
              label={`${t('due_soon')}: ${dueSoonWOs.length}`}
              color="warning"
              variant={
                expandedStatus === '__due_soon__' ? 'filled' : 'outlined'
              }
              onClick={() =>
                setExpandedStatus(
                  expandedStatus === '__due_soon__' ? null : '__due_soon__'
                )
              }
            />
          </Stack>
        )}
        {expandedStatus && unscheduled && (
          <Box sx={{ mt: 1 }}>
            {(expandedStatus === '__overdue__'
              ? overdueWOs
              : expandedStatus === '__due_soon__'
              ? dueSoonWOs
              : unscheduled.workOrders.filter(
                  (wo) => wo.status === expandedStatus
                )
            ).map((wo) => (
              <Box key={wo.id} sx={{ display: 'inline-block' }}>
                {renderWOChip(wo, 'unscheduled')}
              </Box>
            ))}
          </Box>
        )}
      </Card>

      <Card sx={{ p: 2 }}>
        <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
          {t('user_capacity')}
        </Typography>
        {loadingOverview ? (
          <CircularProgress size={24} />
        ) : (
          <Box sx={{ overflowX: 'auto' }}>
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: `${GRID_LABEL_WIDTH}px repeat(7, 1fr)`,
                width: '100%'
              }}
            >
              {renderGridHeader(t('team_member'))}
              {allUniqueUsers.length === 0 && overview && (
                <Box sx={{ gridColumn: '1 / -1', p: 2, textAlign: 'center' }}>
                  <Typography variant="body2" color="text.secondary">
                    {t('no_data')}
                  </Typography>
                </Box>
              )}
              {allUniqueUsers.map((userSummary) => (
                <>
                  <Box
                    key={`name-${userSummary.userId}`}
                    sx={{
                      p: 1,
                      borderBottom: 1,
                      borderColor: 'divider',
                      display: 'flex',
                      alignItems: 'center'
                    }}
                  >
                    <Typography variant="body2" noWrap>
                      {userSummary.fullName ||
                        allUsers.find((u) => u.id === userSummary.userId)
                          ?.firstName ||
                        ''}
                    </Typography>
                  </Box>
                  {weekDays.map((day) => {
                    const dayData = getDayData(overview, day);
                    const userDayData = dayData?.users?.find(
                      (u) => u.userId === userSummary.userId
                    );
                    return (
                      <Box
                        key={`${userSummary.userId}-${day.toISOString()}`}
                        onDragOver={onDragOverUser}
                        onDrop={(e) => onDropOnUser(e, day, userSummary.userId)}
                        sx={{
                          borderBottom: 1,
                          borderLeft: 1,
                          borderColor: 'divider',
                          p: 0.5,
                          minHeight: 60,
                          transition: 'background-color 0.2s',
                          '&:hover': draggedWO
                            ? { backgroundColor: 'action.hover' }
                            : {}
                        }}
                      >
                        {userDayData ? (
                          <Box>
                            {renderCapacityBar(
                              userDayData.allocatedMinutes,
                              userDayData.capacityMinutes
                            )}
                            <Typography
                              variant="caption"
                              color="text.secondary"
                            >
                              {Math.round(userDayData.allocatedMinutes)}/
                              {userDayData.capacityMinutes}min
                            </Typography>
                            {userDayData.workOrders.map((wo) =>
                              renderWOChip(wo, 'user')
                            )}
                          </Box>
                        ) : (
                          <Typography
                            variant="caption"
                            color="text.disabled"
                            sx={{ p: 0.5, display: 'block' }}
                          >
                            -
                          </Typography>
                        )}
                      </Box>
                    );
                  })}
                </>
              ))}
            </Box>
          </Box>
        )}
      </Card>
    </Box>
  );
}

export default WorkloadView;
