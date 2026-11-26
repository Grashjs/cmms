import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import { WebhookDeliveryLog } from '../models/owns/webhookDeliveryLog';
import {
  getInitialPage,
  Page,
  Pageable,
  pageableToQueryParams
} from '../models/owns/page';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';

const basePath = 'webhook-endpoints';

interface WebhookDeliveryLogState {
  logs: Page<WebhookDeliveryLog>;
  loadingGet: boolean;
}

const initialState: WebhookDeliveryLogState = {
  logs: getInitialPage<WebhookDeliveryLog>(),
  loadingGet: false
};

const slice = createSlice({
  name: 'webhookDeliveryLogs',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    setLogs(
      state: WebhookDeliveryLogState,
      action: PayloadAction<{ logs: Page<WebhookDeliveryLog> }>
    ) {
      state.logs = action.payload.logs;
    },
    clearLogs(state: WebhookDeliveryLogState) {
      state.logs = getInitialPage<WebhookDeliveryLog>();
    },
    setLoadingGet(
      state: WebhookDeliveryLogState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      state.loadingGet = action.payload.loading;
    }
  }
});

export const reducer = slice.reducer;

export const clearWebhookDeliveryLogs = (): AppThunk => (dispatch) => {
  dispatch(slice.actions.clearLogs());
};

export const getWebhookDeliveryLogs =
  (endpointId: number, pageable: Pageable): AppThunk =>
  async (dispatch) => {
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const logs = await api.get<Page<WebhookDeliveryLog>>(
        `${basePath}/${endpointId}/logs?${pageableToQueryParams(pageable)}`
      );
      dispatch(slice.actions.setLogs({ logs }));
    } finally {
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };

export const retryWebhookDeliveryLog =
  (logId: number): AppThunk =>
  async () => {
    return await api.post<WebhookDeliveryLog>(
      `${basePath}/logs/${logId}/retry`,
      {}
    );
  };

export default slice;
