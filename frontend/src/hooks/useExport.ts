import { useState, useEffect, useCallback } from 'react';
import { Stomp } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { apiUrl } from 'src/config';
import api from 'src/utils/api';
import { SearchCriteria } from 'src/models/owns/page';
import useAuth from './useAuth';

/**
 * Passing this turns the export into a filtered one: it POSTs the criteria and the column
 * selection instead of GETting the whole entity. Only work orders and assets accept it; the
 * other entities have no column registry on the backend yet.
 */
export interface ExportOptions {
  criteria?: SearchCriteria;
  /** Column keys in output order. Omit for the entity's default column set. */
  columns?: string[];
}

interface UseExportReturn {
  exportEntity: (
    entity: ExportEntityType,
    options?: ExportOptions
  ) => Promise<void>;
  loadingExport: Record<ExportEntityType, boolean>;
}

/**
 * Table columns that render controls or images rather than data. When a page builds an export
 * column selection from its visible columns, these are the ones to leave out — everything else
 * must have a counterpart in the backend's column registry, which rejects unknown keys.
 */
export const NON_EXPORTABLE_COLUMNS = new Set([
  'actions',
  'expander',
  'image'
]);

export type ExportEntityType =
  | 'work-orders'
  | 'assets'
  | 'locations'
  | 'parts'
  | 'meters'
  | 'preventive-maintenances'
  | 'costs-times'
  | 'part-transactions';

/**
 * Custom hook for exporting entities with WebSocket support
 * Generates a UUID, subscribes to WebSocket topic, then makes the export request
 */
export const useExport = (): UseExportReturn => {
  const [loadingExport, setLoadingExport] = useState<
    Record<ExportEntityType, boolean>
  >({
    'work-orders': false,
    assets: false,
    locations: false,
    parts: false,
    meters: false,
    'preventive-maintenances': false,
    'costs-times': false,
    'part-transactions': false
  });
  const [stompClient, setStompClient] = useState(null);
  const { user } = useAuth();
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
    async (
      entity: ExportEntityType,
      options?: ExportOptions
    ): Promise<void> => {
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
          `/user/${user.email}/exports/${uuid}`,
          function (message) {
            try {
              const url = message.body;
              if (url.includes('error:')) reject();
              else {
                window.open(url, '_blank');
                resolve();
              }
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

        // Make request with UUID. The response only acknowledges the job; the finished file
        // arrives on the websocket topic subscribed to above, for both variants.
        setLoadingExport((prev) => ({ ...prev, [entity]: true }));
        const request = options
          ? api.post<{ success: boolean; message: string }>(
              `export/${entity}?uuid=${uuid}`,
              { criteria: options.criteria ?? null, columns: options.columns ?? null }
            )
          : api.get<{ success: boolean; message: string }>(
              `export/${entity}?uuid=${uuid}`
            );
        request.catch((error) => {
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
