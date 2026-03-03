import { Audit } from './audit';
import User, { users } from './user';
import Category, { categories } from './category';

export default interface Labor extends Audit {
  assignedTo: User | null;
  id: number;
  hourlyRate: number;
  includeToTotalTime: boolean;
  startedAt: string;
  timeCategory: Category;
  logged: boolean;
  status: 'RUNNING' | 'STOPPED';
  duration: number;
}

export const labors: Labor[] = [
  {
    assignedTo: users[0],
    id: 54,
    hourlyRate: 22,
    includeToTotalTime: false,
    startedAt: 'fdtyg',
    timeCategory: categories[0],
    logged: false,
    duration: 4,
    createdAt: 'fghb',
    status: 'STOPPED',
    createdBy: 1,
    updatedAt: 'string',
    updatedBy: 1
  }
];
