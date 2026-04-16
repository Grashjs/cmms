import { lazy, Suspense } from 'react';
import { Navigate } from 'react-router-dom';

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
const FeaturesSettings = Loader(
  lazy(() => import('../content/own/Settings/Features'))
);
const WorkOrderSettings = Loader(
  lazy(() => import('../content/own/Settings/Features/WorkOrder'))
);
const ConfigureFields = Loader(
  lazy(
    () => import('../content/own/Settings/Features/WorkOrder/ConfigureFields')
  )
);
const RequestConfigureFields = Loader(
  lazy(() => import('../content/own/Settings/Features/Request/ConfigureFields'))
);
const WorkOrderCustomFields = Loader(
  lazy(() => import('../content/own/Settings/Features/WorkOrder/CustomFields'))
);

const RequestSettings = Loader(
  lazy(() => import('../content/own/Settings/Features/Request'))
);
const AssetSettings = Loader(
  lazy(() => import('../content/own/Settings/Features/Asset'))
);
const AssetCustomFields = Loader(
  lazy(() => import('../content/own/Settings/Features/Asset/CustomFields'))
);
const LocationSettings = Loader(
  lazy(() => import('../content/own/Settings/Features/Location'))
);
const LocationCustomFields = Loader(
  lazy(() => import('../content/own/Settings/Features/Location/CustomFields'))
);
const PartsSettings = Loader(
  lazy(() => import('../content/own/Settings/Features/Parts'))
);
const PartsCustomFields = Loader(
  lazy(() => import('../content/own/Settings/Features/Parts/CustomFields'))
);
const MetersSettings = Loader(
  lazy(() => import('../content/own/Settings/Features/Meters'))
);
const MetersCustomFields = Loader(
  lazy(() => import('../content/own/Settings/Features/Meters/CustomFields'))
);
const ContractorsSettings = Loader(
  lazy(() => import('../content/own/Settings/Features/Contractors'))
);
const ContractorsCustomFields = Loader(
  lazy(() => import('../content/own/Settings/Features/Contractors/CustomFields'))
);
const VendorsSettings = Loader(
  lazy(() => import('../content/own/Settings/Features/Vendors'))
);
const VendorsCustomFields = Loader(
  lazy(() => import('../content/own/Settings/Features/Vendors/CustomFields'))
);
const RolesSettings = Loader(
  lazy(() => import('../content/own/Settings/Roles'))
);
const ChecklistsSettings = Loader(
  lazy(() => import('../content/own/Settings/Checklists'))
);
const WorkflowsSettings = Loader(
  lazy(() => import('../content/own/Settings/Features/Workflows'))
);

const RequestPortalSettings = Loader(
  lazy(() => import('../content/own/Settings/Features/RequestPortal'))
);
const IntegrationsSettings = Loader(
  lazy(() => import('../content/own/Settings/Integrations'))
);
const ApiKeysPage = Loader(
  lazy(() => import('../content/own/Settings/Integrations/ApiKeysPage'))
);
const WebhooksPage = Loader(
  lazy(() => import('../content/own/Settings/Integrations/Webhooks'))
);
const Connectors = Loader(
  lazy(() => import('../content/own/Settings/Integrations/Connectors'))
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
        path: 'features',
        children: [
          { index: true, element: <FeaturesSettings /> },
          {
            path: 'work-order',
            children: [
              { index: true, element: <WorkOrderSettings /> },
              { path: 'configure-fields', element: <ConfigureFields /> },
              { path: 'custom-fields', element: <WorkOrderCustomFields /> }
            ]
          },
          {
            path: 'request',
            children: [
              { index: true, element: <RequestSettings /> },
              { path: 'configure-fields', element: <RequestConfigureFields /> }
            ]
          },
          {
            path: 'asset',
            children: [
              { index: true, element: <AssetSettings /> },
              { path: 'custom-fields', element: <AssetCustomFields /> }
            ]
          },
          {
            path: 'location',
            children: [
              { index: true, element: <LocationSettings /> },
              { path: 'custom-fields', element: <LocationCustomFields /> }
            ]
          },
          {
            path: 'parts',
            children: [
              { index: true, element: <PartsSettings /> },
              { path: 'custom-fields', element: <PartsCustomFields /> }
            ]
          },
          {
            path: 'meters',
            children: [
              { index: true, element: <MetersSettings /> },
              { path: 'custom-fields', element: <MetersCustomFields /> }
            ]
          },
          {
            path: 'contractors',
            children: [
              { index: true, element: <ContractorsSettings /> },
              { path: 'custom-fields', element: <ContractorsCustomFields /> }
            ]
          },
          {
            path: 'vendors',
            children: [
              { index: true, element: <VendorsSettings /> },
              { path: 'custom-fields', element: <VendorsCustomFields /> }
            ]
          },
          { path: 'request-portals', element: <RequestPortalSettings /> },
          { path: 'request-portals/:id', element: <RequestPortalSettings /> },
          { path: 'workflows', element: <WorkflowsSettings /> }
        ]
      },
      {
        path: 'roles',
        element: <RolesSettings />
      },
      {
        path: 'checklists',
        element: <ChecklistsSettings />
      },
      {
        path: 'integrations',
        element: <IntegrationsSettings />,
        children: [
          { index: true, element: <Navigate to="api-keys" replace /> },
          { path: 'connectors', element: <Connectors /> },
          { path: 'api-keys', element: <ApiKeysPage /> },
          { path: 'webhooks', element: <WebhooksPage /> }
        ]
      }
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
      { path: 'meters', element: <Imports /> },
      { path: 'preventive-maintenances', element: <Imports /> }
    ]
  },
  { path: 'upgrade', element: <Upgrade /> },
  { path: 'downgrade', element: <Downgrade /> },
  { path: 'switch-account', element: <SwitchAccount /> }
];

export default appRoutes;
