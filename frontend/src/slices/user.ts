import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import { getInitialPage, Page, SearchCriteria } from 'src/models/owns/page';
import type { AppThunk } from 'src/store';
import { OwnUser as User, UserMiniDTO } from '../models/user';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';
import { UiConfiguration } from '../models/owns/uiConfiguration';

const basePath = 'users';
interface UserState {
  users: Page<User>;
  singleUser: User;
  usersMini: UserMiniDTO[];
  disabledUsersMini: UserMiniDTO[];
  loadingGet: boolean;
}

const initialState: UserState = {
  users: getInitialPage<User>(),
  singleUser: null,
  usersMini: [],
  disabledUsersMini: [],
  loadingGet: false
};

export const slice = createSlice({
  name: 'users',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getUsers(state: UserState, action: PayloadAction<{ users: Page<User> }>) {
      const { users } = action.payload;
      state.users = users;
    },
    getSingleUser(state: UserState, action: PayloadAction<{ user: User }>) {
      const { user } = action.payload;
      state.singleUser = user;
    },
    editUser(state: UserState, action: PayloadAction<{ user: User }>) {
      const { user } = action.payload;
      const inContent = state.users.content.some(
        (user1) => user1.id === user.id
      );
      if (inContent) {
        state.users.content = state.users.content.map((user1) => {
          if (user1.id === user.id) {
            return user;
          }
          return user1;
        });
      } else {
        state.singleUser = user;
      }
    },
    getSingleUserMini(
      state: UserState,
      action: PayloadAction<{ user: User; id: number }>
    ) {
      const { user, id } = action.payload;
      const index = state.usersMini.findIndex((user1) => user1.id === id);
      if (index !== -1) {
        state.usersMini[index] = user;
      } else state.usersMini.push(user);
    },
    getUsersMini(
      state: UserState,
      action: PayloadAction<{ users: UserMiniDTO[] }>
    ) {
      const { users } = action.payload;
      state.usersMini = users;
    },
    getDisabledUsersMini(
      state: UserState,
      action: PayloadAction<{ users: UserMiniDTO[] }>
    ) {
      const { users } = action.payload;
      state.disabledUsersMini = users;
    },
    addUser(state: UserState, action: PayloadAction<{ user: User }>) {
      const { user } = action.payload;
      state.users.content = [...state.users.content, user];
    },
    deleteUser(state: UserState, action: PayloadAction<{ id: number }>) {
      const { id } = action.payload;
      const userIndex = state.users.content.findIndex((user) => user.id === id);
      state.users.content.splice(userIndex, 1);
    },
    setLoadingGet(
      state: UserState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    },
    clearSingleUser(state: UserState, action: PayloadAction<{}>) {
      state.singleUser = null;
    }
  }
});

export const reducer = slice.reducer;

export const getUsers =
  (criteria: SearchCriteria): AppThunk =>
  async (dispatch) => {
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const users = await api.post<Page<User>>(`${basePath}/search`, criteria);
      dispatch(slice.actions.getUsers({ users }));
    } finally {
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };

export const getSingleUser =
  (id: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    const user = await api.get<User>(`${basePath}/${id}`);
    dispatch(slice.actions.getSingleUser({ user }));
    dispatch(slice.actions.setLoadingGet({ loading: false }));
  };

export const editUser =
  (id: number, user): AppThunk =>
  async (dispatch) => {
    const userResponse = await api.patch<User>(`${basePath}/${id}`, user);
    dispatch(slice.actions.editUser({ user: userResponse }));
  };

export const disableUser =
  (id: number): AppThunk =>
  async (dispatch) => {
    const userResponse = await api.patch<User>(`${basePath}/${id}/disable`, {});
    dispatch(slice.actions.editUser({ user: userResponse }));
  };
export const editUserRole =
  (id: number, roleId: number): AppThunk =>
  async (dispatch) => {
    const userResponse = await api.patch<User>(
      `${basePath}/${id}/role?role=${roleId}`,
      {}
    );
    dispatch(slice.actions.editUser({ user: userResponse }));
  };
export const getSingleUserMini =
  (id: number): AppThunk =>
  async (dispatch) => {
    const user = await api.get<User>(`users/${id}`);
    dispatch(slice.actions.getSingleUserMini({ user, id }));
  };
export const getUsersMini = (): AppThunk => async (dispatch) => {
  const users = await api.get<UserMiniDTO[]>('users/mini');
  dispatch(slice.actions.getUsersMini({ users }));
};
export const getDisabledUsersMini = (): AppThunk => async (dispatch) => {
  const users = await api.get<UserMiniDTO[]>('users/mini/disabled');
  dispatch(slice.actions.getDisabledUsersMini({ users }));
};
export const addUser =
  (user): AppThunk =>
  async (dispatch) => {
    const userResponse = await api.post<User>('users', user);
    dispatch(slice.actions.addUser({ user: userResponse }));
  };
export const deleteUser =
  (id: number): AppThunk =>
  async (dispatch) => {
    const userResponse = await api.deletes<{ success: boolean }>(`users/${id}`);
    const { success } = userResponse;
    if (success) {
      dispatch(slice.actions.deleteUser({ id }));
    }
  };

export const inviteUsers =
  (roleId: number, emails: string[]): AppThunk =>
  async (dispatch) => {
    const successResponse = await api.post<{ success: boolean }>(
      'users/invite',
      {
        role: { id: roleId },
        emails
      }
    );
  };

export const clearSingleUser = (): AppThunk => async (dispatch) => {
  dispatch(slice.actions.clearSingleUser({}));
};

export default slice;
