export type ProjectStatus = 'not_started' | 'in_progress' | 'completed';

export interface Member {
  id: string;
  avatar: string | null;
  name: string;
}

export interface Project {
  id: string;
  name: string;
  screenshot: string;
  description: string;
  tags: string[];
  startDate: number;
  dueDate: number;
  memberIds: Member[];
  progress: number;
  status: ProjectStatus;
  [key: string]: any;
}
