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
  customFieldValues: CustomFieldValue[];
}

export interface VendorMiniDTO {
  companyName: string;
  id: number;
}
