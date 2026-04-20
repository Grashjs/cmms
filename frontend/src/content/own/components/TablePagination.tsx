import { Box, MenuItem, Pagination, Select, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';

interface TablePaginationProps {
  totalItems: number;
  pageSize: number;
  pageNum: number;
  pageSizeOptions?: number[];
  onPageSizeChange: (pageSize: number) => void;
  onPageChange: (page: number) => void;
}

const TablePagination = ({
  totalItems,
  pageSize,
  pageNum,
  pageSizeOptions = [10, 20, 50],
  onPageSizeChange,
  onPageChange
}: TablePaginationProps) => {
  const { t } = useTranslation();

  return (
    <Stack
      direction="row"
      justifyContent="space-between"
      alignItems="center"
      sx={{ mt: 2, mb: 2 }}
    >
      <Typography variant="body2" color="text.secondary">
        {totalItems} {t('total_items')}
      </Typography>
      <Stack direction="row" spacing={2} alignItems="center">
        <Typography variant="body2">{t('items_per_page')}:</Typography>
        <Select
          value={pageSize}
          onChange={(e) => onPageSizeChange(Number(e.target.value))}
        >
          {pageSizeOptions.map((size) => (
            <MenuItem key={size} value={size}>
              {size}
            </MenuItem>
          ))}
        </Select>
        <Pagination
          count={Math.ceil(totalItems / pageSize)}
          page={pageNum + 1}
          onChange={(_event, page) => onPageChange(page - 1)}
          color="primary"
        />
      </Stack>
    </Stack>
  );
};

export default TablePagination;
