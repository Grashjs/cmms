import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import { SubscriptionPlan } from '../models/owns/subscriptionPlan';
import api from '../utils/api';

const basePath = 'subscription-plans';
interface SubscriptionPlanState {
  subscriptionPlans: SubscriptionPlan[];
}

const initialState: SubscriptionPlanState = {
  subscriptionPlans: []
};

const slice = createSlice({
  name: 'subscriptionPlans',
  initialState,
  reducers: {
    getSubscriptionPlans(
      state: SubscriptionPlanState,
      action: PayloadAction<{ subscriptionPlans: SubscriptionPlan[] }>
    ) {
      const { subscriptionPlans } = action.payload;
      state.subscriptionPlans = subscriptionPlans.filter(
        (plan) => plan.code !== 'FREE'
      );
    }
  }
});

export const reducer = slice.reducer;

export const getSubscriptionPlans = (): AppThunk => async (dispatch) => {
  const subscriptionPlans = await api.get<SubscriptionPlan[]>(basePath);
  dispatch(slice.actions.getSubscriptionPlans({ subscriptionPlans }));
};

export default slice;
