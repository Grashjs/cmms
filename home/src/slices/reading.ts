import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import Reading from '../models/owns/reading';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';

const basePath = 'readings';
interface ReadingState {
  readingsByMeter: { [id: number]: Reading[] };
  loadingGet: boolean;
}

const initialState: ReadingState = {
  readingsByMeter: {},
  loadingGet: false
};

const slice = createSlice({
  name: 'readings',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getReadings(
      state: ReadingState,
      action: PayloadAction<{ id: number; readings: Reading[] }>
    ) {
      const { readings, id } = action.payload;
      state.readingsByMeter[id] = readings;
    },
    createReading(
      state: ReadingState,
      action: PayloadAction<{
        meterId: number;
        reading: Reading;
      }>
    ) {
      const { reading, meterId } = action.payload;
      if (state.readingsByMeter[meterId]) {
        state.readingsByMeter[meterId].push(reading);
      } else state.readingsByMeter[meterId] = [reading];
    },
    deleteReading(
      state: ReadingState,
      action: PayloadAction<{
        meterId: number;
        id: number;
      }>
    ) {
      const { id, meterId } = action.payload;
      state.readingsByMeter[meterId] = state.readingsByMeter[meterId].filter(
        (reading) => reading.id !== id
      );
    },
    setLoadingGet(
      state: ReadingState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    }
  }
});

export const reducer = slice.reducer;

export const getReadings =
  (id: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    const readings = await api.get<Reading[]>(`${basePath}/meter/${id}`);
    dispatch(slice.actions.getReadings({ id, readings }));
    dispatch(slice.actions.setLoadingGet({ loading: false }));
  };

export const createReading =
  (id: number, reading: Partial<Reading>): AppThunk =>
  async (dispatch) => {
    const readingResponse = await api.post<Reading>(`${basePath}`, {
      ...reading,
      meter: { id }
    });
    dispatch(
      slice.actions.createReading({
        meterId: id,
        reading: readingResponse
      })
    );
  };

export const deleteReading =
  (meterId: number, id: number): AppThunk =>
  async (dispatch) => {
    const response = await api.deletes<{ success: boolean }>(
      `${basePath}/${id}`
    );
    const { success } = response;
    if (success) {
      dispatch(slice.actions.deleteReading({ meterId, id }));
    }
  };

export default slice;
