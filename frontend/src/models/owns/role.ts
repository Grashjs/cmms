export enum PermissionEntity {
  PEOPLE_AND_TEAMS = 'PEOPLE_AND_TEAMS',
  CATEGORIES = 'CATEGORIES',
  WORK_ORDERS = 'WORK_ORDERS',
  PREVENTIVE_MAINTENANCES = 'PREVENTIVE_MAINTENANCES',
  ASSETS = 'ASSETS',
  PARTS_AND_MULTIPARTS = 'PARTS_AND_MULTIPARTS',
  PURCHASE_ORDERS = 'PURCHASE_ORDERS',
  METERS = 'METERS',
  VENDORS_AND_CUSTOMERS = 'VENDORS_AND_CUSTOMERS',
  FILES = 'FILES',
  LOCATIONS = 'LOCATIONS',
  SETTINGS = 'SETTINGS',
  REQUESTS = 'REQUESTS',
  ANALYTICS = 'ANALYTICS'
}
export type PermissionRoot =
  | 'createPermissions'
  | 'viewPermissions'
  | 'viewOtherPermissions'
  | 'editOtherPermissions'
  | 'deleteOtherPermissions';
export type RoleCode =
  | 'ADMIN'
  | 'LIMITED_ADMIN'
  | 'TECHNICIAN'
  | 'LIMITED_TECHNICIAN'
  | 'VIEW_ONLY'
  | 'REQUESTER'
  | 'USER_CREATED';
export interface Role {
  id: number;
  name: string;
  users: number;
  externalId?: string;
  description?: string;
  paid: boolean;
  code: RoleCode;
  createPermissions: PermissionEntity[];
  viewPermissions: PermissionEntity[];
  viewOtherPermissions: PermissionEntity[];
  editOtherPermissions: PermissionEntity[];
  deleteOtherPermissions: PermissionEntity[];
}
