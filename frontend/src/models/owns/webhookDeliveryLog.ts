export interface WebhookDeliveryLog {
  id: number;
  webhookEndpointId: number;
  eventType: string;
  payload: string | null;
  responseStatus: number | null;
  responseBody: string | null;
  durationMs: number | null;
  success: boolean;
  errorMessage: string | null;
  createdAt: string;
}
