import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import AdditionalCost from '../models/owns/additionalCost';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';

const basePath = 'additional-costs';

interface AdditionalCostState {
  costsByWorkOrder: { [id: number]: AdditionalCost[] };
  loadingCosts: { [id: number]: boolean };
}

const initialState: AdditionalCostState = {
  costsByWorkOrder: {},
  loadingCosts: {}
};

const slice = createSlice({
  name: 'additionalCosts',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getAdditionalCosts(
      state: AdditionalCostState,
      action: PayloadAction<{ id: number; additionalCosts: AdditionalCost[] }>
    ) {
      const { additionalCosts, id } = action.payload;
      state.costsByWorkOrder[id] = additionalCosts;
    },
    createAdditionalCost(
      state: AdditionalCostState,
      action: PayloadAction<{
        workOrderId: number;
        additionalCost: AdditionalCost;
      }>
    ) {
      const { additionalCost, workOrderId } = action.payload;
      if (state.costsByWorkOrder[workOrderId]) {
        state.costsByWorkOrder[workOrderId].push(additionalCost);
      } else state.costsByWorkOrder[workOrderId] = [additionalCost];
    },
    deleteAdditionalCost(
      state: AdditionalCostState,
      action: PayloadAction<{
        workOrderId: number;
        id: number;
      }>
    ) {
      const { id, workOrderId } = action.payload;
      state.costsByWorkOrder[workOrderId] = state.costsByWorkOrder[
        workOrderId
      ].filter((additionalCost) => additionalCost.id !== id);
    },
    setLoadingByWorkOrder(
      state: AdditionalCostState,
      action: PayloadAction<{ loading: boolean; id: number }>
    ) {
      const { loading, id } = action.payload;
      state.loadingCosts = { ...state.loadingCosts, [id]: loading };
    }
  }
});

export const reducer = slice.reducer;

export const getAdditionalCosts =
  (id: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingByWorkOrder({ id, loading: true }));
    try {
      const additionalCosts = await api.get<AdditionalCost[]>(
        `${basePath}/work-order/${id}`
      );
      dispatch(slice.actions.getAdditionalCosts({ id, additionalCosts }));
    } catch {
    } finally {
      dispatch(slice.actions.setLoadingByWorkOrder({ id, loading: false }));
    }
  };

export const createAdditionalCost =
  (id: number, additionalCost: Partial<AdditionalCost>): AppThunk =>
  async (dispatch) => {
    const additionalCostResponse = await api.post<AdditionalCost>(
      `${basePath}`,
      { ...additionalCost, workOrder: { id } }
    );
    dispatch(
      slice.actions.createAdditionalCost({
        workOrderId: id,
        additionalCost: additionalCostResponse
      })
    );
  };

export const deleteAdditionalCost =
  (workOrderId: number, id: number): AppThunk =>
  async (dispatch) => {
    const response = await api.deletes<{ success: boolean }>(
      `${basePath}/${id}`
    );
    const { success } = response;
    if (success) {
      dispatch(slice.actions.deleteAdditionalCost({ workOrderId, id }));
    }
  };

export default slice;
