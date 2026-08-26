import type { ReactNode } from 'react';

import AnalyticsTwoToneIcon from '@mui/icons-material/AnalyticsTwoTone';
import InsertChartTwoToneIcon from '@mui/icons-material/InsertChartTwoTone';
import HealthAndSafetyTwoToneIcon from '@mui/icons-material/HealthAndSafetyTwoTone';
import AssignmentIndTwoToneIcon from '@mui/icons-material/AssignmentIndTwoTone';
import AccountTreeTwoToneIcon from '@mui/icons-material/AccountTreeTwoTone';
import StorefrontTwoToneIcon from '@mui/icons-material/StorefrontTwoTone';
import VpnKeyTwoToneIcon from '@mui/icons-material/VpnKeyTwoTone';
import ErrorTwoToneIcon from '@mui/icons-material/ErrorTwoTone';
import DesignServicesTwoToneIcon from '@mui/icons-material/DesignServicesTwoTone';
import SupportTwoToneIcon from '@mui/icons-material/SupportTwoTone';
import ReceiptTwoToneIcon from '@mui/icons-material/ReceiptTwoTone';
import BackupTableTwoToneIcon from '@mui/icons-material/BackupTableTwoTone';
import SmartToyTwoToneIcon from '@mui/icons-material/SmartToyTwoTone';
import SettingsTwoToneIcon from '@mui/icons-material/SettingsTwoTone';
import CategoryTwoToneIcon from '@mui/icons-material/CategoryTwoTone';
import AttachFileTwoToneIcon from '@mui/icons-material/AttachFileTwoTone';
import { GroupsTwoTone, People } from '@mui/icons-material';
import LocationOnTwoToneIcon from '@mui/icons-material/LocationOnTwoTone';
import Inventory2TwoToneIcon from '@mui/icons-material/Inventory2TwoTone';
import HandymanTwoToneIcon from '@mui/icons-material/HandymanTwoTone';
import SpeedTwoToneIcon from '@mui/icons-material/SpeedTwoTone';
import MoveToInboxTwoToneIcon from '@mui/icons-material/MoveToInboxTwoTone';
import AssignmentTwoToneIcon from '@mui/icons-material/AssignmentTwoTone';
import PendingActionsTwoToneIcon from '@mui/icons-material/PendingActionsTwoTone';
import { PermissionEntity } from '../../../../models/owns/role';
import { PlanFeature } from '../../../../models/owns/subscriptionPlan';
import { IS_LOCALHOST } from '../../../../config';
import { UiConfiguration } from '../../../../models/owns/uiConfiguration';

export interface MenuItem {
  link?: string;
  icon?: ReactNode;
  badge?: string;
  badgeTooltip?: string;
  permission?: PermissionEntity;
  planFeature?: PlanFeature;
  uiConfigKey?: keyof Omit<UiConfiguration, 'id'>;

  /**
   * Path prefix that marks this entry active, for sections whose page has tabs or detail
   * routes. Without it a leaf entry is highlighted only on an exact match of `link`, so
   * switching to the second tab — or opening a record — would visibly unhighlight the section
   * you are still in.
   */
  activePath?: string;
  items?: MenuItem[];
  name: string;
}

export interface MenuItems {
  items: MenuItem[];
  heading: string;
  hidden?: PermissionEntity;
}

/**
 * Order follows the facility-management workflow rather than the upstream default: where a
 * thing is (locations, assets), what came in about it (requests), what is being done
 * (work orders, maintenance), what it consumes (meters, purchase orders, parts), and only then
 * the master data and settings. Statistics leads because it is the entry point people open
 * first.
 *
 * Sections whose page is tab-based (parts, people, vendors) are single entries, not dropdowns.
 * A dropdown whose two children land on the same page — which then shows those two as tabs —
 * makes the user choose twice for one destination.
 */
const ownMenuItems: MenuItems[] = [
  {
    heading: '',
    items: [
      {
        name: 'Statistics',
        icon: InsertChartTwoToneIcon,
        permission: PermissionEntity.ANALYTICS,
        planFeature: PlanFeature.ANALYTICS,
        items: [
          {
            name: 'work_orders',
            icon: AssignmentTwoToneIcon,
            items: [
              {
                name: 'status_report',
                link: '/app/analytics/work-orders/status'
              },
              {
                name: 'wo_analysis',
                link: '/app/analytics/work-orders/analysis'
              },
              {
                name: 'wo_aging',
                link: '/app/analytics/work-orders/aging'
              },
              {
                name: 'time_and_cost',
                link: '/app/analytics/work-orders/time-cost'
              }
            ]
          },
          {
            name: 'assets',
            icon: HandymanTwoToneIcon,
            items: [
              {
                name: 'reliability_dashboard',
                link: '/app/analytics/assets/reliability'
              },
              {
                name: 'total_maintenance_cost',
                link: '/app/analytics/assets/cost'
              }
              // {
              //   name: 'useful_life',
              //   link: '/app/analytics/assets/useful-life'
              // }
            ]
          },
          {
            name: 'parts',
            icon: Inventory2TwoToneIcon,
            items: [
              {
                name: 'parts_consumption',
                link: '/app/analytics/parts/consumption'
              }
            ]
          },
          {
            name: 'requests',
            icon: MoveToInboxTwoToneIcon,
            items: [
              {
                name: 'requests_analysis',
                link: '/app/analytics/requests/analysis'
              }
            ]
          }
        ]
      },
      {
        name: 'locations',
        link: '/app/locations',
        icon: LocationOnTwoToneIcon,
        permission: PermissionEntity.LOCATIONS,
        uiConfigKey: 'locations'
      },
      {
        // The box and the tools are swapped relative to upstream: a box reads as stock kept on
        // a shelf, which is Material, and tools read as technical equipment, which is an Anlage.
        // Upstream had it the other way round. Kept consistent everywhere these two icons stand
        // for assets and parts — the analytics submenu above, the notification icons and the
        // feature settings — so the sidebar does not contradict the rest of the app.
        name: 'assets',
        link: '/app/assets',
        icon: HandymanTwoToneIcon,
        permission: PermissionEntity.ASSETS
      },
      {
        name: 'requests',
        link: '/app/requests',
        icon: MoveToInboxTwoToneIcon,
        permission: PermissionEntity.REQUESTS,
        uiConfigKey: 'requests'
      },
      {
        name: 'work_orders',
        link: '/app/work-orders',
        icon: AssignmentTwoToneIcon
      },
      {
        name: 'preventive_maintenance',
        link: '/app/preventive-maintenances',
        icon: PendingActionsTwoToneIcon,
        permission: PermissionEntity.PREVENTIVE_MAINTENANCES
      },
      {
        name: 'meters',
        link: '/app/meters',
        icon: SpeedTwoToneIcon,
        permission: PermissionEntity.METERS,
        planFeature: PlanFeature.METER,
        uiConfigKey: 'meters'
      },
      {
        name: 'purchase_orders',
        link: '/app/purchase-orders',
        icon: ReceiptTwoToneIcon,
        permission: PermissionEntity.PURCHASE_ORDERS,
        planFeature: PlanFeature.PURCHASE_ORDER
      },
      {
        // Links to the first tab, not to /app/inventory: that parent path has no route of its
        // own, only `parts` and `sets` children, so it would render an empty page. Same for
        // people-teams below.
        name: 'parts_and_inventory',
        link: '/app/inventory/parts',
        activePath: '/app/inventory',
        icon: Inventory2TwoToneIcon,
        permission: PermissionEntity.PARTS_AND_MULTIPARTS
      },
      {
        name: 'people_teams',
        link: '/app/people-teams/people',
        activePath: '/app/people-teams',
        icon: People,
        permission: PermissionEntity.PEOPLE_AND_TEAMS
      },
      {
        name: 'vendors_customers',
        link: '/app/vendors-customers/vendors',
        activePath: '/app/vendors-customers',
        icon: GroupsTwoTone,
        permission: PermissionEntity.VENDORS_AND_CUSTOMERS,
        uiConfigKey: 'vendorsAndCustomers'
      },
      {
        name: 'files',
        link: '/app/files',
        icon: AttachFileTwoToneIcon,
        permission: PermissionEntity.FILES,
        planFeature: PlanFeature.FILE
      },
      {
        name: 'categories',
        link: '/app/categories',
        icon: CategoryTwoToneIcon,
        permission: PermissionEntity.CATEGORIES
      },
      {
        name: 'settings',
        link: '/app/settings',
        icon: SettingsTwoToneIcon,
        permission: PermissionEntity.SETTINGS
      }
    ]
  }
];

export default ownMenuItems;
