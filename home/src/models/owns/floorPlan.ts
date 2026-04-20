import { Audit } from './audit';

export default interface FloorPlan extends Audit {
  id: number;
  name: string;
  area: number;
}
