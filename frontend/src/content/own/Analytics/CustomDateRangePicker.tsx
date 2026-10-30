import { useState, useEffect, useRef, useContext } from 'react';
import {
  Box,
  Button,
  Card,
  Checkbox,
  CircularProgress,
  FormControlLabel,
  Menu,
  MenuItem,
  Stack,
  TextField,
  Tooltip,
  Popover,
  useTheme
} from '@mui/material';
import { DateRange } from 'react-date-range';
import 'react-date-range/dist/styles.css';
import 'react-date-range/dist/theme/default.css';
import { useTranslation } from 'react-i18next';
import { format } from 'date-fns';
import en from 'date-fns/locale/en-US';
import useAuth from '../../../hooks/useAuth';
import BusinessTwoToneIcon from '@mui/icons-material/BusinessTwoTone';
import PictureAsPdfTwoToneIcon from '@mui/icons-material/PictureAsPdfTwoTone';
import { FilterField } from '../../../models/owns/page';
import AnalyticsFilters from './AnalyticsFilters';
import { downloadMaintenanceSummary } from '../../../slices/analytics/summary';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';

interface OwnProps {
  start: Date;
  end: Date;
  setStart: (date: Date) => void;
  setEnd: (date: Date) => void;
  companyId?: number;
  onCompanyChange?: (companyId: number | undefined) => void;
  // shows the location / asset category / team / category / priority filters
  filterFields?: FilterField[];
  onFilterFieldsChange?: (filterFields: FilterField[]) => void;
}

export default function ({
  start,
  end,
  setEnd,
  setStart,
  companyId,
  onCompanyChange,
  filterFields,
  onFilterFieldsChange
}: OwnProps) {
  const { t }: { t: any } = useTranslation();
  const { user } = useAuth();
  const anchorRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const openMenu = Boolean(anchorEl);
  const theme = useTheme();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [exporting, setExporting] = useState(false);

  const handleExport = () => {
    setExporting(true);
    downloadMaintenanceSummary(start, end, companyId, filterFields ?? [])
      .catch(() => showSnackBar(t('maintenance_summary_error'), 'error'))
      .finally(() => setExporting(false));
  };

  const [state, setState] = useState([
    {
      startDate: start,
      endDate: end,
      key: 'selection'
    }
  ]);

  useEffect(() => {
    setState([
      {
        startDate: start,
        endDate: end,
        key: 'selection'
      }
    ]);
  }, [start, end]);

  const handleSelect = (ranges: any) => {
    const selection = ranges.selection;
    setStart(selection.startDate);
    setEnd(selection.endDate);
    setState([selection]);
  };

  const handleOpen = () => {
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
  };

  const handleOpenMenu = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };
  const handleCloseMenu = () => {
    setAnchorEl(null);
  };

  const handleCompanyChange = (id: number) => {
    if (companyId === id) {
      onCompanyChange?.(undefined);
    } else {
      onCompanyChange?.(id);
    }
    handleCloseMenu();
  };

  const isSuperUser = user?.superAccountRelations?.length > 0;
  const selectedName = companyId
    ? user?.superAccountRelations?.find(
        (rel) => rel.childCompanyId === companyId
      )?.childCompanyName
    : null;

  const displayStartDate = start ? format(start, 'MMM dd, yyyy') : '';
  const displayEndDate = end ? format(end, 'MMM dd, yyyy') : '';

  return (
    <>
      <Card sx={{ display: 'flex', p: 2, justifyContent: 'center' }}>
        {/* wraps on narrow screens: date, filters, PDF and company buttons */}
        <Stack
          direction="row"
          alignItems="center"
          justifyContent="center"
          flexWrap="wrap"
          sx={{ gap: 1 }}
        >
          <TextField
            sx={{ width: 250, maxWidth: '100%' }}
            value={`${displayStartDate} ${t('to')} ${displayEndDate}`}
            onClick={handleOpen}
            InputProps={{
              readOnly: true
            }}
            ref={anchorRef}
          />
          {onFilterFieldsChange && (
            <AnalyticsFilters
              filterFields={filterFields ?? []}
              onChange={onFilterFieldsChange}
            />
          )}
          <Tooltip title={t('maintenance_summary_description')}>
            <span>
              <Button
                onClick={handleExport}
                disabled={exporting || !start || !end}
                variant="outlined"
                size="large"
                startIcon={
                  exporting ? (
                    <CircularProgress size={18} />
                  ) : (
                    <PictureAsPdfTwoToneIcon />
                  )
                }
              >
                {t('maintenance_summary')}
              </Button>
            </span>
          </Tooltip>
          {isSuperUser && (
            <>
              <Button
                onClick={handleOpenMenu}
                sx={{
                  '& .MuiButton-startIcon': { margin: '0px' },
                  minWidth: 0
                }}
                variant={selectedName ? 'contained' : 'outlined'}
                startIcon={<BusinessTwoToneIcon />}
                size={'large'}
              >
                {selectedName ?? t('all_companies')}
              </Button>
              <Menu
                anchorEl={anchorEl}
                open={openMenu}
                onClose={handleCloseMenu}
              >
                {user.superAccountRelations.map((relation) => {
                  const isChecked = companyId === relation.childCompanyId;
                  return (
                    <MenuItem
                      key={relation.childCompanyId}
                      onClick={() =>
                        handleCompanyChange(relation.childCompanyId)
                      }
                    >
                      <FormControlLabel
                        control={<Checkbox checked={isChecked} />}
                        label={relation.childCompanyName}
                        onClick={(e) => e.preventDefault()}
                      />
                    </MenuItem>
                  );
                })}
              </Menu>
            </>
          )}
        </Stack>
      </Card>
      <Popover
        open={open}
        anchorEl={anchorRef.current}
        onClose={handleClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'left'
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'left'
        }}
      >
        <DateRange
          ranges={state}
          onChange={handleSelect}
          months={2}
          locale={en}
          direction="horizontal"
          rangeColors={[theme.palette.primary.main]}
        />
      </Popover>
    </>
  );
}
