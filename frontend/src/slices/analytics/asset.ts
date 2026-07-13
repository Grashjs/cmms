import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import api from '../../utils/api';
import {
  AssetOverview,
  AssetOverviewStats,
  AssetsCost,
  DowntimesAndCostsByAsset,
  DowntimesByAsset,
  DowntimesByDate,
  DowntimesMeantimeByDate,
  Meantimes, MTBFByAsset,
  RepairTimeByAsset,
  TimeCostByAsset
} from '../../models/owns/analytics/asset';
import { revertAll } from 'src/utils/redux';

const basePath = 'analytics/assets';

interface AssetStatstate {
  overview: AssetOverviewStats;
  completeTimeCostByAsset: TimeCostByAsset[];
  downtimesByAsset: DowntimesByAsset[];
  assetsCosts: AssetsCost;
  downtimesAndCostsByAsset: DowntimesAndCostsByAsset[];
  downtimesByDate: DowntimesByDate[];
  downtimesMeantimeByDate: DowntimesMeantimeByDate[];
  meantimes: Meantimes;
  repairTimeByAsset: RepairTimeByAsset[];
  assetDetailsOverview: AssetOverview;
  mtbfByAsset: MTBFByAsset[];
  loading: Omit<Record<keyof AssetStatstate, boolean>, 'loading'>;
}

type Operation = keyof AssetStatstate;

const initialState: AssetStatstate = {
  completeTimeCostByAsset: [],
  overview: {
    downtime: 0,
    availability: 100,
    downtimeEvents: 0
  },
  downtimesByAsset: [],
  assetsCosts: {
    rav: 0,
    totalWOCosts: 0,
    totalAcquisitionCost: 0
  },
  downtimesAndCostsByAsset: [],
  downtimesByDate: [],
  downtimesMeantimeByDate: [],
  meantimes: { betweenDowntimes: 0, betweenMaintenances: 0 },
  repairTimeByAsset: [],
  assetDetailsOverview: {
    mtbf: 0,
    mttr: 0,
    downtime: 0,
    uptime: 0,
    totalCost: 0
  },
  mtbfByAsset: [],
  loading: {
    completeTimeCostByAsset: false,
    overview: false,
    downtimesByAsset: false,
    assetsCosts: false,
    downtimesAndCostsByAsset: false,
    downtimesByDate: false,
    downtimesMeantimeByDate: false,
    meantimes: false,
    repairTimeByAsset: false,
    assetDetailsOverview: false,
    mtbfByAsset: false
  }
};

const slice = createSlice({
  name: 'overviewStats',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getWOTimeCostByAsset(
      state: AssetStatstate,
      action: PayloadAction<{ stats: TimeCostByAsset[] }>
    ) {
      const { stats } = action.payload;
      state.completeTimeCostByAsset = stats;
    },
    getOverview(
      state: AssetStatstate,
      action: PayloadAction<{ stats: AssetOverviewStats }>
    ) {
      const { stats } = action.payload;
      state.overview = stats;
    },
    getAssetsCosts(
      state: AssetStatstate,
      action: PayloadAction<{ stats: AssetsCost }>
    ) {
      const { stats } = action.payload;
      state.assetsCosts = stats;
    },
    getMeantimes(
      state: AssetStatstate,
      action: PayloadAction<{ stats: Meantimes }>
    ) {
      const { stats } = action.payload;
      state.meantimes = stats;
    },
    getDowntimesByAsset(
      state: AssetStatstate,
      action: PayloadAction<{ stats: DowntimesByAsset[] }>
    ) {
      const { stats } = action.payload;
      state.downtimesByAsset = stats;
    },
    getMTBFByAsset(
      state: AssetStatstate,
      action: PayloadAction<{ stats: MTBFByAsset[] }>
    ) {
      const { stats } = action.payload;
      state.mtbfByAsset = stats;
    },
    getDowntimesAndCostsByAsset(
      state: AssetStatstate,
      action: PayloadAction<{ stats: DowntimesAndCostsByAsset[] }>
    ) {
      const { stats } = action.payload;
      state.downtimesAndCostsByAsset = stats;
    },
    getDowntimesByDate(
      state: AssetStatstate,
      action: PayloadAction<{ stats: DowntimesByDate[] }>
    ) {
      const { stats } = action.payload;
      state.downtimesByDate = stats;
    },
    getDowntimesMeantimeByDate(
      state: AssetStatstate,
      action: PayloadAction<{ stats: DowntimesMeantimeByDate[] }>
    ) {
      const { stats } = action.payload;
      state.downtimesMeantimeByDate = stats;
    },
    getRepairTimeByAsset(
      state: AssetStatstate,
      action: PayloadAction<{ stats: RepairTimeByAsset[] }>
    ) {
      const { stats } = action.payload;
      state.repairTimeByAsset = stats;
    },
    getAssetDetailsOverview(
      state: AssetStatstate,
      action: PayloadAction<{ stats: AssetOverview }>
    ) {
      const { stats } = action.payload;
      state.assetDetailsOverview = stats;
    },
    setLoading(
      state: AssetStatstate,
      action: PayloadAction<{ loading: boolean; operation: Operation }>
    ) {
      const { loading, operation } = action.payload;
      state.loading[operation] = loading;
    }
  }
});

export const reducer = slice.reducer;

export const getWOTimeCostByAsset = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'completeTimeCostByAsset',
      loading: true
    })
  );
  const stats = await api.post<TimeCostByAsset[]>(`${basePath}/time-cost`, { start, end });
  dispatch(slice.actions.getWOTimeCostByAsset({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'completeTimeCostByAsset',
      loading: false
    })
  );
};
export const getAssetOverview = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'overview',
      loading: true
    })
  );
  const stats = await api.post<AssetOverviewStats>(`${basePath}/overview`, { start, end });
  dispatch(slice.actions.getOverview({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'overview',
      loading: false
    })
  );
};
export const getAssetDetailsOverview = (id: number, start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'assetDetailsOverview',
      loading: true
    })
  );
  const stats = await api.post<AssetOverview>(`${basePath}/${id}/overview`, { start, end });
  dispatch(slice.actions.getAssetDetailsOverview({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'assetDetailsOverview',
      loading: false
    })
  );
};
export const getAssetsCosts = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'assetsCosts',
      loading: true
    })
  );
  const stats = await api.post<AssetsCost>(`${basePath}/costs/overview`, { start, end });
  dispatch(slice.actions.getAssetsCosts({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'assetsCosts',
      loading: false
    })
  );
};
export const getMeantimes = (): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'meantimes',
      loading: true
    })
  );
  const stats = await api.get<Meantimes>(`${basePath}/meantimes`);
  dispatch(slice.actions.getMeantimes({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'meantimes',
      loading: false
    })
  );
};
export const getDowntimesByAsset = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'downtimesByAsset',
      loading: true
    })
  );
  const stats = await api.post<DowntimesByAsset[]>(`${basePath}/downtimes`, { start, end });
  dispatch(slice.actions.getDowntimesByAsset({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'downtimesByAsset',
      loading: false
    })
  );
};
export const getMTBFByAsset = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'mtbfByAsset',
      loading: true
    })
  );
  const stats = await api.post<MTBFByAsset[]>(`${basePath}/mtbf`, { start, end });
  dispatch(slice.actions.getMTBFByAsset({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'mtbfByAsset',
      loading: false
    })
  );
};
export const getDowntimesAndCostsByAsset = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'downtimesAndCostsByAsset',
      loading: true
    })
  );
  const stats = await api.post<DowntimesAndCostsByAsset[]>(
    `${basePath}/downtimes/costs`, { start, end }
  );
  dispatch(slice.actions.getDowntimesAndCostsByAsset({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'downtimesAndCostsByAsset',
      loading: false
    })
  );
};
export const getDowntimesByDate = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'downtimesByDate',
      loading: true
    })
  );
  const stats = await api.post<DowntimesByDate[]>(
    `${basePath}/downtimes/costs/date`, { start, end }
  );
  dispatch(slice.actions.getDowntimesByDate({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'downtimesByDate',
      loading: false
    })
  );
};
export const getDowntimesMeantimeByDate = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'downtimesMeantimeByDate',
      loading: true
    })
  );
  const stats = await api.post<DowntimesMeantimeByDate[]>(
    `${basePath}/downtimes/meantime/date`, { start, end }
  );
  dispatch(slice.actions.getDowntimesMeantimeByDate({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'downtimesMeantimeByDate',
      loading: false
    })
  );
};
export const getRepairTimeByAsset = (start: Date, end: Date): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'repairTimeByAsset',
      loading: true
    })
  );
  const stats = await api.post<RepairTimeByAsset[]>(`${basePath}/repair-times`, { start, end });
  dispatch(slice.actions.getRepairTimeByAsset({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'repairTimeByAsset',
      loading: false
    })
  );
};
export default slice;
