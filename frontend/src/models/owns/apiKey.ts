import { Audit } from './audit';
import { UserMiniDTO } from './user';

export interface ApiKey extends Audit {
  id: number;
  label: string;
  code: string;
  user: UserMiniDTO;
  lastUsed?: Date;
  expiresAt: string;
  revokedAt: string;
}

export interface ApiKeyPostDTO {
  label: string;
}

export interface ApiKeyShowDTO extends Audit {
  id: number;
  label: string;
  code?: string;
  user: UserMiniDTO;
  lastUsed?: Date;
  expiresAt?: string;
  revokedAt?: string;
}

export interface ApiKeyCriteria {
  active?: boolean;
}
