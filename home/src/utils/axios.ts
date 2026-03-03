import axios from 'axios';
import AxiosMockAdapter from 'axios-mock-adapter';

const axiosInt = axios.create();

axiosInt.interceptors.response.use(
  (response) => response,
  (error) =>
    Promise.reject(
      (error.response && error.response.data) || 'There is an error!'
    )
);

export const mock = new AxiosMockAdapter(axiosInt, { delayResponse: 0 });

export default axiosInt;
