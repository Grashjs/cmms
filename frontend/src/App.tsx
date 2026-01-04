import { useLocation, useNavigate, useRoutes } from 'react-router-dom';
import router from 'src/router';

import { SnackbarProvider } from 'notistack';
import AdapterDateFns from '@mui/lab/AdapterDateFns';
import LocalizationProvider from '@mui/lab/LocalizationProvider';
import useAuth from 'src/hooks/useAuth';

import { Alert, CssBaseline } from '@mui/material';
import ThemeProvider from './theme/ThemeProvider';
import AppInit from './components/AppInit';
import { CustomSnackBarProvider } from './contexts/CustomSnackBarContext';
import ReactGA from 'react-ga4';
import {
  customLogoPaths,
  googleTrackingId,
  IS_LOCALHOST,
  isCloudVersion,
  isWhiteLabeled
} from './config';
import { useEffect, useState } from 'react';
import { CompanySettingsProvider } from './contexts/CompanySettingsContext';
import { getLicenseValidity } from './slices/license';
import { useDispatch, useSelector } from './store';
import { useBrand } from './hooks/useBrand';
import { useTranslation } from 'react-i18next';
import { UtmTrackerProvider } from '@nik0di3m/utm-tracker-hook';
import { useLicenseEntitlement } from './hooks/useLicenseEntitlement';

if (!IS_LOCALHOST && googleTrackingId) ReactGA.initialize(googleTrackingId);

const DemoAlert = () => {
  const [show, setShow] = useState<boolean>(true);
  const { t } = useTranslation();
  return (
    show && (
      <Alert
        onClose={() => {
          setShow(false);
        }}
        sx={{
          position: 'fixed',
          bottom: 10,
          left: '50%',
          transform: 'translateX(-50%)',
          zIndex: 1000,
          width: '50%'
        }}
        severity="error"
      >
        {t('demo_warning')}
      </Alert>
    )
  );
};

const DemoCleaningAlert = () => {
  const { isInitialized, company, isAuthenticated, user } = useAuth();
  const { t } = useTranslation();
  const userCreatedAt = new Date(user?.createdAt);
  const [show, setShow] = useState<boolean>(true);
  const sevenDaysAgo = new Date();
  sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);

  if (
    isCloudVersion &&
    isAuthenticated &&
    user?.ownsCompany &&
    show &&
    userCreatedAt > sevenDaysAgo &&
    !localStorage.getItem('demoDataCleaningHint')
  )
    return (
      <Alert
        onClose={() => {
          setShow(false);
          localStorage.setItem('demoDataCleaningHint', 'shown');
        }}
        sx={{
          position: 'fixed',
          bottom: 10,
          left: '50%',
          transform: 'translateX(-50%)',
          zIndex: 1000,
          width: '50%'
        }}
        severity="info"
      >
        {t('You can delete demo data from General Settings')}
      </Alert>
    );
  return null;
};
function App() {
  const content = useRoutes(router);
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { logo } = useBrand();
  const { isInitialized, company, isAuthenticated, user } = useAuth();
  const { state: licensingState } = useSelector((state) => state.license);
  const hasBrandingEntitlement = useLicenseEntitlement('BRANDING');
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

  useEffect(() => {
    dispatch(getLicenseValidity());
  }, []);

  useEffect(() => {
    if (customLogoPaths && hasBrandingEntitlement) {
      let link: HTMLLinkElement = document.querySelector("link[rel~='icon']");
      if (!link) {
        link = document.createElement('link');
        link.rel = 'icon';
        document.getElementsByTagName('head')[0].appendChild(link);
      }
      link.href = logo.dark;
    }
  }, [logo.dark, hasBrandingEntitlement]);

  useEffect(() => {
    if (isCloudVersion) {
      const referrer = document.referrer || '';
      localStorage.setItem('referrerData', referrer);
    }
  }, []);

  return (
    <UtmTrackerProvider customParams={['msclkid']}>
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
                {user && company?.demo && <DemoAlert />}
                <DemoCleaningAlert />
              </CompanySettingsProvider>
            </CustomSnackBarProvider>
          </SnackbarProvider>
        </LocalizationProvider>
      </ThemeProvider>
    </UtmTrackerProvider>
  );
}
export default App;
