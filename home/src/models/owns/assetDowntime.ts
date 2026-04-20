import { Audit } from './audit';

export default interface AssetDowntime extends Audit {
  duration: number;
  startsOn: string;
}
