import { lazy, Suspense } from 'react';

import SuspenseLoader from 'src/components/SuspenseLoader';
import analyticsRoutes from './analytics';

const Loader = (Component) => (props) =>
  (
    <Suspense fallback={<SuspenseLoader />}>
      <Component {...props} />
    </Suspense>
  );

const SettingsLayout = Loader(
  lazy(() => import('../content/own/Settings/SettingsLayout'))
);
const GeneralSettings = Loader(
  lazy(() => import('../content/own/Settings/General'))
);
const WorkOrderSettings = Loader(
  lazy(() => import('../content/own/Settings/WorkOrder'))
);
const RequestSettings = Loader(
  lazy(() => import('../content/own/Settings/Request'))
);
const RolesSettings = Loader(
  lazy(() => import('../content/own/Settings/Roles'))
);
const ChecklistsSettings = Loader(
  lazy(() => import('../content/own/Settings/Checklists'))
);
const WorkflowsSettings = Loader(
  lazy(() => import('../content/own/Settings/Workflows'))
);
const UIConfigurationSettings = Loader(
  lazy(() => import('../content/own/Settings/UiConfiguration'))
);

const UserProfile = Loader(lazy(() => import('../content/own/UserProfile')));
const CompanyProfile = Loader(
  lazy(() => import('../content/own/CompanyProfile'))
);
const WorkOrderCategories = Loader(
  lazy(() => import('../content/own/Categories/WorkOrder'))
);
const PartCategories = Loader(
  lazy(() => import('../content/own/Categories/Part'))
);
const AssetCategories = Loader(
  lazy(() => import('../content/own/Categories/Asset'))
);
const PurchaseOrderCategories = Loader(
  lazy(() => import('../content/own/Categories/PurchaseOrder'))
);
const MeterCategories = Loader(
  lazy(() => import('../content/own/Categories/Meter'))
);
const TimeCategories = Loader(
  lazy(() => import('../content/own/Categories/Timer'))
);
const CostCategories = Loader(
  lazy(() => import('../content/own/Categories/Cost'))
);
const SubscriptionPlans = Loader(
  lazy(() => import('../content/own/Subscription/Plans'))
);
const Files = Loader(lazy(() => import('../content/own/Files')));
const Meters = Loader(lazy(() => import('../content/own/Meters')));
const PurchaseOrders = Loader(
  lazy(() => import('../content/own/PurchaseOrders'))
);
const CreatePurchaseOrders = Loader(
  lazy(() => import('../content/own/PurchaseOrders/Create'))
);
const Locations = Loader(lazy(() => import('../content/own/Locations')));
const WorkOrders = Loader(lazy(() => import('../content/own/WorkOrders')));

const VendorsAndCustomers = Loader(
  lazy(() => import('../content/own/VendorsAndCustomers'))
);

const Assets = Loader(lazy(() => import('../content/own/Assets')));
const ShowAsset = Loader(lazy(() => import('../content/own/Assets/Show')));
const Inventory = Loader(lazy(() => import('../content/own/Inventory')));
const Requests = Loader(lazy(() => import('../content/own/Requests')));
const PreventiveMaintenances = Loader(
  lazy(() => import('../content/own/PreventiveMaintenance'))
);

const PeopleAndTeams = Loader(
  lazy(() => import('../content/own/PeopleAndTeams'))
);

const Imports = Loader(lazy(() => import('../content/own/Imports')));
const Upgrade = Loader(
  lazy(() => import('../content/own/UpgradeAndDowngrade/Upgrade'))
);
const Downgrade = Loader(
  lazy(() => import('../content/own/UpgradeAndDowngrade/Downgrade'))
);
const SwitchAccount = Loader(
  lazy(() => import('../content/own/SwitchAccount'))
);
const appRoutes = [
  {
    path: 'settings',
    element: <SettingsLayout />,
    children: [
      {
        path: '',
        element: <GeneralSettings />
      },
      {
        path: 'work-order',
        element: <WorkOrderSettings />
      },
      {
        path: 'request',
        element: <RequestSettings />
      },
      {
        path: 'roles',
        element: <RolesSettings />
      },
      {
        path: 'checklists',
        element: <ChecklistsSettings />
      },
      { path: 'workflows', element: <WorkflowsSettings /> },
      { path: 'ui-configuration', element: <UIConfigurationSettings /> }
    ]
  },
  {
    path: 'account',
    children: [
      {
        path: 'profile',
        element: <UserProfile />
      },
      {
        path: 'company-profile',
        element: <CompanyProfile />
      }
    ]
  },
  {
    path: 'subscription',
    children: [
      {
        path: 'plans',
        element: <SubscriptionPlans />
      }
    ]
  },
  {
    path: 'files',
    element: <Files />
  },
  {
    path: 'meters',
    children: [
      {
        path: '',
        element: <Meters />
      },
      {
        path: ':meterId',
        element: <Meters />
      }
    ]
  },
  {
    path: 'requests',
    children: [
      {
        path: '',
        element: <Requests />
      },
      {
        path: ':requestId',
        element: <Requests />
      }
    ]
  },
  {
    path: 'preventive-maintenances',
    children: [
      {
        path: '',
        element: <PreventiveMaintenances />
      },
      {
        path: ':preventiveMaintenanceId',
        element: <PreventiveMaintenances />
      }
    ]
  },
  {
    path: 'purchase-orders',
    children: [
      {
        path: '',
        element: <PurchaseOrders />
      },
      {
        path: ':purchaseOrderId',
        element: <PurchaseOrders />
      },
      {
        path: 'create',
        element: <CreatePurchaseOrders />
      }
    ]
  },
  {
    path: 'locations',
    children: [
      { path: '', element: <Locations /> },
      { path: ':locationId', element: <Locations /> }
    ]
  },
  {
    path: 'work-orders',
    children: [
      { path: '', element: <WorkOrders /> },
      { path: ':workOrderId', element: <WorkOrders /> }
    ]
  },
  {
    path: 'inventory',
    children: [
      {
        path: 'parts',
        children: [
          { path: '', element: <Inventory /> },
          { path: ':partId', element: <Inventory /> }
        ]
      },
      {
        path: 'sets',
        children: [
          { path: '', element: <Inventory /> },
          { path: ':setId', element: <Inventory /> }
        ]
      }
    ]
  },
  {
    path: 'assets',
    children: [
      { path: '', element: <Assets /> },
      {
        path: ':assetId',
        children: [
          { path: 'work-orders', element: <ShowAsset /> },
          { path: 'details', element: <ShowAsset /> },
          { path: 'parts', element: <ShowAsset /> },
          { path: 'files', element: <ShowAsset /> },
          { path: 'meters', element: <ShowAsset /> },
          { path: 'downtimes', element: <ShowAsset /> },
          { path: 'analytics', element: <ShowAsset /> }
        ]
      }
    ]
  },
  {
    path: 'analytics',
    children: analyticsRoutes
  },
  {
    path: 'categories',
    children: [
      {
        path: '',
        element: <WorkOrderCategories />
      },
      {
        path: 'asset',
        element: <AssetCategories />
      },
      {
        path: 'purchase-order',
        element: <PurchaseOrderCategories />
      },
      {
        path: 'meter',
        element: <MeterCategories />
      },
      {
        path: 'time',
        element: <TimeCategories />
      },
      {
        path: 'cost',
        element: <CostCategories />
      },
      {
        path: 'part',
        element: <PartCategories />
      }
    ]
  },
  {
    path: 'vendors-customers',
    children: [
      {
        path: 'vendors',
        children: [
          { path: '', element: <VendorsAndCustomers /> },
          { path: ':vendorId', element: <VendorsAndCustomers /> }
        ]
      },
      {
        path: 'customers',
        children: [
          { path: '', element: <VendorsAndCustomers /> },
          { path: ':customerId', element: <VendorsAndCustomers /> }
        ]
      }
    ]
  },
  {
    path: 'people-teams',
    children: [
      {
        path: 'people',
        children: [
          { path: '', element: <PeopleAndTeams /> },
          { path: ':peopleId', element: <PeopleAndTeams /> }
        ]
      },
      {
        path: 'teams',
        children: [
          { path: '', element: <PeopleAndTeams /> },
          { path: ':teamId', element: <PeopleAndTeams /> }
        ]
        // element: <PeopleAndTeams />
      }
    ]
  },
  {
    path: 'imports',
    children: [
      { path: 'work-orders', element: <Imports /> },
      { path: 'assets', element: <Imports /> },
      { path: 'locations', element: <Imports /> },
      { path: 'parts', element: <Imports /> },
      { path: 'meters', element: <Imports /> }
    ]
  },
  { path: 'upgrade', element: <Upgrade /> },
  { path: 'downgrade', element: <Downgrade /> },
  { path: 'switch-account', element: <SwitchAccount /> }
];

export default appRoutes;
