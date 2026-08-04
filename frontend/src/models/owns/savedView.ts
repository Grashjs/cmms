import { SearchCriteria } from './page';
import { UserMiniDTO } from '../user';

export type SavedViewEntityType =
  | 'WORK_ORDER'
  | 'ASSET'
  | 'LOCATION'
  | 'PART'
  | 'METER'
  | 'PREVENTIVE_MAINTENANCE'
  | 'REQUEST'
  | 'PURCHASE_ORDER';

/**
 * The part of the table state a view restores. Mirrors what useTableState holds, minus
 * pagination: which page you were on is not worth reproducing, and a saved page index that
 * no longer exists shows an empty table.
 *
 * The backend stores this untouched as JSON, so adding a field here needs no API change.
 */
export interface TableLayout {
  sorting?: { id: string; desc: boolean }[];
  columnOrder?: string[];
  columnSizing?: Record<string, number>;
  columnVisibility?: Record<string, boolean>;
  pinnedColumns?: string[];
  pageSize?: number;
}

export interface SavedView {
  id: number;
  name: string;
  entityType: SavedViewEntityType;
  criteria?: SearchCriteria;
  columnLayout?: TableLayout;
  shared: boolean;
  owner?: UserMiniDTO;
  /** Resolved by the backend: owner or company owner. Do not re-derive it here. */
  editable: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface SavedViewPostPayload {
  name: string;
  entityType: SavedViewEntityType;
  criteria?: SearchCriteria;
  columnLayout?: TableLayout;
  shared?: boolean;
}

/** Every field optional: the backend leaves out what is not sent unchanged. */
export interface SavedViewPatchPayload {
  name?: string;
  criteria?: SearchCriteria;
  columnLayout?: TableLayout;
  shared?: boolean;
}
