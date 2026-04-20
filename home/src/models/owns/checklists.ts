import { TaskBase, tasks } from './tasks';

export interface Checklist {
  id: number;
  name: string;
  category: string;
  description: string;
  taskBases: TaskBase[];
}
export const checklists = [
  {
    id: 44,
    name: 'Checklist1',
    taskBases: tasks.map((task) => task.taskBase),
    category: 'SSd',
    description: 'dsds'
  },
  {
    id: 11,
    name: 'Checklist2',
    taskBases: tasks.map((task) => task.taskBase),
    category: 'FSdd',
    description: 'fafay'
  }
];
