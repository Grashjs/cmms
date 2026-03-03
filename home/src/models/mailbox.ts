export interface Mail {
  id: string;
  category: string;
  opened: boolean;
  tagIds: string[];
  subject: string;
  summary: string;
  content: string;
  from: {
    name: string;
    email: string;
    avatar: string | null;
  };
  to: {
    name: string;
    email: string;
    avatar: string | null;
  }[];
  date: number;
}

export interface Tag {
  id: string;
  type: string;
  name: string;
  newMails?: number;
  color?: string;
}
