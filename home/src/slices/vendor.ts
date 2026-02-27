import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import { Vendor, VendorMiniDTO } from '../models/owns/vendor';
import api from '../utils/api';
import { getInitialPage, Page, SearchCriteria } from '../models/owns/page';
import { revertAll } from 'src/utils/redux';

const basePath = 'vendors';

interface VendorState {
  vendors: Page<Vendor>;
  singleVendor: Vendor;
  vendorsMini: VendorMiniDTO[];
  loadingGet: boolean;
}

const initialState: VendorState = {
  vendors: getInitialPage<Vendor>(),
  singleVendor: null,
  vendorsMini: [],
  loadingGet: false
};

const slice = createSlice({
  name: 'vendors',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getVendors(
      state: VendorState,
      action: PayloadAction<{ vendors: Page<Vendor> }>
    ) {
      const { vendors } = action.payload;
      state.vendors = vendors;
    },
    getSingleVendor(
      state: VendorState,
      action: PayloadAction<{ vendor: Vendor }>
    ) {
      const { vendor } = action.payload;
      state.singleVendor = vendor;
    },
    editVendor(state: VendorState, action: PayloadAction<{ vendor: Vendor }>) {
      const { vendor } = action.payload;
      const inContent = state.vendors.content.some(
        (vendor1) => vendor1.id === vendor.id
      );
      if (inContent) {
        state.vendors.content = state.vendors.content.map((vendor1) => {
          if (vendor1.id === vendor.id) {
            return vendor;
          }
          return vendor1;
        });
      } else {
        state.singleVendor = vendor;
      }
    },
    getVendorsMini(
      state: VendorState,
      action: PayloadAction<{ vendors: VendorMiniDTO[] }>
    ) {
      const { vendors } = action.payload;
      state.vendorsMini = vendors;
    },
    addVendor(state: VendorState, action: PayloadAction<{ vendor: Vendor }>) {
      const { vendor } = action.payload;
      state.vendors.content = [vendor, ...state.vendors.content];
    },
    deleteVendor(state: VendorState, action: PayloadAction<{ id: number }>) {
      const { id } = action.payload;
      const vendorIndex = state.vendors.content.findIndex(
        (vendor) => vendor.id === id
      );
      state.vendors.content.splice(vendorIndex, 1);
    },
    setLoadingGet(
      state: VendorState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    },
    clearSingleVendor(state: VendorState, action: PayloadAction<{}>) {
      state.singleVendor = null;
    }
  }
});

export const reducer = slice.reducer;

export const getVendors =
  (criteria: SearchCriteria): AppThunk =>
    async (dispatch) => {
      try {
        dispatch(slice.actions.setLoadingGet({ loading: true }));
        const vendors = await api.post<Page<Vendor>>(
          `${basePath}/search`,
          criteria
        );
        dispatch(slice.actions.getVendors({ vendors }));
      } finally {
        dispatch(slice.actions.setLoadingGet({ loading: false }));
      }
    };

export const getSingleVendor =
  (id: number): AppThunk =>
    async (dispatch) => {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const vendor = await api.get<Vendor>(`${basePath}/${id}`);
      dispatch(slice.actions.getSingleVendor({ vendor }));
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    };

export const editVendor =
  (id: number, vendor): AppThunk =>
    async (dispatch) => {
      const vendorResponse = await api.patch<Vendor>(`${basePath}/${id}`, vendor);
      dispatch(slice.actions.editVendor({ vendor: vendorResponse }));
    };
export const getVendorsMini = (): AppThunk => async (dispatch) => {
  const vendors = await api.get<Vendor[]>('vendors/mini');
  dispatch(slice.actions.getVendorsMini({ vendors }));
};
export const addVendor =
  (vendor): AppThunk =>
    async (dispatch) => {
      const vendorResponse = await api.post<Vendor>('vendors', vendor);
      dispatch(slice.actions.addVendor({ vendor: vendorResponse }));
    };
export const deleteVendor =
  (id: number): AppThunk =>
    async (dispatch) => {
      const vendorResponse = await api.deletes<{ success: boolean }>(
        `vendors/${id}`
      );
      const { success } = vendorResponse;
      if (success) {
        dispatch(slice.actions.deleteVendor({ id }));
      }
    };
export const clearSingleVendor = (): AppThunk => async (dispatch) => {
  dispatch(slice.actions.clearSingleVendor({}));
};

export default slice;
