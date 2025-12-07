import { Audit } from './audit';
import Request from './request';
import Team from './team';
import Asset from './asset';
import File from './file';
import Location from './location';
import Category from './category';
import { OwnUser, UserMiniDTO } from '../user';
import { CustomerMiniDTO } from './customer';
import PreventiveMaintenance from './preventiveMaintenance';
import { WorkOrderBase } from './workOrderBase';

export type Priority = 'HIGH' | 'MEDIUM' | 'LOW' | 'NONE';

export default interface WorkOrder extends WorkOrderBase {
  category: Category | null;
  id: number;
  completedBy: OwnUser;
  completedOn: string;
  archived: boolean;
  parentRequest: Request;
  parentPreventiveMaintenance: PreventiveMaintenance;
  signature: string;
  feedback: string;
  requiredSignature: boolean;
  status: string;
  audioDescription: File;
  customId: string;
  //parentPreventiveMaintenance:
}
