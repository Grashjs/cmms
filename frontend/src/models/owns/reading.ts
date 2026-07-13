import { Audit } from './audit';

export default interface Reading extends Audit {
  id: number;
  value: number;
}

export interface ReadingHistogram {
  date: string;
  value: number;
  count: number;
}
