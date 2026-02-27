const licenseEntitlements = [
  'SSO',
  'WORK_ORDER_HISTORY',
  'WORKFLOW',
  'MULTI_INSTANCE',
  'WEBHOOK',
  'BRANDING',
  'NFC_BARCODE',
  'CUSTOM_ROLES',
  'FILE_ATTACHMENTS',
  'TIME_TRACKING',
  'COST_TRACKING',
  'WORK_ORDER_LINKING',
  'SIGNATURE_CAPTURE',
  'PM_CALENDAR',
  'CONDITION_BASED_PM',
  'ASSET_HIERARCHY',
  'ASSET_DOWNTIME',
  'LOW_STOCK_ALERTS',
  'PARTS_COST_TRACKING',
  'CUSTOMER_VENDOR',
  'FIELD_CONFIGURATION',
  'VOICE_NOTES',
  'ADVANCED_ANALYTICS',
  'API_ACCESS',
  'UNLIMITED_ASSETS',
  'UNLIMITED_LOCATIONS',
  'UNLIMITED_PARTS',
  'UNLIMITED_PM_SCHEDULES',
  'UNLIMITED_ACTIVE_WORK_ORDERS',
  'UNLIMITED_CHECKLIST',
  'UNLIMITED_METERS'
] as const;
export type LicensingState = {
  valid: boolean;
  entitlements: LicenseEntitlement[];
  expirationDate: string;
  planName: string;
};
export type LicenseEntitlement = typeof licenseEntitlements[number];
