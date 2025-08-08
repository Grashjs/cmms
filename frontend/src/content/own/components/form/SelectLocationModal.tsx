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
import CustomDataGrid, { CustomDatagridColumn } from '../CustomDatagrid';
import {
  GridEventListener,
  GridRenderCellParams,
  GridRow,
  GridSelectionModel
} from '@mui/x-data-grid';
import { DataGridProProps, useGridApiRef } from '@mui/x-data-grid-pro';
import { LocationMiniDTO } from '../../../../models/owns/location';
import { GroupingCellWithLazyLoading } from '../../Assets/GroupingCellWithLazyLoading';
import ReplayTwoToneIcon from '@mui/icons-material/ReplayTwoTone';
import { Pageable } from '../../../../models/owns/page';
import NoRowsMessageWrapper from '../NoRowsMessageWrapper';
import { usePrevious } from '../../../../hooks/usePrevious';

interface SelectLocationModalProps {
  open: boolean;
  onClose: () => void;
  onSelect: (locations: LocationMiniDTO[]) => void; // Changed to handle array of locations
  excludedLocationIds?: number[]; // Changed to array for multiple exclusions
  maxSelections?: number; // Optional limit for selections
  initialSelectedLocations?: LocationMiniDTO[]; // Optional pre-selected locations
}

const getLocationRows = (locations: LocationMiniDTO[]): IRow[] => {
  // Build a map of parent to children
  const locationsByParent: { [key: number]: number[] } = {};
  const locationMap: { [key: number]: LocationMiniDTO } = {};

  // Create location map for quick lookup
  locations.forEach((location) => {
    locationMap[location.id] = location;
  });

  // Build parent-children relationships
  locations.forEach((location) => {
    if (location.parentId) {
      if (!locationsByParent[location.parentId]) {
        locationsByParent[location.parentId] = [];
      }
      locationsByParent[location.parentId].push(location.id);
    }
  });

  // Helper function to build hierarchy path
  const buildHierarchy = (locationId: number): number[] => {
    const hierarchy: number[] = [];
    let currentLocation = locationMap[locationId];
    hierarchy.unshift(currentLocation.id);

    while (
      currentLocation.parentId &&
      !hierarchy.includes(currentLocation.parentId)
    ) {
      hierarchy.unshift(currentLocation.parentId);
      currentLocation = locationMap[currentLocation.parentId];
    }

    return hierarchy;
  };

  return locations.map((location) => {
    const hierarchy = buildHierarchy(location.id);
    return {
      ...location,
      hierarchy,
      hasChildren: !!locationsByParent[location.id]
    };
  });
};
type IRow = LocationMiniDTO & { hierarchy: number[]; hasChildren: boolean };
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
  const apiRef = useGridApiRef();
  const theme = useTheme();
  const { loadingGet, locationsMini } = useSelector((state) => state.locations);
  const initialized = useRef<boolean>(false);
  const single = maxSelections === 1;

  const locationsHierarchy: IRow[] = useMemo(
    () => getLocationRows(locationsMini),
    [locationsMini.length]
  );

  // State for tracking selected locations
  const [selectedLocations, setSelectedLocations] = useState<LocationMiniDTO[]>(
    initialSelectedLocations
  );
  const [selectionModel, setSelectionModel] = useState<GridSelectionModel>(
    initialSelectedLocations.map((location) => location.id)
  );
  const previousInitialSelectedLocations = usePrevious(
    initialSelectedLocations
  );

  const handleReset = (callApi: boolean) => {
    if (callApi) {
      dispatch(getLocationsMini());
    }
  };

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
        setSelectionModel(
          initialSelectedLocations.map((location) => location.id)
        );
      } else {
        setSelectedLocations([]);
        setSelectionModel([]);
      }
    }
  }, [open, initialSelectedLocations, previousInitialSelectedLocations]);

  useEffect(() => {
    if (single && open) {
      setSelectedLocations([]);
      setSelectionModel([]);
    }
  }, [open]);

  const columns: CustomDatagridColumn[] = [
    {
      field: 'customId',
      headerName: t('id'),
      flex: 1
    },
    {
      field: 'name',
      headerName: t('name'),
      flex: 1,
      renderCell: (params: GridRenderCellParams<string>) => (
        <Box sx={{ fontWeight: 'bold' }}>{params.value}</Box>
      )
    }
  ];

  const groupingColDef: DataGridProProps['groupingColDef'] = {
    headerName: t('hierarchy'),
    renderCell: (params) => <GroupingCellWithLazyLoading {...params} />
  };

  const CustomRow = (props: React.ComponentProps<typeof GridRow>) => {
    const rowNode = apiRef.current.getRowNode(props.rowId);
    return (
      <GridRow
        {...props}
        style={
          (rowNode?.depth ?? 0) > 0
            ? {
                backgroundColor:
                  rowNode.depth % 2 === 0
                    ? theme.colors.primary.light
                    : theme.colors.primary.main,
                color: 'white'
              }
            : undefined
        }
      />
    );
  };

  const handleRowClick: GridEventListener<'rowClick'> = (params) => {
    // Prevent selection of loading rows or excluded locations
    if (typeof params.id === 'string' && params.id.startsWith('loading_'))
      return;
    if (excludedLocationIds.includes(params.id as number)) return;

    // Get the current selection model
    const currentSelectionModel = [...selectionModel];

    // Check if the item is already selected
    const selectedIndex = currentSelectionModel.indexOf(params.id);

    // Toggle selection
    if (selectedIndex === -1) {
      // Check maximum selections limit if applicable
      if (maxSelections && currentSelectionModel.length >= maxSelections) {
        return; // Do not add if max is reached
      }
      currentSelectionModel.push(params.id);
    } else {
      currentSelectionModel.splice(selectedIndex, 1);
    }
    setSelectionModel(currentSelectionModel);

    // Update the selected locations array
    const updatedSelectedLocations = currentSelectionModel.map((id) => {
      return apiRef.current.getRow(id) as IRow;
    });
    setSelectedLocations(updatedSelectedLocations);
    if (single) {
      onSelect(updatedSelectedLocations);
      onClose();
    }
  };

  const handleConfirmSelection = () => {
    onSelect(selectedLocations);
    onClose();
  };

  const handleRemoveSelection = (locationId: number) => {
    const updatedSelectionModel = selectionModel.filter(
      (id) => id !== locationId
    );
    setSelectionModel(updatedSelectionModel);

    const updatedSelectedLocations = selectedLocations.filter(
      (location) => location.id !== locationId
    );
    setSelectedLocations(updatedSelectedLocations);
  };

  const filteredLocationsHierarchy = locationsHierarchy.filter(
    (location) => !excludedLocationIds.includes(location.id)
  );

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
          <CustomDataGrid
            pro
            treeData
            apiRef={apiRef}
            columns={columns}
            rows={filteredLocationsHierarchy}
            loading={loadingGet}
            getRowId={(row) => row.id}
            getRowHeight={() => 'auto'}
            getTreeDataPath={(row) => row.hierarchy.map(String)}
            groupingColDef={groupingColDef}
            disableColumnFilter
            checkboxSelection={!single}
            selectionModel={selectionModel}
            onSelectionModelChange={(newSelectionModel) => {
              if (loadingGet) return;
              if (maxSelections && newSelectionModel.length > maxSelections) {
                return;
              }
              setSelectionModel(newSelectionModel);
              const updatedSelectedLocations = newSelectionModel.map((id) => {
                return apiRef.current.getRow(id) as IRow;
              });

              setSelectedLocations(updatedSelectedLocations);
            }}
            components={{
              Row: CustomRow,
              NoRowsOverlay: () => (
                <NoRowsMessageWrapper
                  message={t('noRows.location.message')}
                  action={t('noRows.location.action')}
                />
              )
            }}
            onRowClick={handleRowClick}
            initialState={{
              columns: { columnVisibilityModel: {} }
            }}
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
