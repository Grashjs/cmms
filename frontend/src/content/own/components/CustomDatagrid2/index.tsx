import {
  flexRender,
  getCoreRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  SortingState,
  useReactTable,
  ColumnDef,
  PaginationState,
  OnChangeFn,
  ColumnFiltersState,
  getFilteredRowModel,
  RowData,
  getCoreRowModel as getTanstackCoreRowModel,
  ColumnOrderState,
  ColumnSizingState,
  VisibilityState,
  ColumnResizeMode
} from '@tanstack/react-table';
import {
  Box,
  Stack,
  Typography,
  useTheme,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Checkbox,
  IconButton,
  Paper,
  TablePagination,
  CircularProgress,
  alpha,
  Menu,
  MenuItem,
  Switch,
  Divider
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import {
  useEffect,
  useRef,
  useState,
  useMemo,
  DragEventHandler,
  DragEvent
} from 'react';
import useWindowDimensions from '../../../../hooks/useWindowDimensions';
import useAuth from '../../../../hooks/useAuth';
import { UiConfiguration } from '../../../../models/owns/uiConfiguration';
import { SortDirection } from '../../../../models/owns/page';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import FirstPageIcon from '@mui/icons-material/FirstPage';
import LastPageIcon from '@mui/icons-material/LastPage';
import KeyboardArrowLeft from '@mui/icons-material/KeyboardArrowLeft';
import KeyboardArrowRight from '@mui/icons-material/KeyboardArrowRight';
import DragIndicatorIcon from '@mui/icons-material/DragIndicator';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import PushPinIcon from '@mui/icons-material/PushPin';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import ViewColumnIcon from '@mui/icons-material/ViewColumn';
import NoRowsMessageWrapper from '../NoRowsMessageWrapper';

declare module '@tanstack/react-table' {
  interface ColumnMeta<TData extends RowData, TValue> {
    uiConfigKey?: keyof Omit<UiConfiguration, 'id'>;
  }
}

export type CustomDatagridColumn2<TData extends RowData = any> =
  ColumnDef<TData> & {
    uiConfigKey?: keyof Omit<UiConfiguration, 'id'>;
  };

interface CustomDatagrid2Props<TData extends RowData> {
  columns: CustomDatagridColumn2<TData>[];
  data: TData[];
  notClickable?: boolean;
  onRowClick?: (row: TData) => void;
  loading?: boolean;
  // Pagination props
  pagination?: PaginationState;
  onPaginationChange?: (pagination: PaginationState) => void;
  totalRows?: number;
  pageSizeOptions?: number[];
  // Sorting props
  sorting?: SortingState;
  onSortingChange?: OnChangeFn<SortingState>;
  // Column order props
  columnOrder?: ColumnOrderState;
  onColumnOrderChange?: OnChangeFn<ColumnOrderState>;
  // Column sizing props
  columnSizing?: ColumnSizingState;
  onColumnSizingChange?: OnChangeFn<ColumnSizingState>;
  // Column visibility props
  columnVisibility?: VisibilityState;
  onColumnVisibilityChange?: OnChangeFn<VisibilityState>;
  // Filtering props (optional)
  columnFilters?: ColumnFiltersState;
  onColumnFiltersChange?: OnChangeFn<ColumnFiltersState>;
  // Selection props (optional)
  enableRowSelection?: boolean;
  rowSelection?: Record<string, boolean>;
  onRowSelectionChange?: OnChangeFn<Record<string, boolean>>;
  // Custom render for no rows
  noRowsMessage?: string;
  noRowsAction?: string;
  // Enable column reordering
  enableColumnReordering?: boolean;
  // Enable column resizing
  enableColumnResizing?: boolean;
  // Pinned columns (array of column IDs)
  pinnedColumns?: string[];
  onPinnedColumnsChange?: (pinnedColumns: string[]) => void;
}

const PINNED_BG = '#F2F5F9';

function CustomDatagrid2<TData extends RowData>({
  columns,
  data,
  notClickable,
  onRowClick,
  loading,
  pagination = { pageIndex: 0, pageSize: 10 },
  onPaginationChange = () => {},
  totalRows = 0,
  pageSizeOptions = [10, 25, 50, 100],
  sorting = [],
  onSortingChange = () => {},
  columnOrder,
  onColumnOrderChange,
  columnSizing = {},
  onColumnSizingChange = () => {},
  columnVisibility = {},
  onColumnVisibilityChange = () => {},
  columnFilters,
  onColumnFiltersChange,
  enableRowSelection,
  rowSelection,
  onRowSelectionChange,
  noRowsMessage,
  enableColumnReordering = true,
  enableColumnResizing = true,
  pinnedColumns: externalPinnedColumns,
  onPinnedColumnsChange,
  noRowsAction
}: CustomDatagrid2Props<TData>) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const { height } = useWindowDimensions();
  const tableRef = useRef<HTMLDivElement>(null);
  const [tableHeight, setTableHeight] = useState<number>(500);
  const { user } = useAuth();
  const scrollContainerRef = useRef<HTMLDivElement>(null);

  // Internal pinned columns state (used if external not provided)
  const [internalPinnedColumns, setInternalPinnedColumns] = useState<string[]>(
    []
  );
  const pinnedColumns = externalPinnedColumns ?? internalPinnedColumns;
  const setPinnedColumns = (cols: string[]) => {
    if (onPinnedColumnsChange) {
      onPinnedColumnsChange(cols);
    } else {
      setInternalPinnedColumns(cols);
    }
  };

  // Drag state for column reordering
  const [draggedColumnId, setDraggedColumnId] = useState<string | null>(null);

  // Column menu state
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [currentColumnId, setCurrentColumnId] = useState<string | null>(null);
  const [columnsMenuAnchor, setColumnsMenuAnchor] =
    useState<null | HTMLElement>(null);

  const openMenu = Boolean(anchorEl);
  const openColumnsMenu = Boolean(columnsMenuAnchor);

  const handleMenuClick = (
    event: React.MouseEvent<HTMLButtonElement>,
    columnId: string
  ) => {
    event.stopPropagation();
    setAnchorEl(event.currentTarget);
    setCurrentColumnId(columnId);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
    setCurrentColumnId(null);
  };

  const handleColumnsMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    event.stopPropagation();
    // Anchor to the same MoreVert button that opened the first menu (anchorEl),
    // so the columns menu stays positioned near the column header even after
    // the first menu unmounts.
    setColumnsMenuAnchor(anchorEl);
    setAnchorEl(null); // close first menu without nulling currentColumnId
  };

  const handleColumnsMenuClose = () => {
    setColumnsMenuAnchor(null);
  };

  const handleHideColumn = () => {
    if (currentColumnId && onColumnVisibilityChange) {
      onColumnVisibilityChange((prev) => ({
        ...prev,
        [currentColumnId]: false
      }));
    }
    handleMenuClose();
  };

  // Check if a column is currently pinned
  const isColumnPinned = (columnId: string) => pinnedColumns.includes(columnId);

  const handlePinColumn = () => {
    if (!currentColumnId) return;

    if (isColumnPinned(currentColumnId)) {
      // Unpin: remove from pinned list and move after last pinned column in order
      const newPinned = pinnedColumns.filter((id) => id !== currentColumnId);
      setPinnedColumns(newPinned);

      if (onColumnOrderChange) {
        const allColumnIds = table.getAllColumns().map((col) => col.id);
        const currentOrder = columnOrder?.length
          ? columnOrder.filter((id) => allColumnIds.includes(id))
          : [...allColumnIds];

        // Remove from current position, insert after last pinned column
        const withoutCurrent = currentOrder.filter(
          (id) => id !== currentColumnId
        );
        const lastPinnedIndex = newPinned.reduce((maxIdx, pinnedId) => {
          const idx = withoutCurrent.indexOf(pinnedId);
          return idx > maxIdx ? idx : maxIdx;
        }, -1);
        const insertAt = lastPinnedIndex + 1;
        withoutCurrent.splice(insertAt, 0, currentColumnId);
        onColumnOrderChange(withoutCurrent);
      }
    } else {
      // Pin: add to pinned list and move to front
      const newPinned = [...pinnedColumns, currentColumnId];
      setPinnedColumns(newPinned);

      if (onColumnOrderChange) {
        const allColumnIds = table.getAllColumns().map((col) => col.id);
        const currentOrder = columnOrder?.length
          ? columnOrder.filter((id) => allColumnIds.includes(id))
          : [...allColumnIds];

        const newOrder = currentOrder.filter((id) => id !== currentColumnId);
        // Insert after existing pinned columns
        const lastExistingPinnedIndex = pinnedColumns.reduce(
          (maxIdx, pinnedId) => {
            const idx = newOrder.indexOf(pinnedId);
            return idx > maxIdx ? idx : maxIdx;
          },
          -1
        );
        newOrder.splice(lastExistingPinnedIndex + 1, 0, currentColumnId);
        onColumnOrderChange(newOrder);
      }
    }

    handleMenuClose();
  };

  const handleToggleColumnVisibility = (columnId: string) => {
    if (onColumnVisibilityChange) {
      onColumnVisibilityChange((prev: VisibilityState) => {
        const currentVisibility = prev[columnId] !== false;
        return {
          ...prev,
          [columnId]: !currentVisibility
        };
      });
    }
  };

  const getTableHeight = () => {
    if (tableRef.current) {
      const viewportOffset = tableRef.current.getBoundingClientRect();
      const top = viewportOffset.top;
      return height - top - 15;
    }
    return 500;
  };

  useEffect(() => {
    setTableHeight(getTableHeight());
  }, [tableRef.current, height]);

  // Filter columns based on uiConfiguration
  const filteredColumns = useMemo(() => {
    return columns.filter((col) => {
      const uiConfigKey = col.uiConfigKey || col.meta?.uiConfigKey;
      return uiConfigKey ? user.uiConfiguration[uiConfigKey] : true;
    });
  }, [columns, user.uiConfiguration]);

  const table = useReactTable({
    columns: filteredColumns,
    data,
    state: {
      pagination,
      sorting,
      columnFilters,
      rowSelection,
      columnOrder,
      columnSizing,
      columnVisibility
    },
    onPaginationChange: onPaginationChange,
    onSortingChange: onSortingChange,
    onColumnFiltersChange: onColumnFiltersChange,
    onRowSelectionChange: onRowSelectionChange,
    onColumnOrderChange: onColumnOrderChange,
    onColumnSizingChange: onColumnSizingChange,
    onColumnVisibilityChange: onColumnVisibilityChange,
    getCoreRowModel: getTanstackCoreRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    manualPagination: true,
    manualSorting: true,
    manualFiltering: true,
    rowCount: totalRows,
    enableColumnResizing: enableColumnResizing,
    columnResizeMode: 'onChange' as ColumnResizeMode
  });

  // Compute sticky left offsets for pinned columns
  // Pinned columns are those whose IDs are in pinnedColumns array
  const getPinnedStickyLeft = (columnId: string): number | undefined => {
    if (!isColumnPinned(columnId)) return undefined;

    const visibleHeaders = table.getHeaderGroups()[0]?.headers ?? [];
    let left = 0;
    for (const header of visibleHeaders) {
      if (header.id === columnId) break;
      if (isColumnPinned(header.id)) {
        left += header.getSize();
      }
    }
    // Account for selection checkbox column
    if (enableRowSelection) left += 42; // approximate checkbox cell width
    return left;
  };

  const TablePaginationActions = (props: any) => {
    const { page, onPageChange, rowsPerPage } = props;
    const totalPages = Math.ceil(totalRows / rowsPerPage);

    const handleFirstPageClick = (
      event: React.MouseEvent<HTMLButtonElement>
    ) => {
      onPageChange(event, 0);
    };

    const handleLastPageClick = (
      event: React.MouseEvent<HTMLButtonElement>
    ) => {
      onPageChange(event, Math.max(0, totalPages - 1));
    };

    const handleBackClick = (event: React.MouseEvent<HTMLButtonElement>) => {
      onPageChange(event, page - 1);
    };

    const handleNextClick = (event: React.MouseEvent<HTMLButtonElement>) => {
      onPageChange(event, page + 1);
    };

    return (
      <Box sx={{ flexShrink: 0, ml: 2.5 }}>
        <IconButton
          onClick={handleFirstPageClick}
          disabled={page === 0}
          aria-label="first page"
        >
          <FirstPageIcon />
        </IconButton>
        <IconButton
          onClick={handleBackClick}
          disabled={page === 0}
          aria-label="previous page"
        >
          <KeyboardArrowLeft />
        </IconButton>
        <IconButton
          onClick={handleNextClick}
          disabled={page >= totalPages - 1}
          aria-label="next page"
        >
          <KeyboardArrowRight />
        </IconButton>
        <IconButton
          onClick={handleLastPageClick}
          disabled={page >= totalPages - 1}
          aria-label="last page"
        >
          <LastPageIcon />
        </IconButton>
      </Box>
    );
  };

  // Handle column drag start
  const handleDragStart = (
    e: DragEvent<HTMLTableCellElement>,
    columnId: string
  ) => {
    setDraggedColumnId(columnId);
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.dropEffect = 'move';
  };

  // Handle column drag over
  const handleDragOver = (e: DragEvent<HTMLTableCellElement>) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
  };

  // Handle column drop
  const handleDrop = (
    e: DragEvent<HTMLTableCellElement>,
    targetColumnId: string
  ) => {
    e.preventDefault();
    e.stopPropagation();
    if (
      draggedColumnId &&
      draggedColumnId !== targetColumnId &&
      onColumnOrderChange
    ) {
      const currentOrder = columnOrder || table.getState().columnOrder || [];
      const filteredColumnIds = table.getAllColumns().map((col) => col.id);

      const order =
        currentOrder.length > 0
          ? currentOrder.filter((id) => filteredColumnIds.includes(id))
          : filteredColumnIds;

      const draggedIndex = order.indexOf(draggedColumnId);
      const targetIndex = order.indexOf(targetColumnId);

      if (draggedIndex !== -1 && targetIndex !== -1) {
        const newOrder = [...order];
        newOrder.splice(draggedIndex, 1);
        newOrder.splice(targetIndex, 0, draggedColumnId);
        onColumnOrderChange(newOrder);
      }
    }
    setDraggedColumnId(null);
  };

  const handleDragEnd = () => {
    setDraggedColumnId(null);
  };

  return (
    <Paper
      ref={tableRef}
      sx={{
        height: tableHeight,
        width: '100%',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        boxShadow: 'none'
      }}
      variant="outlined"
    >
      <Box
        ref={scrollContainerRef}
        sx={{
          overflow: 'auto',
          flex: 1,
          '&::-webkit-scrollbar': {
            width: 8,
            height: 8
          },
          '&::-webkit-scrollbar-thumb': {
            backgroundColor: theme.palette.grey[400],
            borderRadius: 4
          },
          '&::-webkit-scrollbar-track': {
            backgroundColor: theme.palette.grey[100]
          }
        }}
      >
        <Table
          stickyHeader
          style={{ width: table.getTotalSize() }}
          sx={{
            tableLayout: 'fixed',
            minWidth: '100%',
            borderCollapse: 'separate',
            '& .MuiTableHead-root': {
              position: 'sticky',
              top: 0,
              zIndex: 2,
              '& .MuiTableCell-head': {
                fontWeight: 'bold',
                textTransform: 'uppercase',
                borderBottom: `1px solid ${theme.palette.divider}`,
                backgroundColor: '#E8EAEE',
                position: 'sticky',
                top: 0,
                zIndex: 3
              }
            },
            '& .MuiTableBody-root': {
              '& .MuiTableRow-root': {
                cursor: notClickable ? 'auto' : 'pointer',
                '&:hover': {
                  backgroundColor: alpha(theme.palette.primary.main, 0.04)
                }
              }
            }
          }}
        >
          <TableHead>
            {table.getHeaderGroups().map((headerGroup) => (
              <TableRow key={headerGroup.id}>
                {headerGroup.headers.map((header) => {
                  const isSortable = header.column.getCanSort();
                  const sortDirection = header.column.getIsSorted();
                  const canResize = header.column.getCanResize();
                  const isResizing = header.column.getIsResizing();
                  const isPinned = isColumnPinned(header.id);
                  const stickyLeft = getPinnedStickyLeft(header.id);

                  return (
                    <TableCell
                      key={header.id}
                      sx={{
                        whiteSpace: 'nowrap',
                        position: isPinned ? 'sticky' : 'relative',
                        left: isPinned ? stickyLeft : undefined,
                        backgroundColor: PINNED_BG,
                        userSelect: isResizing ? 'none' : 'auto',
                        cursor: enableColumnReordering ? 'pointer' : 'default',
                        borderRight: isPinned
                          ? `2px solid ${theme.palette.divider}`
                          : `1px solid #F2F5F9`,
                        boxShadow: isPinned
                          ? `2px 0 4px ${alpha(
                              theme.palette.common.black,
                              0.08
                            )}`
                          : undefined,
                        '&:hover .sort-icon, &:hover .more-vert-icon': {
                          opacity: 1
                        }
                      }}
                      style={{
                        width: header.getSize(),
                        // Use inline style for zIndex so it beats MUI's global .MuiTableCell-head rule
                        zIndex: isPinned ? 5 : 3
                      }}
                      draggable={enableColumnReordering}
                      onDragStart={(e) => handleDragStart(e, header.id)}
                      onDragOver={handleDragOver}
                      onDrop={(e) => handleDrop(e, header.id)}
                      onDragEnd={handleDragEnd}
                      onClick={(e) => {
                        e.stopPropagation();
                        if (isSortable) {
                          header.column.toggleSorting();
                        }
                      }}
                    >
                      <Box
                        sx={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: 0.5
                        }}
                      >
                        <Box
                          sx={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 0.5,
                            flexGrow: 1
                          }}
                        >
                          {flexRender(
                            header.column.columnDef.header,
                            header.getContext()
                          )}
                          {isSortable && (
                            <ArrowDownwardIcon
                              className="sort-icon"
                              sx={{
                                fontSize: 16,
                                opacity: sortDirection ? 1 : 0,
                                transform:
                                  sortDirection === 'asc'
                                    ? 'rotate(180deg)'
                                    : 'rotate(0deg)',
                                transition: 'all 0.2s'
                              }}
                            />
                          )}
                        </Box>
                        <IconButton
                          className="more-vert-icon"
                          size="small"
                          onClick={(e) => {
                            e.stopPropagation();
                            handleMenuClick(e, header.id);
                          }}
                          sx={{
                            padding: 0.5,
                            opacity: 0,
                            transition: 'opacity 0.2s',
                            '&:hover': {
                              backgroundColor: alpha(
                                theme.palette.common.black,
                                0.1
                              )
                            }
                          }}
                        >
                          <MoreVertIcon fontSize="small" />
                        </IconButton>
                      </Box>
                      {canResize && enableColumnResizing && (
                        <Box
                          onMouseDown={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            header.getResizeHandler()(e);
                          }}
                          onTouchStart={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            header.getResizeHandler()(e);
                          }}
                          sx={{
                            position: 'absolute',
                            right: 0,
                            top: 0,
                            bottom: 0,
                            width: 5,
                            cursor: 'col-resize',
                            userSelect: 'none',
                            '&:hover': {
                              backgroundColor: alpha(
                                theme.palette.primary.main,
                                0.3
                              )
                            },
                            ...(isResizing && {
                              backgroundColor: alpha(
                                theme.palette.primary.main,
                                0.5
                              )
                            })
                          }}
                        />
                      )}
                    </TableCell>
                  );
                })}
              </TableRow>
            ))}
          </TableHead>
          <TableBody>
            {loading ? (
              <Box
                style={{
                  marginTop: 130,
                  width: scrollContainerRef.current?.clientWidth ?? 500,
                  maxWidth: scrollContainerRef.current?.clientWidth ?? 500,
                  display: 'flex',
                  justifyContent: 'center'
                }}
              >
                <CircularProgress />
              </Box>
            ) : table.getRowModel().rows.length === 0 ? (
              <Box
                style={{
                  marginTop: 130,
                  width: scrollContainerRef.current?.clientWidth ?? 500,
                  maxWidth: scrollContainerRef.current?.clientWidth ?? 500
                }}
              >
                <NoRowsMessageWrapper
                  message={noRowsMessage}
                  action={noRowsAction}
                />
              </Box>
            ) : (
              table.getRowModel().rows.map((row) => (
                <TableRow
                  key={row.id}
                  onClick={() => onRowClick && onRowClick(row.original)}
                  sx={{
                    '&:last-child td, &:last-child th': { border: 0 }
                  }}
                >
                  {enableRowSelection && (
                    <TableCell padding="checkbox">
                      <Checkbox
                        checked={row.getIsSelected()}
                        onChange={row.getToggleSelectedHandler()}
                      />
                    </TableCell>
                  )}
                  {row.getVisibleCells().map((cell) => {
                    const isPinned = isColumnPinned(cell.column.id);
                    const stickyLeft = getPinnedStickyLeft(cell.column.id);

                    return (
                      <TableCell
                        key={cell.id}
                        sx={{
                          whiteSpace: 'nowrap',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          position: isPinned ? 'sticky' : undefined,
                          left: isPinned ? stickyLeft : undefined,
                          backgroundColor: isPinned ? PINNED_BG : undefined,
                          borderRight: isPinned
                            ? `2px solid ${theme.palette.divider}`
                            : undefined,
                          boxShadow: isPinned
                            ? `2px 0 4px ${alpha(
                                theme.palette.common.black,
                                0.08
                              )}`
                            : undefined
                        }}
                        style={{
                          width: cell.column.getSize(),
                          zIndex: isPinned ? 2 : undefined
                        }}
                      >
                        {flexRender(
                          cell.column.columnDef.cell,
                          cell.getContext()
                        )}
                      </TableCell>
                    );
                  })}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </Box>

      {/* Column header menu */}
      <Menu
        anchorEl={anchorEl}
        open={openMenu}
        onClose={handleMenuClose}
        onClick={(e) => e.stopPropagation()}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        transformOrigin={{ vertical: 'top', horizontal: 'left' }}
      >
        <MenuItem onClick={handleColumnsMenuOpen}>
          <ViewColumnIcon sx={{ mr: 1, fontSize: 20 }} />
          {t('show_columns')}
        </MenuItem>
        <Divider sx={{ my: 0.5 }} />
        <MenuItem onClick={handlePinColumn}>
          <PushPinIcon
            sx={{
              mr: 1,
              fontSize: 20,
              color:
                currentColumnId && isColumnPinned(currentColumnId)
                  ? theme.palette.primary.main
                  : 'inherit'
            }}
          />
          {currentColumnId && isColumnPinned(currentColumnId)
            ? t('unpin')
            : t('pin')}
        </MenuItem>
        <MenuItem onClick={handleHideColumn}>
          <VisibilityOffIcon sx={{ mr: 1, fontSize: 20 }} />
          {t('hide')}
        </MenuItem>
      </Menu>

      {/* Columns visibility menu */}
      <Menu
        anchorEl={columnsMenuAnchor}
        open={openColumnsMenu}
        onClose={handleColumnsMenuClose}
        onClick={(e) => e.stopPropagation()}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
        transformOrigin={{ vertical: 'top', horizontal: 'left' }}
        PaperProps={{
          sx: {
            maxHeight: 400,
            minWidth: 250
          }
        }}
      >
        <Box sx={{ p: 1, display: 'flex', flexDirection: 'column' }}>
          <Typography variant="subtitle2" sx={{ px: 1, py: 0.5, mb: 1 }}>
            {t('toggle_columns')}
          </Typography>
          <Divider sx={{ mb: 1 }} />
          {table.getAllColumns().map((column) => {
            const headerLabel =
              typeof column.columnDef.header === 'function'
                ? column.columnDef.header({} as any)
                : String(column.columnDef.header);

            return (
              <Box
                key={column.id}
                sx={{
                  display: 'flex',
                  flexDirection: 'row',
                  alignItems: 'center',
                  px: 1,
                  py: 0.5,
                  gap: 1
                }}
              >
                <Switch
                  size="small"
                  checked={column.getIsVisible()}
                  onChange={() => handleToggleColumnVisibility(column.id)}
                  onClick={(e) => e.stopPropagation()}
                />
                <Typography
                  sx={{
                    fontSize: '0.75rem',
                    color: 'text.secondary',
                    mb: 0.5
                  }}
                >
                  {headerLabel}
                </Typography>
              </Box>
            );
          })}
        </Box>
      </Menu>

      <TablePagination
        component="div"
        count={totalRows}
        page={pagination.pageIndex}
        onPageChange={(_, newPage) =>
          onPaginationChange({ ...pagination, pageIndex: newPage })
        }
        rowsPerPage={pagination.pageSize}
        onRowsPerPageChange={(event) =>
          onPaginationChange({
            ...pagination,
            pageIndex: 0,
            pageSize: Number(event.target.value)
          })
        }
        rowsPerPageOptions={pageSizeOptions}
        // labelRowsPerPage={t('rows_per_page')}
        ActionsComponent={TablePaginationActions}
        sx={{
          borderTop: `1px solid ${theme.palette.divider}`,
          '& .MuiTablePagination-toolbar': {
            minHeight: '52px'
          }
        }}
      />
    </Paper>
  );
}

export default CustomDatagrid2;
