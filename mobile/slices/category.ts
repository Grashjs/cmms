import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from '../store';
import Category from '../models/category';
import api from '../utils/api';
import { revertAll } from '../utils/redux';

interface CategoryState {
  categories: { [basePath: string]: Category[] };
}

const initialState: CategoryState = {
  categories: {}
};

const slice = createSlice({
  name: 'categories',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getCategories(
      state: CategoryState,
      action: PayloadAction<{ categories: Category[]; basePath: string }>
    ) {
      const { categories, basePath } = action.payload;
      state.categories[basePath] = categories;
    },
    addCategory(
      state: CategoryState,
      action: PayloadAction<{ category: Category; basePath: string }>
    ) {
      const { category, basePath } = action.payload;
      state.categories[basePath].push(category);
    },
    editCategory(
      state: CategoryState,
      action: PayloadAction<{ category: Category; basePath: string }>
    ) {
      const { category, basePath } = action.payload;
      state.categories[basePath] = state.categories[basePath].map(
        (category1) => {
          if (category1.id === category.id) {
            return category;
          }
          return category1;
        }
      );
    },
    deleteCategory(
      state: CategoryState,
      action: PayloadAction<{ id: number; basePath: string }>
    ) {
      const { id, basePath } = action.payload;
      const categoryIndex = state.categories[basePath].findIndex(
        (category) => category.id === id
      );
      state.categories[basePath].splice(categoryIndex, 1);
    }
  }
});

export const reducer = slice.reducer;

export const getCategories =
  (basePath): AppThunk =>
  async (dispatch) => {
    const categories = await api.get<Category[]>(basePath);
    dispatch(slice.actions.getCategories({ categories, basePath }));
  };

export const addCategory =
  (category, basePath): AppThunk =>
  async (dispatch) => {
    const categoryResponse = await api.post<Category>(basePath, category);
    dispatch(
      slice.actions.addCategory({ category: categoryResponse, basePath })
    );
  };
export const editCategory =
  (id: number, category, basePath): AppThunk =>
  async (dispatch) => {
    const categoryResponse = await api.patch<Category>(
      `${basePath}/${id}`,
      category
    );
    dispatch(
      slice.actions.editCategory({ category: categoryResponse, basePath })
    );
  };
export const deleteCategory =
  (id: number, basePath): AppThunk =>
  async (dispatch) => {
    const categoryResponse = await api.deletes<{ success: boolean }>(
      `${basePath}/${id}`
    );
    const { success } = categoryResponse;
    if (success) {
      dispatch(slice.actions.deleteCategory({ id, basePath }));
    }
  };

export default slice;
