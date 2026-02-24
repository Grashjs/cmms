import { lazy, Suspense } from 'react';
import { Navigate } from 'react-router-dom';

import SuspenseLoader from 'src/components/SuspenseLoader';
import FoodAndBeverage from '../content/landing/industries/FoodAndBeverage';
import Healthcare from '../content/landing/industries/Healthcare';
import Education from '../content/landing/industries/Education';

const Loader = (Component) => (props) =>
  (
    <Suspense fallback={<SuspenseLoader />}>
      <Component {...props} />
    </Suspense>
  );

// Pages

const Overview = Loader(lazy(() => import('../content/overview')));
const Pricing = Loader(lazy(() => import('../content/pricing')));
const TermsOfService = Loader(
  lazy(() => import('../content/terms-of-service'))
);

// Status

const Status404 = Loader(
  lazy(() => import('../content/pages/Status/Status404'))
);
const Status500 = Loader(
  lazy(() => import('../content/pages/Status/Status500'))
);
const StatusComingSoon = Loader(
  lazy(() => import('../content/pages/Status/ComingSoon'))
);
const StatusMaintenance = Loader(
  lazy(() => import('../content/pages/Status/Maintenance'))
);
const PrivacyPolicy = Loader(lazy(() => import('../content/privacyPolicy')));
const DeletionPolicy = Loader(
  lazy(() => import('../content/own/deletionPolicy'))
);
const FreeCMMSLanding = Loader(
  lazy(() => import('../content/landing/FreeCMMS'))
);
const EnergyPage = Loader(
  lazy(() => import('../content/landing/industries/Energy'))
);
const FacilityManagement = Loader(
  lazy(() => import('../content/landing/industries/FacilityManagement'))
);
const ManufacturingPage = Loader(
  lazy(() => import('../content/landing/industries/Manufacturing'))
);
const WorkOrdersPage = Loader(
  lazy(() => import('../content/landing/features/WorkOrders'))
);
const AssetManagementPage = Loader(
  lazy(() => import('../content/landing/features/Assets'))
);

const InventoryPage = Loader(
  lazy(() => import('../content/landing/features/Inventory'))
);
const AnalyticsPage = Loader(
  lazy(() => import('../content/landing/features/Analytics'))
);

const PMFeaturePage = Loader(
  lazy(() => import('../content/landing/features/PreventiveMaintenance'))
);

const HospitalityPage = Loader(
  lazy(() => import('../content/landing/industries/Hospitality'))
);

const FoodAndBeveragePage = Loader(
  lazy(() => import('../content/landing/industries/FoodAndBeverage'))
);

const HealthcarePage = Loader(
  lazy(() => import('../content/landing/industries/Healthcare'))
);

const EducationPage = Loader(
  lazy(() => import('../content/landing/industries/Education'))
);

const ConstructionPage = Loader(
  lazy(() => import('../content/landing/industries/Construction'))
);

const baseRoutes = [
  {
    path: '',
    element: <Overview />
  },
  {
    path: 'free-cmms',
    element: <FreeCMMSLanding />
  },
  {
    path: 'industries/open-source-energy-utilities-maintenance-software',
    element: <EnergyPage />
  },
  {
    path: 'industries/open-source-manufacturing-maintenance-software',
    element: <ManufacturingPage />
  },
  {
    path: 'industries/open-source-facility-management-software',
    element: <FacilityManagement />
  },
  {
    path: 'industries/open-source-hospitality-maintenance-software',
    element: <HospitalityPage />
  },
  {
    path: 'industries/open-source-construction-maintenance-software',
    element: <ConstructionPage />
  },
  {
    path: 'industries/open-source-healthcare-maintenance-software',
    element: <HealthcarePage />
  },
  {
    path: 'industries/open-source-education-maintenance-software',
    element: <EducationPage />
  },
  {
    path: 'industries/open-source-food-and-beverage-maintenance-software',
    element: <FoodAndBeveragePage />
  },
  {
    path: 'features/assets',
    element: <AssetManagementPage />
  },
  {
    path: 'features/inventory',
    element: <InventoryPage />
  },
  {
    path: 'features/analytics',
    element: <AnalyticsPage />
  },
  {
    path: 'features/preventive-maintenance',
    element: <PMFeaturePage />
  },
  {
    path: 'features/work-orders',
    element: <WorkOrdersPage />
  },
  {
    path: 'pricing',
    element: <Pricing />
  },
  {
    path: 'privacy',
    element: <PrivacyPolicy />
  },
  {
    path: 'deletion-policy',
    element: <DeletionPolicy />
  },
  { path: 'terms-of-service', element: <TermsOfService /> },
  {
    path: 'overview',
    element: <Navigate to="/" replace />
  }
];

export default baseRoutes;
