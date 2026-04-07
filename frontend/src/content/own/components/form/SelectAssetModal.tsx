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
import { getAssetsMini, resetAssetsHierarchy } from '../../../../slices/asset';
import { AssetMiniDTO } from '../../../../models/owns/asset';
import ReplayTwoToneIcon from '@mui/icons-material/ReplayTwoTone';
import NoRowsMessageWrapper from '../NoRowsMessageWrapper';
import { usePrevious } from '../../../../hooks/usePrevious';
import { createColumnHelper } from '@tanstack/react-table';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import CustomDatagrid2, { CustomDatagridColumn2 } from '../CustomDatagrid2';

interface SelectAssetModalProps {
  open: boolean;
  onClose: () => void;
  onSelect: (assets: AssetMiniDTO[]) => void;
  excludedAssetIds?: number[];
  locationId?: number;
  maxSelections?: number;
  initialSelectedAssets?: AssetMiniDTO[];
}

type AssetRow = AssetMiniDTO & { depth?: number };
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
  const theme = useTheme();
  const { loadingGet, assetsMini } = useSelector((state) => state.assets);
  const initialized = useRef<boolean>(false);
  const single = maxSelections === 1;

  // State for tracking expanded rows
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});

  // State for tracking selected assets
  const [selectedAssets, setSelectedAssets] = useState<AssetMiniDTO[]>(
    initialSelectedAssets
  );
  const [rowSelection, setRowSelection] = useState<Record<string, boolean>>(
    initialSelectedAssets.reduce((acc, asset) => {
      acc[asset.id] = true;
      return acc;
    }, {} as Record<string, boolean>)
  );
  const previousInitialSelectedAssets = usePrevious(initialSelectedAssets);

  const handleReset = (callApi: boolean) => {
    if (callApi) {
      dispatch(getAssetsMini());
    }
  };

  // Flatten hierarchy based on expanded state
  const getHierarchicalData = (
    flatList: AssetMiniDTO[],
    expanded: Record<string, boolean>,
    parentId: number | null = null,
    depth: number = 0
  ): (AssetMiniDTO & { depth: number })[] => {
    let result: (AssetMiniDTO & { depth: number })[] = [];

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
    () => getHierarchicalData(assetsMini, expanded),
    [assetsMini, expanded]
  );

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
        setRowSelection(
          initialSelectedAssets.reduce((acc, asset) => {
            acc[asset.id] = true;
            return acc;
          }, {} as Record<string, boolean>)
        );
      } else {
        setSelectedAssets([]);
        setRowSelection({});
      }
    }
  }, [open, initialSelectedAssets, previousInitialSelectedAssets]);

  useEffect(() => {
    if (single && open) {
      setSelectedAssets([]);
      setRowSelection({});
    }
  }, [open, single]);

  const handleToggleExpand = (row: AssetRow) => {
    setExpanded((prev) => ({ ...prev, [row.id]: !prev[row.id] }));
  };

  const columnHelper = createColumnHelper<AssetMiniDTO>();

  const columns: CustomDatagridColumn2<AssetMiniDTO>[] = [
    columnHelper.display({
      id: 'expander',
      header: '',
      cell: ({ row }) => {
        const isExpanded = expanded[row.original.id];
        const hasChildren = assetsMini.some(
          (asset) => asset.parentId === row.original.id
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

  const handleRowClick = (row: AssetMiniDTO) => {
    // Prevent selection of excluded assets
    if (excludedAssetIds.includes(row.id)) return;

    if (single) {
      // Single selection mode
      const newSelection = [row];
      setSelectedAssets(newSelection);
      onSelect(newSelection);
      onClose();
    } else {
      // Multiple selection mode
      const isSelected = rowSelection[row.id];
      let newRowSelection: Record<string, boolean>;
      let updatedSelectedAssets: AssetMiniDTO[];

      if (isSelected) {
        // Remove from selection
        newRowSelection = { ...rowSelection };
        delete newRowSelection[row.id];
        updatedSelectedAssets = selectedAssets.filter(
          (asset) => asset.id !== row.id
        );
      } else {
        // Add to selection
        if (maxSelections && selectedAssets.length >= maxSelections) {
          return;
        }
        newRowSelection = { ...rowSelection, [row.id]: true };
        updatedSelectedAssets = [...selectedAssets, row];
      }

      setRowSelection(newRowSelection);
      setSelectedAssets(updatedSelectedAssets);
    }
  };

  const handleConfirmSelection = () => {
    onSelect(selectedAssets);
    onClose();
  };

  const handleRemoveSelection = (assetId: number) => {
    const newRowSelection = { ...rowSelection };
    delete newRowSelection[assetId];
    setRowSelection(newRowSelection);

    const updatedSelectedAssets = selectedAssets.filter(
      (asset) => asset.id !== assetId
    );
    setSelectedAssets(updatedSelectedAssets);
  };

  const filteredTableData = tableData.filter(
    (asset) =>
      !excludedAssetIds.includes(asset.id) &&
      (locationId ? asset.locationId === locationId : true)
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
          <CustomDatagrid2
            columns={columns}
            data={filteredTableData}
            loading={loadingGet}
            enableRowSelection={!single}
            rowSelection={rowSelection}
            onRowSelectionChange={setRowSelection}
            onRowClick={handleRowClick}
            getRowId={(row) => row.id.toString()}
            noRowsMessage={t('noRows.asset.message')}
            noRowsAction={t('noRows.asset.action')}
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
