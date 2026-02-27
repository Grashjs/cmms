import { CategoryMiniDTO } from '../category';
import { AssetMiniDTO } from '../asset';
import { PartMiniDTO } from '../part';

export interface PartConsumptionsByDate {
  cost: number;
  date: string;
}

export interface PartStats {
  totalConsumptionCost: number;
  consumedCount: number;
}

export interface PartConsumptionByCategory extends CategoryMiniDTO {
  cost: number;
}

export interface PartConsumptionsByAsset extends AssetMiniDTO {
  cost: number;
}

export interface PartConsumptionsByPart extends PartMiniDTO {
  cost: number;
}

export interface PartConsumptionByWOCategory extends CategoryMiniDTO {
  cost: number;
}