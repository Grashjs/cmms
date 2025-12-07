import { Audit } from './audit';

export default interface Schedule extends Audit {
  disabled: boolean;
  name: string;
  startsOn: string;
  endsOn: string;
  frequency: number;
  dueDateDelay: number;
  recurrenceBasedOn: 'SCHEDULED_DATE' | 'COMPLETED_DATE';
  recurrenceType: 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';
  daysOfWeek: number[];
}
