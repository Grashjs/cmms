import { RouteObject } from 'react-router';

import Authenticated from 'src/components/Authenticated';
import BaseLayout from 'src/layouts/BaseLayout';
import ExtendedSidebarLayout from 'src/layouts/ExtendedSidebarLayout';

const Loader = (Component) => (props) =>
  (
    <Suspense fallback={<SuspenseLoader />}>
      <Component {...props} />
    </Suspense>
  );

const PaymentSuccess = Loader(
  lazy(() => import('../content/pages/Payment/Success'))
);
import appRoutes from './app';
import accountRoutes from './account';
import baseRoutes from './base';
import oauthRoutes from './oauth';
import { lazy, Suspense } from 'react';
import SuspenseLoader from '../components/SuspenseLoader';

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
    path: '',
    element: <BaseLayout />,
    children: baseRoutes
  },
  {
    path: 'app',
    element: (
      <Authenticated>
        <ExtendedSidebarLayout />
      </Authenticated>
    ),
    children: appRoutes
  }
];

export default router;
