import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import Part from 'src/models/owns/part';
import type { AppThunk } from 'src/store';
import PartQuantity from '../models/owns/partQuantity';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';

const basePath = 'part-quantities';

interface PartQuantityState {
  partQuantitiesByWorkOrder: { [id: number]: PartQuantity[] };
  partQuantitiesByPurchaseOrder: { [id: number]: PartQuantity[] };
  loadingPartQuantities: { [id: number]: boolean };
}

const initialState: PartQuantityState = {
  partQuantitiesByWorkOrder: {},
  partQuantitiesByPurchaseOrder: {},
  loadingPartQuantities: {}
};

const slice = createSlice({
  name: 'partQuantities',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getPartQuantitiesByWorkOrder(
      state: PartQuantityState,
      action: PayloadAction<{ id: number; partQuantities: PartQuantity[] }>
    ) {
      const { partQuantities, id } = action.payload;
      state.partQuantitiesByWorkOrder[id] = partQuantities;
    },
    getPartQuantitiesByPurchaseOrder(
      state: PartQuantityState,
      action: PayloadAction<{ id: number; partQuantities: PartQuantity[] }>
    ) {
      const { partQuantities, id } = action.payload;
      state.partQuantitiesByPurchaseOrder[id] = partQuantities;
    },
    editWOPartQuantity(
      state: PartQuantityState,
      action: PayloadAction<{
        workOrderId: number;
        id: number;
        partQuantity: PartQuantity;
      }>
    ) {
      const { partQuantity, id, workOrderId } = action.payload;
      state.partQuantitiesByWorkOrder[workOrderId] =
        state.partQuantitiesByWorkOrder[workOrderId].map((pq) => {
          if (pq.id === id) {
            return partQuantity;
          } else return pq;
        });
    },
    editPOPartQuantity(
      state: PartQuantityState,
      action: PayloadAction<{
        purchaseOrderId: number;
        id: number;
        partQuantity: PartQuantity;
      }>
    ) {
      const { partQuantity, id, purchaseOrderId } = action.payload;
      state.partQuantitiesByPurchaseOrder[purchaseOrderId] =
        state.partQuantitiesByPurchaseOrder[purchaseOrderId].map((pq) => {
          if (pq.id === id) {
            return partQuantity;
          } else return pq;
        });
    },
    setLoadingByWorkOrder(
      state: PartQuantityState,
      action: PayloadAction<{ loading: boolean; id: number }>
    ) {
      const { loading, id } = action.payload;
      state.loadingPartQuantities = {
        ...state.loadingPartQuantities,
        [id]: loading
      };
    }
  }
});

export const reducer = slice.reducer;

export const getPartQuantitiesByWorkOrder =
  (id: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingByWorkOrder({ id, loading: true }));
    try {
      const partQuantities = await api.get<PartQuantity[]>(
        `${basePath}/work-order/${id}`
      );
      dispatch(
        slice.actions.getPartQuantitiesByWorkOrder({ id, partQuantities })
      );
    } catch {
    } finally {
      dispatch(slice.actions.setLoadingByWorkOrder({ id, loading: false }));
    }
  };

export const getPartQuantitiesByPurchaseOrder =
  (id: number): AppThunk =>
  async (dispatch) => {
    const partQuantities = await api.get<PartQuantity[]>(
      `${basePath}/purchase-order/${id}`
    );
    dispatch(
      slice.actions.getPartQuantitiesByPurchaseOrder({ id, partQuantities })
    );
  };

export const editWOPartQuantities =
  (id: number, parts: number[]): AppThunk =>
  async (dispatch) => {
    const partQuantities = await api.patch<PartQuantity[]>(
      `${basePath}/work-order/${id}`,
      parts,
      null
    );
    dispatch(
      slice.actions.getPartQuantitiesByWorkOrder({
        id,
        partQuantities
      })
    );
  };

export const editPOPartQuantities =
  (id: number, partQuantities: { part: Part; quantity: number }[]): AppThunk =>
  async (dispatch) => {
    const partQuantitiesResponse = await api.patch<PartQuantity[]>(
      `${basePath}/purchase-order/${id}`,
      partQuantities,
      null
    );
    dispatch(
      slice.actions.getPartQuantitiesByPurchaseOrder({
        id,
        partQuantities: partQuantitiesResponse
      })
    );
  };
export const editPartQuantity =
  (rootId: number, id: number, quantity: number, isPO: boolean): AppThunk =>
  async (dispatch) => {
    const partQuantity = await api.patch<PartQuantity>(`${basePath}/${id}`, {
      quantity
    });
    if (isPO)
      dispatch(
        slice.actions.editPOPartQuantity({
          purchaseOrderId: rootId,
          id,
          partQuantity
        })
      );
    else
      dispatch(
        slice.actions.editWOPartQuantity({
          workOrderId: rootId,
          id,
          partQuantity
        })
      );
  };

export default slice;
