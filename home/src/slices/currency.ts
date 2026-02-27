import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import Currency from '../models/owns/currency';
import api from '../utils/api';

const basePath = 'currencies';
interface CurrencyState {
  currencies: Currency[];
}

const initialState: CurrencyState = {
  currencies: []
};

const slice = createSlice({
  name: 'currencies',
  initialState,
  reducers: {
    getCurrencies(
      state: CurrencyState,
      action: PayloadAction<{ currencies: Currency[] }>
    ) {
      const { currencies } = action.payload;
      state.currencies = currencies;
    }
  }
});

export const reducer = slice.reducer;

export const getCurrencies = (): AppThunk => async (dispatch) => {
  const currencies = await api.get<Currency[]>(basePath);
  dispatch(slice.actions.getCurrencies({ currencies }));
};

export default slice;
