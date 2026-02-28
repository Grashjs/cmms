import { Priority } from './workOrder';
import { AssetMiniDTO, AssetStatus } from './asset';
import { LocationMiniDTO } from './location';
import { UserMiniDTO } from '../user';
import { TeamMiniDTO } from './team';
import Category from './category';
import { Checklist } from './checklists';
import { VendorMiniDTO } from './vendor';
import { PartMiniDTO } from './part';
import { PurchaseOrderStatus } from './purchaseOrder';

export const partActions = ['CREATE_PURCHASE_ORDER'] as const;
export const partConditions = ['PART_IS', 'QUANTITY_INFERIOR'] as const;
type PartAction = typeof partActions[number];
type PartCondition = typeof partConditions[number];
export const purchaseOrderActions = [
  'ASSIGN_VENDOR',
  'ASSIGN_CATEGORY',
  'APPROVE',
  'REJECT',
  'SEND_REMINDER_EMAIL'
] as const;
export const purchaseOrderConditions = [
  'VENDOR_IS',
  'CATEGORY_IS',
  'STATUS_IS',
  'DUE_DATE_BETWEEN',
  'DUE_DATE_AFTER'
] as const;
export const requestActions = [
  'ASSIGN_PRIORITY',
  'ASSIGN_ASSET',
  'ASSIGN_LOCATION',
  'ASSIGN_USER',
  'ASSIGN_TEAM',
  'ASSIGN_CATEGORY',
  'ADD_CHECKLIST',
  'APPROVE',
  'SEND_REMINDER_EMAIL'
] as const;
export const requestConditions = [
  'PRIORITY_IS',
  'ASSET_IS',
  'LOCATION_IS',
  'USER_IS',
  'TEAM_IS',
  'CATEGORY_IS',
  'CREATED_AT_BETWEEN',
  'DUE_DATE_BETWEEN',
  'DUE_DATE_AFTER',
  'TITLE_CONTAINS'
] as const;
export const taskActions = [
  'CREATE_REQUEST',
  'CREATE_WORK_ORDER',
  'SET_ASSET_STATUS'
] as const;
export const taskConditions = [
  'NAME_IS',
  'NAME_CONTAINS',
  'VALUE_IS',
  'VALUE_CONTAINS',
  'NUMBER_VALUE_SUPERIOR',
  'NUMBER_VALUE_INFERIOR'
] as const;
export const mainConditions = [
  'WORK_ORDER_CREATED',
  'WORK_ORDER_CLOSED',
  'WORK_ORDER_ARCHIVED',
  'REQUEST_CREATED',
  'REQUEST_APPROVED',
  'REQUEST_REJECTED',
  'PURCHASE_ORDER_CREATED',
  'PURCHASE_ORDER_UPDATED',
  'TASK_UPDATED',
  'PART_UPDATED'
] as const;
export const workOrderActions = [
  'ASSIGN_PRIORITY',
  'ASSIGN_ASSET',
  'ASSIGN_LOCATION',
  'ASSIGN_USER',
  'ASSIGN_TEAM',
  'ASSIGN_CATEGORY',
  'ADD_CHECKLIST',
  'SEND_REMINDER_EMAIL'
] as const;
export const workOrderConditions = [
  'PRIORITY_IS',
  'ASSET_IS',
  'LOCATION_IS',
  'USER_IS',
  'TEAM_IS',
  'CATEGORY_IS',
  'CREATED_AT_BETWEEN',
  'DUE_DATE_BETWEEN',
  'DUE_DATE_AFTER',
  'STATUS_IS',
  'TITLE_CONTAINS'
] as const;
type PurchaseOrderAction = typeof purchaseOrderActions[number];
type PurchaseOrderCondition = typeof purchaseOrderConditions[number];
type RequestAction = typeof requestActions[number];
type RequestCondition = typeof requestConditions[number];
type TaskAction = typeof taskActions[number];
type TaskCondition = typeof taskConditions[number];
type WorkOrderAction = typeof workOrderActions[number];
type WorkOrderCondition = typeof workOrderConditions[number];
export type WFMainCondition = typeof mainConditions[number];
export type WorkflowConditionType =
  | WorkOrderCondition
  | TaskCondition
  | RequestCondition
  | PurchaseOrderCondition
  | PartCondition;
export type WorkflowActionType =
  | WorkOrderAction
  | TaskAction
  | RequestAction
  | PurchaseOrderAction
  | PartAction;
export interface WorkflowAction {
  id: number;
  workOrderAction: WorkOrderAction;
  requestAction: RequestAction;
  purchaseOrderAction: PurchaseOrderAction;
  partAction: PartAction;
  taskAction: TaskAction;
  priority: Priority;
  asset: AssetMiniDTO;
  location: LocationMiniDTO;
  user: UserMiniDTO;
  team: TeamMiniDTO;
  workOrderCategory: Category;
  checklist: Checklist;
  vendor: VendorMiniDTO;
  purchaseOrderCategory: Category;
  value: string;
  assetStatus: AssetStatus;
  numberValue: number;
}
export interface WorkflowCondition {
  workOrderCondition: WorkOrderCondition;
  requestCondition: RequestCondition;
  purchaseOrderCondition: PurchaseOrderCondition;
  partCondition: PartCondition;
  taskCondition: TaskCondition;
  priority: Priority;
  asset: AssetMiniDTO;
  location: LocationMiniDTO;
  user: UserMiniDTO;
  team: TeamMiniDTO;
  workOrderCategory: Category;
  checklist: Checklist;
  createdTimeStart: number;
  createdTimeEnd: number;
  vendor: VendorMiniDTO;
  part: PartMiniDTO;
  purchaseOrderCategory: Category;
  workOrderStatus: string;
  purchaseOrderStatus: PurchaseOrderStatus;
  startDate: string;
  endDate: string;
  label: string;
  value: string;
  numberValue: number;
}
export interface Workflow {
  id: number;
  title: string;
  mainCondition: WFMainCondition;
  secondaryConditions: WorkflowCondition[];
  action: WorkflowAction;
  enabled: boolean;
}
