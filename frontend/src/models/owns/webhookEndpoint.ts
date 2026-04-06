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
  serialize?: boolean;
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
  serialize: boolean;
  lastTriggeredAt: string | null;
  createdBy: number;
  createdByName: string;
}

// Events that ask for asset statuses
export const EVENT_ASKS_ASSET_STATUSES: WebhookEvent[] = ['ASSET_STATUS_CHANGE'];

// Events that ask for work order statuses
export const EVENT_ASKS_WO_STATUSES: WebhookEvent[] = [
  'WORK_ORDER_STATUS_CHANGE',
  'WORK_REQUEST_STATUS_CHANGE'
];

// Events that ask for work order categories
export const EVENT_ASKS_WO_CATEGORIES: WebhookEvent[] = [
  'NEW_CATEGORY_ON_WORK_ORDER'
];

// Events that ask for WO fields (only WORK_ORDER_CHANGE)
export const EVENT_ASKS_WO_FIELDS: WebhookEvent[] = ['WORK_ORDER_CHANGE'];

// Events that ask for part fields
export const EVENT_ASKS_PART_FIELDS: WebhookEvent[] = ['PART_CHANGE'];

// New and delete events that show a serialize switch
export const EVENT_ASKS_SERIALIZE: WebhookEvent[] = [
  'NEW_ASSET',
  'NEW_CATEGORY_ON_WORK_ORDER',
  'NEW_COMMENT_ON_WORK_ORDER',
  'NEW_LOCATION',
  'NEW_PART',
  'NEW_PURCHASE_ORDER',
  'NEW_VENDOR',
  'NEW_WORK_ORDER',
  'NEW_REQUEST',
  'PART_DELETE',
  'WORK_ORDER_DELETE'
];
