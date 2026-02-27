import { Audit } from './audit';

export default interface Category extends Audit {
  id: number;
  name: string;
  description: string;
}
export interface CategoryMiniDTO {
  id: number;
  name: string;
}
export const categories: Category[] = [
  {
    id: 6,
    name: 'hdvykg',
    createdBy: 1,
    updatedBy: 4,
    createdAt: 'sd',
    updatedAt: 'sdd',
    description: ''
  },
  {
    id: 7,
    name: 'vvuykydr',
    createdBy: 1,
    updatedBy: 4,
    createdAt: 'sd',
    updatedAt: 'sdd',
    description: ''
  }
];
