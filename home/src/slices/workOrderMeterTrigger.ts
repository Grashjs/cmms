import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import WorkOrderMeterTrigger from '../models/owns/workOrderMeterTrigger';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';

const basePath = 'work-order-meter-triggers';
interface WorkOrderMeterTriggerState {
  metersTriggers: { [id: number]: WorkOrderMeterTrigger[] };
}

const initialState: WorkOrderMeterTriggerState = {
  metersTriggers: {}
};

const slice = createSlice({
  name: 'workOrderMeterTriggers',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getWorkOrderMeterTriggers(
      state: WorkOrderMeterTriggerState,
      action: PayloadAction<{
        id: number;
        workOrderMeterTriggers: WorkOrderMeterTrigger[];
      }>
    ) {
      const { workOrderMeterTriggers, id } = action.payload;
      state.metersTriggers[id] = workOrderMeterTriggers;
    },
    createWorkOrderMeterTrigger(
      state: WorkOrderMeterTriggerState,
      action: PayloadAction<{
        meterId: number;
        workOrderMeterTrigger: WorkOrderMeterTrigger;
      }>
    ) {
      const { workOrderMeterTrigger, meterId } = action.payload;
      if (state.metersTriggers[meterId]) {
        state.metersTriggers[meterId].push(workOrderMeterTrigger);
      } else state.metersTriggers[meterId] = [workOrderMeterTrigger];
    },
    deleteWorkOrderMeterTrigger(
      state: WorkOrderMeterTriggerState,
      action: PayloadAction<{
        meterId: number;
        id: number;
      }>
    ) {
      const { id, meterId } = action.payload;
      state.metersTriggers[meterId] = state.metersTriggers[meterId].filter(
        (workOrderMeterTrigger) => workOrderMeterTrigger.id !== id
      );
    },
    editWorkOrderMeterTrigger(
      state: WorkOrderMeterTriggerState,
      action: PayloadAction<{
        meterId: number;
        workOrderMeterTrigger: WorkOrderMeterTrigger;
      }>
    ) {
      const { meterId, workOrderMeterTrigger } = action.payload;
      state.metersTriggers[meterId] = state.metersTriggers[meterId].map(
        (woMT) => {
          if (woMT.id === workOrderMeterTrigger.id) {
            return workOrderMeterTrigger;
          }
          return woMT;
        }
      );
    }
  }
});

export const reducer = slice.reducer;

export const getWorkOrderMeterTriggers =
  (id: number): AppThunk =>
  async (dispatch) => {
    const workOrderMeterTriggers = await api.get<WorkOrderMeterTrigger[]>(
      `${basePath}/meter/${id}`
    );
    dispatch(
      slice.actions.getWorkOrderMeterTriggers({ id, workOrderMeterTriggers })
    );
  };

export const createWorkOrderMeterTrigger =
  (
    id: number,
    workOrderMeterTrigger: Partial<WorkOrderMeterTrigger>
  ): AppThunk =>
  async (dispatch) => {
    const workOrderMeterTriggerResponse = await api.post<WorkOrderMeterTrigger>(
      `${basePath}`,
      { ...workOrderMeterTrigger, meter: { id } }
    );
    dispatch(
      slice.actions.createWorkOrderMeterTrigger({
        meterId: id,
        workOrderMeterTrigger: workOrderMeterTriggerResponse
      })
    );
  };

export const deleteWorkOrderMeterTrigger =
  (meterId: number, id: number): AppThunk =>
  async (dispatch) => {
    const response = await api.deletes<{ success: boolean }>(
      `${basePath}/${id}`
    );
    const { success } = response;
    if (success) {
      dispatch(slice.actions.deleteWorkOrderMeterTrigger({ meterId, id }));
    }
  };

export const editWorkOrderMeterTrigger =
  (meterId: number, id: number, values: any): AppThunk =>
  async (dispatch) => {
    const response = await api.patch<WorkOrderMeterTrigger>(
      `${basePath}/${id}`,
      values
    );
    dispatch(
      slice.actions.editWorkOrderMeterTrigger({
        meterId,
        workOrderMeterTrigger: response
      })
    );
  };
export default slice;
