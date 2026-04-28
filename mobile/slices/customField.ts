import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import api from '../utils/api';
import { CustomField } from '../models/customField';
import { revertAll } from '../utils/redux';
import { AppThunk } from '../store';

const basePath = 'custom-fields';

interface CustomFieldState {
  customFields: CustomField[];
  loadingGet: boolean;
}

const initialState: CustomFieldState = {
  customFields: [],
  loadingGet: false
};

const slice = createSlice({
  name: 'customFields',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getCustomFields(
      state: CustomFieldState,
      action: PayloadAction<{ customFields: CustomField[] }>
    ) {
      const { customFields } = action.payload;
      state.customFields = customFields;
    },
    addCustomField(
      state: CustomFieldState,
      action: PayloadAction<{ customField: CustomField }>
    ) {
      const { customField } = action.payload;
      state.customFields = [...state.customFields, customField];
    },
    editCustomField(
      state: CustomFieldState,
      action: PayloadAction<{ customField: CustomField }>
    ) {
      const { customField } = action.payload;
      const index = state.customFields.findIndex(
        (cf) => cf.id === customField.id
      );
      if (index !== -1) {
        state.customFields[index] = customField;
      }
    },
    deleteCustomField(
      state: CustomFieldState,
      action: PayloadAction<{ id: number }>
    ) {
      const { id } = action.payload;
      state.customFields = state.customFields.filter((cf) => cf.id !== id);
    },
    setLoadingGet(
      state: CustomFieldState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    },
    reorderCustomFields(
      state: CustomFieldState,
      action: PayloadAction<{ customFields: CustomField[] }>
    ) {
      const { customFields } = action.payload;
      state.customFields = customFields;
    }
  }
});

export const reducer = slice.reducer;

export const getCustomFields = (): AppThunk => async (dispatch) => {
  try {
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    const customFields = await api.get<CustomField[]>(basePath);
    dispatch(slice.actions.getCustomFields({ customFields }));
  } finally {
    dispatch(slice.actions.setLoadingGet({ loading: false }));
  }
};

export const createCustomField =
  (
    customField: Omit<CustomField, 'id' | 'createdAt' | 'updatedAt' | 'order'>
  ): AppThunk =>
  async (dispatch) => {
    const customFieldResponse = await api.post<CustomField>(
      basePath,
      customField
    );
    dispatch(
      slice.actions.addCustomField({ customField: customFieldResponse })
    );
    return customFieldResponse;
  };

export const updateCustomField =
  (id: number, customField: Partial<CustomField>): AppThunk =>
  async (dispatch) => {
    const customFieldResponse = await api.patch<CustomField>(
      `${basePath}/${id}`,
      customField
    );
    dispatch(
      slice.actions.editCustomField({ customField: customFieldResponse })
    );
  };

export const deleteCustomField =
  (id: number): AppThunk =>
  async (dispatch) => {
    const response = await api.deletes<{ success: boolean }>(
      `${basePath}/${id}`
    );
    const { success } = response;
    if (success) {
      dispatch(slice.actions.deleteCustomField({ id }));
    }
  };
export const reorderCustomFieldsAPI =
  (reorderList: number[]): AppThunk =>
  async (dispatch, getState) => {
    const state = getState();
    const allCustomFields = state.customFields.customFields;

    const reordered = reorderList
      .map((id, index) => {
        const cf = allCustomFields.find((cf) => cf.id === id);
        return cf ? { ...cf, order: index + 1 } : null; // spread to avoid mutating
      })
      .filter(Boolean) as CustomField[];

    const updated = allCustomFields.map(
      (cf) => reordered.find((r) => r.id === cf.id) || cf
    );

    dispatch(slice.actions.reorderCustomFields({ customFields: updated }));

    try {
      await api.patch(`${basePath}/reorder`, reorderList);
    } catch (error) {
      dispatch(getCustomFields());
    }
  };

export default slice;
