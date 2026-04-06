import { Audit } from './audit';

export type WebhookEvent =
  | 'ASSET_STATUS_CHANGE'
  | 'METER_TRIGGER_STATUS_CHANGE'
  | 'NEW_ASSET'
  | 'NEW_CATEGORY_ON_WORK_ORDER'
  | 'NEW_COMMENT_ON_WORK_ORDER'
  | 'NEW_LOCATION'
  | 'NEW_PART'
  | 'NEW_PURCHASE_ORDER'
  | 'NEW_VENDOR'
  | 'NEW_WORK_ORDER'
  | 'NEW_REQUEST'
  | 'PART_CHANGE'
  | 'PART_DELETE'
  | 'PART_QUANTITY_CHANGED'
  | 'PURCHASE_ORDER_CHANGE'
  | 'PURCHASE_ORDER_STATUS_CHANGE'
  | 'WORK_ORDER_CHANGE'
  | 'WORK_ORDER_DELETE'
  | 'WORK_ORDER_OVERDUE'
  | 'WORK_ORDER_STATUS_CHANGE'
  | 'WORK_REQUEST_STATUS_CHANGE';

export type WOField =
  | 'ASSET'
  | 'ASSIGNEES'
  | 'CATEGORY'
  | 'DESCRIPTION'
  | 'DUE_DATE'
  | 'ESTIMATED_DURATION'
  | 'LOCATION'
  | 'PARTS'
  | 'PRIORITY'
  | 'TITLE'
  | 'TEAM'
  | 'CUSTOMERS';

export type PartField =
  | 'NAME'
  | 'COST'
  | 'ASSIGNED_TO'
  | 'BARCODE'
  | 'DESCRIPTION'
  | 'CATEGORY'
  | 'QUANTITY'
  | 'AREA'
  | 'ADDITIONAL_INFOS'
  | 'NON_STOCK'
  | 'CUSTOMERS'
  | 'VENDORS'
  | 'MIN_QUANTITY'
  | 'TEAMS'
  | 'ASSETS'
  | 'MULTI_PARTS'
  | 'UNIT';

export interface WebhookEndpointPostDTO {
  url: string;
  code?: string;
  event?: WebhookEvent;
  assetStatuses?: string[];
  workOrderStatuses?: string[];
  workOrderCategories?: { id: number; name: string }[];
  woFields?: WOField[];
  partFields?: PartField[];
}

export interface WebhookEndpointShowDTO extends Audit {
  id: number;
  url: string;
  code: string;
  event: WebhookEvent;
  secret: string;
  assetStatuses: string[];
  workOrderStatuses: string[];
  workOrderCategories: { id: number; name: string }[];
  woFields: WOField[];
  partFields: PartField[];
  lastTriggeredAt: string | null;
  createdBy: number;
  createdByName: string;
}

export const CHANGE_EVENTS: WebhookEvent[] = [
  'ASSET_STATUS_CHANGE',
  'WORK_ORDER_STATUS_CHANGE',
  'WORK_REQUEST_STATUS_CHANGE',
  'PURCHASE_ORDER_STATUS_CHANGE',
  'WORK_ORDER_CHANGE',
  'PART_CHANGE',
  'PURCHASE_ORDER_CHANGE'
];

export const EVENT_REQUIRES_STATUS_FILTER: Record<WebhookEvent, boolean> = {
  ASSET_STATUS_CHANGE: true,
  WORK_ORDER_STATUS_CHANGE: true,
  WORK_REQUEST_STATUS_CHANGE: false,
  PURCHASE_ORDER_STATUS_CHANGE: false,
  WORK_ORDER_CHANGE: false,
  PART_CHANGE: false,
  PURCHASE_ORDER_CHANGE: false,
  METER_TRIGGER_STATUS_CHANGE: false,
  NEW_ASSET: false,
  NEW_CATEGORY_ON_WORK_ORDER: false,
  NEW_COMMENT_ON_WORK_ORDER: false,
  NEW_LOCATION: false,
  NEW_PART: false,
  NEW_PURCHASE_ORDER: false,
  NEW_VENDOR: false,
  NEW_WORK_ORDER: false,
  NEW_REQUEST: false,
  PART_DELETE: false,
  PART_QUANTITY_CHANGED: false,
  WORK_ORDER_DELETE: false,
  WORK_ORDER_OVERDUE: false
};
