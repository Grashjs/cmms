import CompanySettings from './companySettings';
import OwnSubscription from './ownSubscription';
import File from './file';
import { Audit } from './audit';

export interface Company extends Audit {
  logo: File;
  name: string;
  address: string;
  website: string;
  phone: string;
  subscription: OwnSubscription;
  companySettings: CompanySettings;
  demo: boolean;
}
