import {
  ChangeEvent,
  FC,
  ReactNode,
  SyntheticEvent,
  useRef,
  useState
} from 'react';
import PropTypes from 'prop-types';
import {
  Box,
  Checkbox,
  Divider,
  IconButton,
  InputAdornment,
  Stack,
  styled,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  Tabs,
  TextField,
  Tooltip,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import BulkActions, { BulkAction } from '../Settings/Roles/BulkActions';
import SearchTwoToneIcon from '@mui/icons-material/SearchTwoTone';
import { useSnackbar } from 'notistack';
import { TableCustomizedColumnType, TableCustomizedDataType } from '../type';
import { OverridableStringUnion } from '@mui/types';
import { IconButtonPropsColorOverrides } from '@mui/material/IconButton/IconButton';
import { matchesSearch } from '../../../utils/search';

interface TableCustomizedProps {
  data: TableCustomizedDataType[];
  columns: TableCustomizedColumnType[];
  itemLabelBg?: {
    column: string;
    map: { [propName: string]: { [propName: string]: string } };
  }[];
  limitRows?: number;
  enablePagination?: boolean;
  searchFilterProperties?: string[];
  enableTabsFilter?: boolean;
  tabsFilter?: {
    accessor: string;
    tabs: {
      value: string;
      label: any;
    }[];
  };
  actions?: {
    name: string;
    color: OverridableStringUnion<
      | 'inherit'
      | 'default'
      | 'primary'
      | 'secondary'
      | 'error'
      | 'info'
      | 'success'
      | 'warning',
      IconButtonPropsColorOverrides
    >;
    callback: (id: number) => void;
    icon: ReactNode;
  }[];
  bulkActions?: BulkAction[];
}

interface Filters {
  type?: any;
  // [propName: string]: any
}

const TabsWrapper = styled(Tabs)(
  ({ theme }) => `
      @media (max-width: ${theme.breakpoints.values.md}px) {
        .MuiTabs-scrollableX {
          overflow-x: auto !important;
        }
  
        .MuiTabs-indicator {
            box-shadow: none;
        }
      }
      `
);

const applyFilters = (
  data: TableCustomizedDataType[],
  query: string,
  filters: Filters,
  properties: string[]
): TableCustomizedDataType[] => {
  return data.filter((row) => {
    let matches = true;

    if (query) {
      let containsQuery = false;

      properties.forEach((property) => {
        if (matchesSearch(row[property], query)) {
          containsQuery = true;
        }
      });

      if (filters.type && row.type !== filters.type) {
        matches = false;
      }

      if (!containsQuery) {
        matches = false;
      }
    }

    Object.keys(filters).forEach((key) => {
      const value = filters[key];

      if (value && row[key] !== value) {
        matches = false;
      }
    });

    return matches;
  });
};

const applyPagination = (
  data: TableCustomizedDataType[],
  page: number,
  limit: number
): TableCustomizedDataType[] => {
  return data.slice(page * limit, page * limit + limit);
};

const TableCustomized: FC<TableCustomizedProps> = ({
  data,
  columns,
  tabsFilter,
  searchFilterProperties,
  limitRows = 5,
  actions,
  bulkActions
}) => {
  const [selectedItems, setSelectedItems] = useState<(string | number)[]>([]);
  const { t }: { t: any } = useTranslation();
  const { enqueueSnackbar } = useSnackbar();
  // const location = useLocation();

  const [page, setPage] = useState<number>(0);
  const [limit, setLimit] = useState<number>(limitRows);
  const rowsPerPage = useRef(
    [...Array(Math.floor(data.length / limit) + 1)].map(
      (_, i) => (i + 1) * limit
    )
  );
  const [query, setQuery] = useState<string>('');
  const [filters, setFilters] = useState<Filters>({
    type: null
  });

  const handleTabsChange = (_event: SyntheticEvent, tabsValue: unknown) => {
    let value = null;

    if (tabsValue !== 'all') {
      value = tabsValue;
    }

    setFilters((prevFilters) => ({
      ...prevFilters,
      [tabsFilter.accessor]: value
    }));

    setSelectedItems([]);
  };

  const handleQueryChange = (event: ChangeEvent<HTMLInputElement>): void => {
    event.persist();
    setQuery(event.target.value);
  };

  const handleSelectAllRows = (event: ChangeEvent<HTMLInputElement>): void => {
    setSelectedItems(event.target.checked ? data.map((row) => row.id) : []);
  };

  const handleSelectOneRow = (
    _event: ChangeEvent<HTMLInputElement>,
    rowId: string | number
  ): void => {
    if (!selectedItems.includes(rowId)) {
      setSelectedItems((prevSelected) => [...prevSelected, rowId]);
    } else {
      setSelectedItems((prevSelected) =>
        prevSelected.filter((id) => id !== rowId)
      );
    }
  };

  const handlePageChange = (_event: any, newPage: number): void => {
    setPage(newPage);
  };

  const handleLimitChange = (event: ChangeEvent<HTMLInputElement>): void => {
    setLimit(parseInt(event.target.value));
  };

  const filteredRows = applyFilters(
    data,
    query,
    filters,
    searchFilterProperties
  );
  const paginatedRows = applyPagination(filteredRows, page, limit);
  const selectedBulkActions = selectedItems.length > 0;
  const selectedSomeRows =
    selectedItems.length > 0 && selectedItems.length < data.length;
  const selectedAllRows = selectedItems.length === data.length;

  return (
    <Box width="100%">
      {tabsFilter && (
        <Box
          display="flex"
          alignItems="center"
          flexDirection={{ xs: 'column', sm: 'row' }}
          justifyContent={{ xs: 'center', sm: 'space-between' }}
          pb={3}
        >
          <TabsWrapper
            onChange={handleTabsChange}
            scrollButtons="auto"
            textColor="secondary"
            value={filters.type || 'all'}
            variant="scrollable"
          >
            {tabsFilter.tabs.map((tab) => (
              <Tab key={tab.value} value={tab.value} label={tab.label} />
            ))}
          </TabsWrapper>
        </Box>
      )}
      <Box p={2}>
        {!selectedBulkActions && searchFilterProperties && (
          <TextField
            sx={{
              m: 0
            }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchTwoToneIcon />
                </InputAdornment>
              )
            }}
            onChange={handleQueryChange}
            placeholder={t(`Search by ${searchFilterProperties}...`)}
            value={query}
            size="small"
            fullWidth
            margin="normal"
            variant="outlined"
          />
        )}
        {selectedBulkActions && bulkActions && (
          <BulkActions actions={bulkActions} />
        )}
      </Box>

      <Divider />

      {paginatedRows.length === 0 ? (
        <>
          <Typography
            sx={{
              py: 10
            }}
            variant="h3"
            fontWeight="normal"
            color="text.secondary"
            align="center"
          >
            {t("We couldn't find any data matching your search criteria")}
          </Typography>
        </>
      ) : (
        <Stack direction="column">
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  {bulkActions && (
                    <TableCell padding="checkbox">
                      <Checkbox
                        checked={selectedAllRows}
                        indeterminate={selectedSomeRows}
                        onChange={handleSelectAllRows}
                      />
                    </TableCell>
                  )}

                  {columns.map((col) => (
                    <TableCell key={col.accessor}>{t(col.label)}</TableCell>
                  ))}

                  {actions && (
                    <TableCell align="center">{t('actions')}</TableCell>
                  )}
                </TableRow>
              </TableHead>
              <TableBody>
                {paginatedRows.map((row) => {
                  const isRowSelected = selectedItems.includes(row.id);
                  const rowValues = columns.map((col) => row[col.accessor]);

                  return (
                    <TableRow hover key={row.id} selected={isRowSelected}>
                      {bulkActions && (
                        <TableCell padding="checkbox">
                          <Checkbox
                            checked={isRowSelected}
                            onChange={(event) =>
                              handleSelectOneRow(event, row.id)
                            }
                            value={isRowSelected}
                          />
                        </TableCell>
                      )}

                      {rowValues.map((value, i) => (
                        <TableCell key={`${value}_${i}`}>
                          <Typography variant="h6">{value}</Typography>
                        </TableCell>
                      ))}

                      {actions && (
                        <TableCell align="center">
                          {actions.map((action) => (
                            <Tooltip
                              key={action.name}
                              title={action.name}
                              arrow
                            >
                              <IconButton
                                onClick={() => action.callback(Number(row.id))}
                                color={action.color}
                              >
                                {action.icon}
                              </IconButton>
                            </Tooltip>
                          ))}
                        </TableCell>
                      )}
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
          <Box p={2}>
            <TablePagination
              component="div"
              count={filteredRows.length}
              onPageChange={handlePageChange}
              onRowsPerPageChange={handleLimitChange}
              page={page}
              rowsPerPage={limit}
              rowsPerPageOptions={rowsPerPage.current}
            />
          </Box>
        </Stack>
      )}
    </Box>
  );
};

TableCustomized.propTypes = {
  data: PropTypes.array.isRequired
};

TableCustomized.defaultProps = {
  data: []
};

export default TableCustomized;
