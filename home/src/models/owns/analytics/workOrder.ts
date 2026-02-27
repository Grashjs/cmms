import { UserMiniDTO } from 'src/user';
import { CategoryMiniDTO } from '../category';
import { AssetMiniDTO } from '../asset';

export interface WoOverviewStats {
  total: number;
  complete: number;
  compliant: number;
  avgCycleTime: number;
  mtta: number;
}

export interface IncompleteWoStats {
  total: number;
  averageAge: number;
}

interface BasicStats {
  count: number;
  estimatedHours: number;
}

export interface WOStatsByPriority {
  none: BasicStats;
  high: BasicStats;
  medium: BasicStats;
  low: BasicStats;
}

export interface WOStatsByStatus {
  open: number;
  inProgress: number;
  onHold: number;
  complete: number;
}

export interface WOHours {
  estimated: number;
  actual: number;
}

export interface WOCountsByUser extends UserMiniDTO {
  count: number;
}

export interface WOCountsByCategory extends CategoryMiniDTO {
  count: number;
}

export interface WOCountsByWeek {
  count: number;
  compliant: number;
  reactive: number;
  date: string;
}

export interface WOTimeByWeek {
  total: number;
  reactive: number;
  date: string;
}

export interface WOCostByDate {
  partCost: number;
  additionalCost: number;
  laborCost: number;
  date: string;
}

export interface WOCostsAndTime {
  total: number;
  average: number;
  additionalCost: number;
  laborCost: number;
  partCost: number;
  laborTime: number;
}

export interface IncompleteWOByAsset extends AssetMiniDTO {
  count: number;
  averageAge: number;
}

export interface IncompleteWOByUser extends UserMiniDTO {
  count: number;
  averageAge: number;
}

export interface WOStatusesByDate extends WOStatsByStatus {
  date: string;
}
