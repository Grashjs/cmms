import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import type { FilterField } from 'src/models/owns/page';
import api from '../../utils/api';
import {
  AssetAvailability,
  MaintenanceKpis,
  MaintenanceKpisByDate
} from '../../models/owns/analytics/kpi';
import { revertAll } from 'src/utils/redux';

const companyQuery = (companyId?: number) =>
  companyId ? `?companyId=${companyId}` : '';

interface KpiState {
  kpis: MaintenanceKpis;
  trend: MaintenanceKpisByDate[];
  availability: AssetAvailability[];
  loading: Omit<Record<keyof KpiState, boolean>, 'loading'>;
}

type Operation = keyof Omit<KpiState, 'loading'>;

const initialState: KpiState = {
  kpis: {
    asOf: null,
    pmDue: 0,
    pmCompletedOnTime: 0,
    pmCompliance: null,
    planned: 0,
    reactive: 0,
    plannedRatio: null,
    backlog: 0,
    backlogHours: 0,
    overdue: 0,
    byPriority: []
  },
  trend: [],
  availability: [],
  loading: {
    kpis: false,
    trend: false,
    availability: false
  }
};

const slice = createSlice({
  name: 'kpiAnalytics',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    setKpis(state: KpiState, action: PayloadAction<MaintenanceKpis>) {
      state.kpis = action.payload;
    },
    setTrend(state: KpiState, action: PayloadAction<MaintenanceKpisByDate[]>) {
      state.trend = action.payload;
    },
    setAvailability(
      state: KpiState,
      action: PayloadAction<AssetAvailability[]>
    ) {
      state.availability = action.payload;
    },
    setLoading(
      state: KpiState,
      action: PayloadAction<{ loading: boolean; operation: Operation }>
    ) {
      const { loading, operation } = action.payload;
      state.loading[operation] = loading;
    }
  }
});

export const reducer = slice.reducer;

const fetcher =
  <T>(path: string) =>
  (
    start: Date,
    end: Date,
    companyId?: number,
    filterFields: FilterField[] = []
  ) =>
    api.post<T>(`${path}${companyQuery(companyId)}`, {
      start,
      end,
      filterFields
    });

// also used to compare with the previous period (see usePreviousPeriod)
export const fetchMaintenanceKpis = fetcher<MaintenanceKpis>(
  'analytics/work-orders/kpis'
);

const load =
  <T>(
    operation: Operation,
    path: string,
    setter: (payload: T) => PayloadAction<T>
  ) =>
  (
    start: Date,
    end: Date,
    companyId?: number,
    filterFields: FilterField[] = []
  ): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoading({ operation, loading: true }));
    try {
      const result = await fetcher<T>(path)(
        start,
        end,
        companyId,
        filterFields
      );
      dispatch(setter(result));
    } finally {
      dispatch(slice.actions.setLoading({ operation, loading: false }));
    }
  };

export const getMaintenanceKpis = load<MaintenanceKpis>(
  'kpis',
  'analytics/work-orders/kpis',
  slice.actions.setKpis
);
export const getMaintenanceKpisTrend = load<MaintenanceKpisByDate[]>(
  'trend',
  'analytics/work-orders/kpis/trend',
  slice.actions.setTrend
);
export const getAvailabilityByAsset = load<AssetAvailability[]>(
  'availability',
  'analytics/assets/availability',
  slice.actions.setAvailability
);

export default slice;
