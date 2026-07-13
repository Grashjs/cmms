import { RouteObject } from 'react-router';

import Authenticated from 'src/components/Authenticated';
import BaseLayout from 'src/layouts/BaseLayout';
import ExtendedSidebarLayout from 'src/layouts/ExtendedSidebarLayout';

import appRoutes from './app';
import accountRoutes from './account';
import baseRoutes from './base';
import oauthRoutes from './oauth';

const router: RouteObject[] = [
  {
    path: 'account',
    children: accountRoutes
  },
  { path: 'oauth2', children: oauthRoutes },
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
