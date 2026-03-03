import { WorkOrderBase } from './workOrderBase';
import Schedule from './schedule';
import { Task } from './tasks';

export default interface PreventiveMaintenance extends WorkOrderBase {
  name: string;
  schedule: Schedule;
  customId: string;
  nextWorkOrderDate: string;
}
export interface PreventiveMaintenancePost extends PreventiveMaintenance {
  frequency: number;
  startsOn: string;
  endsOn: string;
  tasks: Task[];
}
