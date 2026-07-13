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
export const customers: Customer[] = [
  {
    id: 1,
    name: 'Customer 1',
    address: 'casa, maroc',
    phone: '+00212611223344',
    website: 'https://web-site.com',
    email: 'john.doe@gmail.com',
    customerType: 'Plumbing',
    description: 'Describe...',
    rate: 10,
    billingAddress: 'Add 1',
    billingAddress2: '-',
    billingName: 'Add 3',
    billingCurrency: null,
    createdBy: 1,
    updatedBy: 4,
    createdAt: 'sd',
    updatedAt: 'sdd'
  },
  {
    id: 2,
    name: 'Customer 2',
    address: 'casa, maroc',
    phone: '+00212611223344',
    website: 'https://web-site.com',
    email: 'john.doe@gmail.com',
    customerType: 'Electrical',
    description: 'Describe 2...',
    rate: 15,
    billingAddress: 'Add 1',
    billingAddress2: '-',
    billingName: '-',
    billingCurrency: null,
    createdBy: 1,
    updatedBy: 4,
    createdAt: 'sd',
    updatedAt: 'sdd'
  }
];
