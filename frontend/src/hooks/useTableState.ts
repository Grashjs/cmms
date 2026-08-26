import { useState, useEffect, useRef, useCallback } from 'react';
import {
  SortingState,
  PaginationState,
  ColumnOrderState,
  ColumnSizingState,
  VisibilityState,
  OnChangeFn
} from '@tanstack/react-table';
import useTableStatePersist from './useTableStatePersist';
import { SearchCriteria, SortDirection } from '../models/owns/page';
import { TableLayout } from '../models/owns/savedView';

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
  pinnedColumns: string[];
  setPinnedColumns: (pinnedColumns: string[]) => void;
  /** The current layout, for storing in a saved view. */
  getLayout: () => TableLayout;
  /** Restores a layout from a saved view. Fields the layout omits are left as they are. */
  applyLayout: (layout: TableLayout) => void;
}

interface UseTableStateProps {
  prefix: string;
  initialSorting?: SortingState;
  initialPagination?: PaginationState;
  pageSizeOptions?: number[];
  // Optional: when this changes, pagination will be reset to initial value
  // Useful for resetting pagination when navigating between different pages
  contextKey?: string;
  // Optional: whether to persist pageIndex in localStorage (default: true)
  persistPageIndex?: boolean;
  setCriteria?: React.Dispatch<React.SetStateAction<SearchCriteria>>;
  fieldMapping?: Record<string, string>;
  /**
   * Columns that start hidden, as `{ id: false }`. A list can offer a wide
   * choice of columns without every one of them landing on screen. Stored
   * layouts win for the columns they mention, so a column added later keeps
   * this default instead of appearing unannounced in everybody's table.
   */
  initialColumnVisibility?: VisibilityState;
}

const useTableState = ({
  prefix,
  initialSorting = [],
  initialPagination = { pageIndex: 0, pageSize: 10 },
  pageSizeOptions = [10, 25, 50, 100],
  persistPageIndex = false,
  setCriteria = () => null,
  fieldMapping = {},
  initialColumnVisibility = {}
}: UseTableStateProps): TableStateReturn => {
  const stateItem = `${prefix}TableState`;
  const hasRestoredRef = useRef(false);

  // Initialize state
  const [sorting, setSortingState] = useState<SortingState>(initialSorting);
  const [pagination, setPaginationState] =
    useState<PaginationState>(initialPagination);
  const [columnOrder, setColumnOrderState] = useState<ColumnOrderState>([]);
  const [columnSizing, setColumnSizingState] = useState<ColumnSizingState>({});
  const [columnVisibility, setColumnVisibilityState] =
    useState<VisibilityState>(initialColumnVisibility);
  const [pinnedColumns, setPinnedColumnsState] = useState<string[]>([]);

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
      setPaginationState({
        pageIndex: persistPageIndex
          ? state.pageSize
          : initialPagination.pageIndex,
        pageSize: state.pageSize ?? initialPagination.pageSize
      });
      if (state.columnOrder && state.columnOrder.length > 0) {
        setColumnOrderState(state.columnOrder);
      }
      if (state.columnSizing && Object.keys(state.columnSizing).length > 0) {
        setColumnSizingState(state.columnSizing);
      }
      if (
        state.columnVisibility &&
        Object.keys(state.columnVisibility).length > 0
      ) {
        // Merged, not replaced: a stored layout predates any column added
        // later and says nothing about it, so replacing would drop that
        // column's default and show it to everyone who ever saved a layout.
        setColumnVisibilityState({
          ...initialColumnVisibility,
          ...state.columnVisibility
        });
      }
      if (state.pinnedColumns && Array.isArray(state.pinnedColumns)) {
        setPinnedColumnsState(state.pinnedColumns);
      }

      hasRestoredRef.current = true;
    } catch (error) {
      console.error('Error restoring table state:', error);
    }
  }, [stateItem, persistPageIndex]);

  // Wrap state setters to work with TanStack Table's OnChangeFn
  const setSorting = useCallback<OnChangeFn<SortingState>>((updater) => {
    setSortingState(updater);
  }, []);

  const setPagination = useCallback<OnChangeFn<PaginationState>>((updater) => {
    setPaginationState(updater);
  }, []);

  const setColumnOrder = useCallback<OnChangeFn<ColumnOrderState>>(
    (updater) => {
      setColumnOrderState(updater);
    },
    []
  );

  const setColumnSizing = useCallback<OnChangeFn<ColumnSizingState>>(
    (updater) => {
      setColumnSizingState(updater);
    },
    []
  );

  const setColumnVisibility = useCallback<OnChangeFn<VisibilityState>>(
    (updater) => {
      setColumnVisibilityState(updater);
    },
    []
  );

  const setPinnedColumns = useCallback((newPinnedColumns: string[]) => {
    setPinnedColumnsState(newPinnedColumns);
  }, []);

  const getLayout = useCallback(
    (): TableLayout => ({
      sorting: sorting.map((sort) => ({ id: sort.id, desc: sort.desc })),
      columnOrder,
      columnSizing,
      columnVisibility,
      pinnedColumns,
      pageSize: pagination.pageSize
    }),
    [sorting, columnOrder, columnSizing, columnVisibility, pinnedColumns, pagination]
  );

  /**
   * Only assigns what the layout actually carries. A view saved before a field existed must
   * not blank that field out — it would silently reset column widths or unpin a column when
   * an older view is applied.
   *
   * pageIndex always goes back to 0: the rows are about to change, so staying on page 4 would
   * likely land on an empty page.
   */
  const applyLayout = useCallback((layout: TableLayout) => {
    if (!layout) return;
    if (layout.sorting) setSortingState(layout.sorting);
    if (layout.columnOrder) setColumnOrderState(layout.columnOrder);
    if (layout.columnSizing) setColumnSizingState(layout.columnSizing);
    // Same merge as the localStorage restore above: a saved view written
    // before a column existed must not decide that column's visibility.
    if (layout.columnVisibility)
      setColumnVisibilityState({
        ...initialColumnVisibility,
        ...layout.columnVisibility
      });
    if (layout.pinnedColumns) setPinnedColumnsState(layout.pinnedColumns);
    setPaginationState((prev) => ({
      pageIndex: 0,
      pageSize: layout.pageSize ?? prev.pageSize
    }));
  }, []);

  // Sync criteria with TanStack Table state
  useEffect(() => {
    setCriteria((prev) => {
      if (
        prev.pageSize === pagination.pageSize &&
        prev.pageNum === pagination.pageIndex
      )
        return prev; // no change, no re-render
      return {
        ...prev,
        pageSize: pagination.pageSize,
        pageNum: pagination.pageIndex
      };
    });
  }, [pagination]);

  useEffect(() => {
    setCriteria((prev) => {
      if (sorting.length === 0) {
        if (!prev.sortField && !prev.direction) return prev; // no change
        return { ...prev, sortField: undefined, direction: undefined };
      }
      const sort = sorting[0];
      const mappedField = fieldMapping[sort.id];
      if (!mappedField) return prev;
      const newDirection: SortDirection = sort.desc ? 'DESC' : 'ASC';
      if (prev.sortField === mappedField && prev.direction === newDirection)
        return prev; // no change
      return { ...prev, sortField: mappedField, direction: newDirection };
    });
  }, [sorting]);

  // Persist state changes
  useTableStatePersist({
    prefix,
    sorting,
    columnOrder,
    columnSizing,
    columnVisibility,
    pagination,
    pinnedColumns,
    persistPageIndex
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
    setColumnVisibility,
    pinnedColumns,
    setPinnedColumns,
    getLayout,
    applyLayout
  };
};

export default useTableState;
