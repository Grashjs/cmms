import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import { getInitialPage, Page, SearchCriteria } from 'src/models/owns/page';
import type { AppThunk } from 'src/store';
import PurchaseOrder from '../models/owns/purchaseOrder';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';

const basePath = 'purchase-orders';
interface PurchaseOrderState {
  purchaseOrders: Page<PurchaseOrder>;
  singlePurchaseOrder: PurchaseOrder;
  loadingGet: boolean;
}

const initialState: PurchaseOrderState = {
  purchaseOrders: getInitialPage<PurchaseOrder>(),
  singlePurchaseOrder: null,
  loadingGet: false
};

const slice = createSlice({
  name: 'purchaseOrders',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getPurchaseOrders(
      state: PurchaseOrderState,
      action: PayloadAction<{ purchaseOrders: Page<PurchaseOrder> }>
    ) {
      const { purchaseOrders } = action.payload;
      state.purchaseOrders = purchaseOrders;
    },
    getSinglePurchaseOrder(
      state: PurchaseOrderState,
      action: PayloadAction<{ purchaseOrder: PurchaseOrder }>
    ) {
      const { purchaseOrder } = action.payload;
      state.singlePurchaseOrder = purchaseOrder;
    },
    addPurchaseOrder(
      state: PurchaseOrderState,
      action: PayloadAction<{ purchaseOrder: PurchaseOrder }>
    ) {
      const { purchaseOrder } = action.payload;
      state.purchaseOrders.content = [
        ...state.purchaseOrders.content,
        purchaseOrder
      ];
    },
    editPurchaseOrder(
      state: PurchaseOrderState,
      action: PayloadAction<{
        purchaseOrder: PurchaseOrder;
      }>
    ) {
      const { purchaseOrder } = action.payload;
      const inContent = state.purchaseOrders.content.some(
        (po) => po.id === purchaseOrder.id
      );
      if (inContent) {
        state.purchaseOrders.content = state.purchaseOrders.content.map(
          (purchaseOrder1) => {
            if (purchaseOrder1.id === purchaseOrder.id) {
              return purchaseOrder;
            }
            return purchaseOrder1;
          }
        );
      } else {
        state.singlePurchaseOrder = purchaseOrder;
      }
    },
    deletePurchaseOrder(
      state: PurchaseOrderState,
      action: PayloadAction<{ id: number }>
    ) {
      const { id } = action.payload;
      const purchaseOrderIndex = state.purchaseOrders.content.findIndex(
        (purchaseOrder) => purchaseOrder.id === id
      );
      state.purchaseOrders.content.splice(purchaseOrderIndex, 1);
    },
    setLoadingGet(
      state: PurchaseOrderState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    },
    clearSinglePurchaseOrder(
      state: PurchaseOrderState,
      action: PayloadAction<{}>
    ) {
      state.singlePurchaseOrder = null;
    }
  }
});

export const reducer = slice.reducer;

export const getPurchaseOrders =
  (criteria: SearchCriteria): AppThunk =>
  async (dispatch) => {
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const purchaseOrders = await api.post<Page<PurchaseOrder>>(
        `${basePath}/search`,
        criteria
      );
      dispatch(slice.actions.getPurchaseOrders({ purchaseOrders }));
    } finally {
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };

export const getSinglePurchaseOrder =
  (id: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    const purchaseOrder = await api.get<PurchaseOrder>(`${basePath}/${id}`);
    dispatch(slice.actions.getSinglePurchaseOrder({ purchaseOrder }));
    dispatch(slice.actions.setLoadingGet({ loading: false }));
  };

export const editPurchaseOrder =
  (id: number, purchaseOrder): AppThunk =>
  async (dispatch) => {
    const purchaseOrderResponse = await api.patch<PurchaseOrder>(
      `${basePath}/${id}`,
      purchaseOrder
    );
    dispatch(
      slice.actions.editPurchaseOrder({
        purchaseOrder: purchaseOrderResponse
      })
    );
  };
export const addPurchaseOrder =
  (purchaseOrder): AppThunk =>
  async (dispatch) => {
    const purchaseOrderResponse = await api.post<PurchaseOrder>(
      basePath,
      purchaseOrder
    );
    dispatch(
      slice.actions.addPurchaseOrder({ purchaseOrder: purchaseOrderResponse })
    );
    return purchaseOrderResponse.id;
  };

export const respondPurchaseOrder =
  (id: number, approved: boolean): AppThunk =>
  async (dispatch) => {
    const purchaseOrderResponse = await api.patch<PurchaseOrder>(
      `${basePath}/${id}/respond?approved=${approved}`,
      {}
    );
    dispatch(
      slice.actions.editPurchaseOrder({ purchaseOrder: purchaseOrderResponse })
    );
  };

export const deletePurchaseOrder =
  (id: number): AppThunk =>
  async (dispatch) => {
    const purchaseOrderResponse = await api.deletes<{ success: boolean }>(
      `${basePath}/${id}`
    );
    const { success } = purchaseOrderResponse;
    if (success) {
      dispatch(slice.actions.deletePurchaseOrder({ id }));
    }
  };

export const clearSinglePurchaseOrder = (): AppThunk => async (dispatch) => {
  dispatch(slice.actions.clearSinglePurchaseOrder({}));
};

export default slice;
