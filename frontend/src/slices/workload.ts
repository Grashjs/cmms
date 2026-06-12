import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import api from '../utils/api';
import type {
  UnscheduledWorkOrdersDTO,
  WorkloadOverviewDTO,
  WorkloadWorkOrderDTO
} from '../models/owns/workload';
import { updateWorkOrderInContent } from './workOrder';

const basePath = 'workload';

interface WorkloadState {
  overview: WorkloadOverviewDTO | null;
  unscheduled: UnscheduledWorkOrdersDTO | null;
  loadingOverview: boolean;
  loadingUnscheduled: boolean;
}

const initialState: WorkloadState = {
  overview: null,
  unscheduled: null,
  loadingOverview: false,
  loadingUnscheduled: false
};

const slice = createSlice({
  name: 'workload',
  initialState,
  reducers: {
    getOverview(
      state: WorkloadState,
      action: PayloadAction<{ overview: WorkloadOverviewDTO }>
    ) {
      const { overview } = action.payload;
      state.overview = overview;
    },
    getUnscheduled(
      state: WorkloadState,
      action: PayloadAction<{ unscheduled: UnscheduledWorkOrdersDTO }>
    ) {
      const { unscheduled } = action.payload;
      state.unscheduled = unscheduled;
    },
    removeFromUnscheduled(
      state: WorkloadState,
      action: PayloadAction<{ id: number }>
    ) {
      const { id } = action.payload;
      if (state.unscheduled) {
        const wo = state.unscheduled.workOrders.find((w) => w.id === id);
        if (wo) {
          if (state.unscheduled.statusCounts[wo.status] > 1) {
            state.unscheduled.statusCounts[wo.status]--;
          } else {
            delete state.unscheduled.statusCounts[wo.status];
          }
        }
        state.unscheduled.workOrders = state.unscheduled.workOrders.filter(
          (w) => w.id !== id
        );
      }
    },
    addToUnscheduled(
      state: WorkloadState,
      action: PayloadAction<{ workOrder: WorkloadWorkOrderDTO }>
    ) {
      const { workOrder } = action.payload;
      if (state.unscheduled) {
        state.unscheduled.workOrders.push(workOrder);
        state.unscheduled.statusCounts[workOrder.status] =
          (state.unscheduled.statusCounts[workOrder.status] ?? 0) + 1;
      }
    },
    setLoadingOverview(
      state: WorkloadState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingOverview = loading;
    },
    setLoadingUnscheduled(
      state: WorkloadState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingUnscheduled = loading;
    },
    addWoToUserDay(
      state: WorkloadState,
      action: PayloadAction<{
        workOrder: WorkloadWorkOrderDTO;
        userId: number;
        date: string;
      }>
    ) {
      const { workOrder, userId, date } = action.payload;
      if (!state.overview) return;
      const day = state.overview.days.find((d) => d.date === date);
      if (!day) return;
      let userDay = day.users.find((u) => u.userId === userId);
      if (!userDay) {
        userDay = {
          userId,
          fullName: '',
          capacityMinutes: 480,
          allocatedMinutes: 0,
          workOrders: []
        };
        day.users.push(userDay);
      }
      userDay.workOrders.push(workOrder);
      userDay.allocatedMinutes += workOrder.estimatedDuration * 60;
      day.teamAllocatedMinutes += workOrder.estimatedDuration * 60;
      state.overview.teamAllocatedMinutes += workOrder.estimatedDuration * 60;
    },
    removeWoFromOverview(
      state: WorkloadState,
      action: PayloadAction<{ workOrderId: number }>
    ) {
      const { workOrderId } = action.payload;
      if (!state.overview) return;
      for (const day of state.overview.days) {
        for (const userDay of day.users) {
          const idx = userDay.workOrders.findIndex((w) => w.id === workOrderId);
          if (idx !== -1) {
            const removed = userDay.workOrders.splice(idx, 1)[0];
            userDay.allocatedMinutes -= removed.estimatedDuration * 60;
            day.teamAllocatedMinutes -= removed.estimatedDuration * 60;
            state.overview.teamAllocatedMinutes -=
              removed.estimatedDuration * 60;
            return;
          }
        }
      }
    }
  }
});

export const reducer = slice.reducer;

export const getOverview =
  (startDate: string, endDate: string, userIds?: number[]): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingOverview({ loading: true }));
    try {
      const params = new URLSearchParams({ startDate, endDate });
      if (userIds?.length) {
        params.append('userIds', userIds.join(','));
      }
      const overview = await api.get<WorkloadOverviewDTO>(
        `${basePath}/overview?${params}`
      );
      dispatch(slice.actions.getOverview({ overview }));
    } finally {
      dispatch(slice.actions.setLoadingOverview({ loading: false }));
    }
  };

export const getUnscheduled =
  (statuses?: string[]): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingUnscheduled({ loading: true }));
    try {
      const params = new URLSearchParams();
      if (statuses?.length) {
        params.append('statuses', statuses.join(','));
      }
      const query = params.toString();
      const unscheduled = await api.get<UnscheduledWorkOrdersDTO>(
        `${basePath}/unscheduled${query ? `?${query}` : ''}`
      );
      dispatch(slice.actions.getUnscheduled({ unscheduled }));
    } finally {
      dispatch(slice.actions.setLoadingUnscheduled({ loading: false }));
    }
  };

interface ScheduleWorkOrderResponse {
  userId: number;
  userLastName: string;
  userFirstName: string;
  estimatedStartDate: string;
}
export const scheduleWorkOrder =
  (
    workOrderId: number,
    dto: {
      localDate: string | null;
      estimatedDuration: number | null;
      primaryUserId: number | null;
    }
  ): AppThunk =>
  async (dispatch, getState) => {
    const response = await api.patch<ScheduleWorkOrderResponse>(
      `${basePath}/work-orders/${workOrderId}/schedule`,
      dto
    );
    const woInContent = getState().workOrders.workOrders.content.find(
      (w) => w.id === workOrderId
    );
    const dayWos = getState().workload.overview?.days ?? [];

    const woInDays = dayWos
      .flatMap((day) => day.users)
      .flatMap((user) => user.workOrders)
      .find((workOrder) => workOrder.id === workOrderId);
    if (woInContent) {
      dispatch(
        updateWorkOrderInContent({
          workOrder: {
            ...woInContent,
            estimatedStartDate: response.estimatedStartDate,
            primaryUser: {
              id: response.userId,
              firstName: response.userFirstName,
              lastName: response.userLastName,
              image: null
            }
          }
        })
      );
    }
    dispatch(slice.actions.removeFromUnscheduled({ id: workOrderId }));
    dispatch(slice.actions.removeWoFromOverview({ workOrderId }));
    const woInUnscheduled = getState().workload.unscheduled?.workOrders.find(
      (w) => w.id === workOrderId
    );

    const workOrder = {
      ...woInUnscheduled,
      ...woInContent,
      ...woInDays
    };
    dispatch(
      slice.actions.addWoToUserDay({
        workOrder: {
          id: workOrderId,
          customId: workOrder?.customId ?? '',
          title: workOrder?.title ?? '',
          status: workOrder?.status ?? '',
          estimatedDuration:
            workOrder?.estimatedDuration ??
            woInDays?.estimatedDuration ??
            dto.estimatedDuration ??
            0,
          estimatedStartDate: response.estimatedStartDate,
          dueDate: workOrder?.dueDate ?? ''
        },
        userId: dto.primaryUserId,
        date: dto.localDate.substring(0, 10)
      })
    );
  };

export const unscheduleWorkOrder =
  (workOrderId: number, workOrder: WorkloadWorkOrderDTO): AppThunk =>
  async (dispatch, getState) => {
    const response = await api.patch<ScheduleWorkOrderResponse>(
      `${basePath}/work-orders/${workOrderId}/schedule`,
      {
        localDate: null,
        primaryUserId: null
      }
    );
    const wo = getState().workOrders.workOrders.content.find(
      (w) => w.id === workOrderId
    );
    if (wo) {
      dispatch(
        updateWorkOrderInContent({
          workOrder: {
            ...wo,
            estimatedStartDate: response.estimatedStartDate,
            primaryUser: {
              id: response.userId,
              firstName: response.userFirstName,
              lastName: response.userLastName,
              image: null
            }
          }
        })
      );
    }
    dispatch(slice.actions.addToUnscheduled({ workOrder }));
    dispatch(slice.actions.removeWoFromOverview({ workOrderId }));
  };

export const { removeFromUnscheduled, addToUnscheduled } = slice.actions;

export default slice;
