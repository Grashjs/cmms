import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from '../store';
import PreventiveMaintenance, {
  PreventiveMaintenancePost
} from '../models/preventiveMaintenance';
import api from '../utils/api';
import Schedule from '../models/schedule';
import { getInitialPage, Page, SearchCriteria } from '../models/page';
import { revertAll } from '../utils/redux';

interface PreventiveMaintenanceState {
  preventiveMaintenances: Page<PreventiveMaintenance>;
  singlePreventiveMaintenance: PreventiveMaintenance;
  loadingGet: boolean;
}

const initialState: PreventiveMaintenanceState = {
  preventiveMaintenances: getInitialPage<PreventiveMaintenance>(),
  singlePreventiveMaintenance: null,
  loadingGet: false
};
const basePath = 'preventive-maintenances';
const slice = createSlice({
  name: 'preventiveMaintenance',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getPreventiveMaintenances(
      state: PreventiveMaintenanceState,
      action: PayloadAction<{
        preventiveMaintenances: Page<PreventiveMaintenance>;
      }>
    ) {
      const { preventiveMaintenances } = action.payload;
      state.preventiveMaintenances = preventiveMaintenances;
    },
    getSinglePreventiveMaintenance(
      state: PreventiveMaintenanceState,
      action: PayloadAction<{ preventiveMaintenance: PreventiveMaintenance }>
    ) {
      const { preventiveMaintenance } = action.payload;
      state.singlePreventiveMaintenance = preventiveMaintenance;
    },

    addPreventiveMaintenance(
      state: PreventiveMaintenanceState,
      action: PayloadAction<{ preventiveMaintenance: PreventiveMaintenance }>
    ) {
      const { preventiveMaintenance } = action.payload;
      state.preventiveMaintenances.content = [
        ...state.preventiveMaintenances.content,
        preventiveMaintenance
      ];
    },
    editPreventiveMaintenance(
      state: PreventiveMaintenanceState,
      action: PayloadAction<{
        preventiveMaintenance: PreventiveMaintenance;
      }>
    ) {
      const { preventiveMaintenance } = action.payload;
      const inContent = state.preventiveMaintenances.content.some(
        (pm) => pm.id === preventiveMaintenance.id
      );
      if (inContent) {
        state.preventiveMaintenances.content =
          state.preventiveMaintenances.content.map((preventiveMaintenance1) => {
            if (preventiveMaintenance1.id === preventiveMaintenance.id) {
              return preventiveMaintenance;
            }
            return preventiveMaintenance1;
          });
      } else {
        state.singlePreventiveMaintenance = preventiveMaintenance;
      }
    },
    patchSchedule(
      state: PreventiveMaintenanceState,
      action: PayloadAction<{ schedule: Schedule; pmId: number }>
    ) {
      const { schedule, pmId } = action.payload;
      state.preventiveMaintenances.content =
        state.preventiveMaintenances.content.map((preventiveMaintenance) => {
          if (preventiveMaintenance.id === pmId) {
            return { ...preventiveMaintenance, schedule };
          }
          return preventiveMaintenance;
        });
      if (state.singlePreventiveMaintenance?.id === pmId) {
        state.singlePreventiveMaintenance = {
          ...state.singlePreventiveMaintenance,
          schedule
        };
      }
    },
    deletePreventiveMaintenance(
      state: PreventiveMaintenanceState,
      action: PayloadAction<{ id: number }>
    ) {
      const { id } = action.payload;
      const preventiveMaintenanceIndex =
        state.preventiveMaintenances.content.findIndex(
          (preventiveMaintenance) => preventiveMaintenance.id === id
        );
      if (preventiveMaintenanceIndex !== -1)
        state.preventiveMaintenances.content.splice(
          preventiveMaintenanceIndex,
          1
        );
    },
    setLoadingGet(
      state: PreventiveMaintenanceState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    },
    clearSinglePM(
      state: PreventiveMaintenanceState,
      action: PayloadAction<{}>
    ) {
      state.singlePreventiveMaintenance = null;
    }
  }
});

export const reducer = slice.reducer;

export const getPreventiveMaintenances =
  (criteria: SearchCriteria): AppThunk =>
  async (dispatch) => {
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const preventiveMaintenances = await api.post<
        Page<PreventiveMaintenance>
      >(`${basePath}/search`, criteria);
      dispatch(
        slice.actions.getPreventiveMaintenances({ preventiveMaintenances })
      );
    } finally {
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };

export const getSinglePreventiveMaintenance =
  (id: number): AppThunk =>
  async (dispatch) => {
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const preventiveMaintenance = await api.get<PreventiveMaintenance>(
        `${basePath}/${id}`
      );
      dispatch(
        slice.actions.getSinglePreventiveMaintenance({ preventiveMaintenance })
      );
    } finally {
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };
export const addPreventiveMaintenance =
  (preventiveMaintenance: Partial<PreventiveMaintenancePost>): AppThunk =>
  async (dispatch) => {
    const preventiveMaintenanceResponse = await api.post<PreventiveMaintenance>(
      basePath,
      preventiveMaintenance
    );
    dispatch(
      slice.actions.addPreventiveMaintenance({
        preventiveMaintenance: preventiveMaintenanceResponse
      })
    );
  };
export const editPreventiveMaintenance =
  (id: number, preventiveMaintenance): AppThunk =>
  async (dispatch) => {
    const preventiveMaintenanceResponse =
      await api.patch<PreventiveMaintenance>(
        `${basePath}/${id}`,
        preventiveMaintenance
      );
    dispatch(
      slice.actions.editPreventiveMaintenance({
        preventiveMaintenance: preventiveMaintenanceResponse
      })
    );
  };
export const patchSchedule =
  (scheduleId: number, pmId: number, schedule: Partial<Schedule>): AppThunk =>
  async (dispatch) => {
    const scheduleResponse = await api.patch<Schedule>(
      `schedules/${scheduleId}`,
      schedule
    );
    dispatch(
      slice.actions.patchSchedule({
        pmId,
        schedule: scheduleResponse
      })
    );
  };
export const deletePreventiveMaintenance =
  (id: number): AppThunk =>
  async (dispatch) => {
    const preventiveMaintenanceResponse = await api.deletes<{
      success: boolean;
    }>(`${basePath}/${id}`);
    const { success } = preventiveMaintenanceResponse;
    if (success) {
      dispatch(slice.actions.deletePreventiveMaintenance({ id }));
    }
  };

export const clearSinglePM = (): AppThunk => async (dispatch) => {
  dispatch(slice.actions.clearSinglePM({}));
};

export default slice;
