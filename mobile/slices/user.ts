import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import { getInitialPage, Page, SearchCriteria } from '../models/page';
import type { AppThunk } from '../store';
import { OwnUser as User, UserMiniDTO } from '../models/user';
import api from '../utils/api';
import { revertAll } from '../utils/redux';

const basePath = 'users';

interface UserState {
  users: Page<User>;
  userInfos: { [key: number]: { user?: User } };
  usersMini: UserMiniDTO[];
  disabledUsersMini: UserMiniDTO[];
  currentPageNum: number;
  lastPage: boolean;
  loadingGet: boolean;
}

const initialState: UserState = {
  users: getInitialPage<User>(),
  userInfos: {},
  usersMini: [],
  disabledUsersMini: [],
  currentPageNum: 0,
  lastPage: true,
  loadingGet: false
};

const slice = createSlice({
  name: 'users',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getUsers(state: UserState, action: PayloadAction<{ users: Page<User> }>) {
      const { users } = action.payload;
      state.users = users;
      state.currentPageNum = 0;
      state.lastPage = users.last;
    },
    getMoreUsers(
      state: UserState,
      action: PayloadAction<{ users: Page<User> }>
    ) {
      const { users } = action.payload;
      state.users.content = state.users.content.concat(users.content);
      state.currentPageNum = state.currentPageNum + 1;
      state.lastPage = users.last;
    },
    getUserDetails(
      state: UserState,
      action: PayloadAction<{ user; id: number }>
    ) {
      const { user, id } = action.payload;
      if (state.userInfos[id]) {
        state.userInfos[id] = { ...state.userInfos[id], user };
      } else state.userInfos[id] = { user };
    },

    editUser(state: UserState, action: PayloadAction<{ user: User }>) {
      const { user } = action.payload;
      if (state.userInfos[user.id]) {
        state.userInfos[user.id].user = user;
      } else state.userInfos[user.id] = { user };
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
export const getMoreUsers =
  (criteria: SearchCriteria, pageNum: number): AppThunk =>
  async (dispatch) => {
    criteria = { ...criteria, pageNum };
    try {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const users = await api.post<Page<User>>(`${basePath}/search`, criteria);
      dispatch(slice.actions.getMoreUsers({ users }));
    } finally {
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    }
  };

export const getUserDetails =
  (id: number): AppThunk =>
  async (dispatch) => {
    dispatch(slice.actions.setLoadingGet({ loading: true }));
    const user = await api.get<User>(`${basePath}/${id}`);
    dispatch(
      slice.actions.getUserDetails({
        id,
        user
      })
    );
    dispatch(slice.actions.setLoadingGet({ loading: false }));
  };
export const editUser =
  (id: number, user): AppThunk =>
  async (dispatch) => {
    const userResponse = await api.patch<User>(`${basePath}/${id}`, user);
    dispatch(slice.actions.editUser({ user: userResponse }));
  };
export const getSingleUserMini =
  (id: number): AppThunk =>
  async (dispatch) => {
    const user = await api.get<User>(`users/${id}`);
    dispatch(slice.actions.getSingleUserMini({ user, id }));
  };
export const getUsersMini = (): AppThunk => async (dispatch) => {
  dispatch(slice.actions.setLoadingGet({ loading: true }));
  const users = await api.get<UserMiniDTO[]>('users/mini');
  dispatch(slice.actions.getUsersMini({ users }));
  dispatch(slice.actions.setLoadingGet({ loading: false }));
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

export default slice;
