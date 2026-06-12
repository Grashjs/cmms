import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  IconButton,
  InputLabel,
  LinearProgress,
  MenuItem,
  OutlinedInput,
  Select,
  Stack,
  TextField,
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
  unscheduleWorkOrder
} from 'src/slices/workload';
import type {
  WorkloadDayDTO,
  WorkloadOverviewDTO,
  WorkloadUserDayDTO,
  WorkloadWorkOrderDTO
} from 'src/models/owns/workload';
import {
  DragDropContext,
  Draggable,
  Droppable,
  type DropResult
} from 'react-beautiful-dnd';
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

const userCellDroppableId = (userId: number, dateStr: string) =>
  `user-${userId}-${dateStr}`;

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
  const [durationModalOpen, setDurationModalOpen] = useState(false);
  const [durationHours, setDurationHours] = useState(0);
  const [durationMinutes, setDurationMinutes] = useState(0);
  const [pendingWorkOrder, setPendingWorkOrder] = useState<{
    id: number;
    dateStr: string;
    userId: number;
  } | null>(null);

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

  const renderCapacityBar = (
    allocated: number,
    capacity: number,
    disabled?: boolean
  ) => {
    const percent = pct(allocated, capacity);
    return (
      <Tooltip title={`${Math.round(allocated)}/${capacity} ${t('minutes')}`}>
        <LinearProgress
          variant="determinate"
          value={Math.min(percent, 100)}
          color={disabled ? 'inherit' : barColor(percent)}
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

  const findWOInUnscheduled = (id: number) =>
    unscheduled?.workOrders.find((w) => w.id === id) ?? null;

  const findWOInUserDays = (id: number): WorkloadWorkOrderDTO | null => {
    for (const day of overview?.days ?? []) {
      for (const u of day.users) {
        const found = u.workOrders.find((w) => w.id === id);
        if (found) return found;
      }
    }
    return null;
  };

  const findWOByDraggableId = (draggableId: string) => {
    const id = Number(draggableId.replace('wo-', ''));
    return findWOInUnscheduled(id) ?? findWOInUserDays(id);
  };

  const doSchedule = useCallback(
    (
      workOrderId: number,
      primaryUserId: number,
      dateStr: string,
      estimatedDuration?: number | null
    ) => {
      dispatch(
        scheduleWorkOrder(workOrderId, {
          localDate: dateStr,
          estimatedDuration: estimatedDuration ?? null,
          primaryUserId
        })
      ).then(() => loadOverview());
    },
    [dispatch, loadOverview]
  );

  const handleDragEnd = (result: DropResult) => {
    const { source, destination, draggableId } = result;
    if (!destination) return;
    if (
      source.droppableId === destination.droppableId &&
      source.index === destination.index
    )
      return;

    const wo = findWOByDraggableId(draggableId);
    if (!wo) return;

    const isUnscheduled = (id: string) => id === 'unscheduled';
    const parseDroppable = (id: string) =>
      /^user-(\d+)-(\d{4}-\d{2}-\d{2})$/.exec(id);

    if (
      isUnscheduled(source.droppableId) &&
      isUnscheduled(destination.droppableId)
    )
      return;

    if (
      !isUnscheduled(source.droppableId) &&
      isUnscheduled(destination.droppableId)
    ) {
      dispatch(unscheduleWorkOrder(wo.id, wo)).then(() => loadOverview());
      return;
    }

    const match = parseDroppable(destination.droppableId);
    if (!match) return;

    const userId = Number(match[1]);
    const dateStr = format(new Date(match[2]), "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    if (!wo.estimatedDuration) {
      setPendingWorkOrder({ id: wo.id, dateStr, userId });
      setDurationHours(0);
      setDurationMinutes(0);
      setDurationModalOpen(true);
      return;
    }

    doSchedule(wo.id, userId, dateStr);
  };

  const handleDurationSubmit = () => {
    if (!pendingWorkOrder) return;
    const totalMinutes = durationHours * 60 + durationMinutes;
    if (totalMinutes <= 0) return;
    doSchedule(
      pendingWorkOrder.id,
      pendingWorkOrder.userId,
      pendingWorkOrder.dateStr,
      totalMinutes
    );
    setDurationModalOpen(false);
    setPendingWorkOrder(null);
  };

  const handleDurationCancel = () => {
    setDurationModalOpen(false);
    setPendingWorkOrder(null);
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

  const displayWOs = (status: string) => {
    if (status === '__overdue__') return overdueWOs;
    if (status === '__due_soon__') return dueSoonWOs;
    return unscheduled?.workOrders.filter((wo) => wo.status === status) ?? [];
  };

  return (
    <DragDropContext onDragEnd={handleDragEnd}>
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

        <Droppable droppableId="unscheduled">
          {(provided, snapshot) => (
            <Card
              ref={provided.innerRef}
              {...provided.droppableProps}
              sx={{
                p: 2,
                mb: 2,
                bgcolor: snapshot.isDraggingOver ? 'action.hover' : undefined
              }}
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
                          variant={
                            expandedStatus === status ? 'filled' : 'outlined'
                          }
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
                    variant={
                      expandedStatus === '__overdue__' ? 'filled' : 'outlined'
                    }
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
                        expandedStatus === '__due_soon__'
                          ? null
                          : '__due_soon__'
                      )
                    }
                  />
                </Stack>
              )}
              {expandedStatus && (
                <Stack
                  direction="row"
                  spacing={0.5}
                  sx={{ mt: 1, flexWrap: 'wrap' }}
                >
                  {displayWOs(expandedStatus).map((wo, index) => (
                    <Draggable
                      key={wo.id}
                      draggableId={`wo-${wo.id}`}
                      index={index}
                    >
                      {(provided, snapshot) => {
                        const isOverdue = wo.dueDate && new Date(wo.dueDate) < new Date();
                        return (
                          <Chip
                            ref={provided.innerRef}
                            {...provided.draggableProps}
                            {...provided.dragHandleProps}
                            label={wo.title}
                            size="small"
                            variant="outlined"
                            color={isOverdue ? 'error' : 'default'}
                            onClick={() => handleOpenDetails(wo.id, 'WORK_ORDER')}
                            sx={{
                              maxWidth: 250,
                              mb: 0.5,
                              borderRadius: '4px',
                              ...(snapshot.isDragging ? { boxShadow: 3 } : {})
                            }}
                          />
                        );
                      }}
                    </Draggable>
                  ))}
                </Stack>
              )}
              {provided.placeholder}
            </Card>
          )}
        </Droppable>

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
                      const droppableId = userCellDroppableId(
                        userSummary.userId,
                        formatDate(day)
                      );
                      return (
                        <Droppable key={droppableId} droppableId={droppableId}>
                          {(provided, snapshot) => (
                            <Box
                              ref={provided.innerRef}
                              {...provided.droppableProps}
                              sx={{
                                borderBottom: 1,
                                borderLeft: 1,
                                borderColor: 'divider',
                                p: 0.5,
                                minHeight: 60,
                                bgcolor: snapshot.isDraggingOver
                                  ? 'action.selected'
                                  : undefined
                              }}
                            >
                              {userDayData ? (
                                <Box>
                                  {renderCapacityBar(
                                    userDayData.allocatedMinutes,
                                    userDayData.capacityMinutes,
                                    userDayData.capacityMinutes === 0
                                  )}
                                  <Typography
                                    variant="caption"
                                    color="text.secondary"
                                  >
                                    {(() => {
                                      const rem = Math.max(0, userDayData.capacityMinutes - userDayData.allocatedMinutes);
                                      const h = Math.floor(rem / 60);
                                      const m = Math.round(rem % 60);
                                      return `${h}H${String(m).padStart(2, '0')} ${t('left')}`;
                                    })()}
                                  </Typography>
                                  {userDayData.workOrders.map((wo, index) => (
                                    <Draggable
                                      key={wo.id}
                                      draggableId={`wo-${wo.id}`}
                                      index={index}
                                    >
                                      {(provided, snapshot) => {
                                        const isOverdue = wo.dueDate && new Date(wo.dueDate) < new Date();
                                        return (
                                          <Chip
                                            ref={provided.innerRef}
                                            {...provided.draggableProps}
                                            {...provided.dragHandleProps}
                                            label={wo.title}
                                            size="small"
                                            variant="outlined"
                                            color={isOverdue ? 'error' : 'default'}
                                            onClick={() =>
                                              handleOpenDetails(
                                                wo.id,
                                                'WORK_ORDER'
                                              )
                                            }
                                            sx={{
                                              mt: 0.3,
                                              mr: 0.3,
                                              maxWidth: 110,
                                              fontSize: 10,
                                              borderRadius: '4px',
                                              ...(snapshot.isDragging
                                                ? { boxShadow: 3 }
                                                : {})
                                            }}
                                          />
                                        );
                                      }}
                                    </Draggable>
                                  ))}
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
                              {provided.placeholder}
                            </Box>
                          )}
                        </Droppable>
                      );
                    })}
                  </>
                ))}
              </Box>
            </Box>
          )}
        </Card>
      </Box>
    </DragDropContext>

      <Dialog open={durationModalOpen} onClose={handleDurationCancel} maxWidth="xs" fullWidth>
        <DialogTitle>{t('set_estimated_duration')}</DialogTitle>
        <DialogContent>
          <Stack direction="row" spacing={2} sx={{ mt: 1 }}>
            <TextField
              label={t('hours')}
              type="number"
              value={durationHours}
              onChange={(e) => setDurationHours(Math.max(0, Number(e.target.value)))}
              inputProps={{ min: 0 }}
              fullWidth
            />
            <TextField
              label={t('minutes')}
              type="number"
              value={durationMinutes}
              onChange={(e) => setDurationMinutes(Math.max(0, Math.min(59, Number(e.target.value))))}
              inputProps={{ min: 0, max: 59 }}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleDurationCancel}>{t('cancel')}</Button>
          <Button
            variant="contained"
            onClick={handleDurationSubmit}
            disabled={durationHours === 0 && durationMinutes === 0}
          >
            {t('schedule')}
          </Button>
        </DialogActions>
      </Dialog>
  );
}

export default WorkloadView;
