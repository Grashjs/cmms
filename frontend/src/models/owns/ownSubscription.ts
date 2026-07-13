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
  activated: boolean;
  upgradeNeeded: boolean;
  downgradeNeeded: boolean;
}
