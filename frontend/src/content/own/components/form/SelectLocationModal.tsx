import React, { useEffect, useMemo, useRef, useState } from 'react';
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Typography,
  useTheme
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useDispatch, useSelector } from '../../../../store';
import {
  getLocationsMini,
  resetLocationsHierarchy
} from '../../../../slices/location';
import { LocationMiniDTO } from '../../../../models/owns/location';
import ReplayTwoToneIcon from '@mui/icons-material/ReplayTwoTone';
import NoRowsMessageWrapper from '../NoRowsMessageWrapper';
import { usePrevious } from '../../../../hooks/usePrevious';
import { createColumnHelper } from '@tanstack/react-table';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import CustomDatagrid2, { CustomDatagridColumn2 } from '../CustomDatagrid2';

interface SelectLocationModalProps {
  open: boolean;
  onClose: () => void;
  onSelect: (locations: LocationMiniDTO[]) => void;
  excludedLocationIds?: number[];
  maxSelections?: number;
  initialSelectedLocations?: LocationMiniDTO[];
}

type LocationRow = LocationMiniDTO & { depth?: number };

const SelectLocationModal: React.FC<SelectLocationModalProps> = ({
  open,
  onClose,
  onSelect,
  excludedLocationIds = [],
  maxSelections,
  initialSelectedLocations = []
}) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const theme = useTheme();
  const { loadingGet, locationsMini } = useSelector((state) => state.locations);
  const initialized = useRef<boolean>(false);
  const single = maxSelections === 1;

  // State for tracking expanded rows
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});

  // State for tracking selected locations
  const [selectedLocations, setSelectedLocations] = useState<LocationMiniDTO[]>(
    initialSelectedLocations
  );
  const [rowSelection, setRowSelection] = useState<Record<string, boolean>>(
    initialSelectedLocations.reduce((acc, loc) => {
      acc[loc.id] = true;
      return acc;
    }, {} as Record<string, boolean>)
  );
  const previousInitialSelectedLocations = usePrevious(
    initialSelectedLocations
  );

  const handleReset = (callApi: boolean) => {
    if (callApi) {
      dispatch(getLocationsMini());
    }
  };

  // Flatten hierarchy based on expanded state
  const getHierarchicalData = (
    flatList: LocationMiniDTO[],
    expanded: Record<string, boolean>,
    parentId: number | null = null,
    depth: number = 0
  ): (LocationMiniDTO & { depth: number })[] => {
    let result: (LocationMiniDTO & { depth: number })[] = [];

    const nodes = flatList.filter((item) => {
      if (parentId === null) {
        return !item.parentId;
      }
      return item.parentId === parentId;
    });

    for (const node of nodes) {
      result.push({ ...node, depth });

      if (expanded[node.id]) {
        const children = getHierarchicalData(
          flatList,
          expanded,
          node.id,
          depth + 1
        );
        result = [...result, ...children];
      }
    }

    return result;
  };

  const tableData = useMemo(
    () => getHierarchicalData(locationsMini, expanded),
    [locationsMini, expanded]
  );

  const filteredTableData = tableData.filter(
    (location) => !excludedLocationIds.includes(location.id)
  );

  useEffect(() => {
    if (
      open &&
      (!initialized.current ||
        JSON.stringify(previousInitialSelectedLocations) !==
          JSON.stringify(initialSelectedLocations))
    ) {
      initialized.current = true;
      handleReset(true);
      if (initialSelectedLocations?.length) {
        setSelectedLocations(initialSelectedLocations);
        setRowSelection(
          initialSelectedLocations.reduce((acc, loc) => {
            acc[loc.id] = true;
            return acc;
          }, {} as Record<string, boolean>)
        );
      } else {
        setSelectedLocations([]);
        setRowSelection({});
      }
    }
  }, [open, initialSelectedLocations, previousInitialSelectedLocations]);

  useEffect(() => {
    if (single && open) {
      setSelectedLocations([]);
      setRowSelection({});
    }
  }, [open, single]);

  const handleToggleExpand = (row: LocationRow) => {
    setExpanded((prev) => ({ ...prev, [row.id]: !prev[row.id] }));
  };

  const columnHelper = createColumnHelper<LocationMiniDTO>();

  const columns: CustomDatagridColumn2<LocationMiniDTO>[] = [
    columnHelper.display({
      id: 'expander',
      header: '',
      cell: ({ row }) => {
        const isExpanded = expanded[row.original.id];
        const hasChildren = locationsMini.some(
          (loc) => loc.parentId === row.original.id
        );

        if (!hasChildren) {
          return <Box sx={{ width: 24 }} />;
        }

        return (
          <IconButton
            size="small"
            onClick={(e) => {
              e.stopPropagation();
              handleToggleExpand(row.original);
            }}
            sx={{ padding: 0.5 }}
          >
            {isExpanded ? (
              <ExpandMoreIcon fontSize="small" />
            ) : (
              <ChevronRightIcon fontSize="small" />
            )}
          </IconButton>
        );
      },
      size: 50
    }),
    columnHelper.accessor('customId', {
      id: 'customId',
      header: () => t('id'),
      cell: (info) => info.getValue(),
      size: 100
    }),
    columnHelper.accessor('name', {
      id: 'name',
      header: () => t('name'),
      cell: (info) => (
        <Box
          sx={{
            py: 1,
            fontWeight: 'bold',
            ml: (info.row.depth || 0) * 24
          }}
        >
          {info.getValue()}
        </Box>
      ),
      size: Number.MAX_SAFE_INTEGER
    })
  ];

  const handleRowClick = (row: LocationMiniDTO) => {
    // Prevent selection of excluded locations
    if (excludedLocationIds.includes(row.id)) return;

    if (single) {
      // Single selection mode
      const newSelection = [row];
      setSelectedLocations(newSelection);
      onSelect(newSelection);
      onClose();
    } else {
      // Multiple selection mode
      const isSelected = rowSelection[row.id];
      let newRowSelection: Record<string, boolean>;
      let updatedSelectedLocations: LocationMiniDTO[];

      if (isSelected) {
        // Remove from selection
        newRowSelection = { ...rowSelection };
        delete newRowSelection[row.id];
        updatedSelectedLocations = selectedLocations.filter(
          (loc) => loc.id !== row.id
        );
      } else {
        // Add to selection
        if (maxSelections && selectedLocations.length >= maxSelections) {
          return;
        }
        newRowSelection = { ...rowSelection, [row.id]: true };
        updatedSelectedLocations = [...selectedLocations, row];
      }

      setRowSelection(newRowSelection);
      setSelectedLocations(updatedSelectedLocations);
    }
  };

  const handleConfirmSelection = () => {
    onSelect(selectedLocations);
    onClose();
  };

  const handleRemoveSelection = (locationId: number) => {
    const newRowSelection = { ...rowSelection };
    delete newRowSelection[locationId];
    setRowSelection(newRowSelection);

    const updatedSelectedLocations = selectedLocations.filter(
      (location) => location.id !== locationId
    );
    setSelectedLocations(updatedSelectedLocations);
  };

  return (
    <Dialog fullWidth maxWidth="md" open={open} onClose={onClose}>
      <DialogTitle
        sx={{
          p: 2,
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}
      >
        <Typography variant="h4">{t('select_location')}</Typography>
        <IconButton
          onClick={() => handleReset(true)}
          color="primary"
          size="small"
        >
          <ReplayTwoToneIcon />
        </IconButton>
      </DialogTitle>

      {selectedLocations.length > 0 && (
        <Box sx={{ px: 2, py: 1, display: 'flex', flexWrap: 'wrap', gap: 1 }}>
          {selectedLocations.map((location) => (
            <Chip
              key={location.id}
              label={`${location.customId}: ${location.name}`}
              onDelete={() => handleRemoveSelection(location.id)}
              color="primary"
              variant="outlined"
            />
          ))}
        </Box>
      )}

      <DialogContent dividers sx={{ p: 1, height: '60vh' }}>
        <Box sx={{ height: '100%', width: '100%' }}>
          <CustomDatagrid2
            columns={columns}
            data={filteredTableData}
            loading={loadingGet}
            enableRowSelection={!single}
            rowSelection={rowSelection}
            onRowSelectionChange={setRowSelection}
            onRowClick={handleRowClick}
            getRowId={(row) => row.id.toString()}
            noRowsMessage={t('noRows.location.message')}
            noRowsAction={t('noRows.location.action')}
            pagination={{ pageIndex: 0, pageSize: 100 }}
            onPaginationChange={() => {}}
            totalRows={filteredTableData.length}
            hidePagination
          />
        </Box>
      </DialogContent>
      {!single && (
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={onClose} color="secondary">
            {t('cancel')}
          </Button>
          <Button
            onClick={handleConfirmSelection}
            color="primary"
            variant="contained"
            disabled={selectedLocations.length === 0}
          >
            {t('select')} ({selectedLocations.length})
          </Button>
        </DialogActions>
      )}
    </Dialog>
  );
};

export default SelectLocationModal;
