import { useLocation, useNavigate, useRoutes } from 'react-router-dom';
import router from 'src/router';

import { SnackbarProvider } from 'notistack';
import AdapterDateFns from '@mui/lab/AdapterDateFns';
import LocalizationProvider from '@mui/lab/LocalizationProvider';
import useAuth from 'src/hooks/useAuth';

import { CssBaseline } from '@mui/material';
import ThemeProvider from './theme/ThemeProvider';
import AppInit from './components/AppInit';
import { CustomSnackBarProvider } from './contexts/CustomSnackBarContext';
import ReactGA from 'react-ga4';
import { googleTrackingId, IS_LOCALHOST } from './config';
import { useEffect } from 'react';
import { CompanySettingsProvider } from './contexts/CompanySettingsContext';

if (!IS_LOCALHOST && googleTrackingId) ReactGA.initialize(googleTrackingId);
function App() {
  const content = useRoutes(router);
  const navigate = useNavigate();
  const { isInitialized, company, isAuthenticated, user } = useAuth();
  let location = useLocation();
  useEffect(() => {
    if (!IS_LOCALHOST && googleTrackingId)
      ReactGA.send({
        hitType: 'pageview',
        page: location.pathname + location.search
      });
  }, [location]);
  useEffect(() => {
    const arr = location.pathname.split('/');
    if (
      !['downgrade', 'upgrade'].includes(arr[arr.length - 1]) &&
      isInitialized &&
      isAuthenticated
    )
      if (company.subscription.downgradeNeeded) {
        navigate('/app/downgrade');
      } else if (user.ownsCompany && company.subscription.upgradeNeeded) {
        navigate('/app/upgrade');
      }
  }, [company, isInitialized, isAuthenticated, location]);

  useEffect(() => {
    const arr = location.pathname.split('/');
    if (
      !['switch-account'].includes(arr[arr.length - 1]) &&
      isInitialized &&
      isAuthenticated
    )
      if (user.superAccountRelations.length) {
        navigate('/app/switch-account');
      }
  }, [user, isInitialized, isAuthenticated, location]);

  return (
    <ThemeProvider>
      <LocalizationProvider dateAdapter={AdapterDateFns}>
        <SnackbarProvider
          maxSnack={6}
          anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'right'
          }}
        >
          <CustomSnackBarProvider>
            <CompanySettingsProvider>
              <CssBaseline />
              {isInitialized ? content : <AppInit />}
            </CompanySettingsProvider>
          </CustomSnackBarProvider>
        </SnackbarProvider>
      </LocalizationProvider>
    </ThemeProvider>
  );
}
export default App;
