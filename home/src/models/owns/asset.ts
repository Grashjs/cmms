import { UserMiniDTO } from '../user';
import { Audit } from './audit';
import { PartMiniDTO } from './part';
import { TeamMiniDTO } from './team';
import { VendorMiniDTO } from './vendor';
import Location from './location';
import { CustomerMiniDTO } from './customer';
import File, { FileMiniDTO } from './file';
import Category from './category';

export default interface Asset extends Audit {
  id: number;
  name: string;
  description: string;
  customId: string;
}

export const assetStatuses = [
  { status: 'OPERATIONAL', color: (theme) => theme.palette.success.main },
  { status: 'MODERNIZATION', color: (theme) => '#CBC3E3' },
  { status: 'DOWN', color: (theme) => theme.palette.error.main },
  { status: 'STANDBY', color: (theme) => theme.palette.primary.main },
  {
    status: 'INSPECTION_SCHEDULED',
    color: (theme) => theme.palette.warning.main
  },
  { status: 'COMMISSIONING', color: (theme) => 'grey' },
  { status: 'EMERGENCY_SHUTDOWN', color: (theme) => theme.palette.error.dark }
] as const;

export type AssetStatus = typeof assetStatuses[number]['status'];
export interface AssetDTO extends Audit {
  id: number;
  name: string;
  image: File;
  location: Location;
  area: string;
  model: string;
  serialNumber: string;
  status: AssetStatus;
  barCode: string;
  category: Category;
  description: string;
  primaryUser: UserMiniDTO;
  assignedTo: UserMiniDTO[];
  teams: TeamMiniDTO[];
  vendors: VendorMiniDTO[];
  customers: CustomerMiniDTO[];
  parentAsset: AssetMiniDTO;
  openWorkOrders: number;
  additionalInfos: string;
  hasChildren?: boolean;
  warrantyExpirationDate?: string;
  acquisitionCost: number;
  inServiceDate?: string;
  parts: PartMiniDTO[];
  files: FileMiniDTO[];
  power: string;
  manufacturer: string;
  customId: string;
}
export interface AssetRow extends AssetDTO {
  hierarchy: number[];
  childrenFetched?: boolean;
}
export interface AssetMiniDTO {
  id: number;
  name: string;
  customId: string;
  locationId: number;
  parentId: number;
}
