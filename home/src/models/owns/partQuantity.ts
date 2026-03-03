import { Audit } from './audit';
import Part, { PartMiniDTO } from './part';

export default interface PartQuantity extends Audit {
  id: number;
  quantity: number;
  part: Part;
}

export interface PartQuantityMiniDTO {
  id: number;
  quantity: number;
  part: PartMiniDTO;
}
