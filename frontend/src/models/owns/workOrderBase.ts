import File, { FileThumbnailDTO } from './file';
import { AssetMiniDTO } from './asset';
import { LocationMiniDTO } from './location';
import { UserMiniDTO } from '../user';
import Team from './team';
import { Audit } from './audit';
import { CustomerMiniDTO } from './customer';
import Category from './category';
import { Priority } from './workOrder';
import { RequestPortalMiniDTO } from './requestPortal';
import { CustomField } from './customField';

export interface WorkOrderBase extends Audit {
  title: string;
  id: number;
  description: string;
  estimatedStartDate: string;
  estimatedDuration: number;
  priority: Priority;
  image: FileThumbnailDTO;
  asset: AssetMiniDTO;
  location: LocationMiniDTO;
  primaryUser: UserMiniDTO;
  assignedTo: UserMiniDTO[];
  customers: CustomerMiniDTO[];
  dueDate: string;
  category: Category | null;
  team: Team;
  files: File[];
  requestPortal: RequestPortalMiniDTO | null;
  requiredSignature: boolean;
  customFieldValues: { customField: CustomField; value: string }[];
}

export interface WorkOrderBaseMiniDTO {
  id: number;
  title: string;
  dueDate: string;
  createdAt: string;
  priority: Priority;
  // The backend DTO has always sent these two; the type simply never listed them.
  // customId is what people quote when they refer to an order ("WO000001").
  status: string;
  customId: string;
}
