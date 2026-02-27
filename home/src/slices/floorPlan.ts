import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import FloorPlan from '../models/owns/floorPlan';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';

const basePath = 'floor-plans';
interface FloorPlanState {
  floorPlansByLocation: { [id: number]: FloorPlan[] };
}

const initialState: FloorPlanState = {
  floorPlansByLocation: {}
};

const slice = createSlice({
  name: 'floorPlans',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getFloorPlans(
      state: FloorPlanState,
      action: PayloadAction<{ id: number; floorPlans: FloorPlan[] }>
    ) {
      const { floorPlans, id } = action.payload;
      state.floorPlansByLocation[id] = floorPlans;
    },
    createFloorPlan(
      state: FloorPlanState,
      action: PayloadAction<{
        locationId: number;
        floorPlan: FloorPlan;
      }>
    ) {
      const { floorPlan, locationId } = action.payload;
      state.floorPlansByLocation[locationId].push(floorPlan);
    },
    deleteFloorPlan(
      state: FloorPlanState,
      action: PayloadAction<{
        locationId: number;
        id: number;
      }>
    ) {
      const { id, locationId } = action.payload;
      state.floorPlansByLocation[locationId] = state.floorPlansByLocation[
        locationId
      ].filter((floorPlan) => floorPlan.id !== id);
    }
  }
});

export const reducer = slice.reducer;

export const getFloorPlans =
  (id: number): AppThunk =>
  async (dispatch) => {
    const floorPlans = await api.get<FloorPlan[]>(`${basePath}/location/${id}`);
    dispatch(slice.actions.getFloorPlans({ id, floorPlans }));
  };

export const createFloorPlan =
  (id: number, floorPlan: Partial<FloorPlan>): AppThunk =>
  async (dispatch) => {
    const floorPlanResponse = await api.post<FloorPlan>(`${basePath}`, {
      ...floorPlan,
      location: { id }
    });
    dispatch(
      slice.actions.createFloorPlan({
        locationId: id,
        floorPlan: floorPlanResponse
      })
    );
  };

export const deleteFloorPlan =
  (locationId: number, id: number): AppThunk =>
  async (dispatch) => {
    const response = await api.deletes<{ success: boolean }>(
      `${basePath}/${id}`
    );
    const { success } = response;
    if (success) {
      dispatch(slice.actions.deleteFloorPlan({ locationId, id }));
    }
  };

export default slice;
