import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from '../store';
import WorkOrder from '../models/workOrder';
import api from '../utils/api';
import { Task } from '../models/tasks';
import { getInitialPage, Page, SearchCriteria } from '../models/page';
import { revertAll } from '../utils/redux';
import { getPreparedRuntime } from '../offline/runtime';
import {
  assertSessionGeneration,
  sessionGeneration,
  sessionChangeInProgress
} from '../offline/sessionBridge';

const basePath = 'work-orders';

interface WorkOrderState {
  preparedIdsInPage: number[];
  orderedIdsInPage: number[];
  workOrders: Page<WorkOrder>;
  workOrdersByLocation: { [key: number]: WorkOrder[] };
  workOrdersByPart: { [key: number]: WorkOrder[] };
  preparedIdsByLocation: { [key: number]: number[] };
  preparedIdsByPart: { [key: number]: number[] };
  workOrderInfos: { [key: number]: { workOrder?: WorkOrder } };
  loadingGet: boolean;
  currentPageNum: number;
  lastPage: boolean;
  calendar: {
    events: WorkOrder[];
    preparedIds: number[];
  };
}

const initialState: WorkOrderState = {
  preparedIdsInPage: [],
  orderedIdsInPage: [],
  workOrders: getInitialPage<WorkOrder>(),
  workOrdersByLocation: {},
  workOrdersByPart: {},
  preparedIdsByLocation: {},
  preparedIdsByPart: {},
  workOrderInfos: {},
  loadingGet: false,
  currentPageNum: 0,
  lastPage: true,
  calendar: {
    events: [],
    preparedIds: []
  }
};

const slice = createSlice({
  name: 'workOrders',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    removePreparedWorkOrders(state, action: PayloadAction<number[]>) {
      // Preserve the original server ordering before replacing prepared records with local projections.
      state.orderedIdsInPage = state.orderedIdsInPage?.length
        ? state.orderedIdsInPage
        : state.workOrders.content.map((wo) => wo.id);
      state.preparedIdsByLocation = state.preparedIdsByLocation || {};
      state.preparedIdsByPart = state.preparedIdsByPart || {};
      const ids = new Set(action.payload);
      const removed = state.workOrders.content
        .filter((wo) => ids.has(wo.id))
        .map((wo) => wo.id);
      state.preparedIdsInPage = [
        ...new Set([...(state.preparedIdsInPage || []), ...removed])
      ];
      state.workOrders.content = state.workOrders.content.filter(
        (wo) => !ids.has(wo.id)
      );
      ids.forEach((id) => {
        delete state.workOrderInfos[id];
      });
      Object.keys(state.workOrdersByLocation).forEach((id) => {
        state.preparedIdsByLocation[id] = [
          ...new Set([
            ...(state.preparedIdsByLocation[id] || []),
            ...state.workOrdersByLocation[id]
              .filter((wo) => ids.has(wo.id))
              .map((wo) => wo.id)
          ])
        ];
        state.workOrdersByLocation[id] = state.workOrdersByLocation[id].filter(
          (wo) => !ids.has(wo.id)
        );
      });
      Object.keys(state.workOrdersByPart).forEach((id) => {
        state.preparedIdsByPart[id] = [
          ...new Set([
            ...(state.preparedIdsByPart[id] || []),
            ...state.workOrdersByPart[id]
              .filter((wo) => ids.has(wo.id))
              .map((wo) => wo.id)
          ])
        ];
        state.workOrdersByPart[id] = state.workOrdersByPart[id].filter(
          (wo) => !ids.has(wo.id)
        );
      });
      state.calendar.preparedIds = [
        ...new Set([
          ...(state.calendar.preparedIds || []),
          ...state.calendar.events
            .filter((wo) => ids.has(wo.id))
            .map((wo) => wo.id)
        ])
      ];
      state.calendar.events = state.calendar.events.filter(
        (wo) => !ids.has(wo.id)
      );
    },
    getWorkOrders(
      state: WorkOrderState,
      action: PayloadAction<{
        workOrders: Page<WorkOrder>;
        preparedIds?: number[];
        orderedIds?: number[];
      }>
    ) {
      const { workOrders } = action.payload;
      state.workOrders = workOrders;
      state.preparedIdsInPage = action.payload.preparedIds ?? [];
      state.orderedIdsInPage =
        action.payload.orderedIds ?? workOrders.content.map((wo) => wo.id);
      state.currentPageNum = 0;
      state.lastPage = workOrders.last;
    },
    getMoreWorkOrders(
      state: WorkOrderState,
      action: PayloadAction<{
        workOrders: Page<WorkOrder>;
        preparedIds?: number[];
        orderedIds?: number[];
      }>
    ) {
      const { workOrders } = action.payload;
      state.orderedIdsInPage = [
        ...new Set([
          ...(state.orderedIdsInPage ||
            state.workOrders.content.map((wo) => wo.id)),
          ...(action.payload.orderedIds ||
            workOrders.content.map((wo) => wo.id))
        ])
      ];
      state.workOrders.content = state.workOrders.content.concat(
        workOrders.content
      );
      state.preparedIdsInPage = [
        ...new Set([
          ...(state.preparedIdsInPage || []),
          ...(action.payload.preparedIds ?? [])
        ])
      ];
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
      state.orderedIdsInPage = [
        workOrder.id,
        ...(state.orderedIdsInPage || [])
      ];
    },
    editWorkOrder(
      state: WorkOrderState,
      action: PayloadAction<{ workOrder: WorkOrder }>
    ) {
      const { workOrder } = action.payload;
      if (state.workOrderInfos[workOrder.id]) {
        state.workOrderInfos[workOrder.id].workOrder = workOrder;
      } else state.workOrderInfos[workOrder.id] = { workOrder };
      const workOrderIndex = state.workOrders.content.findIndex(
        (wo) => wo.id === workOrder.id
      );
      if (workOrderIndex !== -1) {
        state.workOrders.content[workOrderIndex] = workOrder;
      }
    },
    deleteWorkOrder(
      state: WorkOrderState,
      action: PayloadAction<{ id: number }>
    ) {
      const { id } = action.payload;
      state.orderedIdsInPage = (state.orderedIdsInPage || []).filter(
        (value) => value !== id
      );
      const workOrderIndex = state.workOrders.content.findIndex(
        (workOrder) => workOrder.id === id
      );
      if (workOrderIndex !== -1)
        state.workOrders.content.splice(workOrderIndex, 1);
    },
    getWorkOrdersByLocation(
      state: WorkOrderState,
      action: PayloadAction<{
        workOrders: WorkOrder[];
        id: number;
        preparedIds?: number[];
      }>
    ) {
      const { workOrders, id } = action.payload;
      state.workOrdersByLocation[id] = workOrders;
      state.preparedIdsByLocation = state.preparedIdsByLocation || {};
      state.preparedIdsByLocation[id] = action.payload.preparedIds || [];
    },
    getWorkOrdersByPart(
      state: WorkOrderState,
      action: PayloadAction<{
        workOrders: WorkOrder[];
        id: number;
        preparedIds?: number[];
      }>
    ) {
      const { workOrders, id } = action.payload;
      state.workOrdersByPart[id] = workOrders;
      state.preparedIdsByPart = state.preparedIdsByPart || {};
      state.preparedIdsByPart[id] = action.payload.preparedIds || [];
    },
    getEvents(
      state: WorkOrderState,
      action: PayloadAction<{ events: WorkOrder[]; preparedIds?: number[] }>
    ) {
      const { events } = action.payload;
      state.calendar.events = events;
      state.calendar.preparedIds = action.payload.preparedIds || [];
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

function partitionPrepared(workOrders: Page<WorkOrder>) {
  const preparedIds = workOrders.content
    .filter((wo) => getPreparedRuntime(wo.id))
    .map((wo) => wo.id);
  return {
    preparedIds,
    orderedIds: workOrders.content.map((wo) => wo.id),
    workOrders: {
      ...workOrders,
      content: workOrders.content.filter((wo) => !preparedIds.includes(wo.id))
    }
  };
}

function partitionPreparedList(workOrders: WorkOrder[]) {
  const preparedIds = workOrders
    .filter((wo) => getPreparedRuntime(wo.id))
    .map((wo) => wo.id);
  return {
    preparedIds,
    workOrders: workOrders.filter((wo) => !preparedIds.includes(wo.id))
  };
}

export const getWorkOrders =
  (criteria: SearchCriteria): AppThunk =>
  async (dispatch) => {
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const workOrders = await api.post<Page<WorkOrder>>(
        `${basePath}/search`,
        criteria
      );
      assertSessionGeneration(generation);
      dispatch(slice.actions.getWorkOrders(partitionPrepared(workOrders)));
    } finally {
      if (generation === sessionGeneration() && !sessionChangeInProgress())
        dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };
export const getMoreWorkOrders =
  (criteria: SearchCriteria, pageNum: number): AppThunk =>
  async (dispatch) => {
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    criteria = { ...criteria, pageNum };
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const workOrders = await api.post<Page<WorkOrder>>(
        `${basePath}/search`,
        criteria
      );
      assertSessionGeneration(generation);
      dispatch(slice.actions.getMoreWorkOrders(partitionPrepared(workOrders)));
    } finally {
      if (generation === sessionGeneration() && !sessionChangeInProgress())
        dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };
export const getWorkOrderDetails =
  (id: number): AppThunk =>
  async (dispatch) => {
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    const prepared = getPreparedRuntime(id);
    if (prepared) return prepared.refresh(id);
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    try {
      const workOrder = await api.get<WorkOrder>(`${basePath}/${id}`);
      assertSessionGeneration(generation);
      const nowPrepared = getPreparedRuntime(id);
      if (nowPrepared) return nowPrepared.refresh(id);
      dispatch(
        slice.actions.getWorkOrderDetails({
          id,
          workOrder
        })
      );
    } finally {
      if (generation === sessionGeneration() && !sessionChangeInProgress())
        dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };
export const addWorkOrder =
  (workOrder): AppThunk =>
  async (dispatch) => {
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    const workOrderResponse = await api.post<WorkOrder>(basePath, workOrder);
    assertSessionGeneration(generation);
    dispatch(slice.actions.addWorkOrder({ workOrder: workOrderResponse }));
    const taskBases =
      workOrder.tasks?.map((task) => {
        return {
          ...task.taskBase,
          options: task.taskBase.options.map((option) => option.label)
        };
      }) ?? [];
    if (taskBases.length) {
      assertSessionGeneration(generation);
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
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    const workOrderResponse = await api.patch<WorkOrder>(
      `${basePath}/${id}`,
      workOrder
    );
    assertSessionGeneration(generation);
    const prepared = getPreparedRuntime(id);
    if (prepared) {
      await prepared.ingestWorkOrder(workOrderResponse);
      return;
    }
    dispatch(slice.actions.editWorkOrder({ workOrder: workOrderResponse }));
    if (workOrder.archived) dispatch(slice.actions.deleteWorkOrder({ id }));
  };
export const deleteWorkOrder =
  (id: number): AppThunk =>
  async (dispatch) => {
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    const workOrderResponse = await api.deletes<{ success: boolean }>(
      `${basePath}/${id}`
    );
    const { success } = workOrderResponse;
    assertSessionGeneration(generation);
    if (success) {
      await getPreparedRuntime(id)?.markUnavailable(id, 'NOT_FOUND');
      assertSessionGeneration(generation);
      dispatch(slice.actions.deleteWorkOrder({ id }));
    }
  };

export const getWorkOrdersByLocation =
  (id: number): AppThunk =>
  async (dispatch) => {
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    try {
      const workOrders = await api.get<WorkOrder[]>(
        `${basePath}/location/${id}`
      );
      assertSessionGeneration(generation);
      dispatch(
        slice.actions.getWorkOrdersByLocation({
          id,
          ...partitionPreparedList(workOrders)
        })
      );
    } finally {
      if (generation === sessionGeneration() && !sessionChangeInProgress())
        dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };

export const getWorkOrdersByPart =
  (id: number): AppThunk =>
  async (dispatch) => {
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    const workOrders = await api.get<WorkOrder[]>(`${basePath}/part/${id}`);
    assertSessionGeneration(generation);
    dispatch(
      slice.actions.getWorkOrdersByPart({
        id,
        ...partitionPreparedList(workOrders)
      })
    );
  };
export const getPDFReport =
  (id: number): AppThunk =>
  async (dispatch): Promise<string> => {
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    const response = await api.get<{ success: boolean; message: string }>(
      `${basePath}/report/${id}`
    );
    const { message } = response;
    assertSessionGeneration(generation);
    return message;
  };

export const getWorkOrderEvents =
  (start: Date, end: Date): AppThunk =>
  async (dispatch) => {
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    const response = await api.post<WorkOrder[]>(`${basePath}/events`, {
      start,
      end
    });
    assertSessionGeneration(generation);
    const partitioned = partitionPreparedList(response);
    dispatch(
      slice.actions.getEvents({
        events: partitioned.workOrders,
        preparedIds: partitioned.preparedIds
      })
    );
  };
export const changeWorkOrderStatus =
  (
    id: number,
    body: { status: string; feedback?: string; signature?: string }
  ): AppThunk =>
  async (dispatch) => {
    const generation = sessionGeneration();
    assertSessionGeneration(generation);
    const workOrderResponse = await api.patch<WorkOrder>(
      `${basePath}/${id}/change-status`,
      body
    );
    assertSessionGeneration(generation);
    const prepared = getPreparedRuntime(id);
    if (prepared) {
      await prepared.ingestWorkOrder(workOrderResponse);
      return;
    }
    dispatch(slice.actions.editWorkOrder({ workOrder: workOrderResponse }));
  };
export const removePreparedWorkOrders = slice.actions.removePreparedWorkOrders;
export default slice;
