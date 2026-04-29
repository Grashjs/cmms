import { Audit } from './audit';
import File from './file';
import { UserMiniDTO } from './user';

export default interface Comment extends Audit {
  user: UserMiniDTO;
  content: string;
  files: File[];
  system: boolean;
}

export interface CommentPostDTO {
  workOrder: { id: number };
  content: string;
  files: { id: number }[];
}

export interface CommentPatchDTO {
  content: string;
  files: { id: number }[];
}
