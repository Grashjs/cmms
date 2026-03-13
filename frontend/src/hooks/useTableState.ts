import { useState, useEffect, useRef, useCallback } from 'react';
import {
  SortingState,
  PaginationState,
  ColumnOrderState,
  ColumnSizingState,
  VisibilityState,
  OnChangeFn
} from '@tanstack/react-table';
import { SortDirection } from '../../models/owns/page';
import useTableStatePersist from './useTableStatePersist';

export interface TableStateReturn {
  sorting: SortingState;
  setSorting: OnChangeFn<SortingState>;
  pagination: PaginationState;
  setPagination: OnChangeFn<PaginationState>;
  columnOrder: ColumnOrderState;
  setColumnOrder: OnChangeFn<ColumnOrderState>;
  columnSizing: ColumnSizingState;
  setColumnSizing: OnChangeFn<ColumnSizingState>;
  columnVisibility: VisibilityState;
  setColumnVisibility: OnChangeFn<VisibilityState>;
}

interface UseTableStateProps {
  prefix: string;
  initialSorting?: SortingState;
  initialPagination?: PaginationState;
  pageSizeOptions?: number[];
}

const useTableState = ({
  prefix,
  initialSorting = [],
  initialPagination = { pageIndex: 0, pageSize: 10 },
  pageSizeOptions = [10, 25, 50, 100]
}: UseTableStateProps): TableStateReturn => {
  const stateItem = `${prefix}TableState`;
  const hasRestoredRef = useRef(false);

  // Initialize state
  const [sorting, setSortingState] = useState<SortingState>(initialSorting);
  const [pagination, setPaginationState] = useState<PaginationState>(initialPagination);
  const [columnOrder, setColumnOrderState] = useState<ColumnOrderState>([]);
  const [columnSizing, setColumnSizingState] = useState<ColumnSizingState>({});
  const [columnVisibility, setColumnVisibilityState] = useState<VisibilityState>({});

  // Restore state from localStorage on mount
  useEffect(() => {
    if (typeof localStorage === 'undefined' || hasRestoredRef.current) return;

    const savedState = localStorage.getItem(stateItem);
    if (!savedState) {
      hasRestoredRef.current = true;
      return;
    }

    try {
      const state = JSON.parse(savedState);

      if (state.sorting && state.sorting.length > 0) {
        setSortingState(state.sorting);
      }
      if (state.pagination) {
        setPaginationState(state.pagination);
      }
      if (state.columnOrder && state.columnOrder.length > 0) {
        setColumnOrderState(state.columnOrder);
      }
      if (state.columnSizing && Object.keys(state.columnSizing).length > 0) {
        setColumnSizingState(state.columnSizing);
      }
      if (state.columnVisibility && Object.keys(state.columnVisibility).length > 0) {
        setColumnVisibilityState(state.columnVisibility);
      }

      hasRestoredRef.current = true;
    } catch (error) {
      console.error('Error restoring table state:', error);
    }
  }, [stateItem]);

  // Wrap state setters to work with TanStack Table's OnChangeFn
  const setSorting = useCallback<OnChangeFn<SortingState>>((updater) => {
    setSortingState(updater);
  }, []);

  const setPagination = useCallback<OnChangeFn<PaginationState>>((updater) => {
    setPaginationState(updater);
  }, []);

  const setColumnOrder = useCallback<OnChangeFn<ColumnOrderState>>((updater) => {
    setColumnOrderState(updater);
  }, []);

  const setColumnSizing = useCallback<OnChangeFn<ColumnSizingState>>((updater) => {
    setColumnSizingState(updater);
  }, []);

  const setColumnVisibility = useCallback<OnChangeFn<VisibilityState>>((updater) => {
    setColumnVisibilityState(updater);
  }, []);

  // Persist state changes
  useTableStatePersist({
    prefix,
    sorting,
    columnOrder,
    columnSizing,
    columnVisibility,
    pagination
  });

  return {
    sorting,
    setSorting,
    pagination,
    setPagination,
    columnOrder,
    setColumnOrder,
    columnSizing,
    setColumnSizing,
    columnVisibility,
    setColumnVisibility
  };
};

export default useTableState;
