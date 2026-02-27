import { Audit } from './audit';
import { UserMiniDTO } from '../user';
import { VendorMiniDTO } from './vendor';
import { CustomerMiniDTO } from './customer';
import { TeamMiniDTO } from './team';
import File from './file';
import Category from './category';

export default interface Part extends Audit {
  files: File[];
  id: number;
  name: string;
  cost: number;
  quantity: number;
  minQuantity: number;
  barcode: string;
  area: string;
  category: Category;
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
}
export interface PartMiniDTO {
  unit: string;
  name: string;
  id: number;
  cost: number;
  description: string;
}
