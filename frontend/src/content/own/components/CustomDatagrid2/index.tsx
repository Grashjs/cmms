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
  DragEvent
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useEffect, useRef, useState, useMemo } from 'react';
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
  pagination: PaginationState;
  onPaginationChange: (pagination: PaginationState) => void;
  totalRows: number;
  pageSizeOptions?: number[];
  // Sorting props
  sorting: SortingState;
  onSortingChange: OnChangeFn<SortingState>;
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
  // Enable column reordering
  enableColumnReordering?: boolean;
  // Enable column resizing
  enableColumnResizing?: boolean;
}

function CustomDatagrid2<TData extends RowData>({
  columns,
  data,
  notClickable,
  onRowClick,
  loading,
  pagination,
  onPaginationChange,
  totalRows,
  pageSizeOptions = [10, 25, 50, 100],
  sorting,
  onSortingChange,
  columnOrder,
  onColumnOrderChange,
  columnSizing,
  onColumnSizingChange,
  columnVisibility,
  onColumnVisibilityChange,
  columnFilters,
  onColumnFiltersChange,
  enableRowSelection,
  rowSelection,
  onRowSelectionChange,
  noRowsMessage,
  enableColumnReordering = true,
  enableColumnResizing = true
}: CustomDatagrid2Props<TData>) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const { height } = useWindowDimensions();
  const tableRef = useRef<HTMLDivElement>(null);
  const [tableHeight, setTableHeight] = useState<number>(500);
  const { user } = useAuth();

  // Drag state for column reordering
  const [draggedColumnId, setDraggedColumnId] = useState<string | null>(null);

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
  const handleDragStart = (e: DragEvent<HTMLDivElement>, columnId: string) => {
    // Prevent horizontal scroll during drag
    e.stopPropagation();
    setDraggedColumnId(columnId);
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.dropEffect = 'move';
  };

  // Handle column drag over
  const handleDragOver = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
  };

  // Handle column drop
  const handleDrop = (e: DragEvent<HTMLDivElement>, targetColumnId: string) => {
    e.preventDefault();
    e.stopPropagation();
    if (
      draggedColumnId &&
      draggedColumnId !== targetColumnId &&
      onColumnOrderChange
    ) {
      const currentOrder = columnOrder || table.getState().columnOrder || [];
      const filteredColumnIds = table.getAllColumns().map((col) => col.id);

      // Get current order or create default order
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

  // Prevent drag when clicking on sort or resize
  const handleDragIconMouseDown = (e: React.MouseEvent) => {
    e.stopPropagation();
  };

  // Lock scroll during drag
  useEffect(() => {
    if (draggedColumnId) {
      const scrollContainer = tableRef.current?.querySelector(
        '[style*="overflow: auto"]'
      );
      if (scrollContainer) {
        scrollContainer.style.overflow = 'hidden';
      }
      return () => {
        if (scrollContainer) {
          scrollContainer.style.overflow = 'auto';
        }
      };
    }
  }, [draggedColumnId]);

  return (
    <Paper
      ref={tableRef}
      sx={{
        height: tableHeight,
        width: '100%',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden'
      }}
      variant="outlined"
    >
      <Box
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
          sx={{
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
                zIndex: 2
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

                  return (
                    <TableCell
                      key={header.id}
                      sx={{
                        whiteSpace: 'nowrap',
                        position: 'relative',
                        userSelect: isResizing ? 'none' : 'auto',
                        cursor: enableColumnReordering ? 'grab' : 'default'
                      }}
                      style={{
                        width: header.getSize()
                      }}
                      draggable={enableColumnReordering}
                      onDragStart={(e) => handleDragStart(e, header.id)}
                      onDragOver={handleDragOver}
                      onDrop={(e) => handleDrop(e, header.id)}
                      onDragEnd={handleDragEnd}
                      onClick={(e) => e.stopPropagation()}
                    >
                      <Box
                        sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}
                      >
                        {isSortable ? (
                          <Box
                            onClick={(e) => {
                              e.stopPropagation();
                              header.column.toggleSorting();
                            }}
                            sx={{
                              display: 'flex',
                              alignItems: 'center',
                              gap: 0.5,
                              cursor: 'pointer',
                              '&:hover': {
                                opacity: 0.8
                              }
                            }}
                          >
                            {flexRender(
                              header.column.columnDef.header,
                              header.getContext()
                            )}
                            <ArrowDownwardIcon
                              sx={{
                                fontSize: 16,
                                opacity: sortDirection ? 1 : 0,
                                transform:
                                  sortDirection === 'desc'
                                    ? 'rotate(180deg)'
                                    : 'rotate(0deg)',
                                transition: 'all 0.2s',
                                '&:hover': {
                                  opacity: 1
                                }
                              }}
                            />
                          </Box>
                        ) : (
                          flexRender(
                            header.column.columnDef.header,
                            header.getContext()
                          )
                        )}
                      </Box>
                      {canResize && enableColumnResizing && (
                        <Box
                          onMouseDown={header.getResizeHandler()}
                          onTouchStart={header.getResizeHandler()}
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
              <TableRow>
                <TableCell
                  colSpan={
                    enableRowSelection
                      ? filteredColumns.length + 1
                      : filteredColumns.length
                  }
                  align="center"
                  sx={{ py: 8 }}
                >
                  <CircularProgress />
                </TableCell>
              </TableRow>
            ) : table.getRowModel().rows.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={
                    enableRowSelection
                      ? filteredColumns.length + 1
                      : filteredColumns.length
                  }
                  align="center"
                  sx={{ py: 8 }}
                >
                  <Typography variant="h3">
                    {noRowsMessage || t('no_content')}
                  </Typography>
                </TableCell>
              </TableRow>
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
                  {row.getVisibleCells().map((cell) => (
                    <TableCell
                      key={cell.id}
                      sx={{
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis'
                      }}
                      style={{
                        maxWidth: cell.column.getSize()
                      }}
                    >
                      {flexRender(
                        cell.column.columnDef.cell,
                        cell.getContext()
                      )}
                    </TableCell>
                  ))}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </Box>
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
        labelRowsPerPage={t('rows_per_page')}
        labelDisplayedRows={({ from, to, count }) =>
          `${from}-${to} ${t('of')} ${
            count !== -1 ? count : `${t('more_than')} ${to}`
          }`
        }
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
