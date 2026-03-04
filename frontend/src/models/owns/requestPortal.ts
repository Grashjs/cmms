import { Audit } from './audit';
import Asset from './asset';

export interface RequestPortal extends Audit {
  title: string;
  welcomeMessage: string;
  uuid: string;
  fields: RequestPortalField[];
}
export interface RequestPortalField {
  type: PortalFieldType;
  location: Location | null;
  asset: Asset | null;
  required: boolean;
}
export type PortalFieldType =
  | 'ASSET'
  | 'DESCRIPTION'
  | 'CONTACT'
  | 'IMAGE'
  | 'FILES';

export interface RequestPortalPostDTO {
  title: string;
  welcomeMessage: string;
  fields: RequestPortalField[];
}
