import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import { getInitialPage, Page, SearchCriteria } from 'src/models/owns/page';
import type { AppThunk } from 'src/store';
import Notification from '../models/owns/notification';
import api from '../utils/api';
import { revertAll } from 'src/utils/redux';
import { getWorkOrders } from './workOrder';

const basePath = 'notifications';

interface NotificationState {
  notifications: Page<Notification>;
  currentPageNum: number;
  lastPage: boolean;
  loadingGet: boolean;
}

const initialState: NotificationState = {
  notifications: getInitialPage<Notification>(),
  currentPageNum: 0,
  lastPage: false,
  loadingGet: false
};

const slice = createSlice({
  name: 'notifications',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getNotifications(
      state: NotificationState,
      action: PayloadAction<{ notifications: Page<Notification> }>
    ) {
      const { notifications } = action.payload;
      state.notifications = notifications;
      state.currentPageNum = 0;
      state.lastPage = notifications.last;
    },
    getMoreNotifications(
      state: NotificationState,
      action: PayloadAction<{ notifications: Page<Notification> }>
    ) {
      const { notifications } = action.payload;
      state.notifications.content = state.notifications.content.concat(
        notifications.content
      );
      state.currentPageNum = state.currentPageNum + 1;
      state.lastPage = notifications.last;
    },
    newReceivedNotification(
      state: NotificationState,
      action: PayloadAction<{ notification: Notification }>
    ) {
      const { notification } = action.payload;
      state.notifications.content.unshift(
        notification
      );
    },
    editNotification(
      state: NotificationState,
      action: PayloadAction<{ notification: Notification }>
    ) {
      const { notification } = action.payload;
      state.notifications.content = state.notifications.content.map(
        (notification1) => {
          if (notification1.id === notification.id) {
            return notification;
          }
          return notification1;
        }
      );
    },
    readAll(
      state: NotificationState,
      action: PayloadAction<{}>
    ) {
      state.notifications.content = state.notifications.content.map(
        (notification1) => {
          return { ...notification1, seen: true };
        }
      );
    },
    setLoadingGet(
      state: NotificationState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    }
  }
});

export const reducer = slice.reducer;

export const getNotifications =
  (criteria: SearchCriteria): AppThunk =>
    async (dispatch) => {
      try {
        dispatch(slice.actions.setLoadingGet({ loading: true }));
        const notifications = await api.post<Page<Notification>>(
          `${basePath}/search`,
          criteria
        );
        dispatch(slice.actions.getNotifications({ notifications }));
      } finally {
        dispatch(slice.actions.setLoadingGet({ loading: false }));
      }
    };

export const getMoreNotifications =
  (criteria: SearchCriteria, pageNum: number): AppThunk =>
    async (dispatch) => {
      criteria = { ...criteria, pageNum };
      try {
        dispatch(slice.actions.setLoadingGet({ loading: true }));
        const notifications = await api.post<Page<Notification>>(
          `${basePath}/search`,
          criteria
        );
        dispatch(slice.actions.getMoreNotifications({ notifications }));
      } finally {
        dispatch(slice.actions.setLoadingGet({ loading: false }));
      }
    };
export const editNotification =
  (id: number, notification): AppThunk =>
    async (dispatch) => {
      const notificationResponse = await api.patch<Notification>(
        `${basePath}/${id}`,
        notification
      );
      dispatch(
        slice.actions.editNotification({ notification: notificationResponse })
      );
    };
export const readAllNotifications =
  (): AppThunk =>
    async (dispatch) => {
      const notificationResponse = await api.get<{ success: boolean }>(
        `${basePath}/read-all`
      );
      if (notificationResponse.success)
        dispatch(
          slice.actions.readAll({})
        );
    };
export const newReceivedNotification =
  (notification: Notification): AppThunk =>
    async (dispatch) => {
      dispatch(
        slice.actions.newReceivedNotification({ notification })
      );
      if (notification.notificationType === 'WORK_ORDER')
        dispatch(getWorkOrders({
          filterFields: [
            {
              field: 'priority',
              operation: 'in',
              values: ['NONE', 'LOW', 'MEDIUM', 'HIGH'],
              value: '',
              enumName: 'PRIORITY'
            },
            {
              field: 'status',
              operation: 'in',
              values: ['OPEN', 'IN_PROGRESS', 'ON_HOLD'],
              value: '',
              enumName: 'STATUS'
            },
            {
              field: 'archived',
              operation: 'eq',
              value: false
            }
          ],
          pageSize: 10,
          pageNum: 0,
          direction: 'DESC'
        }));
    };
export default slice;
