import { WorkOrderBase } from './workOrderBase';

export default interface WorkOrderMeterTrigger extends WorkOrderBase {
  recurrent: boolean;
  name: string;
  triggerCondition: 'LESS_THAN' | 'MORE_THAN';
  value: number;
  waitBefore: number;
}
