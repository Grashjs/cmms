import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import RequestQualification, {
  RequestTriage
} from '../models/owns/requestQualification';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';
import { refreshRequest } from './request';

const basePath = 'request-qualifications';

/**
 * Kept per request id rather than as one current qualification, because the request detail view
 * is opened from a list and the previous request's card must not flash up under the next one
 * while its own fetch is still running.
 */
interface RequestQualificationState {
  byRequestId: Record<number, RequestQualification | null>;
  loadingByRequestId: Record<number, boolean>;
  deciding: boolean;
}

const initialState: RequestQualificationState = {
  byRequestId: {},
  loadingByRequestId: {},
  deciding: false
};

const slice = createSlice({
  name: 'requestQualifications',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getQualification(
      state: RequestQualificationState,
      action: PayloadAction<{
        requestId: number;
        qualification: RequestQualification | null;
      }>
    ) {
      const { requestId, qualification } = action.payload;
      state.byRequestId[requestId] = qualification;
    },
    setLoading(
      state: RequestQualificationState,
      action: PayloadAction<{ requestId: number; loading: boolean }>
    ) {
      const { requestId, loading } = action.payload;
      state.loadingByRequestId[requestId] = loading;
    },
    setDeciding(
      state: RequestQualificationState,
      action: PayloadAction<{ deciding: boolean }>
    ) {
      state.deciding = action.payload.deciding;
    }
  }
});

export const reducer = slice.reducer;

export const getRequestQualification =
  (requestId: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoading({ requestId, loading: true }));
    try {
      const response = await api.get<RequestTriage>(
        `${basePath}/request/${requestId}`
      );
      dispatch(
        slice.actions.getQualification({
          requestId,
          qualification: response.qualification
        })
      );
    } finally {
      dispatch(slice.actions.setLoading({ requestId, loading: false }));
    }
  };

/**
 * Accepting a suggestion writes the asset onto the request, so the request itself has to be
 * refetched afterwards - otherwise the card reports success while the asset field above it still
 * shows nothing, which reads as the button not having worked.
 */
export const applyQualification =
  (id: number, requestId: number, assetId: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setDeciding({ deciding: true }));
    try {
      const response = await api.patch<RequestTriage>(
        `${basePath}/${id}/apply?assetId=${assetId}`,
        {}
      );
      dispatch(
        slice.actions.getQualification({
          requestId,
          qualification: response.qualification
        })
      );
      await dispatch(refreshRequest(requestId));
    } finally {
      dispatch(slice.actions.setDeciding({ deciding: false }));
    }
  };

export const rejectQualification =
  (id: number, requestId: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setDeciding({ deciding: true }));
    try {
      const response = await api.patch<RequestTriage>(
        `${basePath}/${id}/reject`,
        {}
      );
      dispatch(
        slice.actions.getQualification({
          requestId,
          qualification: response.qualification
        })
      );
    } finally {
      dispatch(slice.actions.setDeciding({ deciding: false }));
    }
  };

export const rerunQualification =
  (requestId: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setDeciding({ deciding: true }));
    try {
      const response = await api.post<RequestTriage>(
        `${basePath}/request/${requestId}/rerun`,
        {}
      );
      dispatch(
        slice.actions.getQualification({
          requestId,
          qualification: response.qualification
        })
      );
    } finally {
      dispatch(slice.actions.setDeciding({ deciding: false }));
    }
  };

export default slice;
