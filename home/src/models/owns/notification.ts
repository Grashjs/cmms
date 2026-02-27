import { Audit } from './audit';

export enum NotificationType {
  INFO = 'INFO',
  ASSET = 'ASSET',
  REQUEST = 'REQUEST',
  WORK_ORDER = 'WORK_ORDER',
  PART = 'PART',
  METER = 'METER',
  LOCATION = 'LOCATION',
  TEAM = 'TEAM',
  PURCHASE_ORDER = 'PURCHASE_ORDER'
}

export default interface Notification extends Audit {
  message: string;
  id: number;
  seen: boolean;
  notificationType: NotificationType;
  resourceId: number;
}
