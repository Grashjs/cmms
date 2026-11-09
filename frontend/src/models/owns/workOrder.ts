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
import { CustomField } from './customField';

export type Priority = 'HIGH' | 'MEDIUM' | 'LOW' | 'NONE';

export interface WorkOrderStatusGuard {
  baseRevision: number;
  activeTimerRuns: { laborId: number; timerRunId: string }[];
}

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
  timerStatusRevision: number;
  audioDescription: File;
  customId: string;
  //parentPreventiveMaintenance:
}

export interface WorkOrderMini {
  id: number;
  title: string;
  status: string;
  createdAt: string;
}
