import { Audit } from './audit';

export default interface Schedule extends Audit {
  disabled: boolean;
  name: string;
  startsOn: string;
  endsOn: string;
  frequency: number;
  dueDateDelay: number;
}
