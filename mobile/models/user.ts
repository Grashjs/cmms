import { Audit } from './audit';
import { Role } from './role';
import File from './file';
import { UiConfiguration } from './uiConfiguration';
import { SupportedLanguage } from '../i18n/i18n';

export type UserRole = 'admin' | 'customer' | 'subscriber';
export default interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  jobTitle: string;
  companyName: string;
  lastVisit: string;
  hourlyRate: number;
}

export interface OwnUser extends Audit {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  rate: number;
  phone: string;
  ownsCompany: boolean;
  jobTitle: string;
  role: Role;
  companyId: number;
  image: File;
  language: SupportedLanguage;
}

export interface UserMiniDTO {
  firstName: string;
  lastName: string;
  image: File;
  id: number;
  phone: string;
}

export interface UserResponseDTO extends OwnUser {
  companySettingsId: number;
  userSettingsId: number;
  superAccountRelations: SuperAccountRelation[];
  parentSuperAccount: SuperAccountRelation;
  uiConfiguration: UiConfiguration;
}
export interface SuperAccountRelation {
  childCompanyName: string;
  childCompanyLogo: File;
  childUserId: number;
  superUserId: number;
}
export const users: User[] = [
  {
    id: 1,
    firstName: 'John',
    lastName: 'Doe',
    email: 'john.doe@gmail.com',
    phone: '+00212611223344',
    jobTitle: 'Job',
    companyName: 'Company',
    lastVisit: '02/09/22',
    hourlyRate: 4
  },
  {
    id: 2,
    firstName: 'John',
    lastName: 'Jr',
    email: 'john.doe@gmail.com',
    phone: '+00212611223344',
    jobTitle: 'Job',
    companyName: 'Company',
    lastVisit: '02/09/22',
    hourlyRate: 8
  }
];
