import { useState, useEffect, useCallback } from 'react';
import { Stomp } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { apiUrl } from 'src/config';
import api from 'src/utils/api';

interface UseExportReturn {
  exportEntity: (entity: EntityType) => Promise<void>;
  loadingExport: Record<EntityType, boolean>;
}

type EntityType = 'work-orders' | 'assets' | 'locations' | 'parts' | 'meters';

/**
 * Custom hook for exporting entities with WebSocket support
 * Generates a UUID, subscribes to WebSocket topic, then makes the export request
 */
export const useExport = (): UseExportReturn => {
  const [loadingExport, setLoadingExport] = useState<
    Record<EntityType, boolean>
  >({
    'work-orders': false,
    assets: false,
    locations: false,
    parts: false,
    meters: false
  });
  const [stompClient, setStompClient] = useState(null);

  // Initialize WebSocket connection
  useEffect(() => {
    const socket = new SockJS(`${apiUrl}ws`);
    const client = Stomp.over(socket);
    client.connect({ token: localStorage.getItem('accessToken') }, () => {
      setStompClient(client);
    });

    return () => {
      if (client) {
        client.disconnect();
      }
    };
  }, []);

  const exportEntity = useCallback(
    async (entity: EntityType): Promise<void> => {
      return new Promise((resolve, reject) => {
        // Check if stompClient is initialized
        if (!stompClient) {
          reject(new Error('WebSocket connection not initialized'));
          return;
        }

        // Generate UUID client-side
        const uuid = crypto.randomUUID();

        // Subscribe to WebSocket topic before making request
        const subscription = stompClient.subscribe(
          `/exports/${uuid}`,
          function (message) {
            try {
              const url = message.body;
              window.open(url, '_blank');
              resolve();
            } catch (error) {
              reject(error);
            } finally {
              subscription.unsubscribe();
              setLoadingExport((prev) => ({ ...prev, [entity]: false }));
            }
          }
        );
        // Handle subscription error
        if (!subscription) {
          reject(new Error('Failed to subscribe to WebSocket topic'));
          setLoadingExport((prev) => ({ ...prev, [entity]: false }));
          return;
        }

        // Make request with UUID
        setLoadingExport((prev) => ({ ...prev, [entity]: true }));
        api
          .get<{ success: boolean; message: string }>(
            `export/${entity}?uuid=${uuid}`
          )
          .catch((error) => {
            subscription.unsubscribe();
            setLoadingExport((prev) => ({ ...prev, [entity]: false }));
            reject(error);
          });
      });
    },
    [stompClient]
  );

  return {
    exportEntity,
    loadingExport
  };
};

export default useExport;
