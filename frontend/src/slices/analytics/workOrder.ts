import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import api from '../../utils/api';
import {
  IncompleteWOByAsset,
  IncompleteWOByUser,
  IncompleteWoStats,
  WOCostByDate,
  WOCostsAndTime,
  WOCountsByCategory,
  WOCountsByUser,
  WOCountsByWeek,
  WOHours,
  WoOverviewStats,
  WOStatsByPriority,
  WOStatsByStatus, WOStatusesByDate,
  WOTimeByWeek
} from '../../models/owns/analytics/workOrder';
import { revertAll } from 'src/utils/redux';

const basePath = 'analytics/work-orders';

interface WOStatstate {
  overview: WoOverviewStats;
  incompleteOverview: IncompleteWoStats;
  incompleteByPriority: WOStatsByPriority;
  incompleteByStatus: WOStatsByStatus;
  incompleteByAsset: IncompleteWOByAsset[];
  incompleteByUser: IncompleteWOByUser[];
  completeByPrimaryUser: WOCountsByUser[];
  completeByCompletedBy: WOCountsByUser[];
  completeByCategory: WOCountsByCategory[];
  completeByPriority: { [key: string]: number };
  completeByWeek: WOCountsByWeek[];
  completeTimesByWeek: WOTimeByWeek[];
  completeCosts: WOCostsAndTime;
  completeCostsByDate: WOCostByDate[];
  statusesByDate: WOStatusesByDate[];
  hours: WOHours;
  loading: Omit<Record<keyof WOStatstate, boolean>, 'loading'>;
}

type Operation = keyof WOStatstate;

const initialState: WOStatstate = {
  overview: {
    total: 0,
    complete: 0,
    compliant: 0,
    avgCycleTime: 0,
    mtta: 0
  },
  completeCosts: {
    total: 0,
    average: 0,
    additionalCost: 0,
    laborCost: 0,
    partCost: 0,
    laborTime: 0
  },
  incompleteByPriority: {
    none: {
      count: 0,
      estimatedHours: 0
    },
    high: {
      count: 0,
      estimatedHours: 0
    },
    medium: {
      count: 0,
      estimatedHours: 0
    },
    low: {
      count: 0,
      estimatedHours: 0
    }
  },
  incompleteByStatus: {
    complete: 0,
    inProgress: 0,
    onHold: 0,
    open: 0
  },
  hours: {
    estimated: 0,
    actual: 0
  },
  incompleteOverview: {
    total: 0,
    averageAge: 0
  },
  completeByPrimaryUser: [],
  completeByCompletedBy: [],
  completeByCategory: [],
  completeByPriority: {},
  completeByWeek: [],
  completeTimesByWeek: [],
  incompleteByAsset: [],
  incompleteByUser: [],
  completeCostsByDate: [],
  statusesByDate: [],
  loading: {
    overview: false,
    incompleteByPriority: false,
    incompleteByStatus: false,
    completeByPrimaryUser: false,
    completeByCompletedBy: false,
    completeByCategory: false,
    completeByPriority: false,
    completeCosts: false,
    completeByWeek: false,
    hours: false,
    completeTimesByWeek: false,
    incompleteOverview: false,
    incompleteByAsset: false,
    incompleteByUser: false,
    completeCostsByDate: false,
    statusesByDate: false
  }
};

const slice = createSlice({
  name: 'overviewStats',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getStats(
      state: WOStatstate,
      action: PayloadAction<{ overviewStats: WoOverviewStats }>
    ) {
      const { overviewStats } = action.payload;
      state.overview = overviewStats;
    },
    getIncompleteStats(
      state: WOStatstate,
      action: PayloadAction<{ overviewStats: IncompleteWoStats }>
    ) {
      const { overviewStats } = action.payload;
      state.incompleteOverview = overviewStats;
    },
    getIncompleteByPriority(
      state: WOStatstate,
      action: PayloadAction<{ stats: WOStatsByPriority }>
    ) {
      const { stats } = action.payload;
      state.incompleteByPriority = stats;
    },
    getIncompleteByStatus(
      state: WOStatstate,
      action: PayloadAction<{ stats: WOStatsByStatus }>
    ) {
      const { stats } = action.payload;
      state.incompleteByStatus = stats;
    },
    getIncompleteByAsset(
      state: WOStatstate,
      action: PayloadAction<{ stats: IncompleteWOByAsset[] }>
    ) {
      const { stats } = action.payload;
      state.incompleteByAsset = stats;
    },
    getIncompleteByUser(
      state: WOStatstate,
      action: PayloadAction<{ stats: IncompleteWOByUser[] }>
    ) {
      const { stats } = action.payload;
      state.incompleteByUser = stats;
    },
    getWOHours(state: WOStatstate, action: PayloadAction<{ stats: WOHours }>) {
      const { stats } = action.payload;
      state.hours = stats;
    },
    getCompleteCosts(
      state: WOStatstate,
      action: PayloadAction<{ stats: WOCostsAndTime }>
    ) {
      const { stats } = action.payload;
      state.completeCosts = stats;
    },
    getCountsByUser(
      state: WOStatstate,
      action: PayloadAction<{ stats: WOCountsByUser[] }>
    ) {
      const { stats } = action.payload;
      state.completeByPrimaryUser = stats;
    },
    getCountsByCompletedBy(
      state: WOStatstate,
      action: PayloadAction<{ stats: WOCountsByUser[] }>
    ) {
      const { stats } = action.payload;
      state.completeByCompletedBy = stats;
    },
    getCompleteByPriority(
      state: WOStatstate,
      action: PayloadAction<{ stats: { [key: string]: number } }>
    ) {
      const { stats } = action.payload;
      state.completeByPriority = stats;
    },
    getCompleteByWeek(
      state: WOStatstate,
      action: PayloadAction<{ stats: WOCountsByWeek[] }>
    ) {
      const { stats } = action.payload;
      state.completeByWeek = stats;
    },
    getCompleteCostsByDate(
      state: WOStatstate,
      action: PayloadAction<{ stats: WOCostByDate[] }>
    ) {
      const { stats } = action.payload;
      state.completeCostsByDate = stats;
    },
    getCompleteTimesByWeek(
      state: WOStatstate,
      action: PayloadAction<{ stats: WOTimeByWeek[] }>
    ) {
      const { stats } = action.payload;
      state.completeTimesByWeek = stats;
    },
    getCountsByCategory(
      state: WOStatstate,
      action: PayloadAction<{ stats: WOCountsByCategory[] }>
    ) {
      const { stats } = action.payload;
      state.completeByCategory = stats;
    },
    getStatusesByDate(
      state: WOStatstate,
      action: PayloadAction<{ stats: WOStatusesByDate[] }>
    ) {
      const { stats } = action.payload;
      state.statusesByDate = stats;
    },
    setLoading(
      state: WOStatstate,
      action: PayloadAction<{ loading: boolean; operation: Operation }>
    ) {
      const { loading, operation } = action.payload;
      state.loading[operation] = loading;
    }
  }
});

export const reducer = slice.reducer;

export const getOverviewStats = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(slice.actions.setLoading({ operation: 'overview', loading: true }));
  const overviewStats = await api.post<WoOverviewStats>(
    `${basePath}/complete/overview`, { start, end }
  );
  dispatch(slice.actions.getStats({ overviewStats }));
  dispatch(slice.actions.setLoading({ operation: 'overview', loading: false }));
};
export const getIncompleteStats = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(slice.actions.setLoading({ operation: 'overview', loading: true }));
  const overviewStats = await api.post<IncompleteWoStats>(
    `${basePath}/incomplete/overview`, { start, end }
  );
  dispatch(slice.actions.getIncompleteStats({ overviewStats }));
  dispatch(slice.actions.setLoading({ operation: 'overview', loading: false }));
};
export const getIncompleteByPriority = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'incompleteByPriority',
      loading: true
    })
  );
  const stats = await api.post<WOStatsByPriority>(
    `${basePath}/incomplete/priority`, { start, end }
  );
  dispatch(slice.actions.getIncompleteByPriority({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'incompleteByPriority',
      loading: false
    })
  );
};
export const getIncompleteByStatus = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'incompleteByStatus',
      loading: true
    })
  );
  const stats = await api.post<WOStatsByStatus>(
    `${basePath}/incomplete/statuses`, { start, end }
  );
  dispatch(slice.actions.getIncompleteByStatus({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'incompleteByStatus',
      loading: false
    })
  );
};
export const getWOHours = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'hours',
      loading: true
    })
  );
  const stats = await api.post<WOHours>(`${basePath}/hours`, { start, end });
  dispatch(slice.actions.getWOHours({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'hours',
      loading: false
    })
  );
};
export const getCountsByUser = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'completeByPrimaryUser',
      loading: true
    })
  );
  const stats = await api.post<WOCountsByUser[]>(
    `${basePath}/complete/counts/primaryUser`, { start, end }
  );
  dispatch(slice.actions.getCountsByUser({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'completeByPrimaryUser',
      loading: false
    })
  );
};

export const getCompleteByCompletedBy = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'completeByCompletedBy',
      loading: true
    })
  );
  const stats = await api.post<WOCountsByUser[]>(
    `${basePath}/complete/counts/completedBy`, { start, end }
  );
  dispatch(slice.actions.getCountsByCompletedBy({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'completeByCompletedBy',
      loading: false
    })
  );
};
export const getCompleteByPriority = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'completeByPriority',
      loading: true
    })
  );
  const stats = await api.post<{ [key: string]: number }>(
    `${basePath}/complete/counts/priority`, { start, end }
  );
  dispatch(slice.actions.getCompleteByPriority({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'completeByPriority',
      loading: false
    })
  );
};
export const getCompleteByCategory = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'completeByCategory',
      loading: true
    })
  );
  const stats = await api.post<WOCountsByCategory[]>(
    `${basePath}/complete/counts/category`, { start, end }
  );
  dispatch(slice.actions.getCountsByCategory({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'completeByCategory',
      loading: false
    })
  );
};
export const getCompleteCosts = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'completeCosts',
      loading: true
    })
  );
  const stats = await api.post<WOCostsAndTime>(
    `${basePath}/complete/costs-time`, { start, end }
  );
  dispatch(slice.actions.getCompleteCosts({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'completeCosts',
      loading: false
    })
  );
};
export const getCompleteByWeek = (): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'completeByWeek',
      loading: true
    })
  );
  const stats = await api.get<WOCountsByWeek[]>(
    `${basePath}/complete/counts/week`
  );
  dispatch(slice.actions.getCompleteByWeek({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'completeByWeek',
      loading: false
    })
  );
};
export const getCompleteTimesByWeek = (): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'completeTimesByWeek',
      loading: true
    })
  );
  const stats = await api.get<WOTimeByWeek[]>(`${basePath}/complete/time/week`);
  dispatch(slice.actions.getCompleteTimesByWeek({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'completeTimesByWeek',
      loading: false
    })
  );
};
export const getIncompleteByAsset = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'incompleteByAsset',
      loading: true
    })
  );
  const stats = await api.post<IncompleteWOByAsset[]>(
    `${basePath}/incomplete/age/assets`, { start, end }
  );
  dispatch(slice.actions.getIncompleteByAsset({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'incompleteByAsset',
      loading: false
    })
  );
};
export const getIncompleteByUser = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'incompleteByUser',
      loading: true
    })
  );
  const stats = await api.post<IncompleteWOByUser[]>(
    `${basePath}/incomplete/age/users`, { start, end }
  );
  dispatch(slice.actions.getIncompleteByUser({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'incompleteByUser',
      loading: false
    })
  );
};
export const getCompleteCostsByDate = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'completeCostsByDate',
      loading: true
    })
  );
  const stats = await api.post<WOCostByDate[]>(
    `${basePath}/complete/costs/date`, { start, end }
  );
  dispatch(slice.actions.getCompleteCostsByDate({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'completeCostsByDate',
      loading: false
    })
  );
};
export const getStatusesByDate = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'statusesByDate',
      loading: true
    })
  );
  const stats = await api.post<WOStatusesByDate[]>(
    `${basePath}/statuses`, { start, end }
  );
  dispatch(slice.actions.getStatusesByDate({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'statusesByDate',
      loading: false
    })
  );
};
export default slice;
