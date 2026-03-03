import { Audit } from './audit';
import { PartMiniDTO } from './part';

export default interface SetType extends Audit {
  id: number;
  name: string;
  parts: PartMiniDTO[];
}
