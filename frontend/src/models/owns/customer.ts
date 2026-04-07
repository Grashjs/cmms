import { Audit } from './audit';
import Currency from './currency';

export interface Customer extends Audit {
  id: number;
  name: string;
  address: string;
  phone: string;
  website: string;
  email: string;
  customerType: string;
  description: string;
  rate: number;
  billingAddress: string;
  billingAddress2: string;
  billingName: string;
  billingCurrency: Currency;
}
export interface CustomerMiniDTO {
  name: string;
  id: number;
}
