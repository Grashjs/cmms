import { UserMiniDTO } from '../user';
import { Audit } from './audit';

export default interface Team extends Audit {
  id: number;
  name: string;
  description?: string;
  users?: UserMiniDTO[];
}
export interface TeamMiniDTO {
  name: string;
  id: number;
}
export const teams: Team[] = [
  {
    id: 1,
    name: 'Team one',
    description: 'first',
    users: [],
    createdBy: 1,
    updatedBy: 4,
    createdAt: 'sd',
    updatedAt: 'sdd'
  },
  {
    id: 2,
    name: 'Team two',
    description: 'second team',
    users: [],
    createdBy: 1,
    updatedBy: 4,
    createdAt: 'sd',
    updatedAt: 'sdd'
  }
];
