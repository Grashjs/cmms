import { Audit } from './audit';
import Currency from './currency';
import { CustomFieldValue } from './customField';

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
  customFieldValues: CustomFieldValue[];
}
export interface CustomerMiniDTO {
  name: string;
  id: number;
}
