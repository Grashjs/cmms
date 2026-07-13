import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import WorkOrder from '../models/owns/workOrder';
import api from '../utils/api';
import { Task } from '../models/owns/tasks';
import { getInitialPage, Page, SearchCriteria } from '../models/owns/page';
import { WorkOrderBase, WorkOrderBaseMiniDTO } from 'src/models/owns/workOrderBase';
import PreventiveMaintenance from 'src/models/owns/preventiveMaintenance';
import { revertAll } from 'src/utils/redux';
import File from '../models/owns/file';

const basePath = 'work-orders';

export interface CalendarEvent<T extends WorkOrderBase> {
  type: string;
  date: Date;
  event: T;
}

interface WorkOrderState {
  workOrders: Page<WorkOrder>;
  workOrdersMini: Page<WorkOrderBaseMiniDTO>;
  workOrdersByLocation: { [key: number]: WorkOrder[] };
  workOrdersByPart: { [key: number]: WorkOrder[] };
  singleWorkOrder: WorkOrder;
  urgentCount: number;
  loadingGet: boolean;
  calendar: {
    events: CalendarEvent<WorkOrder | PreventiveMaintenance>[];
  };
}

const initialState: WorkOrderState = {
  workOrders: getInitialPage<WorkOrder>(),
  workOrdersByLocation: {},
  workOrdersByPart: {},
  singleWorkOrder: null,
  urgentCount: 0,
  loadingGet: false,
  workOrdersMini: getInitialPage<WorkOrderBaseMiniDTO>(),
  calendar: {
    events: []
  }
};

const slice = createSlice({
  name: 'workOrders',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getWorkOrders(
      state: WorkOrderState,
      action: PayloadAction<{ workOrders: Page<WorkOrder> }>
    ) {
      const { workOrders } = action.payload;
      state.workOrders = workOrders;
    },
    getWorkOrdersMini(
      state: WorkOrderState,
      action: PayloadAction<{ workOrders: Page<WorkOrderBaseMiniDTO> }>
    ) {
      const { workOrders } = action.payload;
      state.workOrdersMini = workOrders;
    },
    getSingleWorkOrder(
      state: WorkOrderState,
      action: PayloadAction<{ workOrder: WorkOrder }>
    ) {
      const { workOrder } = action.payload;
      state.singleWorkOrder = workOrder;
    },
    addWorkOrder(
      state: WorkOrderState,
      action: PayloadAction<{ workOrder: WorkOrder }>
    ) {
      const { workOrder } = action.payload;
      state.workOrders.content = [workOrder, ...state.workOrders.content];
    },
    editWorkOrder(
      state: WorkOrderState,
      action: PayloadAction<{ workOrder: WorkOrder }>
    ) {
      const { workOrder } = action.payload;
      const inContent = state.workOrders.content.some(
        (workOrder1) => workOrder1.id === workOrder.id
      );
      if (inContent) {
        state.workOrders.content = state.workOrders.content.map(
          (workOrder1) => {
            if (workOrder1.id === workOrder.id) {
              return workOrder;
            }
            return workOrder1;
          }
        );
      }
      if (state.singleWorkOrder?.id === workOrder.id)
        state.singleWorkOrder = workOrder;
    },
    addFilesToWorkOrder(
      state: WorkOrderState,
      action: PayloadAction<{ files: File[], id: number }>
    ) {
      const { files, id } = action.payload;
      const inContent = state.workOrders.content.some(
        (workOrder1) => workOrder1.id === id
      );
      if (inContent) {
        state.workOrders.content = state.workOrders.content.map(
          (workOrder1) => {
            if (workOrder1.id === id) {
              workOrder1.files.push(...files);
            }
            return workOrder1;
          }
        );
      }
      if (state.singleWorkOrder?.id === id)
        state.singleWorkOrder.files.push(...files);
    },
    setFilesForWorkOrder(
      state: WorkOrderState,
      action: PayloadAction<{ files: File[], id: number }>
    ) {
      const { files, id } = action.payload;
      const inContent = state.workOrders.content.some(
        (workOrder1) => workOrder1.id === id
      );
      if (inContent) {
        state.workOrders.content = state.workOrders.content.map(
          (workOrder1) => {
            if (workOrder1.id === id) {
              workOrder1.files = files;
            }
            return workOrder1;
          }
        );
      }
      if (state.singleWorkOrder?.id === id)
        state.singleWorkOrder.files = files;
    },
    deleteWorkOrder(
      state: WorkOrderState,
      action: PayloadAction<{ id: number }>
    ) {
      const { id } = action.payload;
      const workOrderIndex = state.workOrders.content.findIndex(
        (workOrder) => workOrder.id === id
      );
      if (workOrderIndex !== -1)
        state.workOrders.content.splice(workOrderIndex, 1);
    },
    clearSingleWorkOrder(state: WorkOrderState, action: PayloadAction<{}>) {
      state.singleWorkOrder = null;
    },
    getWorkOrdersByLocation(
      state: WorkOrderState,
      action: PayloadAction<{ workOrders: WorkOrder[]; id: number }>
    ) {
      const { workOrders, id } = action.payload;
      state.workOrdersByLocation[id] = workOrders;
    },
    getWorkOrdersByPart(
      state: WorkOrderState,
      action: PayloadAction<{ workOrders: WorkOrder[]; id: number }>
    ) {
      const { workOrders, id } = action.payload;
      state.workOrdersByPart[id] = workOrders;
    },
    getEvents(
      state: WorkOrderState,
      action: PayloadAction<{ events: CalendarEvent<WorkOrder | PreventiveMaintenance>[] }>
    ) {
      const { events } = action.payload;
      state.calendar.events = events;
    },
    setLoadingGet(
      state: WorkOrderState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    },
    getUrgentWorkOrdersCount(
      state: WorkOrderState,
      action: PayloadAction<{ count: number }>
    ) {
      const { count } = action.payload;
      state.urgentCount = count;
    }
  }
});

export const reducer = slice.reducer;

export const getWorkOrders =
  (criteria: SearchCriteria): AppThunk =>
    async (dispatch) => {
      try {
        dispatch(slice.actions.setLoadingGet({ loading: true }));
        const workOrders = await api.post<Page<WorkOrder>>(
          `${basePath}/search`,
          criteria
        );
        dispatch(slice.actions.getWorkOrders({ workOrders }));
      } finally {
        dispatch(slice.actions.setLoadingGet({ loading: false }));
      }
    };

export const getWorkOrdersMini =
  (criteria: SearchCriteria): AppThunk =>
    async (dispatch) => {
      try {
        const workOrders = await api.post<Page<WorkOrderBaseMiniDTO>>(
          `${basePath}/search/mini`,
          criteria
        );
        dispatch(slice.actions.getWorkOrdersMini({ workOrders }));
      } finally {
      }
    };
export const getSingleWorkOrder =
  (id: number): AppThunk =>
    async (dispatch) => {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const workOrder = await api.get<WorkOrder>(`${basePath}/${id}`);
      dispatch(slice.actions.getSingleWorkOrder({ workOrder }));
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    };
export const addWorkOrder =
  (workOrder): AppThunk =>
    async (dispatch) => {
      const workOrderResponse = await api.post<WorkOrder>(basePath, workOrder);
      dispatch(slice.actions.addWorkOrder({ workOrder: workOrderResponse }));
      const taskBases =
        workOrder.tasks?.map((task) => {
          return {
            ...task.taskBase,
            options: task.taskBase.options.map((option) => option.label)
          };
        }) ?? [];
      if (taskBases.length) {
        const tasks = await api.patch<Task[]>(
          `tasks/work-order/${workOrderResponse.id}`,
          taskBases,
          null
        );
      }
    };
export const editWorkOrder =
  (id: number, workOrder): AppThunk =>
    async (dispatch) => {
      const workOrderResponse = await api.patch<WorkOrder>(
        `${basePath}/${id}`,
        workOrder
      );
      dispatch(slice.actions.editWorkOrder({ workOrder: workOrderResponse }));
    };
export const addFilesToWorkOrder =
  (id: number, files: { id: number }[]): AppThunk =>
    async (dispatch) => {
      const response = await api.patch<File[]>(
        `${basePath}/files/${id}/add`,
        files
      );
      dispatch(slice.actions.addFilesToWorkOrder({ files: response, id }));
    };
export const removeFileFromWorkOrder =
  (workOrderId: number, fileId: number): AppThunk =>
    async (dispatch) => {
      const response = await api.deletes<File[]>(
        `${basePath}/files/${workOrderId}/${fileId}/remove`
      );
      dispatch(slice.actions.setFilesForWorkOrder({ files: response, id: workOrderId }));
    };
export const changeWorkOrderStatus =
  (id: number, body: { status: string; feedback?: string; signature?: { id: number } }): AppThunk =>
    async (dispatch) => {
      const workOrderResponse = await api.patch<WorkOrder>(
        `${basePath}/${id}/change-status`,
        body
      );
      dispatch(slice.actions.editWorkOrder({ workOrder: workOrderResponse }));
    };
export const deleteWorkOrder =
  (id: number): AppThunk =>
    async (dispatch) => {
      const workOrderResponse = await api.deletes<{ success: boolean }>(
        `${basePath}/${id}`
      );
      const { success } = workOrderResponse;
      if (success) {
        dispatch(slice.actions.deleteWorkOrder({ id }));
      }
    };

export const getWorkOrdersByLocation =
  (id: number): AppThunk =>
    async (dispatch) => {
      const workOrders = await api.get<WorkOrder[]>(`${basePath}/location/${id}`);
      dispatch(
        slice.actions.getWorkOrdersByLocation({
          id,
          workOrders
        })
      );
    };

export const getWorkOrdersByPart =
  (id: number): AppThunk =>
    async (dispatch) => {
      const workOrders = await api.get<WorkOrder[]>(`${basePath}/part/${id}`);
      dispatch(
        slice.actions.getWorkOrdersByPart({
          id,
          workOrders
        })
      );
    };
export const getPDFReport =
  (id: number): AppThunk =>
    async (dispatch): Promise<string> => {
      const response = await api.get<{ success: boolean; message: string }>(
        `${basePath}/report/${id}`
      );
      const { message } = response;
      return message;
    };

export const getWorkOrderEvents =
  (start: Date, end: Date): AppThunk =>
    async (dispatch) => {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const response = await api.post<CalendarEvent<WorkOrder | PreventiveMaintenance>[]>(`${basePath}/events`, {
        start,
        end
      });
      dispatch(
        slice.actions.getEvents({
          events: response
        })
      );
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    };
export const getUrgentWorkOrdersCount =
  (): AppThunk =>
    async (dispatch) => {
      const response = await api.get<{ success: boolean, message: string }>(`${basePath}/urgent`);
      dispatch(
        slice.actions.getUrgentWorkOrdersCount({
          count: Number(response.message)
        })
      );
    };
export const clearSingleWorkOrder = (): AppThunk => async (dispatch) => {
  dispatch(slice.actions.clearSingleWorkOrder({}));
};
export default slice;
