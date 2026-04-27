import { Audit } from './audit';
import { UserMiniDTO } from './user';
import { VendorMiniDTO } from './vendor';
import { CustomerMiniDTO } from './customer';
import { TeamMiniDTO } from './team';
import File from './file';
import { CustomFieldValue } from './customField';

export default interface Part extends Audit {
  files: File[];
  id: number;
  name: string;
  cost: number;
  quantity: number;
  minQuantity: number;
  barcode: string;
  area: string;
  category: string;
  nonStock: boolean;
  additionalInfos: string;
  image: File | null;
  description: string;
  assignedTo: UserMiniDTO[];
  vendors: VendorMiniDTO[];
  customers: CustomerMiniDTO[];
  teams: TeamMiniDTO[];
  openWorkOrders: number;
  unit: string;
  customFieldValues?: CustomFieldValue[];
}
export interface PartMiniDTO {
  name: string;
  id: number;
  cost: number;
  description: string;
}
