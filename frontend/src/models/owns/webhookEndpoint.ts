import { Audit } from './audit';

// -- Webhook Events --
export const WEBHOOK_EVENTS = [
  'ASSET_STATUS_CHANGE',
  'METER_TRIGGER_STATUS_CHANGE',
  'NEW_ASSET',
  'NEW_CATEGORY_ON_WORK_ORDER',
  'NEW_COMMENT_ON_WORK_ORDER',
  'NEW_LOCATION',
  'NEW_PART',
  'NEW_PURCHASE_ORDER',
  'NEW_VENDOR',
  'NEW_WORK_ORDER',
  'NEW_REQUEST',
  'PART_CHANGE',
  'PART_DELETE',
  'PART_QUANTITY_CHANGED',
  'PURCHASE_ORDER_CHANGE',
  'PURCHASE_ORDER_STATUS_CHANGE',
  'WORK_ORDER_CHANGE',
  'WORK_ORDER_DELETE',
  'WORK_ORDER_OVERDUE',
  'WORK_ORDER_STATUS_CHANGE',
  'WORK_REQUEST_STATUS_CHANGE'
] as const;

export type WebhookEvent = typeof WEBHOOK_EVENTS[number];

export const getWebhookEventLabelKey = (event: WebhookEvent) =>
  `WEBHOOK_${event}`;

// -- WO Fields --
export const WO_FIELDS = [
  { value: 'ASSET', labelKey: 'asset' },
  { value: 'ASSIGNEES', labelKey: 'assigned_to' },
  { value: 'CATEGORY', labelKey: 'category' },
  { value: 'DESCRIPTION', labelKey: 'description' },
  { value: 'DUE_DATE', labelKey: 'due_date' },
  { value: 'ESTIMATED_DURATION', labelKey: 'estimated_duration' },
  { value: 'LOCATION', labelKey: 'location' },
  { value: 'PARTS', labelKey: 'parts' },
  { value: 'PRIORITY', labelKey: 'priority' },
  { value: 'TITLE', labelKey: 'title' },
  { value: 'TEAM', labelKey: 'team' },
  { value: 'CUSTOMERS', labelKey: 'customers' }
] as const;

export type WOField = typeof WO_FIELDS[number]['value'];

// -- Part Fields --
export const PART_FIELDS = [
  { value: 'NAME', labelKey: 'name' },
  { value: 'COST', labelKey: 'cost' },
  { value: 'ASSIGNED_TO', labelKey: 'assigned_to' },
  { value: 'BARCODE', labelKey: 'barcode' },
  { value: 'DESCRIPTION', labelKey: 'description' },
  { value: 'CATEGORY', labelKey: 'category' },
  { value: 'QUANTITY', labelKey: 'quantity' },
  { value: 'AREA', labelKey: 'area' },
  { value: 'ADDITIONAL_INFOS', labelKey: 'additional_information' },
  { value: 'NON_STOCK', labelKey: 'non_stock' },
  { value: 'CUSTOMERS', labelKey: 'customers' },
  { value: 'VENDORS', labelKey: 'vendors' },
  { value: 'MIN_QUANTITY', labelKey: 'minimum_quantity' },
  { value: 'TEAMS', labelKey: 'teams' },
  { value: 'ASSETS', labelKey: 'assets' },
  { value: 'MULTI_PARTS', labelKey: 'sets_of_parts' },
  { value: 'UNIT', labelKey: 'unit' }
] as const;

export type PartField = typeof PART_FIELDS[number]['value'];

// -- Conditional field rules --
export const EVENT_ASKS_ASSET_STATUSES: WebhookEvent[] = [
  'ASSET_STATUS_CHANGE'
];

export const EVENT_ASKS_WO_STATUSES: WebhookEvent[] = [
  'WORK_ORDER_STATUS_CHANGE'
];

export const EVENT_ASKS_WR_APPROVED: WebhookEvent[] = [
  'WORK_REQUEST_STATUS_CHANGE'
];

export const EVENT_ASKS_WO_CATEGORIES: WebhookEvent[] = [
  'NEW_CATEGORY_ON_WORK_ORDER'
];

export const EVENT_ASKS_WO_FIELDS: WebhookEvent[] = ['WORK_ORDER_CHANGE'];

export const EVENT_ASKS_PART_FIELDS: WebhookEvent[] = ['PART_CHANGE'];

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

// -- DTOs --
export interface WebhookEndpointPostDTO {
  url: string;
  code?: string;
  event?: WebhookEvent;
  assetStatuses?: string[];
  workOrderStatuses?: string[];
  approved?: boolean;
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
  approved: boolean | null;
  workOrderCategories: { id: number; name: string }[];
  woFields: WOField[];
  partFields: PartField[];
  serialize: boolean;
  lastTriggeredAt: string | null;
  createdBy: number;
  createdByName: string;
}
