import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import api from '../utils/api';
import { LicenseEntitlement, LicensingState } from '../models/license';
import { AppThunk } from '../store';

const basePath = 'license';
interface LicenseState {
  state: LicensingState;
}

const initialState: LicenseState = {
  state: { valid: false, entitlements: [] }
};

const slice = createSlice({
  name: 'license',
  initialState,
  reducers: {
    getLicenseValidity(
      state: LicenseState,
      action: PayloadAction<LicenseState['state']>
    ) {
      state.state = action.payload;
    }
  }
});

export const reducer = slice.reducer;

export const getLicenseValidity = (): AppThunk => async (dispatch) => {
  const response = await api.get<LicenseState['state']>(`${basePath}/state`);
  dispatch(slice.actions.getLicenseValidity(response));
};

export default slice;
