import { UserMiniDTO } from '../user';
import { Audit } from './audit';
import { CustomerMiniDTO } from './customer';
import { VendorMiniDTO } from './vendor';
import { TeamMiniDTO } from './team';
import { FileMiniDTO } from './file';

export default interface Location extends Audit {
  id: number;
  name: string;
  address: string;
  longitude: number;
  image: FileMiniDTO;
  files: FileMiniDTO[];
  latitude: number;
  parentLocation: LocationMiniDTO | null;
  vendors: VendorMiniDTO[];
  customers: CustomerMiniDTO[];
  workers: UserMiniDTO[];
  teams: TeamMiniDTO[];
  customId: string;
}
export interface LocationMiniDTO {
  id: number;
  name: string;
  address: string;
  customId: string;
  parentId: number;
}

export interface LocationRow extends Location {
  hierarchy: number[];
  childrenFetched?: boolean;
}
