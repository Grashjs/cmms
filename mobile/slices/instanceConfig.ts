import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from '../store';
import api, { authHeader } from '../utils/api';
import { revertAll } from '../utils/redux';

interface InstanceConfigState {
  ldapEnabled: boolean;
  loading: boolean;
}

const initialState: InstanceConfigState = {
  ldapEnabled: false,
  loading: false
};

const slice = createSlice({
  name: 'instanceConfig',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    setInstanceConfig(
      state: InstanceConfigState,
      action: PayloadAction<{ ldapEnabled: boolean }>
    ) {
      const { ldapEnabled } = action.payload;
      state.ldapEnabled = ldapEnabled;
    },
    setLoading(
      state: InstanceConfigState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loading = loading;
    }
  }
});

export const reducer = slice.reducer;

export const getInstanceConfig = (): AppThunk => async (dispatch) => {
  try {
    dispatch(slice.actions.setLoading({ loading: true }));
    const response = await api.get<{ ldapEnabled: boolean }>(
      'instance-config',
      {
        headers: await authHeader(true)
      }
    );
    dispatch(
      slice.actions.setInstanceConfig({ ldapEnabled: response.ldapEnabled })
    );
  } catch (error) {
    console.error('Failed to fetch instance config:', error);
  } finally {
    dispatch(slice.actions.setLoading({ loading: false }));
  }
};

export default slice;
