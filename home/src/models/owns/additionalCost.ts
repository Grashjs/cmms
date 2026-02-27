import { Audit } from './audit';
import User, { users } from './user';
import Category, { categories } from './category';

export default interface AdditionalCost extends Audit {
  assignedTo: User;
  id: number;
  description: string;
  cost: number;
  date: string;
  includeToTotalCost: boolean;
  category: Category;
}

export const additionalCosts: AdditionalCost[] = [
  {
    assignedTo: users[0],
    id: 54,
    description: 'Description',
    includeToTotalCost: false,
    date: 'fchtjg',
    category: categories[1],
    cost: 75,
    createdAt: 'fghb',
    createdBy: 1,
    updatedAt: 'string',
    updatedBy: 1
  }
];
