import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import {
  SavedView,
  SavedViewEntityType,
  SavedViewPatchPayload,
  SavedViewPostPayload
} from '../models/owns/savedView';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';

const basePath = 'saved-views';

/**
 * Views are kept per entity type rather than in one flat list: every list page asks only for
 * its own, and a page must not have to filter someone else's out of a shared array.
 */
interface SavedViewState {
  viewsByEntity: Partial<Record<SavedViewEntityType, SavedView[]>>;
  loadingGet: boolean;
}

const initialState: SavedViewState = {
  viewsByEntity: {},
  loadingGet: false
};

const slice = createSlice({
  name: 'savedViews',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getSavedViews(
      state: SavedViewState,
      action: PayloadAction<{
        entityType: SavedViewEntityType;
        savedViews: SavedView[];
      }>
    ) {
      const { entityType, savedViews } = action.payload;
      state.viewsByEntity[entityType] = savedViews;
    },
    addSavedView(
      state: SavedViewState,
      action: PayloadAction<{ savedView: SavedView }>
    ) {
      const { savedView } = action.payload;
      const current = state.viewsByEntity[savedView.entityType] ?? [];
      state.viewsByEntity[savedView.entityType] = [...current, savedView].sort(
        (a, b) => a.name.localeCompare(b.name)
      );
    },
    editSavedView(
      state: SavedViewState,
      action: PayloadAction<{ savedView: SavedView }>
    ) {
      const { savedView } = action.payload;
      const current = state.viewsByEntity[savedView.entityType] ?? [];
      state.viewsByEntity[savedView.entityType] = current
        .map((view) => (view.id === savedView.id ? savedView : view))
        .sort((a, b) => a.name.localeCompare(b.name));
    },
    deleteSavedView(
      state: SavedViewState,
      action: PayloadAction<{ entityType: SavedViewEntityType; id: number }>
    ) {
      const { entityType, id } = action.payload;
      const current = state.viewsByEntity[entityType] ?? [];
      state.viewsByEntity[entityType] = current.filter(
        (view) => view.id !== id
      );
    },
    setLoadingGet(
      state: SavedViewState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      state.loadingGet = action.payload.loading;
    }
  }
});

export const reducer = slice.reducer;

export const getSavedViews =
  (entityType: SavedViewEntityType): AppThunk =>
  async (dispatch) => {
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const savedViews = await api.get<SavedView[]>(
        `${basePath}?entityType=${entityType}`
      );
      dispatch(slice.actions.getSavedViews({ entityType, savedViews }));
    } finally {
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };

export const createSavedView =
  (payload: SavedViewPostPayload): AppThunk =>
  async (dispatch) => {
    const savedView = await api.post<SavedView>(basePath, payload);
    dispatch(slice.actions.addSavedView({ savedView }));
    return savedView;
  };

export const updateSavedView =
  (id: number, payload: SavedViewPatchPayload): AppThunk =>
  async (dispatch) => {
    const savedView = await api.patch<SavedView>(`${basePath}/${id}`, payload);
    dispatch(slice.actions.editSavedView({ savedView }));
    return savedView;
  };

export const deleteSavedView =
  (entityType: SavedViewEntityType, id: number): AppThunk =>
  async (dispatch) => {
    await api.deletes<{ success: boolean }>(`${basePath}/${id}`);
    dispatch(slice.actions.deleteSavedView({ entityType, id }));
  };
