import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import api from 'src/utils/api';
import { UserWOStats, WOStatsByDay } from 'src/models/owns/analytics/user';
import { revertAll } from 'src/utils/redux';

const basePath = 'analytics/users';
interface UserStatstate {
  workOrdersOverview: UserWOStats;
  twoWeeksWorkOrders: WOStatsByDay[];
  loading: Omit<Record<keyof UserStatstate, boolean>, 'loading'>;
}
type Operation = keyof UserStatstate;

const initialState: UserStatstate = {
  workOrdersOverview: {
    created: 0,
    completed: 0
  },
  twoWeeksWorkOrders: [],
  loading: {
    workOrdersOverview: false,
    twoWeeksWorkOrders: false
  }
};

const slice = createSlice({
  name: 'userStats',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getWorkOrdersOverview(
      state: UserStatstate,
      action: PayloadAction<{ stats: UserWOStats }>
    ) {
      const { stats } = action.payload;
      state.workOrdersOverview = stats;
    },
    getTwoWeeksWorkOrders(
      state: UserStatstate,
      action: PayloadAction<{ stats: WOStatsByDay[] }>
    ) {
      const { stats } = action.payload;
      state.twoWeeksWorkOrders = stats;
    },
    setLoading(
      state: UserStatstate,
      action: PayloadAction<{ loading: boolean; operation: Operation }>
    ) {
      const { loading, operation } = action.payload;
      state.loading[operation] = loading;
    }
  }
});

export const reducer = slice.reducer;

export const getUserWorkOrdersOverview = (): AppThunk => async (dispatch) => {
  dispatch(
    slice.actions.setLoading({
      operation: 'workOrdersOverview',
      loading: true
    })
  );
  const stats = await api.get<UserWOStats>(
    `${basePath}/me/work-orders/overview`
  );
  dispatch(slice.actions.getWorkOrdersOverview({ stats }));
  dispatch(
    slice.actions.setLoading({
      operation: 'workOrdersOverview',
      loading: false
    })
  );
};

export const getTwoWeeksWorkOrders =
  (userId: number): AppThunk =>
  async (dispatch) => {
    dispatch(
      slice.actions.setLoading({
        operation: 'twoWeeksWorkOrders',
        loading: true
      })
    );
    const stats = await api.get<WOStatsByDay[]>(
      `${basePath}/two-weeks/work-orders/${userId}`
    );
    dispatch(slice.actions.getTwoWeeksWorkOrders({ stats }));
    dispatch(
      slice.actions.setLoading({
        operation: 'twoWeeksWorkOrders',
        loading: false
      })
    );
  };

export default slice;
