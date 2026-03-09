import { Audit } from './audit';
import Asset from './asset';
import Location from './location';
import { SupportedLanguage } from '../../i18n/i18n';

export interface RequestPortal extends Audit {
  title: string;
  welcomeMessage: string;
  uuid: string;
  companyLogo: string;
  companyId: number;
  companyName: string;
  companyLanguage: SupportedLanguage;
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

export interface RequestPortalMiniDTO extends Audit {
  title: string;
  uuid: string;
}
