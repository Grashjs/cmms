import { Audit } from './audit';

export default interface Reading extends Audit {
  id: number;
  value: number;
  date: string;
}
