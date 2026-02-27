export interface Task {
  id: string;
  attachments: number | null;
  progress: number;
  sub_items: number;
  description: string | null;
  due_date: number | null;
  comments: number;
  listId: string;
  memberIds: string[];
  name: string;
}

export interface List {
  id: string;
  name: string;
  color: string;
  taskIds: string[];
}

export interface Member {
  id: string;
  avatar: string | null;
  name: string;
}

export interface Project {
  tasks: Task[];
  lists: List[];
  members: Member[];
}
