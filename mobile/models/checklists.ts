import { TaskBase } from './tasks';

export interface Checklist {
  id: number;
  name: string;
  category: string;
  description: string;
  taskBases: TaskBase[];
}
