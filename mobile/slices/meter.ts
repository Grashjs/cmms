import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import { getInitialPage, Page, SearchCriteria } from '../models/page';
import type { AppThunk } from '../store';
import Meter, { MeterMiniDTO } from '../models/meter';
import api from '../utils/api';
import { revertAll } from '../utils/redux';

const basePath = 'meters';

interface MeterState {
  meterInfos: { [key: number]: { meter?: Meter } };
  meters: Page<Meter>;
  metersByAsset: { [id: number]: Meter[] };
  currentPageNum: number;
  lastPage: boolean;
  metersMini: MeterMiniDTO[];
  loadingGet: boolean;
}

const initialState: MeterState = {
  meters: getInitialPage<Meter>(),
  meterInfos: {},
  metersByAsset: {},
  currentPageNum: 0,
  lastPage: true,
  metersMini: [],
  loadingGet: false
};

const slice = createSlice({
  name: 'meters',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getMeters(
      state: MeterState,
      action: PayloadAction<{ meters: Page<Meter> }>
    ) {
      const { meters } = action.payload;
      state.meters = meters;
      state.currentPageNum = 0;
      state.lastPage = meters.last;
    },
    getMoreMeters(
      state: MeterState,
      action: PayloadAction<{ meters: Page<Meter> }>
    ) {
      const { meters } = action.payload;
      state.meters.content = state.meters.content.concat(meters.content);
      state.currentPageNum = state.currentPageNum + 1;
      state.lastPage = meters.last;
    },
    getMetersMini(
      state: MeterState,
      action: PayloadAction<{ meters: MeterMiniDTO[] }>
    ) {
      const { meters } = action.payload;
      state.metersMini = meters;
    },
    getMeterDetails(
      state: MeterState,
      action: PayloadAction<{ meter; id: number }>
    ) {
      const { meter, id } = action.payload;
      if (state.meterInfos[id]) {
        state.meterInfos[id] = { ...state.meterInfos[id], meter };
      } else state.meterInfos[id] = { meter };
    },
    getMetersByAsset(
      state: MeterState,
      action: PayloadAction<{ id: number; meters: Meter[] }>
    ) {
      const { meters, id } = action.payload;
      state.metersByAsset[id] = meters;
    },
    addMeter(state: MeterState, action: PayloadAction<{ meter: Meter }>) {
      const { meter } = action.payload;
      state.meters.content = [...state.meters.content, meter];
    },
    editMeter(state: MeterState, action: PayloadAction<{ meter: Meter }>) {
      const { meter } = action.payload;
      if (state.meterInfos[meter.id]) {
        state.meterInfos[meter.id].meter = meter;
      } else state.meterInfos[meter.id] = { meter };
      const inContent = state.meters.content.some(
        (meter1) => meter1.id === meter.id
      );
      if (inContent) {
        state.meters.content = state.meters.content.map((meter1) => {
          if (meter1.id === meter.id) {
            return meter;
          }
          return meter1;
        });
      }
    },
    deleteMeter(state: MeterState, action: PayloadAction<{ id: number }>) {
      const { id } = action.payload;
      const meterIndex = state.meters.content.findIndex(
        (meter) => meter.id === id
      );
      state.meters.content.splice(meterIndex, 1);
    },
    setLoadingGet(
      state: MeterState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    }
  }
});

export const reducer = slice.reducer;

export const getMeters =
  (criteria: SearchCriteria): AppThunk =>
  async (dispatch) => {
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const meters = await api.post<Page<Meter>>(
        `${basePath}/search`,
        criteria
      );
      dispatch(slice.actions.getMeters({ meters }));
    } finally {
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };
export const getMoreMeters =
  (criteria: SearchCriteria, pageNum: number): AppThunk =>
  async (dispatch) => {
    criteria = { ...criteria, pageNum };
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const meters = await api.post<Page<Meter>>(
        `${basePath}/search`,
        criteria
      );
      dispatch(slice.actions.getMoreMeters({ meters }));
    } finally {
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };
export const getMetersMini = (): AppThunk => async (dispatch) => {
  const meters = await api.get<MeterMiniDTO[]>(`${basePath}/mini`);
  dispatch(slice.actions.getMetersMini({ meters }));
};
export const getMeterDetails =
  (id: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    const meter = await api.get<Meter>(`${basePath}/${id}`);
    dispatch(
      slice.actions.getMeterDetails({
        id,
        meter
      })
    );
    dispatch(slice.actions.setLoadingGet({ loading: false }));
  };
export const editMeter =
  (id: number, meter): AppThunk =>
  async (dispatch) => {
    const meterResponse = await api.patch<Meter>(`${basePath}/${id}`, meter);
    dispatch(slice.actions.editMeter({ meter: meterResponse }));
  };

export const addMeter =
  (meter): AppThunk =>
  async (dispatch) => {
    const meterResponse = await api.post<Meter>(basePath, meter);
    dispatch(slice.actions.addMeter({ meter: meterResponse }));
  };

export const deleteMeter =
  (id: number): AppThunk =>
  async (dispatch) => {
    const meterResponse = await api.deletes<{ success: boolean }>(
      `${basePath}/${id}`
    );
    const { success } = meterResponse;
    if (success) {
      dispatch(slice.actions.deleteMeter({ id }));
    }
  };

export const getMetersByAsset =
  (id: number): AppThunk =>
  async (dispatch) => {
    const meters = await api.get<Meter[]>(`${basePath}/asset/${id}`);
    dispatch(slice.actions.getMetersByAsset({ id, meters }));
  };

export default slice;
