import { Audit } from './audit';
import { AssetMiniDTO } from './asset';
import { LocationMiniDTO } from './location';
import { UserMiniDTO } from '../user';
import File from './file';
import Category from './category';

export default interface Meter extends Audit {
  name: string;
  id: number;
  unit: string;
  updateFrequency: number;
  meterCategory: Category;
  image: File | null;
  users: UserMiniDTO[];
  location?: LocationMiniDTO;
  asset: AssetMiniDTO;
  nextReading: string;
  lastReading: string;
}
export interface MeterMiniDTO {
  name: string;
  id: number;
}
