import { Audit } from './audit';
import { SubscriptionPlan } from './subscriptionPlan';

export default interface OwnSubscription extends Audit {
  id: number;
  usersCount: number;
  monthly: boolean;
  subscriptionPlan: SubscriptionPlan;
  startsOn: string;
  endsOn: string;
  cancelled: boolean;
  scheduledChangeType: 'RESET_TO_FREE';
  scheduledChangeDate: string;
  paddleSubscriptionId: string;
  activated: boolean;
  upgradeNeeded: boolean;
  downgradeNeeded: boolean;
}
