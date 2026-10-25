import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import api from '../../utils/api';
import { MeterAnalytics, MeterOption } from '../../models/owns/analytics/meter';
import { revertAll } from 'src/utils/redux';

const basePath = 'analytics/meters';
const companyQuery = (companyId?: number) =>
  companyId ? `?companyId=${companyId}` : '';

interface MeterAnalyticsState {
  meters: MeterOption[];
  analytics: MeterAnalytics | null;
  loading: { meters: boolean; analytics: boolean };
}

const initialState: MeterAnalyticsState = {
  meters: [],
  analytics: null,
  loading: { meters: false, analytics: false }
};

const slice = createSlice({
  name: 'meterAnalytics',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    setMeters(
      state: MeterAnalyticsState,
      action: PayloadAction<MeterOption[]>
    ) {
      state.meters = action.payload;
    },
    setAnalytics(
      state: MeterAnalyticsState,
      action: PayloadAction<MeterAnalytics | null>
    ) {
      state.analytics = action.payload;
    },
    setLoading(
      state: MeterAnalyticsState,
      action: PayloadAction<{
        loading: boolean;
        operation: keyof MeterAnalyticsState['loading'];
      }>
    ) {
      const { loading, operation } = action.payload;
      state.loading[operation] = loading;
    }
  }
});

export const reducer = slice.reducer;

// also used to compare with the previous period (see usePreviousPeriod)
export const fetchMeterAnalytics = (
  meterId: number,
  start: Date,
  end: Date,
  companyId?: number
) =>
  api.post<MeterAnalytics>(`${basePath}/${meterId}${companyQuery(companyId)}`, {
    start,
    end
  });

export const getAnalyticsMeters =
  (companyId?: number): AppThunk =>
  async (dispatch) => {
    // the meters of another company must not be requested while the new list loads
    dispatch(slice.actions.setMeters([]));
    dispatch(slice.actions.setLoading({ operation: 'meters', loading: true }));
    try {
      const meters = await api.get<MeterOption[]>(
        `${basePath}${companyQuery(companyId)}`
      );
      dispatch(slice.actions.setMeters(meters));
    } finally {
      dispatch(
        slice.actions.setLoading({ operation: 'meters', loading: false })
      );
    }
  };

export const getMeterAnalytics =
  (meterId: number, start: Date, end: Date, companyId?: number): AppThunk =>
  async (dispatch) => {
    dispatch(
      slice.actions.setLoading({ operation: 'analytics', loading: true })
    );
    try {
      dispatch(
        slice.actions.setAnalytics(
          await fetchMeterAnalytics(meterId, start, end, companyId)
        )
      );
    } finally {
      dispatch(
        slice.actions.setLoading({ operation: 'analytics', loading: false })
      );
    }
  };

export const clearMeterAnalytics = (): AppThunk => async (dispatch) => {
  dispatch(slice.actions.setAnalytics(null));
};

export default slice;
