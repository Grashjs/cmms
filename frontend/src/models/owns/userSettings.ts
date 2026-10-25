export type AnalyticsSummaryFrequency = 'NEVER' | 'WEEKLY' | 'MONTHLY';

export default interface UserSettings {
  id: number;
  emailNotified: boolean;
  emailUpdatesForWorkOrders: boolean;
  emailUpdatesForRequests: boolean;
  emailUpdatesForPurchaseOrders: boolean;
  // maintenance summary by email, with the PDF attached
  analyticsSummaryFrequency?: AnalyticsSummaryFrequency;
}
