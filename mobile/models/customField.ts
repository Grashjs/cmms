export interface CustomField {
  id: number;
  label: string;
  fieldType: CustomFieldType;
  entityType: CustomFieldEntityType;
  required: boolean;
  copyOnRepeat: boolean;
  options?: string[];
  order: number;
  createdAt: string;
  updatedAt: string;
}

export enum CustomFieldType {
  SHORT_TEXT = 'SHORT_TEXT',
  LONG_TEXT = 'LONG_TEXT',
  NUMBER = 'NUMBER',
  SINGLE_CHOICE = 'SINGLE_CHOICE',
  DATE = 'DATE',
  DATE_TIME = 'DATE_TIME',
  LINK = 'LINK'
}

export enum CustomFieldEntityType {
  WORK_ORDER = 'WORK_ORDER',
  ASSET = 'ASSET',
  LOCATION = 'LOCATION',
  CUSTOMER = 'CUSTOMER',
  VENDOR = 'VENDOR',
  PART = 'PART',
  PURCHASE_REQUEST = 'PURCHASE_REQUEST',
  METER = 'METER'
}

export interface CustomFieldValue {
  customField: CustomField;
  value: string;
}
