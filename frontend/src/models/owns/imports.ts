export type ImportKeys =
  | keyof WorkOrderImportDTO
  | keyof AssetImportDTO
  | keyof MeterImportDTO
  | keyof PartImportDTO
  | keyof LocationImportDTO
  | keyof PreventiveMaintenanceImportDTO;
export type ImportDTO =
  | WorkOrderImportDTO
  | AssetImportDTO
  | LocationImportDTO
  | PartImportDTO
  | MeterImportDTO
  | PreventiveMaintenanceImportDTO;

export interface WorkOrderImportDTO {
  id: number;
  dueDate: number;
  priority: string;
  estimatedDuration: string;
  description: string;
  title: string;
  requiredSignature: string;
  category: string;
  locationName: string;
  teamName: string;
  primaryUserEmail: string;
  assignedToEmails: string[];
  assetName: string;
  completedByEmail: string;
  completedOn: number;
  archived: string;
  status: string;
  feedback: string;
  customersNames: string[];
}

interface AssetImportDTO {
  id: number;
  archived: string;
  locationName: string;
  parentAssetName: string;
  area: string;
  barCode: string;
  category: string;
  name: string;
  primaryUserEmail: string;
  warrantyExpirationDate: number;
  additionalInfos: string;
  serialNumber: string;
  assignedToEmails: string[];
  teamsNames: string[];
  status: string;
  acquisitionCost: number;
  customersNames: string[];
  vendorsNames: string[];
  partsNames: string[];
  model: string;
  power: string;
  manufacturer: string;
}

interface LocationImportDTO {
  id: number;
  name: string;
  address: string;
  longitude: number;
  latitude: number;
  parentLocationName: string;
  workersEmails: string[];
  teamsNames: string[];
  customersNames: string[];
  vendorsNames: string[];
}

interface MeterImportDTO {
  id: string;
  name: string;
  unit: string;
  updateFrequency: number;
  meterCategory: string;
  locationName: string;
  usersEmails: string[];
}

interface PartImportDTO {
  id: number;
  name: string;
  cost: number;
  category: string;
  nonStock: string;
  barcode: string;
  description: string;
  quantity: number;
  additionalInfos: string;
  area: string;
  minQuantity: number;
  locationName: string;
  assignedToEmails: string[];
  teamsNames: string[];
  customersNames: string[];
  vendorsNames: string[];
}
interface PreventiveMaintenanceImportDTO extends WorkOrderImportDTO {
  startsOn: number;
  name: string;
  frequency: number;
  dueDateDelay: number;
  endsOn: number;
  recurrenceType: string;
  recurrenceBasedOn: string;
  daysOfWeek: string[];
}

export interface ImportResponse {
  created: number;
  updated: number;
}
