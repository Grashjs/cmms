import { FieldConfiguration } from './fieldConfiguration';

export interface WorkOrderConfiguration {
  id: number;
  workOrderFieldConfigurations: FieldConfiguration[];
}
