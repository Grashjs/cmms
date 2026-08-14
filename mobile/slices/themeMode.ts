import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';

export type ThemeMode = 'system' | 'light' | 'dark';

interface ThemeModeState {
  mode: ThemeMode;
}

const initialState: ThemeModeState = {
  mode: 'system'
};

/**
 * Deliberately does not handle `revertAll`. This is a device preference rather
 * than account data, so signing out should not throw it away.
 */
const slice = createSlice({
  name: 'themeMode',
  initialState,
  reducers: {
    setThemeMode(state: ThemeModeState, action: PayloadAction<ThemeMode>) {
      state.mode = action.payload;
    }
  }
});

export const reducer = slice.reducer;
export const { setThemeMode } = slice.actions;
export default slice;
