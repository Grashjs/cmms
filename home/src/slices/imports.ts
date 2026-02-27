import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import api from '../utils/api';
import { ImportDTO, ImportResponse } from '../models/owns/imports';
import { EntityType } from 'src/content/own/Imports';

const basePath = 'import';

interface ImportsState {
}

const initialState: ImportsState = {};

const slice = createSlice({
  name: 'parts',
  initialState,
  reducers: {}
});

export const reducer = slice.reducer;

export const importEntity =
  (values: ImportDTO[], entity: EntityType): AppThunk =>
    async (dispatch) => {
      const response = await api.post<ImportResponse>(
        `${basePath}/${entity}`,
        values,
        {}
      );
      return response;
    };

export default slice;
