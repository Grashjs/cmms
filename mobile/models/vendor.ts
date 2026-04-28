import { Audit } from './audit';
import { CustomFieldValue } from './customField';

export interface Vendor extends Audit {
  id: number;
  companyName: string;
  address: string;
  phone: string;
  website: string;
  name: string;
  email: string;
  vendorType: string;
  description: string;
  rate: number;
  customFieldValues?: CustomFieldValue[];
}

export interface VendorMiniDTO {
  companyName: string;
  id: number;
}
export const vendors: Vendor[] = [
  {
    id: 2,
    companyName: 'string',
    address: 'string',
    phone: 'string',
    website: 'string',
    name: 'string',
    email: 'string',
    vendorType: 'string',
    description: 'string',
    rate: 2,
    createdBy: 1,
    updatedBy: 4,
    createdAt: 'sd',
    updatedAt: 'sdd'
  }
];
