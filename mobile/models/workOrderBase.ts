import File from './file';
import { AssetMiniDTO } from './asset';
import { LocationMiniDTO } from './location';
import { UserMiniDTO } from './user';
import Team from './team';
import { Audit } from './audit';
import { CustomerMiniDTO } from './customer';
import Category from './category';
import { Priority } from './workOrder';

export interface WorkOrderBase extends Audit {
  title: string;
  id: number;
  description: string;
  priority: Priority;
  estimatedStartDate: string;
  estimatedDuration: number;
  image: File;
  asset: AssetMiniDTO;
  location: LocationMiniDTO;
  primaryUser: UserMiniDTO;
  assignedTo: UserMiniDTO[];
  customers: CustomerMiniDTO[];
  dueDate: string;
  category: Category | null;
  team: Team;
  files: File[];
}
