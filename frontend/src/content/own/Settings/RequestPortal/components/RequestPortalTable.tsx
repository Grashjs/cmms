import { Box, Button, IconButton, Link, Stack } from '@mui/material';
import { useTranslation } from 'react-i18next';
import * as React from 'react';
import { useContext, useEffect, useState } from 'react';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import ShareTwoToneIcon from '@mui/icons-material/ShareTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import { useDispatch, useSelector } from '../../../../../store';
import {
  addRequestPortal,
  deleteRequestPortal,
  editRequestPortal,
  getRequestPortals
} from '../../../../../slices/requestPortal';
import { GridEnrichedColDef } from '@mui/x-data-grid/models/colDef/gridColDef';
import { GridRenderCellParams } from '@mui/x-data-grid';
import { RequestPortal } from '../../../../../models/owns/requestPortal';
import { CustomSnackBarContext } from '../../../../../contexts/CustomSnackBarContext';
import useAuth from '../../../../../hooks/useAuth';
import { PermissionEntity } from '../../../../../models/owns/role';
import { onSearchQueryChange } from '../../../../../utils/overall';
import { SearchCriteria, SortDirection } from '../../../../../models/owns/page';
import RequestPortalModal from './RequestPortalModal';
import SharePortalModal from './SharePortalModal';
import ConfirmDialog from '../../../components/ConfirmDialog';
import PermissionErrorMessage from '../../../components/PermissionErrorMessage';
import CustomDataGrid from '../../../components/CustomDatagrid';
import NoRowsMessageWrapper from '../../../components/NoRowsMessageWrapper';
import { CompanySettingsContext } from '../../../../../contexts/CompanySettingsContext';

interface RequestPortalTableProps {
  openModal: boolean;
  currentPortal?: RequestPortal;
  onCloseModal: () => void;
  onOpenModal: (portal?: RequestPortal) => void;
}

export default function RequestPortalTable({
  openModal,
  currentPortal,
  onCloseModal,
  onOpenModal
}: RequestPortalTableProps) {
  const { t }: { t: any } = useTranslation();
  const { hasViewPermission, hasCreatePermission, hasEditPermission } =
    useAuth();
  const { requestPortals, loadingGet } = useSelector(
    (state) => state.requestPortals
  );
  const { getFormattedDate } = useContext(CompanySettingsContext);
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);

  const [criteria, setCriteria] = useState<SearchCriteria>({
    filterFields: [],
    pageSize: 10,
    pageNum: 0,
    direction: 'DESC'
  });

  // Dialog states
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);
  const [shareModalOpen, setShareModalOpen] = useState(false);
  const [selectedPortal, setSelectedPortal] = useState<RequestPortal | null>(
    null
  );

  useEffect(() => {
    if (hasViewPermission(PermissionEntity.SETTINGS)) {
      dispatch(getRequestPortals(criteria));
    }
  }, [criteria]);

  const onPageSizeChange = (size: number) => {
    setCriteria({ ...criteria, pageSize: size });
  };

  const onPageChange = (number: number) => {
    setCriteria({ ...criteria, pageNum: number });
  };

  const handleEdit = (portal: RequestPortal) => {
    onOpenModal(portal);
  };

  const handleDelete = async () => {
    if (!selectedPortal) return;
    try {
      await dispatch(deleteRequestPortal(selectedPortal.id));
      showSnackBar(t('request_portal_delete_success'), 'success');
      setConfirmDeleteOpen(false);
      setSelectedPortal(null);
    } catch (err) {
      showSnackBar(t('request_portal_delete_failure'), 'error');
    }
  };

  const handleShare = (portal: RequestPortal) => {
    setSelectedPortal(portal);
    setShareModalOpen(true);
  };

  const openDeleteConfirm = (
    portal: RequestPortal,
    event: React.MouseEvent
  ) => {
    event.stopPropagation();
    setSelectedPortal(portal);
    setConfirmDeleteOpen(true);
  };

  const openShareDialog = (portal: RequestPortal, event: React.MouseEvent) => {
    event.stopPropagation();
    handleShare(portal);
  };

  const onQueryChange = (event) => {
    onSearchQueryChange<RequestPortal>(event, criteria, setCriteria, [
      'title',
      'welcomeMessage'
    ]);
  };

  const columns: GridEnrichedColDef<RequestPortal>[] = [
    {
      field: 'title',
      headerName: t('title'),
      description: t('title'),
      width: 200,
      renderCell: (params) => (
        <Link
          href={`/request-portal/${params.row.uuid}`}
          sx={{ fontWeight: 'bold' }}
          onClick={(event) => event.stopPropagation()}
        >
          {params.value}
        </Link>
      )
    },
    {
      field: 'asset',
      headerName: t('asset'),
      width: 300,
      valueGetter: (params) =>
        params.row.fields.find((field) => field.type === 'ASSET')?.asset?.name
    },
    {
      field: 'location',
      headerName: t('location'),
      width: 300,
      valueGetter: (params) =>
        params.row.fields.find((field) => field.type === 'LOCATION')?.location
          ?.name
    },
    {
      field: 'createdAt',
      headerName: t('created_at'),
      description: t('created_at'),
      width: 150,
      valueGetter: (params) => {
        return getFormattedDate(params.row.createdAt);
      }
    },
    {
      field: 'actions',
      headerName: t('actions'),
      width: 150,
      sortable: false,
      filterable: false,
      disableColumnMenu: true,
      renderCell: (params: GridRenderCellParams<RequestPortal>) => (
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          <IconButton
            size="small"
            color="primary"
            onClick={(e) => openShareDialog(params.row, e)}
            title={t('share')}
          >
            <ShareTwoToneIcon />
          </IconButton>
          <IconButton
            size="small"
            color="error"
            onClick={(e) => openDeleteConfirm(params.row, e)}
            title={t('delete')}
          >
            <DeleteTwoToneIcon />
          </IconButton>
        </Box>
      )
    }
  ];

  if (!hasViewPermission(PermissionEntity.SETTINGS)) {
    return <PermissionErrorMessage message={'no_access_request_portals'} />;
  }

  return (
    <>
      <Box justifyContent="center" p={4}>
        {hasCreatePermission(PermissionEntity.SETTINGS) && (
          <Box display="flex" flexDirection="row" alignItems="center">
            <Button
              startIcon={<AddTwoToneIcon />}
              variant="contained"
              onClick={() => onOpenModal()}
            >
              {t('create_request_portal')}
            </Button>
          </Box>
        )}
        <Box sx={{ mt: 2, width: '95%' }}>
          <CustomDataGrid
            columns={columns}
            loading={loadingGet}
            pageSize={criteria.pageSize}
            page={criteria.pageNum}
            rows={requestPortals.content}
            rowCount={requestPortals.totalElements}
            pagination
            paginationMode="server"
            onPageSizeChange={onPageSizeChange}
            onPageChange={onPageChange}
            rowsPerPageOptions={[10, 20, 50]}
            onRowClick={(row, event) => {
              handleEdit(row);
            }}
            components={{
              NoRowsOverlay: () => (
                <NoRowsMessageWrapper
                  message={t('noRows.request_portal.message')}
                  action={t('noRows.request_portal.action')}
                />
              )
            }}
            onSortModelChange={(model) => {
              if (model.length === 0) {
                setCriteria({
                  ...criteria,
                  sortField: undefined,
                  direction: undefined
                });
                return;
              }

              const fieldMapping: Record<string, string> = {
                title: 'title',
                welcomeMessage: 'welcomeMessage',
                uuid: 'uuid',
                createdAt: 'createdAt'
              };

              const field = model[0].field;
              const mappedField = fieldMapping[field];

              if (!mappedField) return;

              setCriteria({
                ...criteria,
                sortField: mappedField,
                direction: (model[0].sort?.toUpperCase() ||
                  'ASC') as SortDirection
              });
            }}
            sortingMode={'server'}
            initialState={{
              columns: {
                columnVisibilityModel: {}
              }
            }}
          />
        </Box>
      </Box>
      <RequestPortalModal
        open={openModal}
        onClose={onCloseModal}
        portal={currentPortal}
        onSubmit={async (values, action) => {
          if (action === 'create') {
            await dispatch(addRequestPortal(values));
            showSnackBar(t('request_portal_create_success'), 'success');
          } else {
            await dispatch(editRequestPortal(currentPortal.id, values));
            showSnackBar(t('request_portal_edit_success'), 'success');
          }
          onCloseModal();
        }}
      />

      {/* Confirm Delete Dialog */}
      <ConfirmDialog
        open={confirmDeleteOpen}
        onCancel={() => {
          setConfirmDeleteOpen(false);
          setSelectedPortal(null);
        }}
        onConfirm={handleDelete}
        confirmText={t('delete')}
        question={t('confirm_delete_request_portal')}
      />

      <SharePortalModal
        open={shareModalOpen}
        onClose={() => {
          setShareModalOpen(false);
          setSelectedPortal(null);
        }}
        portal={selectedPortal}
      />
    </>
  );
}
