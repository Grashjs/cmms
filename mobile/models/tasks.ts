import { AssetMiniDTO } from './asset';
import File from './file';
import { UserMiniDTO } from './user';
import { Audit } from './audit';

export type TaskType =
  | 'SUBTASK'
  | 'NUMBER'
  | 'TEXT'
  | 'INSPECTION'
  | 'MULTIPLE'
  | 'METER';

export interface TaskOption {
  id: number;
  label: string;
}

export interface TaskBase {
  id: number;
  label: string;
  taskType: TaskType;
  options?: TaskOption[];
  user?: UserMiniDTO;
  asset?: AssetMiniDTO;
  meter?: number;
}

export interface Task extends Audit {
  id: number;
  value?: string | number;
  notes: string;
  taskBase: TaskBase;
  images: File[];
}

export const isTask = (object): object is Task => {
  return !!object.taskBase;
};
