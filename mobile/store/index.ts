import type { TypedUseSelectorHook } from 'react-redux';
import {
  useDispatch as useReduxDispatch,
  useSelector as useReduxSelector
} from 'react-redux';
import type { ThunkAction } from 'redux-thunk';
import type { Action } from '@reduxjs/toolkit';
import { configureStore } from '@reduxjs/toolkit';
import rootReducer from './rootReducer';
import { ImportResponse } from '../models/imports';
import { persistReducer, persistStore } from 'redux-persist';
import storage from 'redux-persist/lib/storage';
import { AsyncStorage } from 'react-native';

const persistConfig = {
  key: 'root',
  storage: AsyncStorage
};
const persistedReducer = persistReducer(persistConfig, rootReducer);

const store = configureStore({
  reducer: persistedReducer,
  devTools: true,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: false
    })
});

export type RootState = ReturnType<typeof store.getState>;

export type AppDispatch = typeof store.dispatch;

export type StoreReturnType =
  | void
  | number
  | number[]
  | string
  | ImportResponse;
export type AppThunk = ThunkAction<
  Promise<StoreReturnType>,
  RootState,
  null,
  Action<string>
>;

export const useSelector: TypedUseSelectorHook<RootState> = useReduxSelector;

export const useDispatch = () => useReduxDispatch<AppDispatch>();

export const persistor = persistStore(store);

export default store;
