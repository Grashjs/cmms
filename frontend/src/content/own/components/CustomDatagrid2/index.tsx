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
  RowData
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
  TableSortLabel,
  Checkbox,
  IconButton,
  Paper,
  TablePagination,
  CircularProgress,
  alpha
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
  // Filtering props (optional)
  columnFilters?: ColumnFiltersState;
  onColumnFiltersChange?: OnChangeFn<ColumnFiltersState>;
  // Selection props (optional)
  enableRowSelection?: boolean;
  rowSelection?: Record<string, boolean>;
  onRowSelectionChange?: OnChangeFn<Record<string, boolean>>;
  // Custom render for no rows
  noRowsMessage?: string;
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
  columnFilters,
  onColumnFiltersChange,
  enableRowSelection,
  rowSelection,
  onRowSelectionChange,
  noRowsMessage
}: CustomDatagrid2Props<TData>) {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const { height } = useWindowDimensions();
  const tableRef = useRef<HTMLDivElement>(null);
  const [tableHeight, setTableHeight] = useState<number>(500);
  const { user } = useAuth();

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
      rowSelection
    },
    onPaginationChange: onPaginationChange,
    onSortingChange: onSortingChange,
    onColumnFiltersChange: onColumnFiltersChange,
    onRowSelectionChange: onRowSelectionChange,
    getCoreRowModel: getCoreRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    manualPagination: true,
    manualSorting: true,
    manualFiltering: true,
    rowCount: totalRows
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
            '& .MuiTableHead-root': {
              '& .MuiTableCell-head': {
                fontWeight: 'bold',
                textTransform: 'uppercase',
                borderBottom: `1px solid ${theme.palette.divider}`,
                backgroundColor: theme.colors.alpha.black[10]
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

                  return (
                    <TableCell
                      key={header.id}
                      sx={{
                        whiteSpace: 'nowrap'
                      }}
                      style={{
                        width: header.column.getSize() ?? undefined
                      }}
                    >
                      {isSortable ? (
                        <TableSortLabel
                          active={!!sortDirection}
                          direction={sortDirection === 'desc' ? 'desc' : 'asc'}
                          IconComponent={ArrowDownwardIcon}
                          onSortClick={() => {
                            const nextSort =
                              sortDirection === false
                                ? 'asc'
                                : sortDirection === 'asc'
                                ? 'desc'
                                : false;
                            header.column.toggleSorting(nextSort === 'desc');
                          }}
                        >
                          {flexRender(
                            header.column.columnDef.header,
                            header.getContext()
                          )}
                        </TableSortLabel>
                      ) : (
                        flexRender(
                          header.column.columnDef.header,
                          header.getContext()
                        )
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
                        whiteSpace: 'nowrap'
                      }}
                      style={{
                        maxWidth:
                          cell.column.getSize() !== 'auto'
                            ? cell.column.getSize()
                            : undefined
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
