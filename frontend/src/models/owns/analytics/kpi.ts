export type PriorityName = 'NONE' | 'LOW' | 'MEDIUM' | 'HIGH';

export interface PriorityResponseTimes {
  priority: PriorityName;
  count: number;
  completed: number;
  // hours, null when there is no data
  mtta: number | null;
  mttr: number | null;
}

export interface MaintenanceKpis {
  asOf: string | null;
  pmDue: number;
  pmCompletedOnTime: number;
  // percent, null when nothing was due
  pmCompliance: number | null;
  planned: number;
  reactive: number;
  // percent, null when no work order was created
  plannedRatio: number | null;
  backlog: number;
  backlogHours: number;
  overdue: number;
  byPriority: PriorityResponseTimes[];
}

export interface MaintenanceKpisByDate {
  date: string;
  backlog: number;
  backlogHours: number;
  overdue: number;
  planned: number;
  reactive: number;
}

export interface AssetAvailability {
  id: number;
  name: string;
  // percent
  availability: number;
  // seconds
  downtime: number;
  downtimeEvents: number;
}
