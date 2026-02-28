import { GeneralPreferences } from './generalPreferences';
import { WorkOrderConfiguration } from './workOrderConfiguration';
import { WorkOrderRequestConfiguration } from './workOrderRequestConfiguration';

export default interface CompanySettings {
  id: number;
  generalPreferences: GeneralPreferences;
  workOrderConfiguration: WorkOrderConfiguration;
  workOrderRequestConfiguration: WorkOrderRequestConfiguration;
}
