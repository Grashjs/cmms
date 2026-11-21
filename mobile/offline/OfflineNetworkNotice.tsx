import { useContext, useEffect, useRef } from 'react';
import NetInfo from '@react-native-community/netinfo';
import { AppState } from 'react-native';
import { useTranslation } from 'react-i18next';
import { CustomSnackBarContext } from '../contexts/CustomSnackBarContext';
import useAuth from '../hooks/useAuth';
import { useOffline } from './OfflineProvider';

/** Show a notice only when network availability actually changes in the open app. */
export function OfflineNetworkNotice() {
  const { isAuthenticated } = useAuth();
  const { enabled, error, errorSequence, syncSuccessSequence } = useOffline();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { t } = useTranslation();
  const previous = useRef<boolean | null>(null);

  const lastError = useRef(-1);
  useEffect(() => {
    if (error && errorSequence !== lastError.current) {
      lastError.current = errorSequence;
      showSnackBar(error, 'error');
    }
  }, [error, errorSequence, showSnackBar]);

  const lastSuccess = useRef(syncSuccessSequence);
  useEffect(() => {
    if (syncSuccessSequence !== lastSuccess.current) {
      lastSuccess.current = syncSuccessSequence;
      if (enabled) showSnackBar(t('offline_sync_success'), 'success');
    }
  }, [enabled, syncSuccessSequence, showSnackBar, t]);

  useEffect(() => {
    if (!enabled) {
      previous.current = null;
      return;
    }
    return NetInfo.addEventListener((state) => {
      const available =
        state.isConnected === true && state.isInternetReachable !== false;
      if (previous.current === null) {
        previous.current = available;
        return;
      }
      if (previous.current === available) return;
      previous.current = available;
      if (isAuthenticated && AppState.currentState === 'active')
        showSnackBar(
          t(available ? 'offline_network_restored' : 'offline_network_lost'),
          'info'
        );
    });
  }, [enabled, isAuthenticated, showSnackBar, t]);

  return null;
}
