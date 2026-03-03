export default interface UserSettings {
  id: number;
  emailNotified: boolean;
  emailUpdatesForWorkOrders: boolean;
  emailUpdatesForRequests: boolean;
  emailUpdatesForPurchaseOrders: boolean;
}
