import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { revertAll } from '../utils/redux';

interface OfflineQueueState {
  pending: number;
  flushing: boolean;
}

const initialState: OfflineQueueState = {
  pending: 0,
  flushing: false
};

export const offlineQueueSlice = createSlice({
  name: 'offlineQueue',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    setPending(state, action: PayloadAction<{ pending: number }>) {
      state.pending = action.payload.pending;
    },
    setFlushing(state, action: PayloadAction<{ flushing: boolean }>) {
      state.flushing = action.payload.flushing;
    }
  }
});

export const reducer = offlineQueueSlice.reducer;
