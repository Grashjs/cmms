import Currency from './currency';
import { SupportedLanguage } from '../../i18n/i18n';

export interface GeneralPreferences {
  id: number;
  language: SupportedLanguage;
  dateFormat: 'MMDDYY' | 'DDMMYY';
  currency: Currency;
  businessType: string;
  timeZone: string;
  daysBeforePrevMaintNotification: number;

  autoAssignWorkOrders: boolean;

  autoAssignRequests: boolean;

  disableClosedWorkOrdersNotif: boolean;

  askFeedBackOnWOClosed: boolean;

  laborCostInTotalCost: boolean;

  woUpdateForRequesters: boolean;

  simplifiedWorkOrder: boolean;
}
