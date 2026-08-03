export interface CustomFieldAssetCategory {
  id: number;
  name: string;
}

export interface CustomField {
  id: number;
  label: string;
  fieldType: CustomFieldType;
  entityType: CustomFieldEntityType;
  required: boolean;
  copyOnRepeat: boolean;
  options?: string[];
  /** Unit of measure for numeric fields, e.g. m³/h or kW. */
  unit?: string;
  /**
   * Asset categories this field belongs to. Empty means it applies to every asset,
   * which is how all fields behaved before categories were introduced.
   */
  assetCategories?: CustomFieldAssetCategory[];
  order: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * What the API accepts when writing a custom field. Categories go in as ids and come
 * back as objects, so the write shape is not a subset of CustomField.
 */
export type CustomFieldPayload = Omit<
  CustomField,
  'id' | 'createdAt' | 'updatedAt' | 'order' | 'assetCategories'
> & { assetCategoryIds?: number[] };

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
