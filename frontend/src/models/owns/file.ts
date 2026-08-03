import { Audit } from './audit';
import { AssetMiniDTO } from './asset';
import { WorkOrderBaseMiniDTO } from './workOrderBase';

export type FileType = 'IMAGE' | 'OTHER';
export default interface File extends Audit {
  name: string;
  id: number;
  url: string;
  type: FileType;
  hidden: boolean;
  // What the file is attached to. Only assets and work orders are exposed; the entity has
  // the same relation to parts, locations and requests, but nothing needs them yet.
  assets: AssetMiniDTO[];
  workOrders: WorkOrderBaseMiniDTO[];
}
export interface FileMiniDTO {
  name: string;
  id: number;
  url: string;
}
export interface FileThumbnailDTO extends FileMiniDTO {
  thumbnailUrl: string | null;
}
export const files: File[] = [
  {
    name: 'File1',
    id: 54,
    url: 'https://google.com',
    createdAt: 'fghb',
    createdBy: 1,
    updatedAt: 'string',
    updatedBy: 1,
    type: 'OTHER',
    hidden: false,
    assets: [],
    workOrders: []
  }
];
