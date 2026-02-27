import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import AssetDowntime from '../models/owns/assetDowntime';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';

const basePath = 'asset-downtimes';
interface AssetDowntimeState {
  assetDowntimesByAsset: { [id: number]: AssetDowntime[] };
}

const initialState: AssetDowntimeState = {
  assetDowntimesByAsset: {}
};

const slice = createSlice({
  name: 'AssetDowntimes',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getAssetDowntimes(
      state: AssetDowntimeState,
      action: PayloadAction<{
        id: number;
        AssetDowntimes: AssetDowntime[];
      }>
    ) {
      const { AssetDowntimes, id } = action.payload;
      state.assetDowntimesByAsset[id] = AssetDowntimes;
    },
    createAssetDowntime(
      state: AssetDowntimeState,
      action: PayloadAction<{
        assetId: number;
        AssetDowntime: AssetDowntime;
      }>
    ) {
      const { AssetDowntime, assetId } = action.payload;
      if (state.assetDowntimesByAsset[assetId]) {
        state.assetDowntimesByAsset[assetId].push(AssetDowntime);
      } else state.assetDowntimesByAsset[assetId] = [AssetDowntime];
    },
    editAssetDowntime(
      state: AssetDowntimeState,
      action: PayloadAction<{
        id: number;
        assetId: number;
        AssetDowntime: AssetDowntime;
      }>
    ) {
      const { AssetDowntime, assetId, id } = action.payload;
      state.assetDowntimesByAsset[assetId] = state.assetDowntimesByAsset[
        assetId
      ].map((addAssetDowntime) => {
        if (addAssetDowntime.id === id) {
          return AssetDowntime;
        }
        return addAssetDowntime;
      });
    },
    deleteAssetDowntime(
      state: AssetDowntimeState,
      action: PayloadAction<{
        assetId: number;
        id: number;
      }>
    ) {
      const { id, assetId } = action.payload;
      state.assetDowntimesByAsset[assetId] = state.assetDowntimesByAsset[
        assetId
      ].filter((AssetDowntime) => AssetDowntime.id !== id);
    }
  }
});

export const reducer = slice.reducer;

export const getAssetDowntimes =
  (id: number): AppThunk =>
  async (dispatch) => {
    const AssetDowntimes = await api.get<AssetDowntime[]>(
      `${basePath}/asset/${id}`
    );
    dispatch(
      slice.actions.getAssetDowntimes({
        id,
        AssetDowntimes
      })
    );
  };

export const createAssetDowntime =
  (id: number, AssetDowntime: Partial<AssetDowntime>): AppThunk =>
  async (dispatch) => {
    const AssetDowntimeResponse = await api.post<AssetDowntime>(`${basePath}`, {
      ...AssetDowntime,
      asset: { id }
    });
    dispatch(
      slice.actions.createAssetDowntime({
        assetId: id,
        AssetDowntime: AssetDowntimeResponse
      })
    );
  };

export const editAssetDowntime =
  (id: number, assetId: number, AssetDowntime: AssetDowntime): AppThunk =>
  async (dispatch) => {
    const AssetDowntimeResponse = await api.patch<AssetDowntime>(
      `${basePath}/${id}`,
      AssetDowntime
    );
    dispatch(
      slice.actions.editAssetDowntime({
        assetId,
        id,
        AssetDowntime: AssetDowntimeResponse
      })
    );
  };
export const deleteAssetDowntime =
  (assetId: number, id: number): AppThunk =>
  async (dispatch) => {
    const response = await api.deletes<{ success: boolean }>(
      `${basePath}/${id}`
    );
    const { success } = response;
    if (success) {
      dispatch(slice.actions.deleteAssetDowntime({ assetId, id }));
    }
  };

export default slice;
