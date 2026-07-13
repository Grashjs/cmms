import { lazy, Suspense } from 'react';

import SuspenseLoader from 'src/components/SuspenseLoader';
import Guest from 'src/components/Guest';

const Loader = (Component) => (props) =>
  (
    <Suspense fallback={<SuspenseLoader />}>
      <Component {...props} />
    </Suspense>
  );

// Account

const LoginCover = Loader(
  lazy(() => import('../content/pages/Auth/Login/Cover'))
);
const LoginBasic = Loader(
  lazy(() => import('../content/pages/Auth/Login/Basic'))
);

const RegisterCover = Loader(
  lazy(() => import('../content/pages/Auth/Register/Cover'))
);

const RecoverPassword = Loader(
  lazy(() => import('../content/pages/Auth/RecoverPassword'))
);
const VerifyEmail = Loader(
  lazy(() => import('../content/pages/Auth/VerifyEmail'))
);

const OauthSuccess = Loader(
  lazy(() => import('../content/pages/Oauth/OauthSuccess'))
);

const accountRoutes = [
  {
    path: 'login',
    element: (
      <Guest>
        <LoginCover />
      </Guest>
    )
  },
  {
    path: 'recover-password',
    element: <RecoverPassword />
  },
  {
    path: 'verify',
    element: <VerifyEmail />
  },
  {
    path: 'register',
    element: (
      <Guest>
        <RegisterCover />
      </Guest>
    )
  }
];

export default accountRoutes;
