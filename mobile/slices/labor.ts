import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from '../store';
import Labor from '../models/labor';
import api from '../utils/api';
import { revertAll } from '../utils/redux';
import { getPreparedRuntime } from '../offline/runtime';
import {
  assertSessionGeneration,
  sessionGeneration
} from '../offline/sessionBridge';

const basePath = 'labors';

interface LaborState {
  timesByWorkOrder: { [id: number]: Labor[] };
  loadingLabors: { [id: number]: boolean };
}

const initialState: LaborState = {
  timesByWorkOrder: {},
  loadingLabors: {}
};

const slice = createSlice({
  name: 'labors',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    removePreparedLabors(state: LaborState, action: PayloadAction<number[]>) {
      for (const id of action.payload) {
        delete state.timesByWorkOrder[id];
        delete state.loadingLabors[id];
      }
    },
    getLabors(
      state: LaborState,
      action: PayloadAction<{ id: number; labors: Labor[] }>
    ) {
      const { labors, id } = action.payload;
      state.timesByWorkOrder[id] = labors;
    },
    createLabor(
      state: LaborState,
      action: PayloadAction<{
        workOrderId: number;
        labor: Labor;
      }>
    ) {
      const { labor, workOrderId } = action.payload;
      if (state.timesByWorkOrder[workOrderId]) {
        state.timesByWorkOrder[workOrderId].push(labor);
      } else state.timesByWorkOrder[workOrderId] = [labor];
    },
    editLabor(
      state: LaborState,
      action: PayloadAction<{
        id: number;
        workOrderId: number;
        labor: Labor;
      }>
    ) {
      const { labor, workOrderId, id } = action.payload;
      state.timesByWorkOrder[workOrderId] = (
        state.timesByWorkOrder[workOrderId] || []
      ).map((addTime) => {
        if (addTime.id === id) {
          return labor;
        }
        return addTime;
      });
    },
    deleteLabor(
      state: LaborState,
      action: PayloadAction<{
        workOrderId: number;
        id: number;
      }>
    ) {
      const { id, workOrderId } = action.payload;
      state.timesByWorkOrder[workOrderId] = (
        state.timesByWorkOrder[workOrderId] || []
      ).filter((labor) => labor.id !== id);
    },
    controlTimer(
      state: LaborState,
      action: PayloadAction<{
        workOrderId: number;
        labor: Labor;
      }>
    ) {
      const { labor, workOrderId } = action.payload;
      const filteredLabors = (state.timesByWorkOrder[workOrderId] || []).filter(
        (aT) => aT.id !== labor.id
      );
      filteredLabors.push(labor);
      state.timesByWorkOrder[workOrderId] = filteredLabors;
    },
    setLoadingByWorkOrder(
      state: LaborState,
      action: PayloadAction<{ loading: boolean; id: number }>
    ) {
      const { loading, id } = action.payload;
      state.loadingLabors = { ...state.loadingLabors, [id]: loading };
    }
  }
});

export const reducer = slice.reducer;
export const { removePreparedLabors } = slice.actions;

export const getLabors =
  (id: number): AppThunk =>
  async (dispatch) => {
    if (getPreparedRuntime(id)) return;
    const generation = sessionGeneration();
    dispatch(slice.actions.setLoadingByWorkOrder({ id, loading: true }));
    try {
      const labors = await api.get<Labor[]>(`${basePath}/work-order/${id}`);
      assertSessionGeneration(generation);
      if (!getPreparedRuntime(id))
        dispatch(slice.actions.getLabors({ id, labors }));
    } catch {
    } finally {
      if (generation === sessionGeneration() && !getPreparedRuntime(id))
        dispatch(slice.actions.setLoadingByWorkOrder({ id, loading: false }));
    }
  };

export interface ConfirmedLaborMutation {
  labor?: Labor;
  localUpdateError?: unknown;
}

/** Mutation success and a failed canonical refresh are separate outcomes. */
async function mutateLabor(
  workOrderId: number,
  mutation: () => Promise<Labor | undefined>,
  publishConnected: (labor?: Labor) => void
): Promise<ConfirmedLaborMutation> {
  const generation = sessionGeneration();
  assertSessionGeneration(generation);
  const prepared = getPreparedRuntime(workOrderId);
  const execute = async () => {
    assertSessionGeneration(generation);
    const labor = await mutation();
    assertSessionGeneration(generation);
    return labor;
  };
  const labor = await (prepared
    ? prepared.runConnectedLabor(workOrderId, execute)
    : execute());
  assertSessionGeneration(generation);
  const current = getPreparedRuntime(workOrderId);
  if (current) {
    try {
      // refresh owns the same lane, so run it after the mutation leaves that lane.
      await current.refresh(workOrderId);
      assertSessionGeneration(generation);
    } catch (localUpdateError) {
      assertSessionGeneration(generation);
      return { labor, localUpdateError };
    }
  } else publishConnected(labor);
  return { labor };
}

export const createLabor =
  (id: number, labor: Partial<Labor>): AppThunk =>
  async (dispatch) =>
    mutateLabor(
      id,
      () => api.post<Labor>(`${basePath}`, { ...labor, workOrder: { id } }),
      (created) =>
        dispatch(slice.actions.createLabor({ workOrderId: id, labor: created }))
    );

export const editLabor =
  (id: number, workOrderId: number, labor: Partial<Labor>): AppThunk =>
  async (dispatch) =>
    mutateLabor(
      workOrderId,
      () => api.patch<Labor>(`${basePath}/${id}`, labor),
      (updated) =>
        dispatch(slice.actions.editLabor({ workOrderId, id, labor: updated }))
    );

export const deleteLabor =
  (workOrderId: number, id: number, version: number): AppThunk =>
  async (dispatch) =>
    mutateLabor(
      workOrderId,
      async () => {
        await api.deletes<{ success: boolean }>(
          `${basePath}/${id}?version=${version}`
        );
        return undefined;
      },
      () => dispatch(slice.actions.deleteLabor({ workOrderId, id }))
    );

export const controlTimer =
  (start: boolean, id: number, observed?: Labor): AppThunk =>
  async (dispatch) =>
    mutateLabor(
      id,
      () =>
        api.post<Labor>(
          `${basePath}/work-order/${id}?start=${start}${
            !start
              ? `&laborId=${observed?.id}&timerRunId=${encodeURIComponent(
                  observed?.timerRunId || ''
                )}&version=${observed?.version}`
              : ''
          }`,
          {}
        ),
      (labor) =>
        dispatch(slice.actions.controlTimer({ labor, workOrderId: id }))
    );

export default slice;
