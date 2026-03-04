import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import {
  RequestPortal,
  RequestPortalPostDTO
} from '../models/owns/requestPortal';
import api from '../utils/api';
import { getInitialPage, Page, SearchCriteria } from '../models/owns/page';
import { revertAll } from 'src/utils/redux';

const basePath = 'request-portals';

interface RequestPortalState {
  requestPortals: Page<RequestPortal>;
  singleRequestPortal: RequestPortal;
  loadingGet: boolean;
}

const initialState: RequestPortalState = {
  requestPortals: getInitialPage<RequestPortal>(),
  singleRequestPortal: null,
  loadingGet: false
};

const slice = createSlice({
  name: 'requestPortals',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getRequestPortals(
      state: RequestPortalState,
      action: PayloadAction<{ requestPortals: Page<RequestPortal> }>
    ) {
      const { requestPortals } = action.payload;
      state.requestPortals = requestPortals;
    },
    getSingleRequestPortal(
      state: RequestPortalState,
      action: PayloadAction<{ requestPortal: RequestPortal }>
    ) {
      const { requestPortal } = action.payload;
      state.singleRequestPortal = requestPortal;
    },
    editRequestPortal(
      state: RequestPortalState,
      action: PayloadAction<{ requestPortal: RequestPortal }>
    ) {
      const { requestPortal } = action.payload;
      const inContent = state.requestPortals.content.some(
        (requestPortal1) => requestPortal1.id === requestPortal.id
      );
      if (inContent) {
        state.requestPortals.content = state.requestPortals.content.map(
          (requestPortal1) => {
            if (requestPortal1.id === requestPortal.id) {
              return requestPortal;
            }
            return requestPortal1;
          }
        );
      } else {
        state.singleRequestPortal = requestPortal;
      }
    },
    addRequestPortal(
      state: RequestPortalState,
      action: PayloadAction<{ requestPortal: RequestPortal }>
    ) {
      const { requestPortal } = action.payload;
      state.requestPortals.content = [
        requestPortal,
        ...state.requestPortals.content
      ];
    },
    deleteRequestPortal(
      state: RequestPortalState,
      action: PayloadAction<{ id: number }>
    ) {
      const { id } = action.payload;
      const requestPortalIndex = state.requestPortals.content.findIndex(
        (requestPortal) => requestPortal.id === id
      );
      state.requestPortals.content.splice(requestPortalIndex, 1);
    },
    setLoadingGet(
      state: RequestPortalState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    },
    clearSingleRequestPortal(
      state: RequestPortalState,
      action: PayloadAction<{}>
    ) {
      state.singleRequestPortal = null;
    }
  }
});

export const reducer = slice.reducer;

export const getRequestPortals =
  (criteria: {}): AppThunk =>
  async (dispatch) => {
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const requestPortals = await api.post<Page<RequestPortal>>(
        `${basePath}/search`,
        criteria
      );
      dispatch(slice.actions.getRequestPortals({ requestPortals }));
    } finally {
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };

export const getSingleRequestPortal =
  (id: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    const requestPortal = await api.get<RequestPortal>(`${basePath}/${id}`);
    dispatch(slice.actions.getSingleRequestPortal({ requestPortal }));
    dispatch(slice.actions.setLoadingGet({ loading: false }));
  };

export const editRequestPortal =
  (id: number, requestPortal: RequestPortalPostDTO): AppThunk =>
  async (dispatch) => {
    const requestPortalResponse = await api.patch<RequestPortal>(
      `${basePath}/${id}`,
      requestPortal
    );
    dispatch(
      slice.actions.editRequestPortal({ requestPortal: requestPortalResponse })
    );
  };

export const addRequestPortal =
  (requestPortal: RequestPortalPostDTO): AppThunk =>
  async (dispatch) => {
    const requestPortalResponse = await api.post<RequestPortal>(
      basePath,
      requestPortal
    );
    dispatch(
      slice.actions.addRequestPortal({ requestPortal: requestPortalResponse })
    );
  };
export const deleteRequestPortal =
  (id: number): AppThunk =>
  async (dispatch) => {
    const requestPortalResponse = await api.deletes<{ success: boolean }>(
      `${basePath}/${id}`
    );
    const { success } = requestPortalResponse;
    if (success) {
      dispatch(slice.actions.deleteRequestPortal({ id }));
    }
  };
export const clearSingleRequestPortal = (): AppThunk => async (dispatch) => {
  dispatch(slice.actions.clearSingleRequestPortal({}));
};

export const getRequestPortalPublic =
  (uuid: string): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    const requestPortal = await api.get<RequestPortal>(
      `${basePath}/public/${uuid}`
    );
    dispatch(slice.actions.getSingleRequestPortal({ requestPortal }));
    dispatch(slice.actions.setLoadingGet({ loading: false }));
  };

export default slice;
