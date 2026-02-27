import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import { Role } from '../models/owns/role';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';

interface RoleState {
  roles: Role[];
  loadingGet: boolean;
}

const initialState: RoleState = {
  roles: [],
  loadingGet: false
};

const slice = createSlice({
  name: 'roles',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getRoles(state: RoleState, action: PayloadAction<{ roles: Role[] }>) {
      const { roles } = action.payload;
      state.roles = roles;
    },
    addRole(state: RoleState, action: PayloadAction<{ role: Role }>) {
      const { role } = action.payload;
      state.roles = [...state.roles, role];
    },
    editRole(state: RoleState, action: PayloadAction<{ role: Role }>) {
      const { role } = action.payload;
      state.roles = state.roles.map((role1) => {
        if (role1.id === role.id) {
          return role;
        }
        return role1;
      });
    },
    deleteRole(state: RoleState, action: PayloadAction<{ id: number }>) {
      const { id } = action.payload;
      const roleIndex = state.roles.findIndex((role) => role.id === id);
      state.roles.splice(roleIndex, 1);
    },
    setLoadingGet(
      state: RoleState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    }
  }
});

export const reducer = slice.reducer;

export const getRoles = (): AppThunk => async (dispatch) => {
  dispatch(slice.actions.setLoadingGet({ loading: true }));
  const roles = await api.get<Role[]>('roles');
  dispatch(slice.actions.getRoles({ roles }));
  dispatch(slice.actions.setLoadingGet({ loading: false }));
};

export const addRole =
  (role): AppThunk =>
  async (dispatch) => {
    const roleResponse = await api.post<Role>('roles', role);
    dispatch(slice.actions.addRole({ role: roleResponse }));
  };
export const editRole =
  (id: number, role): AppThunk =>
  async (dispatch) => {
    const roleResponse = await api.patch<Role>(`roles/${id}`, role);
    dispatch(slice.actions.editRole({ role: roleResponse }));
  };
export const deleteRole =
  (id: number): AppThunk =>
  async (dispatch) => {
    const roleResponse = await api.deletes<{ success: boolean }>(`roles/${id}`);
    const { success } = roleResponse;
    if (success) {
      dispatch(slice.actions.deleteRole({ id }));
    }
  };

export default slice;
