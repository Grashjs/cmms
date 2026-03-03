import { Audit } from './audit';

export type FileType = 'IMAGE' | 'OTHER';
export default interface File extends Audit {
  name: string;
  id: number;
  url: string;
  type: FileType;
  hidden: boolean;
}
export interface FileMiniDTO {
  name: string;
  id: number;
  url: string;
}
export const files: File[] = [
  {
    name: 'File1',
    id: 54,
    url: 'https://google.com',
    createdAt: 'fghb',
    createdBy: 1,
    updatedAt: 'string',
    updatedBy: 1,
    type: 'OTHER',
    hidden: false
  }
];
