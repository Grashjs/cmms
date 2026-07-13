import { Helmet } from 'react-helmet-async';
import {
  Box,
  Button,
  Card,
  Dialog,
  DialogContent,
  DialogTitle,
  Grid,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useState } from 'react';
import { TitleContext } from '../../../contexts/TitleContext';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import { GridEnrichedColDef } from '@mui/x-data-grid/models/colDef/gridColDef';
import CustomDataGrid from '../components/CustomDatagrid';
import {
  GridActionsCellItem,
  GridRenderCellParams,
  GridRowParams,
  GridToolbar
} from '@mui/x-data-grid';
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
import { SearchCriteria } from '../../../models/owns/page';
import { isNumeric } from '../../../utils/validators';
import { useGridApiRef } from '@mui/x-data-grid-pro';
import useGridStatePersist from '../../../hooks/useGridStatePersist';

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

  const onPageSizeChange = (size: number) => {
    setCriteria({ ...criteria, pageSize: size });
  };
  const onPageChange = (number: number) => {
    setCriteria({ ...criteria, pageNum: number });
  };

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
  const columns: GridEnrichedColDef[] = [
    {
      field: 'id',
      headerName: t('id'),
      description: t('id'),
      flex: 0.5
    },
    {
      field: 'name',
      headerName: t('name'),
      description: t('name'),
      flex: 1.5,
      renderCell: (params: GridRenderCellParams<string>) => (
        <Box sx={{ fontWeight: 'bold' }}>{params.value}</Box>
      )
    },
    {
      field: 'createdBy',
      headerName: t('uploaded_by'),
      description: t('uploaded_by'),
      width: 150,
      flex: 1,
      valueGetter: (params) => getUserNameById(params.value)
    },
    {
      field: 'createdAt',
      headerName: t('uploaded_on'),
      description: t('uploaded_on'),
      width: 150,
      valueGetter: (params) => getFormattedDate(params.row.createdAt)
    },
    {
      field: 'actions',
      type: 'actions',
      headerName: t('actions'),
      description: t('actions'),
      getActions: (params: GridRowParams<File>) => {
        let actions = [
          <GridActionsCellItem
            key="rename"
            icon={<EditTwoToneIcon fontSize="small" color="primary" />}
            onClick={() => handleRename(Number(params.id))}
            label={t('rename')}
          />,
          <GridActionsCellItem
            key="delete"
            icon={<DeleteTwoToneIcon fontSize="small" color="error" />}
            onClick={() => handleOpenDelete(Number(params.id))}
            label={t('to_delete')}
          />
        ];
        if (!hasEditPermission(PermissionEntity.FILES, params.row)) {
          actions.shift();
        }
        if (!hasDeletePermission(PermissionEntity.FILES, params.row)) {
          actions.pop();
        }
        return actions;
      }
    }
  ];
  const apiRef = useGridApiRef();
  useGridStatePersist(apiRef, columns, 'file');
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
          <Helmet>
            <title>{t('files')}</title>
          </Helmet>
          <Grid
            container
            justifyContent="center"
            alignItems="stretch"
            spacing={1}
            paddingX={4}
          >
            {hasCreatePermission(PermissionEntity.FILES) && (
              <Grid
                item
                xs={12}
                display="flex"
                flexDirection="row"
                justifyContent="right"
                alignItems="center"
              >
                <Button
                  startIcon={<AddTwoToneIcon />}
                  onClick={() => setOpenAddModal(true)}
                  sx={{ mx: 6, my: 1 }}
                  variant="contained"
                >
                  {t('file')}
                </Button>
              </Grid>
            )}
            <Grid item xs={12}>
              <Card
                sx={{
                  p: 2,
                  display: 'flex',
                  flexDirection: 'row',
                  alignItems: 'center',
                  justifyContent: 'space-between'
                }}
              >
                <Box sx={{ width: '95%' }}>
                  <CustomDataGrid
                    apiRef={apiRef}
                    columns={columns}
                    pageSize={criteria.pageSize}
                    page={criteria.pageNum}
                    rows={files.content}
                    rowCount={files.totalElements}
                    pagination
                    paginationMode="server"
                    onPageSizeChange={onPageSizeChange}
                    onPageChange={onPageChange}
                    rowsPerPageOptions={[10, 20, 50]}
                    loading={loadingGet}
                    components={{
                      NoRowsOverlay: () => (
                        <NoRowsMessageWrapper
                          message={t('noRows.file.message')}
                          action={t('noRows.file.action')}
                        />
                      )
                    }}
                    onRowClick={(params: GridRowParams<File>) =>
                      window.open(params.row.url, '_blank')
                    }
                    initialState={{
                      columns: {
                        columnVisibilityModel: {}
                      }
                    }}
                  />
                </Box>
              </Card>
            </Grid>
          </Grid>
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
