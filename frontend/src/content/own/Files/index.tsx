import {
  Box,
  Button,
  Card,
  debounce,
  Dialog,
  DialogContent,
  DialogTitle,
  Divider,
  Grid,
  Stack,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useMemo, useRef, useState } from 'react';
import { TitleContext } from '../../../contexts/TitleContext';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import CustomDatagrid2, {
  CustomDatagridColumn2
} from '../components/CustomDatagrid2';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import { useDispatch, useSelector } from '../../../store';
import {
  addFiles,
  clearSingleFile,
  deleteFile,
  editFile,
  getFiles,
  getSingleFile
} from '../../../slices/file';
import { IField } from '../type';
import Form from '../components/form';
import * as Yup from 'yup';
import File from '../../../models/owns/file';
import useAuth from '../../../hooks/useAuth';
import { PermissionEntity } from '../../../models/owns/role';
import PermissionErrorMessage from '../components/PermissionErrorMessage';
import { PlanFeature } from '../../../models/owns/subscriptionPlan';
import FeatureErrorMessage from '../components/FeatureErrorMessage';
import ConfirmDialog from '../components/ConfirmDialog';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import NoRowsMessageWrapper from '../components/NoRowsMessageWrapper';
import { useParams } from 'react-router-dom';
import { FilterField, SearchCriteria } from '../../../models/owns/page';
import { isNumeric } from '../../../utils/validators';
import { createColumnHelper } from '@tanstack/react-table';
import useTableState from '../../../hooks/useTableState';
import SearchInput from '../components/SearchInput';
import FileFilters from './Filters/FileFilters';

function Files() {
  const { t }: { t: any } = useTranslation();
  const { setTitle } = useContext(TitleContext);
  const { getFormattedDate, getUserNameById } = useContext(
    CompanySettingsContext
  );
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { files, loadingGet, singleFile } = useSelector((state) => state.files);
  const [openDrawerFromUrl, setOpenDrawerFromUrl] = useState<boolean>(false);
  const [criteria, setCriteria] = useState<SearchCriteria>({
    filterFields: [],
    pageSize: 10,
    pageNum: 0,
    direction: 'DESC'
  });
  const { fileId } = useParams();
  const [openAddModal, setOpenAddModal] = useState<boolean>(false);
  const [openUpdateModal, setOpenUpdateModal] = useState<boolean>(false);
  const [openDelete, setOpenDelete] = useState<boolean>(false);
  const [currentFile, setCurrentFile] = useState<File>();

  const fieldMapping: Record<string, string> = {
    id: 'id',
    name: 'name',
    createdBy: 'createdBy',
    createdAt: 'createdAt'
  };

  // Use the table state hook for TanStack Table
  const {
    sorting,
    setSorting,
    pagination,
    setPagination,
    columnOrder,
    setColumnOrder,
    columnSizing,
    setColumnSizing,
    columnVisibility,
    setColumnVisibility,
    pinnedColumns,
    setPinnedColumns
  } = useTableState({
    prefix: 'files',
    initialSorting: [],
    initialPagination: {
      pageSize: criteria.pageSize,
      pageIndex: criteria.pageNum
    },
    setCriteria,
    fieldMapping
  });

  // The debounced handler below is memoized once, so it must not close over `criteria`.
  // Parts.tsx does exactly that and gets away with it only because it has no other
  // filters; here every keystroke would otherwise restore the filter state from mount.
  const criteriaRef = useRef(criteria);
  criteriaRef.current = criteria;

  // Narrowing the result set has to go back to the first page — staying on page 3 of the
  // unfiltered list shows an empty table, which reads like "nothing found". pageNum is set
  // in the same update as the filters so the pagination effect in useTableState sees no
  // change and does not trigger a second request.
  const applyFilterFields = (newFilterFields: FilterField[]) => {
    setPagination((prev) => ({ ...prev, pageIndex: 0 }));
    setCriteria((prev) => ({
      ...prev,
      filterFields: newFilterFields,
      pageNum: 0
    }));
  };

  const onQueryChange = (event) => {
    const query = event.target.value;
    const others = criteriaRef.current.filterFields.filter(
      (filterField) => filterField.field !== 'name'
    );
    applyFilterFields(
      query
        ? [...others, { field: 'name', operation: 'cn' as const, value: query }]
        : others
    );
  };
  // 400 ms, not the 1300 ms the other list pages use — at 1.3 s the list feels stuck.
  const debouncedQueryChange = useMemo(() => debounce(onQueryChange, 400), []);

  const handleOpenDelete = (id: number) => {
    setCurrentFile(files.content.find((file) => file.id === id));
    setOpenDelete(true);
  };
  const handleDelete = (id: number) => {
    dispatch(deleteFile(id)).then(onDeleteSuccess).catch(onDeleteFailure);
    setOpenDelete(false);
  };

  const handleRename = (id: number) => {
    setCurrentFile(files.content.find((file) => file.id === id));
    setOpenUpdateModal(true);
  };
  const onDeleteSuccess = () => {
    showSnackBar(t('file_delete_success'), 'success');
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('file_delete_failure'), 'error');

  const {
    hasViewPermission,
    hasViewOtherPermission,
    hasEditPermission,
    hasCreatePermission,
    hasDeletePermission,
    hasFeature
  } = useAuth();
  const dispatch = useDispatch();
  useEffect(() => {
    setTitle(t('files'));
  }, []);

  useEffect(() => {
    if (fileId && isNumeric(fileId)) {
      dispatch(getSingleFile(Number(fileId)));
    }
  }, [fileId]);

  useEffect(() => {
    if (hasViewPermission(PermissionEntity.FILES)) dispatch(getFiles(criteria));
  }, [criteria]);

  //see changes in ui on edit
  useEffect(() => {
    if (singleFile || files.content.length) {
      const currentInContent = files.content.find(
        (file) => file.id === currentFile?.id
      );
      const updatedFile = currentInContent ?? singleFile;
      if (updatedFile) {
        if (openDrawerFromUrl) {
          setCurrentFile(updatedFile);
        } else {
          //TODO
          // handleOpenDrawer(updatedFile);
          setOpenDrawerFromUrl(true);
        }
      }
    }
    return () => {
      dispatch(clearSingleFile());
    };
  }, [singleFile, files]);

  const columnHelper = createColumnHelper<File>();

  const columns: CustomDatagridColumn2<File>[] = [
    columnHelper.accessor('id', {
      id: 'id',
      header: () => t('id'),
      cell: (info) => info.getValue() || '',
      size: 80
    }),
    columnHelper.accessor('name', {
      id: 'name',
      header: () => t('name'),
      cell: (info) => <Box sx={{ fontWeight: 'bold' }}>{info.getValue()}</Box>,
      size: 200
    }),
    columnHelper.display({
      id: 'linkedTo',
      header: () => t('linked_to'),
      // Plain text, not links: the row click already opens the file, and a link inside a
      // clickable row swallows the click on some browsers.
      cell: (info) => {
        const file = info.row.original;
        const labels = [
          ...(file.assets ?? []).map((asset) => asset.name),
          ...(file.workOrders ?? []).map(
            (workOrder) => workOrder.customId ?? workOrder.title
          )
        ].filter(Boolean);
        return labels.length ? labels.join(', ') : '';
      },
      size: 200
    }),
    columnHelper.accessor('createdBy', {
      id: 'createdBy',
      header: () => t('uploaded_by'),
      cell: (info) => getUserNameById(info.getValue()),
      size: 150
    }),
    columnHelper.accessor('createdAt', {
      id: 'createdAt',
      header: () => t('uploaded_on'),
      cell: (info) => getFormattedDate(info.getValue()),
      size: 150
    }),
    columnHelper.display({
      id: 'actions',
      header: () => t('actions'),
      cell: (info) => {
        const file = info.row.original;
        const canEdit = hasEditPermission(PermissionEntity.FILES, file);
        const canDelete = hasDeletePermission(PermissionEntity.FILES, file);

        if (!canEdit && !canDelete) return null;

        return (
          <Stack direction="row" spacing={1}>
            {canEdit && (
              <EditTwoToneIcon
                fontSize="small"
                color="primary"
                sx={{ cursor: 'pointer' }}
                onClick={(e) => {
                  e.stopPropagation();
                  handleRename(file.id);
                }}
              />
            )}
            {canDelete && (
              <DeleteTwoToneIcon
                fontSize="small"
                color="error"
                sx={{ cursor: 'pointer' }}
                onClick={(e) => {
                  e.stopPropagation();
                  handleOpenDelete(file.id);
                }}
              />
            )}
          </Stack>
        );
      },
      size: 100
    })
  ];
  const fields: Array<IField> = [
    {
      name: 'files',
      type: 'file',
      label: t('files'),
      fileType: 'file',
      multiple: true
    }
  ];
  const updateFields: Array<IField> = [
    {
      name: 'name',
      type: 'text',
      label: t('name'),
      required: true
    }
  ];
  const shape = {
    files: Yup.array().required(t('required_files'))
  };
  const updateShape = {
    name: Yup.string().required(t('required_file_name'))
  };
  const renderAddModal = () => {
    return (
      <Dialog
        fullWidth
        maxWidth="sm"
        open={openAddModal}
        onClose={() => setOpenAddModal(false)}
      >
        <DialogTitle
          sx={{
            p: 3
          }}
        >
          <Typography variant="h4" gutterBottom>
            {t('add_files')}
          </Typography>
        </DialogTitle>
        <DialogContent
          dividers
          sx={{
            p: 3
          }}
        >
          <Form
            fields={fields}
            validation={Yup.object().shape(shape)}
            submitText={t('add')}
            values={{}}
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              return dispatch(addFiles(values.files)).then(() =>
                setOpenAddModal(false)
              );
            }}
          />
        </DialogContent>
      </Dialog>
    );
  };
  const renderUpdateModal = () => {
    return (
      <Dialog
        fullWidth
        maxWidth="sm"
        open={openUpdateModal}
        onClose={() => setOpenUpdateModal(false)}
      >
        <DialogTitle
          sx={{
            p: 3
          }}
        >
          <Typography variant="h4" gutterBottom>
            {t('edit_file')}
          </Typography>
        </DialogTitle>
        <DialogContent
          dividers
          sx={{
            p: 3
          }}
        >
          <Form
            fields={updateFields}
            validation={Yup.object().shape(updateShape)}
            submitText={t('save')}
            values={{ ...currentFile }}
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              return dispatch(
                editFile(currentFile.id, { ...currentFile, name: values.name })
              ).then(() => setOpenUpdateModal(false));
            }}
          />
        </DialogContent>
      </Dialog>
    );
  };
  if (hasFeature(PlanFeature.FILE)) {
    if (hasViewPermission(PermissionEntity.FILES))
      return (
        <>
<Box justifyContent="center" alignItems="stretch" paddingX={4}>
            {hasCreatePermission(PermissionEntity.FILES) && (
              <Box
                display="flex"
                flexDirection="row"
                justifyContent="right"
                alignItems="center"
              >
                <Button
                  startIcon={<AddTwoToneIcon />}
                  onClick={() => setOpenAddModal(true)}
                  sx={{ my: 1 }}
                  variant="contained"
                >
                  {t('file')}
                </Button>
              </Box>
            )}
            <Card
              sx={{
                py: 2,
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'space-between'
              }}
            >
              <Stack
                sx={{ ml: 1 }}
                direction="row"
                spacing={1}
                justifyContent="flex-start"
                width="95%"
              >
                <FileFilters
                  filterFields={criteria.filterFields}
                  onChange={applyFilterFields}
                  showCreatedBy={hasViewOtherPermission(PermissionEntity.FILES)}
                />
                <SearchInput onChange={debouncedQueryChange} />
              </Stack>
              <Divider sx={{ mt: 1, width: '95%' }} />
              <Box sx={{ width: '95%' }}>
                <CustomDatagrid2
                  columns={columns}
                  data={files.content}
                  loading={loadingGet}
                  pagination={pagination}
                  onPaginationChange={setPagination}
                  totalRows={files.totalElements}
                  pageSizeOptions={[10, 20, 50]}
                  sorting={sorting}
                  onSortingChange={setSorting}
                  columnOrder={columnOrder}
                  onColumnOrderChange={setColumnOrder}
                  columnSizing={columnSizing}
                  onColumnSizingChange={setColumnSizing}
                  columnVisibility={columnVisibility}
                  onColumnVisibilityChange={setColumnVisibility}
                  onRowClick={(row) => window.open(row.url, '_blank')}
                  noRowsMessage={t('noRows.file.message')}
                  noRowsAction={t('noRows.file.action')}
                  enableColumnReordering
                  enableColumnResizing
                  pinnedColumns={pinnedColumns}
                  onPinnedColumnsChange={setPinnedColumns}
                />
              </Box>
            </Card>
          </Box>
          <ConfirmDialog
            open={openDelete}
            onCancel={() => {
              setOpenDelete(false);
            }}
            onConfirm={() => handleDelete(currentFile?.id)}
            confirmText={t('to_delete')}
            question={t('confirm_delete_file')}
          />
          {renderAddModal()}
          {renderUpdateModal()}
        </>
      );
    else return <PermissionErrorMessage message={'no_access_files'} />;
  } else return <FeatureErrorMessage message={'upgrade_files'} />;
}

export default Files;
