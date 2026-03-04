import { Box, Button, Card, debounce, Divider, Stack } from '@mui/material';
import { useTranslation } from 'react-i18next';
import * as React from 'react';
import { useContext, useEffect, useMemo, useState } from 'react';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import { useDispatch, useSelector } from '../../../../../store';
import {
  addRequestPortal,
  deleteRequestPortal,
  editRequestPortal,
  getRequestPortals
} from '../../../../../slices/requestPortal';
import { GridEnrichedColDef } from '@mui/x-data-grid/models/colDef/gridColDef';
import {
  GridRenderCellParams,
  GridToolbar,
  GridValueGetterParams
} from '@mui/x-data-grid';
import { RequestPortal } from '../../../../../models/owns/requestPortal';
import { CustomSnackBarContext } from '../../../../../contexts/CustomSnackBarContext';
import useAuth from '../../../../../hooks/useAuth';
import { PermissionEntity } from '../../../../../models/owns/role';
import { onSearchQueryChange } from '../../../../../utils/overall';
import { SearchCriteria, SortDirection } from '../../../../../models/owns/page';
import RequestPortalModal from './RequestPortalModal';
import PermissionErrorMessage from '../../../components/PermissionErrorMessage';
import CustomDataGrid from '../../../components/CustomDatagrid';
import NoRowsMessageWrapper from '../../../components/NoRowsMessageWrapper';

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
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);

  const [criteria, setCriteria] = useState<SearchCriteria>({
    filterFields: [],
    pageSize: 10,
    pageNum: 0,
    direction: 'DESC'
  });

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

  const handleDelete = async (id: number) => {
    try {
      await dispatch(deleteRequestPortal(id));
      showSnackBar(t('request_portal_delete_success'), 'success');
    } catch (err) {
      showSnackBar(t('request_portal_delete_failure'), 'error');
    }
  };

  const onQueryChange = (event) => {
    onSearchQueryChange<RequestPortal>(event, criteria, setCriteria, [
      'title',
      'welcomeMessage'
    ]);
  };

  const debouncedQueryChange = useMemo(() => debounce(onQueryChange, 1300), []);

  const columns: GridEnrichedColDef[] = [
    {
      field: 'title',
      headerName: t('title'),
      description: t('title'),
      width: 200,
      renderCell: (params: GridRenderCellParams<string>) => (
        <Box sx={{ fontWeight: 'bold' }}>{params.value}</Box>
      )
    },
    {
      field: 'welcomeMessage',
      headerName: t('welcome_message'),
      description: t('welcome_message'),
      width: 300
    },
    {
      field: 'uuid',
      headerName: t('uuid'),
      description: t('uuid'),
      width: 150
    },
    {
      field: 'createdAt',
      headerName: t('created_at'),
      description: t('created_at'),
      width: 150,
      valueGetter: (params: GridValueGetterParams<null, RequestPortal>) => {
        const date = new Date(params.value);
        return date.toLocaleDateString();
      }
    }
  ];

  if (!hasViewPermission(PermissionEntity.SETTINGS)) {
    return <PermissionErrorMessage message={'no_access_request_portals'} />;
  }

  return (
    <>
      <Box justifyContent="center" alignItems="stretch" paddingX={4}>
        {hasCreatePermission(PermissionEntity.SETTINGS) && (
          <Box
            display="flex"
            flexDirection="row"
            justifyContent="right"
            alignItems="center"
          >
            <Button
              startIcon={<AddTwoToneIcon />}
              sx={{ my: 1 }}
              variant="contained"
              onClick={() => onOpenModal()}
            >
              {t('create_request_portal')}
            </Button>
          </Box>
        )}
        <Card
          sx={{
            py: 2,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center'
          }}
        >
          <Box sx={{ width: '95%' }}>
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
              onRowClick={({ row }) => handleEdit(row)}
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
        </Card>
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
    </>
  );
}
