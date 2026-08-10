import { useEffect, useRef } from 'react';
import { useNetInfo } from '@react-native-community/netinfo';
import { useDispatch, useSelector } from '../store';
import {
  flushOfflineQueue,
  syncOfflineQueueCount
} from '../utils/offlineQueue';

/**
 * Loads the persisted queue length on mount and drains it when connectivity
 * returns after an offline period.
 */
export default function useOfflineSync() {
  const dispatch = useDispatch();
  const { pending, flushing } = useSelector((state) => state.offlineQueue);
  const netInfo = useNetInfo();
  const wasOffline = useRef(false);

  useEffect(() => {
    syncOfflineQueueCount(dispatch);
  }, [dispatch]);

  useEffect(() => {
    const online = netInfo.isInternetReachable ?? netInfo.isConnected ?? false;
    if (!online) {
      wasOffline.current = true;
      return;
    }
    if (wasOffline.current && pending > 0 && !flushing) {
      flushOfflineQueue(dispatch);
    }
    wasOffline.current = false;
  }, [netInfo.isInternetReachable, netInfo.isConnected, pending, flushing, dispatch]);

  return { pending, flushing, online: netInfo.isInternetReachable ?? netInfo.isConnected };
}
