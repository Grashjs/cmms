import React, {
  useState,
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef
} from 'react';
import { GridApiPro } from '@mui/x-data-grid-pro/models/gridApiPro';
import { GridColDef } from '@mui/x-data-grid';

const useGridStatePersist = (
  apiRef: React.MutableRefObject<GridApiPro>,
  columns: GridColDef[],
  prefix: string
) => {
  const stateItem = `${prefix}DataGridState`;
  const hasRestoredSortingRef = useRef(false);
  const hasRestoredPaginationRef = useRef(false);
  const columnAttemptCountRef = useRef(0);
  const MAX_COLUMN_ATTEMPTS = 20;

  const saveSnapshot = useCallback(() => {
    if (apiRef?.current?.exportState && localStorage) {
      const currentState = apiRef.current.exportState();
      localStorage.setItem(stateItem, JSON.stringify(currentState));
    }
  }, [apiRef, stateItem]);

  useLayoutEffect(() => {
    const handleBeforeUnload = () => {
      saveSnapshot();
    };

    window.addEventListener('beforeunload', handleBeforeUnload);

    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
      saveSnapshot();
    };
  }, [saveSnapshot]);

  useEffect(() => {
    // Only proceed if we have columns and API reference
    if (
      columns?.length > 0 &&
      apiRef?.current?.restoreState &&
      localStorage?.getItem(stateItem)
    ) {
      try {
        const state = JSON.parse(localStorage.getItem(stateItem));

        // Restore columns
        if (
          columnAttemptCountRef.current < MAX_COLUMN_ATTEMPTS &&
          state.columns
        ) {
          apiRef.current.restoreState({
            columns: state.columns
          });
          columnAttemptCountRef.current += 1;
        }

        // Restore sorting only once
        if (!hasRestoredSortingRef.current && state.sorting) {
          apiRef.current.restoreState({
            sorting: state.sorting
          });
          hasRestoredSortingRef.current = true;
        }

        // Restore pagination only once
        if (!hasRestoredPaginationRef.current && state.pagination) {
          apiRef.current.restoreState({
            pagination: state.pagination
          });
          hasRestoredPaginationRef.current = true;
        }
      } catch (error) {
        console.error('Error restoring grid state:', error);
      }
    }
  }, [apiRef, columns, stateItem]);
};

export default useGridStatePersist;
