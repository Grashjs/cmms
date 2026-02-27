import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import WorkOrderHistory from '../models/owns/workOrderHistories';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';

const basePath = 'work-order-histories';
interface WorkOrderHistoriestate {
  workOrderHistories: { [id: number]: WorkOrderHistory[] };
}

const initialState: WorkOrderHistoriestate = {
  workOrderHistories: {}
};

const slice = createSlice({
  name: 'workOrderHistories',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getWorkOrderHistories(
      state: WorkOrderHistoriestate,
      action: PayloadAction<{
        id: number;
        workOrderHistories: WorkOrderHistory[];
      }>
    ) {
      const { workOrderHistories, id } = action.payload;
      state.workOrderHistories[id] = workOrderHistories;
    }
  }
});

export const reducer = slice.reducer;

export const getWorkOrderHistories =
  (id: number): AppThunk =>
  async (dispatch) => {
    const workOrderHistories = await api.get<WorkOrderHistory[]>(
      `${basePath}/work-order/${id}`
    );
    dispatch(slice.actions.getWorkOrderHistories({ id, workOrderHistories }));
  };

export default slice;
