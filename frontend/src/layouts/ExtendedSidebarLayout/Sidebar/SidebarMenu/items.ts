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

  items?: MenuItem[];
  name: string;
}

export interface MenuItems {
  items: MenuItem[];
  heading: string;
  hidden?: PermissionEntity;
}

const ownMenuItems: MenuItems[] = [
  {
    heading: '',
    items: [
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
            icon: Inventory2TwoToneIcon,
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
            icon: HandymanTwoToneIcon,
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
        name: 'requests',
        link: '/app/requests',
        icon: MoveToInboxTwoToneIcon,
        permission: PermissionEntity.REQUESTS,
        uiConfigKey: 'requests'
      },
      {
        name: 'assets',
        link: '/app/assets',
        icon: Inventory2TwoToneIcon,
        permission: PermissionEntity.ASSETS
      },
      {
        name: 'locations',
        link: '/app/locations',
        icon: LocationOnTwoToneIcon,
        permission: PermissionEntity.LOCATIONS,
        uiConfigKey: 'locations'
      },
      {
        name: 'parts_and_inventory',
        link: '/app/inventory',
        icon: HandymanTwoToneIcon,
        permission: PermissionEntity.PARTS_AND_MULTIPARTS,
        items: [
          {
            name: 'parts',
            link: '/app/inventory/parts'
          },
          {
            name: 'sets_of_parts',
            link: '/app/inventory/sets'
          }
        ]
      },
      {
        name: 'purchase_orders',
        link: '/app/purchase-orders',
        icon: ReceiptTwoToneIcon,
        permission: PermissionEntity.PURCHASE_ORDERS,
        planFeature: PlanFeature.PURCHASE_ORDER
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
        name: 'people_teams',
        link: '/app/people-teams',
        icon: People,
        permission: PermissionEntity.PEOPLE_AND_TEAMS,
        items: [
          {
            name: 'people',
            link: '/app/people-teams/people'
          },
          {
            name: 'teams',
            link: '/app/people-teams/teams'
          }
        ]
      },
      {
        name: 'vendors_customers',
        link: '/app/vendors-customers/vendors',
        icon: GroupsTwoTone,
        permission: PermissionEntity.VENDORS_AND_CUSTOMERS,
        uiConfigKey: 'vendorsAndCustomers',
        items: [
          {
            name: 'vendors',
            link: '/app/vendors-customers/vendors'
          },
          {
            name: 'customers',
            link: '/app/vendors-customers/customers'
          }
        ]
      },
      {
        name: 'categories',
        link: '/app/categories',
        icon: CategoryTwoToneIcon,
        permission: PermissionEntity.CATEGORIES_WEB
      },
      {
        name: 'files',
        link: '/app/files',
        icon: AttachFileTwoToneIcon,
        permission: PermissionEntity.FILES,
        planFeature: PlanFeature.FILE
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
const templateItems = [
  {
    heading: 'General',
    items: [
      {
        name: 'Blueprints',
        icon: BackupTableTwoToneIcon,
        link: '',
        items: [
          {
            name: 'Extended sidebar',
            link: '/extended-sidebar/dashboards',
            badge: 'v3.0',
            badgeTooltip: 'Added in version 3.1'
          },
          {
            name: 'Accent header',
            link: '/accent-header/dashboards',
            badge: '',
            badgeTooltip: 'Updated'
          },
          {
            name: 'Accent sidebar',
            link: '/accent-sidebar/dashboards'
          },
          {
            name: 'Boxed sidebar',
            link: '/boxed-sidebar/dashboards'
          },
          {
            name: 'Collapsed sidebar',
            link: '/collapsed-sidebar/dashboards'
          },
          {
            name: 'Bottom navigation',
            link: '/bottom-navigation/dashboards'
          },
          {
            name: 'Top navigation',
            link: '/top-navigation/dashboards'
          }
        ]
      },
      {
        name: 'Dashboards',
        icon: SmartToyTwoToneIcon,
        link: '/extended-sidebar/dashboards',
        items: [
          {
            name: 'Reports',
            link: 'dashboards/reports'
          },
          {
            name: 'Expenses',
            link: 'dashboards/expenses',
            badge: '',
            badgeTooltip: 'This is a dot tooltip example'
          },
          {
            name: 'Products',
            link: 'dashboards/products'
          },
          {
            name: 'Statistics',
            link: 'dashboards/statistics'
          },
          {
            name: 'Automation',
            link: 'dashboards/automation'
          },
          {
            name: 'Analytics',
            link: 'dashboards/analytics'
          },
          {
            name: 'Banking',
            link: 'dashboards/banking'
          },
          {
            name: 'Commerce',
            link: 'dashboards/commerce'
          },
          {
            name: 'Crypto',
            link: 'dashboards/crypto'
          },
          {
            name: 'Finance',
            link: 'dashboards/finance'
          },
          {
            name: 'Fitness',
            link: 'dashboards/fitness'
          },
          {
            name: 'Healthcare',
            link: '/extended-sidebar/dashboards/healthcare',
            items: [
              {
                name: 'Doctors',
                link: 'dashboards/healthcare/doctor'
              },
              {
                name: 'Hospital',
                link: 'dashboards/healthcare/hospital'
              }
            ]
          },
          {
            name: 'Helpdesk',
            link: 'dashboards/helpdesk'
          },
          {
            name: 'Learning',
            link: 'dashboards/learning'
          },
          {
            name: 'Monitoring',
            link: 'dashboards/monitoring'
          },
          {
            name: 'Tasks',
            link: 'dashboards/tasks'
          }
        ]
      },
      {
        name: 'Data Display',
        icon: HealthAndSafetyTwoToneIcon,
        badge: '',
        badgeTooltip: 'Tokyo contains over 250 data display blocks',
        link: '/extended-sidebar/blocks',
        items: [
          {
            name: 'Charts large',
            link: 'blocks/charts-large'
          },
          {
            name: 'Charts small',
            link: 'blocks/charts-small'
          },
          {
            name: 'Composed cards',
            link: 'blocks/composed-cards'
          },
          {
            name: 'Grids',
            link: 'blocks/grids'
          },
          {
            name: 'Icon cards',
            link: 'blocks/icon-cards'
          },
          {
            name: 'Image cards',
            link: 'blocks/image-cards'
          },
          {
            name: 'Lists large',
            link: 'blocks/lists-large'
          },
          {
            name: 'Lists small',
            link: 'blocks/lists-small'
          },
          {
            name: 'Navigation',
            link: 'blocks/navigation'
          },
          {
            name: 'Profile cards',
            link: 'blocks/profile-cards'
          },
          {
            name: 'Progress circular',
            link: 'blocks/progress-circular'
          },
          {
            name: 'Progress horizontal',
            link: 'blocks/progress-horizontal'
          },
          {
            name: 'Sparklines large',
            link: 'blocks/sparklines-large'
          },
          {
            name: 'Sparklines small',
            link: 'blocks/sparklines-small'
          },
          {
            name: 'Statistics',
            link: 'blocks/statistics'
          }
        ]
      },
      {
        name: 'Applications',
        icon: AnalyticsTwoToneIcon,
        link: '/extended-sidebar/applications',
        items: [
          {
            name: 'Calendar',
            link: 'applications/calendar'
          },
          {
            name: 'File Manager',
            link: 'applications/file-manager'
          },
          {
            name: 'Jobs Platform',
            link: 'applications/jobs-platform'
          },
          {
            name: 'Mailbox',
            link: 'applications/mailbox/inbox'
          },
          {
            name: 'Messenger',
            link: 'applications/messenger'
          },
          {
            name: 'Projects Board',
            link: 'applications/projects-board'
          }
        ]
      }
    ]
  },
  {
    heading: 'Management',
    items: [
      {
        name: 'Users',
        icon: AssignmentIndTwoToneIcon,
        link: '/extended-sidebar/management/users',
        items: [
          {
            name: 'List',
            link: 'management/users/list'
          },
          {
            name: 'User Profile',
            link: 'management/users/single'
          }
        ]
      },
      {
        name: 'Projects',
        icon: AccountTreeTwoToneIcon,
        link: '/extended-sidebar/management/projects/list'
      },
      {
        name: 'Commerce',
        icon: StorefrontTwoToneIcon,
        link: '/extended-sidebar/management/commerce',
        items: [
          {
            name: 'Shop',
            link: 'management/commerce/shop'
          },
          {
            name: 'List',
            link: 'management/commerce/products/list'
          },
          {
            name: 'Details',
            link: 'management/commerce/products/single/1'
          },
          {
            name: 'Create',
            link: 'management/commerce/products/create'
          }
        ]
      },
      {
        name: 'Invoices',
        icon: ReceiptTwoToneIcon,
        link: '/extended-sidebar/management/invoices',
        items: [
          {
            name: 'List',
            link: 'management/invoices/list'
          },
          {
            name: 'Details',
            link: 'management/invoices/single'
          }
        ]
      }
    ]
  },
  {
    heading: 'Extra Pages',
    items: [
      {
        name: 'Auth Pages',
        icon: VpnKeyTwoToneIcon,
        link: '/auth',
        items: [
          {
            name: 'Login',
            items: [
              {
                name: 'Basic',
                link: '/account/login-basic'
              },
              {
                name: 'Cover',
                link: '/account/login-cover'
              }
            ]
          },
          {
            name: 'Register',
            items: [
              {
                name: 'Basic',
                link: '/account/register-basic'
              },
              {
                name: 'Cover',
                link: '/account/register-cover'
              },
              {
                name: 'Wizard',
                link: '/account/register-wizard'
              }
            ]
          },
          {
            name: 'Recover Password',
            link: '/account/recover-password'
          }
        ]
      },
      {
        name: 'Status',
        icon: ErrorTwoToneIcon,
        link: '/status',
        items: [
          {
            name: 'Error 404',
            link: '/status/404'
          },
          {
            name: 'Error 500',
            link: '/status/500'
          },
          {
            name: 'Maintenance',
            link: '/status/maintenance'
          },
          {
            name: 'Coming Soon',
            link: '/status/coming-soon'
          }
        ]
      }
    ]
  },
  {
    heading: 'Foundation',
    items: [
      {
        name: 'Overview',
        link: '/overview',
        icon: DesignServicesTwoToneIcon
      },
      {
        name: 'Documentation',
        icon: SupportTwoToneIcon,
        link: '/docs'
      }
    ]
  }
];

export default ownMenuItems;
