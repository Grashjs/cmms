import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import {
  WebhookEndpointShowDTO,
  WebhookEndpointPostDTO
} from '../models/owns/webhookEndpoint';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';

const basePath = 'webhook-endpoints';

interface WebhookEndpointState {
  webhookEndpoints: WebhookEndpointShowDTO[];
  loadingGet: boolean;
}

const initialState: WebhookEndpointState = {
  webhookEndpoints: [],
  loadingGet: false
};

const slice = createSlice({
  name: 'webhookEndpoints',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    setWebhookEndpoints(
      state: WebhookEndpointState,
      action: PayloadAction<{ endpoints: WebhookEndpointShowDTO[] }>
    ) {
      state.webhookEndpoints = action.payload.endpoints;
    },
    addWebhookEndpoint(
      state: WebhookEndpointState,
      action: PayloadAction<{ endpoint: WebhookEndpointShowDTO }>
    ) {
      state.webhookEndpoints = [
        action.payload.endpoint,
        ...state.webhookEndpoints
      ];
    },
    removeWebhookEndpoint(
      state: WebhookEndpointState,
      action: PayloadAction<{ id: number }>
    ) {
      state.webhookEndpoints = state.webhookEndpoints.filter(
        (e) => e.id !== action.payload.id
      );
    },
    setLoadingGet(
      state: WebhookEndpointState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      state.loadingGet = action.payload.loading;
    },
    rotateSecretInState(
      state: WebhookEndpointState,
      action: PayloadAction<{ id: number; newSecret: string }>
    ) {
      const endpoint = state.webhookEndpoints.find(
        (e) => e.id === action.payload.id
      );
      if (endpoint) {
        endpoint.secret = action.payload.newSecret;
      }
    }
  }
});

export const reducer = slice.reducer;

export const getWebhookEndpoints = (): AppThunk => async (dispatch) => {
  try {
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    const endpoints = await api.get<WebhookEndpointShowDTO[]>(basePath);
    dispatch(slice.actions.setWebhookEndpoints({ endpoints }));
  } finally {
    dispatch(slice.actions.setLoadingGet({ loading: false }));
  }
};

export const addWebhookEndpoint =
  (data: WebhookEndpointPostDTO): AppThunk =>
  async (dispatch) => {
    const endpoint = await api.post<WebhookEndpointShowDTO>(basePath, data);
    dispatch(slice.actions.addWebhookEndpoint({ endpoint }));
    return endpoint;
  };

export const deleteWebhookEndpoint =
  (id: number): AppThunk =>
  async (dispatch) => {
    const result = await api.deletes<{ success: boolean }>(
      `${basePath}/${id}`
    );
    if (result.success) {
      dispatch(slice.actions.removeWebhookEndpoint({ id }));
    }
  };

export const rotateSecret =
  (id: number): AppThunk =>
  async (dispatch) => {
    const result = await api.patch<{ success: boolean; message: string }>(
      `${basePath}/${id}/rotate-secret`,
      {}
    );
    if (result.success) {
      dispatch(
        slice.actions.rotateSecretInState({
          id,
          newSecret: result.message
        })
      );
      return result.message;
    }
    return null;
  };

export default slice;
