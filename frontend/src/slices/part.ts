import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import { getInitialPage, Page, SearchCriteria } from 'src/models/owns/page';
import type { AppThunk } from 'src/store';
import Part, { PartMiniDTO } from '../models/owns/part';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';

const basePath = 'parts';

interface PartState {
  parts: Page<Part>;
  singlePart: Part;
  partsMini: PartMiniDTO[];
  loadingGet: boolean;
}

const initialState: PartState = {
  parts: getInitialPage<Part>(),
  singlePart: null,
  partsMini: [],
  loadingGet: false
};

const slice = createSlice({
  name: 'parts',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getParts(state: PartState, action: PayloadAction<{ parts: Page<Part> }>) {
      const { parts } = action.payload;
      state.parts = parts;
    },
    getSinglePart(state: PartState, action: PayloadAction<{ part: Part }>) {
      const { part } = action.payload;
      state.singlePart = part;
    },
    editPart(state: PartState, action: PayloadAction<{ part: Part }>) {
      const { part } = action.payload;
      const inContent = state.parts.content.some(
        (part1) => part1.id === part.id
      );
      if (inContent) {
        state.parts.content = state.parts.content.map((part1) => {
          if (part1.id === part.id) {
            return part;
          }
          return part1;
        });
      } else {
        state.singlePart = part;
      }
    },
    getPartsMini(
      state: PartState,
      action: PayloadAction<{ parts: PartMiniDTO[] }>
    ) {
      const { parts } = action.payload;
      state.partsMini = parts;
    },
    addPart(state: PartState, action: PayloadAction<{ part: Part }>) {
      const { part } = action.payload;
      state.parts.content = [part, ...state.parts.content];
    },
    deletePart(state: PartState, action: PayloadAction<{ id: number }>) {
      const { id } = action.payload;
      const partIndex = state.parts.content.findIndex((part) => part.id === id);
      if (partIndex !== -1) state.parts.content.splice(partIndex, 1);
    },
    setLoadingGet(
      state: PartState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    },
    clearSinglePart(state: PartState, action: PayloadAction<{}>) {
      state.singlePart = null;
    }
  }
});

export const reducer = slice.reducer;

export const getParts =
  (criteria: SearchCriteria): AppThunk =>
    async (dispatch) => {
      try {
        dispatch(slice.actions.setLoadingGet({ loading: true }));
        const parts = await api.post<Page<Part>>(`${basePath}/search`, criteria);
        dispatch(slice.actions.getParts({ parts }));
      } finally {
        dispatch(slice.actions.setLoadingGet({ loading: false }));
      }
    };

export const getSinglePart =
  (id: number): AppThunk =>
    async (dispatch) => {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const part = await api.get<Part>(`${basePath}/${id}`);
      dispatch(slice.actions.getSinglePart({ part }));
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    };

export const editPart =
  (id: number, part): AppThunk =>
    async (dispatch) => {
      const partResponse = await api.patch<Part>(`${basePath}/${id}`, part);
      dispatch(slice.actions.editPart({ part: partResponse }));
    };
export const getPartsMini = (): AppThunk => async (dispatch) => {
  const parts = await api.get<PartMiniDTO[]>(`${basePath}/mini`);
  dispatch(slice.actions.getPartsMini({ parts }));
};
export const addPart =
  (part): AppThunk =>
    async (dispatch) => {
      const partResponse = await api.post<Part>(basePath, part);
      dispatch(slice.actions.addPart({ part: partResponse }));
    };
export const deletePart =
  (id: number): AppThunk =>
    async (dispatch) => {
      const partResponse = await api.deletes<{ success: boolean }>(
        `${basePath}/${id}`
      );
      const { success } = partResponse;
      if (success) {
        dispatch(slice.actions.deletePart({ id }));
      }
    };
export const clearSinglePart = (): AppThunk => async (dispatch) => {
  dispatch(slice.actions.clearSinglePart({}));
};

export default slice;
