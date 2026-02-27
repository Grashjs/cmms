import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import { Checklist } from '../models/owns/checklists';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';

interface ChecklistState {
  checklists: Checklist[];
  loadingGet: boolean;
}

const initialState: ChecklistState = {
  checklists: [],
  loadingGet: false
};

const slice = createSlice({
  name: 'checklists',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getChecklists(
      state: ChecklistState,
      action: PayloadAction<{ checklists: Checklist[] }>
    ) {
      const { checklists } = action.payload;
      state.checklists = checklists;
    },
    addChecklist(
      state: ChecklistState,
      action: PayloadAction<{ checklist: Checklist }>
    ) {
      const { checklist } = action.payload;
      state.checklists = [...state.checklists, checklist];
    },
    editChecklist(
      state: ChecklistState,
      action: PayloadAction<{ checklist: Checklist }>
    ) {
      const { checklist } = action.payload;
      state.checklists = state.checklists.map((checklist1) => {
        if (checklist1.id === checklist.id) {
          return checklist;
        }
        return checklist1;
      });
    },
    deleteChecklist(
      state: ChecklistState,
      action: PayloadAction<{ id: number }>
    ) {
      const { id } = action.payload;
      const checklistIndex = state.checklists.findIndex(
        (checklist) => checklist.id === id
      );
      state.checklists.splice(checklistIndex, 1);
    },
    setLoadingGet(
      state: ChecklistState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    }
  }
});

export const reducer = slice.reducer;

export const getChecklists = (): AppThunk => async (dispatch) => {
  try {
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    const checklists = await api.get<Checklist[]>('checklists');
    dispatch(slice.actions.getChecklists({ checklists }));
  } finally {
    dispatch(slice.actions.setLoadingGet({ loading: false }));
  }
};

export const addChecklist =
  (checklist, companySettingsId): AppThunk =>
  async (dispatch) => {
    const checklistResponse = await api.post<Checklist>('checklists', {
      ...checklist,
      companySettings: { id: companySettingsId }
    });
    dispatch(slice.actions.addChecklist({ checklist: checklistResponse }));
  };
export const editChecklist =
  (id: number, checklist): AppThunk =>
  async (dispatch) => {
    const checklistResponse = await api.patch<Checklist>(
      `checklists/${id}`,
      checklist
    );
    dispatch(slice.actions.editChecklist({ checklist: checklistResponse }));
  };
export const deleteChecklist =
  (id: number): AppThunk =>
  async (dispatch) => {
    const checklistResponse = await api.deletes<{ success: boolean }>(
      `checklists/${id}`
    );
    const { success } = checklistResponse;
    if (success) {
      dispatch(slice.actions.deleteChecklist({ id }));
    }
  };

export default slice;
