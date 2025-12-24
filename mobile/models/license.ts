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
};
export type LicenseEntitlement = typeof licenseEntitlements[number];

export const hasLicenseEntitlement = (
  license: LicensingState,
  entitlement: LicenseEntitlement
) => {
  return license.valid && license.entitlements.some((e) => e === entitlement);
};
