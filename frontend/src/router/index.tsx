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

import { supportedLanguages } from '../i18n/i18n';
import Status404 from '../content/pages/Status/Status404';

const languageRoutes = supportedLanguages.map((lang) => ({
  path: lang.code,
  element: <BaseLayout />,
  children: baseRoutes
}));

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
    element: <BaseLayout />,
    children: baseRoutes.filter((route) => route.path !== '*')
  },
  ...languageRoutes,
  {
    path: '*',
    element: <Status404 />
  }
];

export default router;
