import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from '../store';
import WorkOrder from '../models/workOrder';
import api from '../utils/api';
import { Task } from '../models/tasks';
import { getInitialPage, Page, SearchCriteria } from '../models/page';
import { revertAll } from '../utils/redux';

const basePath = 'work-orders';

interface WorkOrderState {
  workOrders: Page<WorkOrder>;
  workOrdersByLocation: { [key: number]: WorkOrder[] };
  workOrdersByPart: { [key: number]: WorkOrder[] };
  workOrderInfos: { [key: number]: { workOrder?: WorkOrder } };
  loadingGet: boolean;
  currentPageNum: number;
  lastPage: boolean;
  calendar: {
    events: WorkOrder[];
  };
}

const initialState: WorkOrderState = {
  workOrders: getInitialPage<WorkOrder>(),
  workOrdersByLocation: {},
  workOrdersByPart: {},
  workOrderInfos: {},
  loadingGet: false,
  currentPageNum: 0,
  lastPage: true,
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
      state.currentPageNum = 0;
      state.lastPage = workOrders.last;
    },
    getMoreWorkOrders(
      state: WorkOrderState,
      action: PayloadAction<{ workOrders: Page<WorkOrder> }>
    ) {
      const { workOrders } = action.payload;
      state.workOrders.content = state.workOrders.content.concat(
        workOrders.content
      );
      state.currentPageNum = state.currentPageNum + 1;
      state.lastPage = workOrders.last;
    },
    getWorkOrderDetails(
      state: WorkOrderState,
      action: PayloadAction<{ workOrder; id: number }>
    ) {
      const { workOrder, id } = action.payload;
      if (state.workOrderInfos[id]) {
        state.workOrderInfos[id] = { ...state.workOrderInfos[id], workOrder };
      } else state.workOrderInfos[id] = { workOrder };
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
      if (state.workOrderInfos[workOrder.id]) {
        state.workOrderInfos[workOrder.id].workOrder = workOrder;
      } else state.workOrderInfos[workOrder.id] = { workOrder };
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
      action: PayloadAction<{ events: WorkOrder[] }>
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
export const getMoreWorkOrders =
  (criteria: SearchCriteria, pageNum: number): AppThunk =>
    async (dispatch) => {
      criteria = { ...criteria, pageNum };
      try {
        dispatch(slice.actions.setLoadingGet({ loading: true }));
        const workOrders = await api.post<Page<WorkOrder>>(
          `${basePath}/search`,
          criteria
        );
        dispatch(slice.actions.getMoreWorkOrders({ workOrders }));
      } finally {
        dispatch(slice.actions.setLoadingGet({ loading: false }));
      }
    };
export const getWorkOrderDetails =
  (id: number): AppThunk =>
    async (dispatch) => {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const workOrder = await api.get<WorkOrder>(`${basePath}/${id}`);
      dispatch(
        slice.actions.getWorkOrderDetails({
          id,
          workOrder
        })
      );
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
          null,
          true
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
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const workOrders = await api.get<WorkOrder[]>(`${basePath}/location/${id}`);
      dispatch(
        slice.actions.getWorkOrdersByLocation({
          id,
          workOrders
        })
      );
      dispatch(slice.actions.setLoadingGet({ loading: false }));
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
      const response = await api.post<WorkOrder[]>(`${basePath}/events`, {
        start,
        end
      });
      dispatch(
        slice.actions.getEvents({
          events: response
        })
      );
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
export default slice;
