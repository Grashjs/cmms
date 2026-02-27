import type { PayloadAction } from '@reduxjs/toolkit';
import { createSlice } from '@reduxjs/toolkit';
import type { AppThunk } from 'src/store';
import { Customer, CustomerMiniDTO } from '../models/owns/customer';
import api from '../utils/api';
import { getInitialPage, Page, SearchCriteria } from '../models/owns/page';
import { revertAll } from 'src/utils/redux';

const basePath = 'customers';

interface CustomerState {
  customers: Page<Customer>;
  singleCustomer: Customer;
  customersMini: CustomerMiniDTO[];
  loadingGet: boolean;
}

const initialState: CustomerState = {
  customers: getInitialPage<Customer>(),
  singleCustomer: null,
  customersMini: [],
  loadingGet: false
};

const slice = createSlice({
  name: 'customers',
  initialState,
  extraReducers: (builder) => builder.addCase(revertAll, () => initialState),
  reducers: {
    getCustomers(
      state: CustomerState,
      action: PayloadAction<{ customers: Page<Customer> }>
    ) {
      const { customers } = action.payload;
      state.customers = customers;
    },
    getSingleCustomer(
      state: CustomerState,
      action: PayloadAction<{ customer: Customer }>
    ) {
      const { customer } = action.payload;
      state.singleCustomer = customer;
    },
    editCustomer(
      state: CustomerState,
      action: PayloadAction<{ customer: Customer }>
    ) {
      const { customer } = action.payload;
      const inContent = state.customers.content.some(
        (customer1) => customer1.id === customer.id
      );
      if (inContent) {
        state.customers.content = state.customers.content.map((customer1) => {
          if (customer1.id === customer.id) {
            return customer;
          }
          return customer1;
        });
      } else {
        state.singleCustomer = customer;
      }
    },
    getCustomersMini(
      state: CustomerState,
      action: PayloadAction<{ customers: CustomerMiniDTO[] }>
    ) {
      const { customers } = action.payload;
      state.customersMini = customers;
    },
    addCustomer(
      state: CustomerState,
      action: PayloadAction<{ customer: Customer }>
    ) {
      const { customer } = action.payload;
      state.customers.content = [customer, ...state.customers.content];
    },
    deleteCustomer(
      state: CustomerState,
      action: PayloadAction<{ id: number }>
    ) {
      const { id } = action.payload;
      const customerIndex = state.customers.content.findIndex(
        (customer) => customer.id === id
      );
      state.customers.content.splice(customerIndex, 1);
    },
    setLoadingGet(
      state: CustomerState,
      action: PayloadAction<{ loading: boolean }>
    ) {
      const { loading } = action.payload;
      state.loadingGet = loading;
    },
    clearSingleCustomer(state: CustomerState, action: PayloadAction<{}>) {
      state.singleCustomer = null;
    }
  }
});

export const reducer = slice.reducer;

export const getCustomers =
  (criteria: SearchCriteria): AppThunk =>
    async (dispatch) => {
      try {
        dispatch(slice.actions.setLoadingGet({ loading: true }));
        const customers = await api.post<Page<Customer>>(
          `${basePath}/search`,
          criteria
        );
        dispatch(slice.actions.getCustomers({ customers }));
      } finally {
        dispatch(slice.actions.setLoadingGet({ loading: false }));
      }
    };

export const getSingleCustomer =
  (id: number): AppThunk =>
    async (dispatch) => {
      dispatch(slice.actions.setLoadingGet({ loading: true }));
      const customer = await api.get<Customer>(`${basePath}/${id}`);
      dispatch(slice.actions.getSingleCustomer({ customer }));
      dispatch(slice.actions.setLoadingGet({ loading: false }));
    };

export const editCustomer =
  (id: number, customer): AppThunk =>
    async (dispatch) => {
      const customerResponse = await api.patch<Customer>(
        `${basePath}/${id}`,
        customer
      );
      dispatch(slice.actions.editCustomer({ customer: customerResponse }));
    };
export const getCustomersMini = (): AppThunk => async (dispatch) => {
  const customers = await api.get<CustomerMiniDTO[]>('customers/mini');
  dispatch(slice.actions.getCustomersMini({ customers }));
};
export const addCustomer =
  (customer): AppThunk =>
    async (dispatch) => {
      const customerResponse = await api.post<Customer>('customers', customer);
      dispatch(slice.actions.addCustomer({ customer: customerResponse }));
    };
export const deleteCustomer =
  (id: number): AppThunk =>
    async (dispatch) => {
      const customerResponse = await api.deletes<{ success: boolean }>(
        `customers/${id}`
      );
      const { success } = customerResponse;
      if (success) {
        dispatch(slice.actions.deleteCustomer({ id }));
      }
    };
export const clearSingleCustomer = (): AppThunk => async (dispatch) => {
  dispatch(slice.actions.clearSingleCustomer({}));
};
export default slice;
