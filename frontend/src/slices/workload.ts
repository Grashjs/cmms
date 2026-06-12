import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import api from '../utils/api';
import type {
  WorkloadOverviewDTO,
  UnscheduledWorkOrdersDTO,
  WorkloadWorkOrderDTO
} from '../models/owns/workload';

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

export const scheduleWorkOrder =
  (
    workOrderId: number,
    dto: {
      localDate: string | null;
      estimatedDuration: number | null;
      primaryUserId: number | null;
    }
  ): AppThunk =>
  async (dispatch) => {
    await api.patch(`${basePath}/work-orders/${workOrderId}/schedule`, dto);
    dispatch(slice.actions.removeFromUnscheduled({ id: workOrderId }));
  };

export const unscheduleWorkOrder =
  (workOrderId: number, workOrder: WorkloadWorkOrderDTO): AppThunk =>
  async (dispatch) => {
    await api.patch(`${basePath}/work-orders/${workOrderId}/schedule`, {
      localDate: null,
      primaryUserId: null
    });
    dispatch(slice.actions.addToUnscheduled({ workOrder }));
  };

export const { removeFromUnscheduled, addToUnscheduled } = slice.actions;

export default slice;
