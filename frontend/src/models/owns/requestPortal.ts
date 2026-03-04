import { Audit } from './audit';
import Asset from './asset';
import Location from './location';

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
  | 'LOCATION'
  | 'FILES';

export interface RequestPortalPostDTO {
  title: string;
  welcomeMessage: string;
  fields: RequestPortalField[];
}

export interface RequestPortalPublicDTO {
  title: string;
  welcomeMessage: string;
  fields: RequestPortalField[];
  companyId: number;
  companyName: string;
  companyLogo: string;
}
