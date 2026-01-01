const licenseEntitlements = [
  'CHECKLIST',
  'SSO',
  'CUSTOM_ROLES',
  'WORK_ORDER_HISTORY',
  'WORKFLOW',
  'MULTI_INSTANCE',
  'NFC_BARCODE',
  'METER',
  'WEBHOOK',
  'BRANDING'
] as const;
export type LicensingState = {
  valid: boolean;
  entitlements: LicenseEntitlement[];
  expirationDate: string;
  planName: string;
};
export type LicenseEntitlement = typeof licenseEntitlements[number];
