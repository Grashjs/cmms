import { useSelector } from '../store';
import { LicenseEntitlement, LicensingState } from '../models/owns/license';

export const useLicenseEntitlement = (entitlement: LicenseEntitlement) => {
  const licensingState = useSelector((state) => state.license.state);

  return hasLicenseEntitlement(licensingState, entitlement);
};
const hasLicenseEntitlement = (
  license: LicensingState,
  entitlement: LicenseEntitlement
) => {
  return license.valid && license.entitlements.some((e) => e === entitlement);
};
