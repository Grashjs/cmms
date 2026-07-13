import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import api from '../../utils/api';
import {
  CountByCategory,
  RequestsByDate, RequestsResolvedByDate,
  RequestStats,
  RequestStatsByPriority
} from '../../models/owns/analytics/request';
import { revertAll } from 'src/utils/redux';

const basePath = 'analytics/requests';

interface RequestStatstate {
  overview: RequestStats;
  requestsByDate: RequestsByDate[];
  requestsResolvedByDate: RequestsResolvedByDate[];
  requestsByCategory: CountByCategory[];
  requestStatsByPriority: RequestStatsByPriority;
  loading: Omit<Record<keyof RequestStatstate, boolean>, 'loading'>;
}

type Operation = keyof RequestStatstate;

const initialState: RequestStatstate = {
  overview: {
    approved: 0,
    pending: 0,
    cancelled: 0,
    cycleTime: 0
  },
  requestStatsByPriority: {
    none: { count: 0 },
    low: { count: 0 },
    medium: { count: 0 },
    high: { count: 0 }
  },
  requestsByDate: [],
  requestsResolvedByDate: [],
  requestsByCategory: [],
  loading: {
    overview: false,
    requestsByDate: false,
    requestStatsByPriority: false,
    requestsResolvedByDate: false,
    requestsByCategory: false
  }
};

const slice = createSlice({
  name: 'overviewStats',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getOverview(
      state: RequestStatstate,
      action: PayloadAction<{ stats: RequestStats }>
    ) {
      const { stats } = action.payload;
      state.overview = stats;
    },
    getRequestStatsByPriority(
      state: RequestStatstate,
      action: PayloadAction<{ stats: RequestStatsByPriority }>
    ) {
      const { stats } = action.payload;
      state.requestStatsByPriority = stats;
    },
    getRequestsByDate(
      state: RequestStatstate,
      action: PayloadAction<{ stats: RequestsByDate[] }>
    ) {
      const { stats } = action.payload;
      state.requestsByDate = stats;
    },
    getRequestsResolvedByDate(
      state: RequestStatstate,
      action: PayloadAction<{ stats: RequestsResolvedByDate[] }>
    ) {
      const { stats } = action.payload;
      state.requestsResolvedByDate = stats;
    },
    getRequestsByCategory(
      state: RequestStatstate,
      action: PayloadAction<{ stats: CountByCategory[] }>
    ) {
      const { stats } = action.payload;
      state.requestsByCategory = stats;
    },
    setLoading(
      state: RequestStatstate,
      action: PayloadAction<{ loading: boolean; operation: Operation }>
    ) {
      const { loading, operation } = action.payload;
      state.loading[operation] = loading;
    }
  }
});

export const reducer = slice.reducer;

export const getRequestOverview = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'overview',
      loading: true
    })
  );
  const stats = await api.post<RequestStats>(`${basePath}/overview`, { start, end });
  dispatch(slice.actions.getOverview({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'overview',
      loading: false
    })
  );
};
export const getRequestStatsByPriority = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'requestStatsByPriority',
      loading: true
    })
  );
  const stats = await api.post<RequestStatsByPriority>(`${basePath}/priority`, { start, end });
  dispatch(slice.actions.getRequestStatsByPriority({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'requestStatsByPriority',
      loading: false
    })
  );
};

export const getRequestsByDate = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'requestsByDate',
      loading: true
    })
  );
  const stats = await api.post<RequestsByDate[]>(
    `${basePath}/cycle-time/date`, { start, end }
  );
  dispatch(slice.actions.getRequestsByDate({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'requestsByDate',
      loading: false
    })
  );
};
export const getRequestsResolvedByDate = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'requestsResolvedByDate',
      loading: true
    })
  );
  const stats = await api.post<RequestsResolvedByDate[]>(`${basePath}/received-and-resolved`, { start, end });
  dispatch(slice.actions.getRequestsResolvedByDate({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'requestsResolvedByDate',
      loading: false
    })
  );
};

export const getRequestsByCategory = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'requestsByCategory',
      loading: true
    })
  );
  const stats = await api.post<CountByCategory[]>(`${basePath}/counts/category`, { start, end });
  dispatch(slice.actions.getRequestsByCategory({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'requestsByCategory',
      loading: false
    })
  );
};
export default slice;
