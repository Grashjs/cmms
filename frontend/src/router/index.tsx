import { RouteObject } from 'react-router';

import Authenticated from 'src/components/Authenticated';
import ExtendedSidebarLayout from 'src/layouts/ExtendedSidebarLayout';
import appRoutes from './app';
import accountRoutes from './account';
import oauthRoutes from './oauth';
import { lazy, Suspense } from 'react';
import SuspenseLoader from '../components/SuspenseLoader';
import Status404 from '../content/pages/Status/Status404';
import { Navigate } from 'react-router-dom';

const Loader = (Component) => (props) =>
  (
    <Suspense fallback={<SuspenseLoader />}>
      <Component {...props} />
    </Suspense>
  );

const PaymentSuccess = Loader(
  lazy(() => import('../content/pages/Payment/Success'))
);

const router: RouteObject[] = [
  {
    path: 'account',
    children: accountRoutes
  },
  { path: 'oauth2', children: oauthRoutes },
  {
    path: 'payment/success',
    element: <PaymentSuccess />
  },
  {
    path: 'app',
    element: (
      <Authenticated>
        <ExtendedSidebarLayout />
      </Authenticated>
    ),
    children: appRoutes
  },
  {
    path: '',
    element: <Navigate to={'/app/work-orders'} />
  },
  {
    path: '*',
    element: <Status404 />
  }
];

export default router;
