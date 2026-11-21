export type MeterInterval = 'HOUR' | 'DAY' | 'WEEK' | 'MONTH';

export interface MeterOption {
  id: number;
  name: string;
  unit: string;
  cumulative: boolean;
  assetName: string | null;
}

export interface MeterAnalyticsPoint {
  start: string;
  count: number;
  // null without readings in the interval
  min: number | null;
  average: number | null;
  max: number | null;
  last: number | null;
  // cumulative meters only
  consumption: number | null;
  outOfRange: number;
}

export interface MeterThreshold {
  name: string;
  condition: 'LESS_THAN' | 'MORE_THAN';
  value: number;
}

export interface MeterAnalytics {
  meterId: number;
  name: string;
  unit: string;
  cumulative: boolean;
  assetId: number | null;
  assetName: string | null;
  interval: MeterInterval;
  points: MeterAnalyticsPoint[];
  thresholds: MeterThreshold[];
  readings: number;
  min: number | null;
  average: number | null;
  max: number | null;
  lastValue: number | null;
  lastReadingAt: string | null;
  outOfRange: number;
  // cumulative meters only, null without readings
  consumption: number | null;
  resets: number;
  failures: number;
  // consumption / failures, null without failures
  consumptionBetweenFailures: number | null;
}
