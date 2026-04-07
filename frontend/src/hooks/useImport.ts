import { useState, useEffect, useCallback } from 'react';
import { Stomp } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { apiUrl } from 'src/config';
import api from 'src/utils/api';

interface UseImportReturn {
  importEntity: (entity: EntityType, payload: any[]) => Promise<ImportResponse>;
  loadingImport: boolean;
}

export type EntityType =
  | 'work-orders'
  | 'assets'
  | 'locations'
  | 'parts'
  | 'meters'
  | 'preventive-maintenances';

export interface ImportResponse {
  success: boolean;
  created: number;
  updated: number;
  message?: string;
}

/**
 * Custom hook for importing entities with WebSocket support
 * Generates a UUID, subscribes to WebSocket topic, then makes the import request
 */
export const useImport = (): UseImportReturn => {
  const [loadingImport, setLoadingImport] = useState<boolean>(false);
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

  const importEntity = useCallback(
    async (entity: EntityType, payload: any[]): Promise<ImportResponse> => {
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
          `/imports/${uuid}`,
          function (message) {
            try {
              const response = JSON.parse(message.body);

              // Check if it's an error message
              if (typeof response === 'string' && response.includes('error:')) {
                const errorMessage =
                  response.split('error: ')[1] || 'Import failed';
                reject(new Error(errorMessage));
              } else {
                // It's a successful ImportResponse
                resolve(response as ImportResponse);
              }
            } catch (error) {
              // Handle non-JSON error messages
              if (
                typeof message.body === 'string' &&
                message.body.includes('error:')
              ) {
                const errorMessage =
                  message.body.split('error: ')[1] || 'Import failed';
                reject(new Error(errorMessage));
              } else {
                reject(error);
              }
            } finally {
              subscription.unsubscribe();
              setLoadingImport(false);
            }
          }
        );

        // Handle subscription error
        if (!subscription) {
          reject(new Error('Failed to subscribe to WebSocket topic'));
          setLoadingImport(false);
          return;
        }

        // Make request with UUID
        setLoadingImport(true);
        api
          .post<ImportResponse>(`${basePath}/${entity}?uuid=${uuid}`, payload)
          .catch((error) => {
            subscription.unsubscribe();
            setLoadingImport(false);
            reject(error);
          });
      });
    },
    [stompClient]
  );

  return {
    importEntity,
    loadingImport
  };
};

const basePath = 'import';

export default useImport;
