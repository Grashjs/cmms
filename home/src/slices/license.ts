import type { PayloadAction } from "@reduxjs/toolkit";
import { createSlice } from "@reduxjs/toolkit";
import type { AppThunk } from "src/store";
import api from "../utils/api";
import { revertAll } from "src/utils/redux";
import { LicenseEntitlement, LicensingState } from "../models/owns/license";

const basePath = "license";
interface LicenseState {
  state: LicensingState;
}

const initialState: LicenseState = {
  state: {
    valid: false,
    entitlements: [],
    expirationDate: null,
    planName: null,
  },
};

const slice = createSlice({
  name: "license",
  initialState,
  reducers: {
    getLicenseValidity(state: LicenseState, action: PayloadAction<LicenseState["state"]>) {
      state.state = action.payload;
    },
  },
});

export const reducer = slice.reducer;

export const getLicenseValidity = (): AppThunk => async (dispatch) => {
  const response = await api.get<LicenseState["state"]>(`${basePath}/state`);
  dispatch(slice.actions.getLicenseValidity(response));
};

import { cache } from "react";
import { apiUrl } from "src/config";

export const getLicenseValidityServer = cache(async (): Promise<LicensingState> => {
  try {
    const response = await fetch(`${apiUrl}${basePath}/state`, {
      next: { revalidate: 3600 * 24 },
    });
    if (!response.ok) return initialState.state;
    return await response.json();
  } catch (error) {
    console.error("Failed to fetch license server-side:", error);
    return initialState.state;
  }
});

export default slice;
