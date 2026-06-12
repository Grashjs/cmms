import { Audit } from './owns/audit';
import { Role } from './owns/role';
import File, { FileThumbnailDTO } from './owns/file';
import { UiConfiguration } from './owns/uiConfiguration';

export type UserRole = 'admin' | 'customer' | 'subscriber';

export interface User {
  id: string;
  avatar: string;
  email: string;
  name: string;
  jobtitle: string;
  username: string;
  location: string;
  role: UserRole;
  posts: string;
  coverImg: string;
  followers: string;
  description: string;
  [key: string]: any;
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
  image: FileThumbnailDTO;
  lastLogin: string;
  enabled: boolean;
  paddleUserId: string;
}
export interface UserMiniDTO {
  firstName: string;
  lastName: string;
  image: FileThumbnailDTO;
  id: number;
}

export interface ShiftDayConfiguration {
  dayOfWeek: string;
  availabilityMinutes: number;
  enabled: boolean;
}

export interface ShiftException {
  exceptionDate: string;
  availabilityMinutes: number;
  enabled: boolean;
}

export interface ShiftConfigurationShowDTO {
  id: number;
  days: ShiftDayConfiguration[];
  exceptions: ShiftException[];
  enabled: boolean;
}

export interface UserResponseDTO extends OwnUser {
  companySettingsId: number;
  userSettingsId: number;
  superAccountRelations: SuperAccountRelation[];
  parentSuperAccount: SuperAccountRelation;
  uiConfiguration: UiConfiguration;
  shiftConfiguration?: ShiftConfigurationShowDTO;
}
export interface SuperAccountRelation {
  childCompanyName: string;
  childCompanyLogo: File;
  childUserId: number;
  superUserId: number;
}
