import { AssetMiniDTO } from './asset';
import File from './file';
import { UserMiniDTO } from './user';

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

export interface Task {
  id: number;
  value?: string | number;
  notes: string;
  taskBase: TaskBase;
  images: File[];
}

export const isTask = (object): object is Task => {
  return !!object.taskBase;
};
export const tasks: Task[] = [
  {
    id: 74,
    taskBase: {
      id: 12,
      label: 'Clean air filter & check its condition',
      taskType: 'SUBTASK'
    },
    value: 'OPEN',
    notes: '',
    images: []
  },
  {
    id: 75,
    taskBase: {
      id: 11,
      label: 'Check nothing',
      taskType: 'SUBTASK'
    },
    value: 'OPEN',
    notes: '',
    images: []
  }
];
