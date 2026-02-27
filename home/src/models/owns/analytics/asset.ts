import { AssetMiniDTO } from '../asset';

export interface TimeCostByAsset extends AssetMiniDTO {
  time: number;
  cost: number;
}

export interface AssetOverviewStats {
  downtime: number;
  availability: number;
  downtimeEvents: number;
}

export interface DowntimesByAsset extends AssetMiniDTO {
  count: number;
  percent: number;
}

export interface AssetsCost {
  rav: number;
  totalWOCosts: number;
  totalAcquisitionCost: number;
}

export interface DowntimesAndCostsByAsset extends AssetMiniDTO {
  duration: number;
  workOrdersCosts: number;
}

export interface MTBFByAsset extends AssetMiniDTO {
  mtbf: number;
}

export interface DowntimesByDate {
  duration: number;
  workOrdersCosts: number;
  date: string;
}

export interface DowntimesMeantimeByDate {
  meantime: number;
  date: string;
}

export interface Meantimes {
  betweenDowntimes: number;
  betweenMaintenances: number;
}

export interface RepairTimeByAsset extends AssetMiniDTO {
  duration: number;
}

export interface AssetOverview {
  mtbf: number;
  mttr: number;
  downtime: number;
  uptime: number;
  totalCost: number;
}