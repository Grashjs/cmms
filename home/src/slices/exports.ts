import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';

const basePath = 'export';
type EntityType = 'work-orders' | 'assets' | 'locations' | 'parts' | 'meters';
interface ExportsState {
  loadingExport: Record<EntityType, boolean>;
}

const initialState: ExportsState = {
  loadingExport: {
    'work-orders': false,
    assets: false,
    locations: false,
    parts: false,
    meters: false
  }
};

const slice = createSlice({
  name: 'exports',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    setLoading(
      state: ExportsState,
      action: PayloadAction<{ entity: EntityType; loading: boolean }>
    ) {
      const { loading, entity } = action.payload;
      state.loadingExport[entity] = loading;
    }
  }
});

export const reducer = slice.reducer;

export const exportEntity =
  (entity: EntityType): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoading({ entity, loading: true }));
    try {
      const response = await api.get<{ success: boolean; message: string }>(
        `${basePath}/${entity}`
      );
      return response.message;
    } finally {
      dispatch(slice.actions.setLoading({ entity, loading: false }));
    }
  };

export default slice;
