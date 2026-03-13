import { useCallback, useEffect, useRef } from 'react';
import {
  SortingState,
  ColumnOrderState,
  ColumnSizingState,
  VisibilityState,
  PaginationState
} from '@tanstack/react-table';

interface TableState {
  sorting?: SortingState;
  columnOrder?: ColumnOrderState;
  columnSizing?: ColumnSizingState;
  columnVisibility?: VisibilityState;
  pagination?: PaginationState;
}

interface UseTableStatePersistProps {
  prefix: string;
  sorting?: SortingState;
  columnOrder?: ColumnOrderState;
  columnSizing?: ColumnSizingState;
  columnVisibility?: VisibilityState;
  pagination?: PaginationState;
}

const useTableStatePersist = ({
  prefix,
  sorting,
  columnOrder,
  columnSizing,
  columnVisibility,
  pagination
}: UseTableStatePersistProps) => {
  const stateItem = `${prefix}TableState`;
  const hasRestoredSortingRef = useRef(false);
  const hasRestoredColumnOrderRef = useRef(false);
  const hasRestoredPaginationRef = useRef(false);
  const hasRestoredColumnSizingRef = useRef(false);
  const hasRestoredColumnVisibilityRef = useRef(false);
  const isInitialMountRef = useRef(true);

  const saveSnapshot = useCallback(() => {
    if (typeof localStorage === 'undefined') return;

    const currentState: TableState = {};

    if (sorting !== undefined) {
      currentState.sorting = sorting;
    }
    if (columnOrder !== undefined) {
      currentState.columnOrder = columnOrder;
    }
    if (columnSizing !== undefined) {
      currentState.columnSizing = columnSizing;
    }
    if (columnVisibility !== undefined) {
      currentState.columnVisibility = columnVisibility;
    }
    if (pagination !== undefined) {
      currentState.pagination = pagination;
    }

    localStorage.setItem(stateItem, JSON.stringify(currentState));
  }, [stateItem, sorting, columnOrder, columnSizing, columnVisibility, pagination]);

  // Save state whenever it changes (after initial mount)
  useEffect(() => {
    if (!isInitialMountRef.current) {
      saveSnapshot();
    }
  }, [saveSnapshot]);

  // Mark initial mount as complete
  useEffect(() => {
    isInitialMountRef.current = false;
  }, []);

  // Save state on beforeunload
  useEffect(() => {
    const handleBeforeUnload = () => {
      saveSnapshot();
    };

    window.addEventListener('beforeunload', handleBeforeUnload);

    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
      saveSnapshot();
    };
  }, [saveSnapshot]);

  // Restore state from localStorage
  useEffect(() => {
    if (typeof localStorage === 'undefined') return;

    const savedState = localStorage.getItem(stateItem);
    if (!savedState) return;

    try {
      const state: TableState = JSON.parse(savedState);

      // Note: The actual state restoration is handled by the parent component
      // This hook primarily handles persistence
      console.log('Restored table state from localStorage:', state);
    } catch (error) {
      console.error('Error restoring table state:', error);
    }
  }, [stateItem]);

  return { saveSnapshot };
};

export default useTableStatePersist;
