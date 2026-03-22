import { Helmet } from 'react-helmet-async';
import {
  Box,
  Button,
  Card,
  CircularProgress,
  debounce,
  Dialog,
  DialogContent,
  DialogTitle,
  Drawer,
  IconButton,
  Menu,
  MenuItem,
  Stack,
  Typography,
  useTheme
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { IField } from '../type';
import {
  addAsset,
  getAssetChildren,
  getAssets,
  resetAssetsHierarchy
} from '../../../slices/asset';
import MoreHorizIcon from '@mui/icons-material/MoreHoriz';
import { useDispatch, useSelector } from '../../../store';
import * as React from 'react';
import { useContext, useEffect, useMemo, useState } from 'react';
import ReplayTwoToneIcon from '@mui/icons-material/ReplayTwoTone';
import { TitleContext } from '../../../contexts/TitleContext';
import CustomDatagrid2, {
  CustomDatagridColumn2
} from '../components/CustomDatagrid2';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import { AssetDTO, AssetRow } from '../../../models/owns/asset';
import Form from '../components/form';
import * as Yup from 'yup';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { formatAssetValues } from '../../../utils/formatters';
import UserAvatars from '../components/UserAvatars';
import { enumerate } from '../../../utils/displayers';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import { getAssetUrl } from '../../../utils/urlPaths';
import useAuth from '../../../hooks/useAuth';
import { PermissionEntity } from '../../../models/owns/role';
import PermissionErrorMessage from '../components/PermissionErrorMessage';
import { isNumeric } from '../../../utils/validators';
import { getSingleLocation } from '../../../slices/location';
import { useExport } from '../../../hooks/useExport';
import MoreVertTwoToneIcon from '@mui/icons-material/MoreVertTwoTone';
import {
  FilterField,
  Pageable,
  SearchCriteria,
  Sort
} from '../../../models/owns/page';
import Filters from './Filters';
import {
  fireGa4Event,
  getImageAndFiles,
  onSearchQueryChange
} from '../../../utils/overall';
import SearchInput from '../components/SearchInput';
import { PlanFeature } from '../../../models/owns/subscriptionPlan';
import AssetStatusTag from './components/AssetStatusTag';
import { getErrorMessage } from '../../../utils/api';
import SplitButton from '../components/SplitButton';
import {
  createColumnHelper,
  SortingState,
  Updater
} from '@tanstack/react-table';
import useTableState from '../../../hooks/useTableState';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

const PAGE_SIZE = 40;

function Assets() {
  const { t }: { t: any } = useTranslation();
  const { setTitle } = useContext(TitleContext);
  const { uploadFiles } = useContext(CompanySettingsContext);
  const [searchParams, setSearchParams] = useSearchParams();
  const locationParam = searchParams.get('location');
  const navigate = useNavigate();
  const {
    hasViewPermission,
    hasCreatePermission,
    hasViewOtherPermission,
    getFilteredFields,
    hasFeature
  } = useAuth();
  const [openAddModal, setOpenAddModal] = useState<boolean>(false);
  const dispatch = useDispatch();
  const {
    assetsHierarchy,
    loadingGet,
    loadingHierarchy,
    assets,
    childrenPages
  } = useSelector((state) => state.assets);
  const { exportEntity, loadingExport } = useExport();
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { locations } = useSelector((state) => state.locations);
  const locationParamObject = locations.find(
    (location) => location.id === Number(locationParam)
  );
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const openMenu = Boolean(anchorEl);
  type ViewType = 'hierarchy' | 'list';
  const theme = useTheme();
  const [view, setView] = useState<ViewType>('hierarchy');
  const [hierarchySorting, setHierarchySorting] = useState<SortingState>([]);
  const [pageable, setPageable] = useState<Pageable>({
    page: 0,
    size: PAGE_SIZE
  });
  const initialCriteria: SearchCriteria = {
    filterFields: [
      {
        field: 'archived',
        operation: 'eq',
        value: false
      }
    ],
    pageSize: 50,
    pageNum: 0,
    direction: 'DESC'
  };
  const [criteria, setCriteria] = useState<SearchCriteria>(initialCriteria);
  const onQueryChange = (event) => {
    setView(event.target.value ? 'list' : 'hierarchy');
    onSearchQueryChange<AssetDTO>(event, criteria, setCriteria, [
      'name',
      'description',
      'model',
      'additionalInfos',
      'barCode',
      'area'
    ]);
  };
  const debouncedQueryChange = useMemo(() => debounce(onQueryChange, 1300), []);
  const onFilterChange = (newFilters: FilterField[]) => {
    const newCriteria = { ...criteria };
    newCriteria.filterFields = newFilters;
    setCriteria(newCriteria);
  };
  const [deployedAssets, setDeployedAssets] = useState<{ id: number }[]>([
    {
      id: 0
    }
  ]);

  // Expanding state for hierarchy view
  const [expanded, setExpanded] = useState<Record<string, boolean>>({});
  const [subRowsMap, setSubRowsMap] = useState<Record<number, AssetRow[]>>({});

  const fetchMoreForParent = (parentId: number) => {
    const parentPage = childrenPages[parentId];
    if (parentPage && !parentPage.last) {
      dispatch(
        getAssetChildren(parentId, {
          ...pageable,
          page: (parentPage.number || 0) + 1
        })
      );
    }
  };

  const handleToggleExpand = async (row: AssetRow) => {
    const isExpanded = expanded[row.id];

    if (!isExpanded) {
      // Check if we already have children for this row in the Redux store
      const hasChildrenLoaded = assetsHierarchy.some(
        (a) => a.parentAsset?.id === row.id
      );

      if (!hasChildrenLoaded && row.hasChildren) {
        // Set temporary loading row
        const loadingRow: AssetRow = {
          id: `loading-${row.id}`,
          name: t('loading_assets', { name: row.name, id: row.id })
        } as unknown as AssetRow;

        setSubRowsMap((prev) => ({ ...prev, [row.id]: [loadingRow] }));
        // Fetch the children
        await dispatch(getAssetChildren(row.id, pageable));
        setDeployedAssets((prevState) => [...prevState, row]);

        // Clean up the loading row once the fetch is complete
        setSubRowsMap((prev) => {
          const newMap = { ...prev };
          delete newMap[row.id];
          return newMap;
        });
      }
    }

    // Toggle expand/collapse state
    setExpanded((prev) => ({ ...prev, [row.id]: !isExpanded }));
  };
  const handleOpenMenu = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };
  const handleCloseMenu = () => {
    setAnchorEl(null);
  };
  const [openFilterDrawer, setOpenFilterDrawer] = useState<boolean>(false);
  useEffect(() => {
    setTitle(t('assets'));
  }, []);

  useEffect(() => {
    if (hasViewPermission(PermissionEntity.ASSETS)) {
      handleReset(false);
      dispatch(getAssetChildren(0, pageable));
    }
  }, [pageable]);
  useEffect(() => {
    if (locationParam) {
      if (locationParam && isNumeric(locationParam)) {
        dispatch(getSingleLocation(Number(locationParam)));
      }
    }
  }, []);
  useEffect(() => {
    let shouldOpen1 = locationParam && locationParamObject;
    if (shouldOpen1) {
      setOpenAddModal(true);
    }
  }, [locationParamObject]);

  useEffect(() => {
    if (view === 'list' && hasViewPermission(PermissionEntity.ASSETS))
      dispatch(getAssets(criteria));
  }, [criteria, view]);

  const onCreationSuccess = () => {
    setOpenAddModal(false);
    showSnackBar(t('asset_create_success'), 'success');
  };
  const onCreationFailure = (err) =>
    showSnackBar(getErrorMessage(err, t('asset_create_failure')), 'error');
  const handleCloseFilterDrawer = () => setOpenFilterDrawer(false);

  const renderMenu = () => (
    <Menu
      id="basic-menu"
      anchorEl={anchorEl}
      open={openMenu}
      onClose={handleCloseMenu}
      MenuListProps={{
        'aria-labelledby': 'basic-button'
      }}
    >
      {hasViewOtherPermission(PermissionEntity.ASSETS) && (
        <MenuItem
          disabled={loadingExport['assets']}
          onClick={async () => {
            try {
              await exportEntity('assets');
            } catch (error) {
              showSnackBar(t('Export failed'), 'error');
            }
          }}
        >
          <Stack spacing={2} direction="row">
            {loadingExport['assets'] && <CircularProgress size="1rem" />}
            <Typography>{t('to_export')}</Typography>
          </Stack>
        </MenuItem>
      )}
      <MenuItem
        onClick={() => {
          setOpenFilterDrawer(true);
          handleCloseMenu();
        }}
      >
        {t('to_filter')}
      </MenuItem>
    </Menu>
  );

  // Mapping for column fields to API field names for sorting
  const fieldMapping: Record<string, string> = {
    customId: 'customId',
    name: 'name',
    status: 'status',
    location: 'location.name',
    image: 'image',
    area: 'area',
    model: 'model',
    barCode: 'barCode',
    category: 'category.name',
    description: 'description',
    primaryUser: 'primaryUser.firstName',
    assignedTo: 'assignedTo.firstName',
    teams: 'teams.name',
    vendors: 'vendors.name',
    parentAsset: 'parentAsset.name',
    createdAt: 'createdAt'
  };

  const onResetFilters = () => {
    setCriteria(initialCriteria);
    setView('hierarchy');
  };

  const columnHelper = createColumnHelper<AssetDTO>();

  const columns: CustomDatagridColumn2<AssetDTO | AssetRow>[] = [
    columnHelper.display({
      id: 'expander',
      header: '',
      meta: { enableReordering: false },
      cell: ({ row }) => {
        const isExpanded = expanded[row.original.id];
        const hasSubRows =
          row.original.hasChildren || subRowsMap[row.original.id]?.length > 0;

        if (!hasSubRows && view === 'hierarchy') {
          return <Box sx={{ width: 24 }} />;
        }

        return (
          <IconButton
            size="small"
            onClick={(e) => {
              e.stopPropagation();
              handleToggleExpand(row.original as AssetRow);
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
      cell: (info) => info.getValue() || '',
      size: 100
    }),
    columnHelper.accessor('name', {
      id: 'name',
      header: () => t('name'),
      cell: (info) => {
        const row = info.row.original as AssetRow & {
          depth: number;
          isLoadMoreRow?: boolean;
          parentId?: number;
        };
        // Render "Load More" row as a clickable button
        if (row.isLoadMoreRow) {
          return (
            <Box
              sx={{
                py: 1,
                fontWeight: 'bold'
              }}
            >
              <Button
                size="small"
                variant="text"
                onClick={(e) => {
                  e.stopPropagation();
                  if (row.parentId) {
                    fetchMoreForParent(row.parentId);
                  }
                }}
                disabled={loadingHierarchy}
                endIcon={
                  loadingHierarchy ? (
                    <CircularProgress size="1rem" />
                  ) : (
                    <MoreHorizIcon />
                  )
                }
              >
                {t('fetch_more')}
              </Button>
            </Box>
          );
        }

        return (
          <Box
            sx={{
              py: 1,
              fontWeight: 'bold',
              ml: view === 'hierarchy' ? (info.row.depth || 0) * 24 : 0
            }}
          >
            {info.getValue()}
          </Box>
        );
      },
      size: 200
    }),
    columnHelper.accessor('status', {
      id: 'status',
      header: () => t('status'),
      cell: (info) => <AssetStatusTag status={info.getValue()} />,
      size: 120
    }),
    columnHelper.accessor((row) => row.location?.name, {
      id: 'location',
      header: () => t('location'),
      cell: (info) => info.getValue() || '',
      size: 150,
      meta: {
        uiConfigKey: 'locations'
      }
    }),
    columnHelper.accessor('image', {
      id: 'image',
      header: () => t('image'),
      cell: (info) =>
        info.getValue() ? (
          <img
            width="100%"
            height="100%"
            src={info.getValue().url}
            alt={info.row.original.name}
          />
        ) : null,
      size: 100
    }),
    columnHelper.accessor('area', {
      id: 'area',
      header: () => t('area'),
      cell: (info) => info.getValue() || '',
      size: 100
    }),
    columnHelper.accessor('model', {
      id: 'model',
      header: () => t('model'),
      cell: (info) => info.getValue() || '',
      size: 120
    }),
    columnHelper.accessor('barCode', {
      id: 'barCode',
      header: () => t('barcode'),
      cell: (info) => info.getValue() || '',
      size: 120
    }),
    columnHelper.accessor((row) => row.category?.name, {
      id: 'category',
      header: () => t('category'),
      cell: (info) => info.getValue() || '',
      size: 120
    }),
    columnHelper.accessor('description', {
      id: 'description',
      header: () => t('description'),
      cell: (info) => info.getValue() || '',
      size: 250
    }),
    columnHelper.accessor(
      (row) =>
        row.primaryUser
          ? `${row.primaryUser.firstName} ${row.primaryUser.lastName}`
          : null,
      {
        id: 'primaryUser',
        header: () => t('primary_worker'),
        cell: (info) => info.getValue() || '',
        size: 150
      }
    ),
    columnHelper.accessor('assignedTo', {
      id: 'assignedTo',
      header: () => t('assigned_to'),
      cell: (info) => <UserAvatars users={info.getValue() ?? []} />,
      size: 150
    }),
    columnHelper.accessor(
      (row) => enumerate(row.teams?.map((team) => team.name) ?? []),
      {
        id: 'teams',
        header: () => t('teams'),
        cell: (info) => info.getValue(),
        size: 150
      }
    ),
    columnHelper.accessor(
      (row) =>
        enumerate(row.vendors?.map((vendor) => vendor.companyName) ?? []),
      {
        id: 'vendors',
        header: () => t('vendors'),
        cell: (info) => info.getValue() || '',
        size: 150,
        meta: {
          uiConfigKey: 'vendorsAndCustomers'
        }
      }
    ),
    columnHelper.accessor((row) => row.parentAsset?.name, {
      id: 'parentAsset',
      header: () => t('parent_asset'),
      cell: (info) => info.getValue() || '',
      size: 150
    }),
    columnHelper.accessor('createdAt', {
      id: 'createdAt',
      header: () => t('created_at'),
      cell: (info) => getFormattedDate(info.getValue()),
      size: 140
    })
  ];
  const defaultFields: Array<IField> = [
    {
      name: 'assetInfo',
      type: 'titleGroupField',
      label: t('asset_information')
    },
    {
      name: 'name',
      type: 'text',
      label: t('name'),
      placeholder: t('asset_name_description'),
      required: true
    },
    {
      name: 'location',
      type: 'select',
      type2: 'location',
      label: t('location'),
      placeholder: t('select_asset_location'),
      required: true,
      midWidth: true
    },
    {
      name: 'acquisitionCost',
      type: 'number',
      label: t('acquisition_cost'),
      placeholder: t('acquisition_cost'),
      midWidth: true
    },
    {
      name: 'description',
      type: 'text',
      label: t('description'),
      placeholder: t('description'),
      multiple: true
    },
    {
      name: 'manufacturer',
      type: 'text',
      label: t('manufacturer'),
      placeholder: t('manufacturer'),
      midWidth: true
    },
    {
      name: 'power',
      type: 'text',
      label: t('power'),
      placeholder: t('power'),
      midWidth: true
    },
    {
      name: 'model',
      type: 'text',
      label: t('model'),
      placeholder: t('model'),
      midWidth: true
    },
    {
      name: 'barCode',
      type: 'text',
      label: t('barcode'),
      placeholder: t('barcode'),
      midWidth: true
    },
    {
      name: 'serialNumber',
      type: 'text',
      label: t('serial_number'),
      placeholder: t('serial_number'),
      midWidth: true
    },
    {
      name: 'category',
      midWidth: true,
      label: t('category'),
      placeholder: t('category'),
      type: 'select',
      type2: 'category',
      category: 'asset-categories'
    },
    {
      name: 'area',
      type: 'text',
      midWidth: true,
      label: t('area'),
      placeholder: t('area')
    },
    {
      name: 'image',
      type: 'file',
      fileType: 'image',
      label: t('image')
    },
    {
      name: 'assignedTo',
      type: 'titleGroupField',
      label: t('assigned_to')
    },
    {
      name: 'primaryUser',
      type: 'select',
      type2: 'user',
      label: t('worker'),
      placeholder: t('primary_user_description')
    },
    {
      name: 'assignedTo',
      type: 'select',
      type2: 'user',
      multiple: true,
      label: t('additional_workers'),
      placeholder: 'additional_workers_description'
    },
    {
      name: 'teams',
      type: 'select',
      type2: 'team',
      multiple: true,
      label: t('teams'),
      placeholder: 'Select teams'
    },
    {
      name: 'moreInfos',
      type: 'titleGroupField',
      label: t('more_informations')
    },
    {
      name: 'customers',
      type: 'select',
      type2: 'customer',
      multiple: true,
      label: t('customers'),
      placeholder: 'customers_description'
    },
    {
      name: 'vendors',
      type: 'select',
      type2: 'vendor',
      multiple: true,
      label: t('vendors'),
      placeholder: t('vendors_description')
    },
    {
      name: 'inServiceDate',
      type: 'date',
      midWidth: true,
      label: t('inServiceDate_description')
    },
    {
      name: 'warrantyExpirationDate',
      type: 'date',
      midWidth: true,
      label: t('warranty_expiration_date')
    },
    {
      name: 'files',
      type: 'file',
      multiple: true,
      label: t('files'),
      fileType: 'file'
    },
    {
      name: 'additionalInfos',
      type: 'text',
      label: t('additional_information'),
      placeholder: t('additional_information'),
      multiple: true
    },
    {
      name: 'structure',
      type: 'titleGroupField',
      label: t('structure')
    },
    { name: 'parts', type: 'select', type2: 'part', label: t('parts') },
    {
      name: 'parentAsset',
      type: 'select',
      type2: 'asset',
      label: t('parent_asset')
    }
  ];
  const shape = {
    name: Yup.string().required(t('required_asset_name'))
  };
  const handleReset = (callApi: boolean) => {
    dispatch(resetAssetsHierarchy(callApi));
  };

  const renderAssetAddModal = () => (
    <Dialog
      fullWidth
      maxWidth="md"
      open={openAddModal}
      onClose={() => setOpenAddModal(false)}
    >
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {t('add_asset')}
        </Typography>
        <Typography variant="subtitle2">
          {t('add_asset_description')}
        </Typography>
      </DialogTitle>
      <DialogContent
        dividers
        sx={{
          p: 3
        }}
      >
        <Box>
          <Form
            fields={getFilteredFields(defaultFields)}
            validation={Yup.object().shape(shape)}
            submitText={t('create_asset')}
            values={{
              inServiceDate: null,
              warrantyExpirationDate: null,
              location: locationParamObject
                ? {
                    label: locationParamObject.name,
                    value: locationParamObject.id
                  }
                : null
            }}
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              if (assetsHierarchy.length === 0)
                fireGa4Event('first_asset_creation');
              let formattedValues = formatAssetValues(values);
              try {
                const uploadedFiles = await uploadFiles(
                  formattedValues.files,
                  formattedValues.image
                );

                const imageAndFiles = getImageAndFiles(uploadedFiles);
                formattedValues = {
                  ...formattedValues,
                  image: imageAndFiles.image,
                  files: imageAndFiles.files
                };

                await dispatch(addAsset(formattedValues));
                onCreationSuccess();
                deployedAssets.forEach((deployedAsset) =>
                  dispatch(getAssetChildren(deployedAsset.id, pageable))
                );
              } catch (err) {
                onCreationFailure(err);
                throw err;
              }
            }}
          />
        </Box>
      </DialogContent>
    </Dialog>
  );

  // Flatten hierarchy based on expanded state and parent-child relationships
  const getHierarchicalData = (
    flatList: AssetRow[],
    expanded: Record<string, boolean>,
    subRowsMap: Record<number, AssetRow[]>,
    parentId: number | null = null,
    depth: number = 0
  ): AssetRow[] => {
    let result: AssetRow[] = [];

    // 1. Find the children of the current parent
    const nodes = flatList.filter((item) => {
      if (parentId === null) {
        return !item.parentAsset; // Root nodes have no parent
      }
      return item.parentAsset?.id === parentId; // Child nodes
    });

    for (const node of nodes) {
      // 2. Add the current node
      result.push({ ...node, depth });

      // 3. If expanded, add its children right below it
      if (expanded[node.id]) {
        const children = getHierarchicalData(
          flatList,
          expanded,
          subRowsMap,
          node.id,
          depth + 1
        );

        if (children.length > 0) {
          result = [...result, ...children];
          // Add "Load More" row after children if there are more pages
          if (hasMorePages(node.id)) {
            result.push({
              id: `load-more-${node.id}`,
              name: t('fetch_more'),
              depth: 0,
              isLoadMoreRow: true,
              parentId: node.id
            } as unknown as AssetRow);
          }
        }
      }
    }

    return result;
  };

  // Check if a parent has more pages to load
  const hasMorePages = (parentId: number): boolean => {
    const childrenPage = childrenPages[parentId];
    return childrenPage ? !childrenPage.last : false;
  };

  // Use table state for list view (server-side pagination and sorting)
  const tableState = useTableState({
    prefix: 'assets',
    setCriteria,
    fieldMapping,
    initialPagination: { pageIndex: 0, pageSize: criteria.pageSize }
  });

  // Prepare data for the table based on view type
  const tableData: (AssetRow | AssetDTO)[] =
    view === 'hierarchy'
      ? getHierarchicalData(assetsHierarchy, expanded, subRowsMap)
      : assets.content || [];

  // Handle pagination change based on view type
  const handlePaginationChange = (newPagination: {
    pageIndex: number;
    pageSize: number;
  }) => {
    if (view === 'hierarchy') {
      setPageable((prev) => ({
        ...prev,
        page: newPagination.pageIndex,
        size: newPagination.pageSize
      }));
    } else {
      tableState.setPagination(newPagination);
    }
  };

  // Handle sorting change based on view type
  const handleSortingChange = (newSorting: Updater<SortingState>) => {
    if (view === 'hierarchy') {
      // Resolve the sorting value (handle both direct value and updater function)
      const resolvedSorting: SortingState =
        typeof newSorting === 'function'
          ? newSorting(hierarchySorting)
          : newSorting;
      setHierarchySorting(newSorting);
      const sortParams =
        resolvedSorting.length > 0
          ? resolvedSorting.map(
              (sort) =>
                `${fieldMapping[sort.id] || sort.id},${
                  sort.desc ? 'desc' : 'asc'
                }` as Sort
            )
          : [];
      setPageable((prev) => ({
        ...prev,
        sort: sortParams.length > 0 ? [...sortParams] : undefined
      }));
    } else {
      tableState.setSorting(newSorting);
    }
  };

  if (hasViewPermission(PermissionEntity.ASSETS))
    return (
      <>
        {renderAssetAddModal()}
        <Helmet>
          <title>{t('assets')}</title>
        </Helmet>
        <Box justifyContent="center" alignItems="stretch" paddingX={4}>
          <Box
            my={1}
            display="flex"
            flexDirection="row"
            justifyContent="space-between"
            alignItems="center"
          >
            <SearchInput onChange={debouncedQueryChange} />
            <Stack direction="row" spacing={1}>
              <IconButton onClick={() => handleReset(true)} color="primary">
                <ReplayTwoToneIcon />
              </IconButton>
              <IconButton onClick={handleOpenMenu} color="primary">
                <MoreVertTwoToneIcon />
              </IconButton>
              {view === 'list' && (
                <Button variant={'outlined'} onClick={onResetFilters}>
                  {t('reset_filters')}
                </Button>
              )}
              {hasCreatePermission(PermissionEntity.ASSETS) && (
                <SplitButton
                  onMainClick={() => setOpenAddModal(true)}
                  startIcon={<AddTwoToneIcon />}
                  sx={{ mx: 6, my: 1 }}
                  label={t('asset')}
                  menuItems={
                    hasViewPermission(PermissionEntity.SETTINGS) &&
                    hasFeature(PlanFeature.IMPORT_CSV)
                      ? [
                          {
                            label: t('to_import'),
                            onClick: () => navigate('/app/imports/assets')
                          }
                        ]
                      : []
                  }
                />
              )}
            </Stack>
          </Box>
          <Card
            sx={{
              py: 2,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'space-between'
            }}
          >
            <Box sx={{ width: '95%' }}>
              <CustomDatagrid2
                columns={view === 'hierarchy' ? columns : columns.slice(1)}
                data={tableData}
                loading={view === 'hierarchy' ? loadingHierarchy : loadingGet}
                pagination={
                  view === 'hierarchy'
                    ? { pageIndex: pageable.page, pageSize: pageable.size }
                    : tableState.pagination
                }
                onPaginationChange={handlePaginationChange}
                totalRows={
                  view === 'hierarchy'
                    ? childrenPages[0]?.totalElements || assetsHierarchy.length
                    : assets.totalElements
                }
                pageSizeOptions={
                  view === 'list'
                    ? [10, 20, 50]
                    : Array.from({ length: 4 }, (_, i) => PAGE_SIZE * (i + 1))
                }
                sorting={
                  view === 'hierarchy' ? hierarchySorting : tableState.sorting
                }
                onSortingChange={handleSortingChange}
                columnOrder={tableState.columnOrder}
                onColumnOrderChange={tableState.setColumnOrder}
                columnSizing={tableState.columnSizing}
                onColumnSizingChange={tableState.setColumnSizing}
                columnVisibility={tableState.columnVisibility}
                onColumnVisibilityChange={tableState.setColumnVisibility}
                pinnedColumns={tableState.pinnedColumns}
                onPinnedColumnsChange={tableState.setPinnedColumns}
                noRowsMessage={t('noRows.asset.message')}
                noRowsAction={t('noRows.asset.action')}
                onRowClick={(row) => {
                  if ('isLoadMoreRow' in row && row.isLoadMoreRow) return;
                  navigate(getAssetUrl(row.id));
                }}
              />
            </Box>
          </Card>
        </Box>
        {renderMenu()}
        <Drawer
          anchor="left"
          open={openFilterDrawer}
          onClose={handleCloseFilterDrawer}
          PaperProps={{
            sx: { width: '30%' }
          }}
        >
          <Filters
            filterFields={criteria.filterFields}
            onFilterChange={onFilterChange}
            onSave={() => {
              handleCloseFilterDrawer();
              setView('list');
            }}
          />
        </Drawer>
      </>
    );
  else return <PermissionErrorMessage message={'no_access_assets'} />;
}

export default Assets;
