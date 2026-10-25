import { lazy, Suspense } from 'react';

import SuspenseLoader from 'src/components/SuspenseLoader';

const Loader = (Component) => (props) =>
  (
    <Suspense fallback={<SuspenseLoader />}>
      <Component {...props} />
    </Suspense>
  );

const WOAnalytics = Loader(
  lazy(() => import('../content/own/Analytics/WorkOrder'))
);
const AssetAnalytics = Loader(
  lazy(() => import('../content/own/Analytics/Asset'))
);
const PartAnalytics = Loader(
  lazy(() => import('../content/own/Analytics/Part'))
);
const RequestAnalytics = Loader(
  lazy(() => import('../content/own/Analytics/Request'))
);
const analyticsRoutes = [
  {
    path: 'work-orders',
    children: [
      { path: 'status', element: <WOAnalytics /> },
      { path: 'analysis', element: <WOAnalytics /> },
      { path: 'aging', element: <WOAnalytics /> },
      { path: 'time-cost', element: <WOAnalytics /> },
      { path: 'kpis', element: <WOAnalytics /> }
    ]
  },
  {
    path: 'assets',
    children: [
      { path: 'reliability', element: <AssetAnalytics /> },
      { path: 'cost', element: <AssetAnalytics /> },
      { path: 'useful-life', element: <AssetAnalytics /> }
    ]
  },
  {
    path: 'parts',
    children: [{ path: 'consumption', element: <PartAnalytics /> }]
  },
  {
    path: 'requests',
    children: [{ path: 'analysis', element: <RequestAnalytics /> }]
  }
];

export default analyticsRoutes;
