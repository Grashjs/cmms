import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import { ApiKey, ApiKeyPostDTO, ApiKeyCriteria } from '../models/owns/apiKey';
import api from '../utils/api';
import {
  getInitialPage,
  Page,
  Pageable,
  pageableToQueryParams,
  SearchCriteria
} from '../models/owns/page';
import { revertAll } from 'src/utils/redux';

const basePath = 'api-keys';

interface ApiKeyState {
  apiKeys: Page<ApiKey>;
  singleApiKey: ApiKey;
  loadingGet: boolean;
}

const initialState: ApiKeyState = {
  apiKeys: getInitialPage<ApiKey>(),
  singleApiKey: null,
  loadingGet: false
};

const slice = createSlice({
  name: 'apiKeys',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getApiKeys(
      state: ApiKeyState,
      action: PayloadAction<{ apiKeys: Page<ApiKey> }>
    ) {
      const { apiKeys } = action.payload;
      state.apiKeys = apiKeys;
    },
    getSingleApiKey(
      state: ApiKeyState,
      action: PayloadAction<{ apiKey: ApiKey }>
    ) {
      const { apiKey } = action.payload;
      state.singleApiKey = apiKey;
    },
    addApiKey(state: ApiKeyState, action: PayloadAction<{ apiKey: ApiKey }>) {
      const { apiKey } = action.payload;
      state.apiKeys.content = [apiKey, ...state.apiKeys.content];
    },
    deleteApiKey(state: ApiKeyState, action: PayloadAction<{ id: number }>) {
      const { id } = action.payload;
      const apiKeyIndex = state.apiKeys.content.findIndex(
        (apiKey) => apiKey.id === id
      );
      state.apiKeys.content.splice(apiKeyIndex, 1);
    },
    setLoadingGet(
      state: ApiKeyState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    },
    clearSingleApiKey(state: ApiKeyState, action: PayloadAction<{}>) {
      state.singleApiKey = null;
    }
  }
});

export const reducer = slice.reducer;

export const getApiKeys =
  ({}, pageable: Pageable): AppThunk =>
  async (dispatch) => {
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const apiKeys = await api.post<Page<ApiKey>>(
        `${basePath}/search?${pageableToQueryParams(pageable)}`,
        {}
      );
      dispatch(slice.actions.getApiKeys({ apiKeys }));
    } finally {
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };

export const getSingleApiKey =
  (id: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    const apiKey = await api.get<ApiKey>(`${basePath}/${id}`);
    dispatch(slice.actions.getSingleApiKey({ apiKey }));
    dispatch(slice.actions.setLoadingGet({ loading: false }));
  };

export const addApiKey =
  (apiKey: ApiKeyPostDTO): AppThunk =>
  async (dispatch) => {
    const apiKeyResponse = await api.post<ApiKey>(basePath, apiKey);
    dispatch(slice.actions.addApiKey({ apiKey: apiKeyResponse }));
    return apiKeyResponse;
  };

export const deleteApiKey =
  (id: number): AppThunk =>
  async (dispatch) => {
    const apiKeyResponse = await api.deletes<{ success: boolean }>(
      `${basePath}/${id}`
    );
    const { success } = apiKeyResponse;
    if (success) {
      dispatch(slice.actions.deleteApiKey({ id }));
    }
  };

export const clearSingleApiKey = (): AppThunk => async (dispatch) => {
  dispatch(slice.actions.clearSingleApiKey({}));
};

export default slice;
