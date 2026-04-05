import { LicenseEntitlement } from '../models/owns/license';
import { PlanFeature } from '../models/owns/subscriptionPlan';
import useAuth from './useAuth';

const entitlementToPlanFeature: Record<string, PlanFeature> = {
  'WORK_ORDER_HISTORY': PlanFeature.CHECKLIST,
  'WORKFLOW': PlanFeature.WORKFLOW,
  'NFC_BARCODE': PlanFeature.METER,
  'FILE_ATTACHMENTS': PlanFeature.FILE,
  'TIME_TRACKING': PlanFeature.ADDITIONAL_TIME,
  'COST_TRACKING': PlanFeature.ADDITIONAL_COST,
  'SIGNATURE_CAPTURE': PlanFeature.SIGNATURE,
  'CUSTOM_ROLES': PlanFeature.ROLE,
  'CONDITION_BASED_PM': PlanFeature.METER,
  'CUSTOMER_VENDOR': PlanFeature.REQUEST_CONFIGURATION,
  'FIELD_CONFIGURATION': PlanFeature.REQUEST_CONFIGURATION,
  'ADVANCED_ANALYTICS': PlanFeature.ANALYTICS,
  'API_ACCESS': PlanFeature.API_ACCESS,
  'WORK_ORDER_LINKING': PlanFeature.PURCHASE_ORDER,
};

export const useLicenseEntitlement = (entitlement: LicenseEntitlement) => {
  const { hasFeature, user } = useAuth();

  if (user?.role?.roleType === 'ROLE_SUPER_ADMIN') return true;

  const planFeature = entitlementToPlanFeature[entitlement];
  if (!planFeature) return true;

  return hasFeature(planFeature);
};
