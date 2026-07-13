import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import { getInitialPage, Page, SearchCriteria } from '../models/page';
import type { AppThunk } from '../store';
import Part, { PartMiniDTO } from '../models/part';
import api from '../utils/api';
import { revertAll } from '../utils/redux';

const basePath = 'parts';

interface PartState {
  parts: Page<Part>;
  partInfos: { [key: number]: { part?: Part } };
  partsMini: PartMiniDTO[];
  currentPageNum: number;
  lastPage: boolean;
  loadingGet: boolean;
}

const initialState: PartState = {
  parts: getInitialPage<Part>(),
  partInfos: {},
  partsMini: [],
  currentPageNum: 0,
  lastPage: true,
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
      state.currentPageNum = 0;
      state.lastPage = parts.last;
    },
    getMoreParts(
      state: PartState,
      action: PayloadAction<{ parts: Page<Part> }>
    ) {
      const { parts } = action.payload;
      state.parts.content = state.parts.content.concat(parts.content);
      state.currentPageNum = state.currentPageNum + 1;
      state.lastPage = parts.last;
    },
    getPartDetails(
      state: PartState,
      action: PayloadAction<{ part; id: number }>
    ) {
      const { part, id } = action.payload;
      if (state.partInfos[id]) {
        state.partInfos[id] = { ...state.partInfos[id], part };
      } else state.partInfos[id] = { part };
    },
    editPart(state: PartState, action: PayloadAction<{ part: Part }>) {
      const { part } = action.payload;
      if (state.partInfos[part.id]) {
        state.partInfos[part.id].part = part;
      } else state.partInfos[part.id] = { part };
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
      state.parts.content = [...state.parts.content, part];
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
export const getMoreParts =
  (criteria: SearchCriteria, pageNum: number): AppThunk =>
  async (dispatch) => {
    criteria = { ...criteria, pageNum };
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const parts = await api.post<Page<Part>>(`${basePath}/search`, criteria);
      dispatch(slice.actions.getMoreParts({ parts }));
    } finally {
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };
export const getPartDetails =
  (id: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    const part = await api.get<Part>(`${basePath}/${id}`);
    dispatch(
      slice.actions.getPartDetails({
        id,
        part
      })
    );
    dispatch(slice.actions.setLoadingGet({ loading: false }));
  };

export const editPart =
  (id: number, part): AppThunk =>
  async (dispatch) => {
    const partResponse = await api.patch<Part>(`${basePath}/${id}`, part);
    dispatch(slice.actions.editPart({ part: partResponse }));
  };
export const getPartsMini = (): AppThunk => async (dispatch) => {
  dispatch(slice.actions.setLoadingGet({ loading: true }));
  const parts = await api.get<PartMiniDTO[]>(`${basePath}/mini`);
  dispatch(slice.actions.getPartsMini({ parts }));
  dispatch(slice.actions.setLoadingGet({ loading: false }));
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
export default slice;
