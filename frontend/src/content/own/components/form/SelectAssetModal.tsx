import React, { useState, useEffect, useContext, useRef } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Box,
  Button,
  CircularProgress,
  IconButton,
  Stack,
  Typography,
  useTheme,
  Chip
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useDispatch, useSelector } from '../../../../store';
import {
  getAssetChildren,
  resetAssetsHierarchy
} from '../../../../slices/asset';
import CustomDataGrid, { CustomDatagridColumn } from '../CustomDatagrid';
import {
  GridRenderCellParams,
  GridRow,
  GridEventListener,
  GridSelectionModel
} from '@mui/x-data-grid';
import { DataGridProProps, useGridApiRef } from '@mui/x-data-grid-pro';
import { AssetRow, AssetMiniDTO } from '../../../../models/owns/asset';
import { GroupingCellWithLazyLoading } from '../../Assets/GroupingCellWithLazyLoading';
import ReplayTwoToneIcon from '@mui/icons-material/ReplayTwoTone';
import { Pageable } from '../../../../models/owns/page';
import NoRowsMessageWrapper from '../NoRowsMessageWrapper';
import { usePrevious } from '../../../../hooks/usePrevious';

interface SelectAssetModalProps {
  open: boolean;
  onClose: () => void;
  onSelect: (assets: AssetMiniDTO[]) => void; // Changed to handle array of assets
  excludedAssetIds?: number[]; // Changed to array for multiple exclusions
  locationId?: number;
  maxSelections?: number; // Optional limit for selections
  initialSelectedAssets?: AssetMiniDTO[]; // Optional pre-selected assets
}

const SelectAssetModal: React.FC<SelectAssetModalProps> = ({
  open,
  onClose,
  onSelect,
  excludedAssetIds = [],
  locationId,
  maxSelections,
  initialSelectedAssets = []
}) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const apiRef = useGridApiRef();
  const theme = useTheme();
  const { assetsHierarchy, loadingGet } = useSelector((state) => state.assets);
  const [pageable, setPageable] = useState<Pageable>({ page: 0, size: 1000 });
  const initialized = useRef<boolean>(false);
  const single = maxSelections === 1;
  const [deployedAssets, setDeployedAssets] = useState<
    { id: number; hierarchy: number[] }[]
  >([{ id: 0, hierarchy: [] }]);

  // State for tracking selected assets
  const [selectedAssets, setSelectedAssets] = useState<AssetMiniDTO[]>(
    initialSelectedAssets
  );
  const [selectionModel, setSelectionModel] = useState<GridSelectionModel>(
    initialSelectedAssets.map((asset) => asset.id)
  );
  const previousInitialSelectedAssets = usePrevious(initialSelectedAssets);

  const handleReset = (callApi: boolean) => {
    dispatch(resetAssetsHierarchy(callApi));
    setDeployedAssets([{ id: 0, hierarchy: [] }]);
    if (callApi) {
      dispatch(getAssetChildren(0, [], pageable));
    }
  };

  useEffect(() => {
    if (
      open &&
      (!initialized.current ||
        JSON.stringify(previousInitialSelectedAssets) !==
          JSON.stringify(initialSelectedAssets))
    ) {
      initialized.current = true;
      handleReset(true);
      if (initialSelectedAssets?.length) {
        setSelectedAssets(initialSelectedAssets);
        setSelectionModel(initialSelectedAssets.map((asset) => asset.id));
      } else {
        setSelectedAssets([]);
        setSelectionModel([]);
      }
    }
  }, [open, initialSelectedAssets, previousInitialSelectedAssets]);

  useEffect(() => {
    if (single && open) {
      setSelectedAssets([]);
      setSelectionModel([]);
    }
  }, [open]);
  useEffect(() => {
    if (apiRef.current.getRow) {
      const handleRowExpansionChange: GridEventListener<
        'rowExpansionChange'
      > = async (node) => {
        const row = apiRef.current.getRow(node.id) as AssetRow | null;
        if (!node.childrenExpanded || !row || row.childrenFetched) {
          return;
        }
        apiRef.current.updateRows([
          {
            id: t('loading_assets', { name: row.name, id: node.id }),
            hierarchy: [...row.hierarchy, '']
          }
        ]);
        if (
          !deployedAssets.find((deployedAsset) => deployedAsset.id === row.id)
        ) {
          setDeployedAssets((prev) => [
            ...prev,
            { id: row.id, hierarchy: row.hierarchy }
          ]);
        }
        dispatch(getAssetChildren(row.id, row.hierarchy, pageable));
      };

      const handleCellKeyDown: GridEventListener<'cellKeyDown'> = (
        params,
        event
      ) => {
        const cellParams = apiRef.current.getCellParams(
          params.id,
          params.field
        );
        if (cellParams.colDef.type === 'treeDataGroup' && event.key === ' ') {
          event.stopPropagation();
          event.preventDefault();
          event.defaultMuiPrevented = true;
          apiRef.current.setRowChildrenExpansion(
            params.id,
            !params.rowNode.childrenExpanded
          );
        }
      };

      const unsubscribeExpansion = apiRef.current.subscribeEvent(
        'rowExpansionChange',
        handleRowExpansionChange
      );
      const unsubscribeKeyDown = apiRef.current.subscribeEvent(
        'cellKeyDown',
        handleCellKeyDown,
        { isFirst: true }
      );

      return () => {
        unsubscribeExpansion();
        unsubscribeKeyDown();
      };
    }
  }, [apiRef, dispatch, pageable, t, deployedAssets]);

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
    },
    {
      field: 'location',
      headerName: t('location'),
      flex: 1,
      valueGetter: (params) => params.row.location?.name ?? ''
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
    // Prevent selection of loading rows or excluded assets
    if (typeof params.id === 'string' && params.id.startsWith('loading_'))
      return;
    if (excludedAssetIds.includes(params.id as number)) return;

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

    // Update the selected assets array
    const updatedSelectedAssets = currentSelectionModel.map((id) => {
      return apiRef.current.getRow(id) as AssetRow;
    });
    setSelectedAssets(updatedSelectedAssets);
    if (single) {
      onSelect(updatedSelectedAssets);
      onClose();
    }
  };

  const handleConfirmSelection = () => {
    onSelect(selectedAssets);
    onClose();
  };

  const handleRemoveSelection = (assetId: number) => {
    const updatedSelectionModel = selectionModel.filter((id) => id !== assetId);
    setSelectionModel(updatedSelectionModel);

    const updatedSelectedAssets = selectedAssets.filter(
      (asset) => asset.id !== assetId
    );
    setSelectedAssets(updatedSelectedAssets);
  };

  const filteredAssetsHierarchy = assetsHierarchy.filter(
    (asset) =>
      !excludedAssetIds.includes(asset.id) &&
      (locationId ? asset.location?.id === locationId : true)
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
        <Typography variant="h4">{t('select_asset')}</Typography>
        <IconButton
          onClick={() => handleReset(true)}
          color="primary"
          size="small"
        >
          <ReplayTwoToneIcon />
        </IconButton>
      </DialogTitle>

      {selectedAssets.length > 0 && (
        <Box sx={{ px: 2, py: 1, display: 'flex', flexWrap: 'wrap', gap: 1 }}>
          {selectedAssets.map((asset) => (
            <Chip
              key={asset.id}
              label={`${asset.customId}: ${asset.name}`}
              onDelete={() => handleRemoveSelection(asset.id)}
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
            rows={filteredAssetsHierarchy}
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
              const updatedSelectedAssets = newSelectionModel.map((id) => {
                const row = apiRef.current.getRow(id) as AssetRow;
                return {
                  id: row.id,
                  name: row.name,
                  customId: row.customId
                };
              });

              setSelectedAssets(updatedSelectedAssets);
            }}
            components={{
              Row: CustomRow,
              NoRowsOverlay: () => (
                <NoRowsMessageWrapper
                  message={t('noRows.asset.message')}
                  action={t('noRows.asset.action')}
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
            disabled={selectedAssets.length === 0}
          >
            {t('select')} ({selectedAssets.length})
          </Button>
        </DialogActions>
      )}
    </Dialog>
  );
};

export default SelectAssetModal;
