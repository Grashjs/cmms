import { Helmet } from 'react-helmet-async';
import {
  Box,
  Button,
  Card,
  debounce,
  Dialog,
  DialogContent,
  DialogTitle,
  Divider,
  Drawer,
  Grid,
  Stack,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useContext, useEffect, useMemo, useState } from 'react';
import { TitleContext } from '../../../contexts/TitleContext';
import {
  addRequest,
  clearSingleRequest,
  deleteRequest,
  editRequest,
  getRequests,
  getSingleRequest
} from '../../../slices/request';
import { useDispatch, useSelector } from '../../../store';
import ConfirmDialog from '../components/ConfirmDialog';
import CustomDatagrid2, {
  CustomDatagridColumn2
} from '../components/CustomDatagrid2';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import Request from '../../../models/owns/request';
import Form from '../components/form';
import * as Yup from 'yup';
import { IField } from '../type';
import RequestDetails from './RequestDetails';
import { useNavigate, useParams } from 'react-router-dom';
import { isNumeric } from '../../../utils/validators';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import PriorityWrapper from '../components/PriorityWrapper';
import { formatSelect, formatSelectMultiple } from '../../../utils/formatters';
import useAuth from '../../../hooks/useAuth';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import { getWOBaseFields, getWOBaseValues } from '../../../utils/woBase';
import { PermissionEntity } from '../../../models/owns/role';
import PermissionErrorMessage from '../components/PermissionErrorMessage';
import NoRowsMessageWrapper from '../components/NoRowsMessageWrapper';
import {
  handleFileUpload,
  getImageAndFiles,
  onSearchQueryChange
} from '../../../utils/overall';
import {
  FilterField,
  SearchCriteria,
  SortDirection
} from '../../../models/owns/page';
import { createColumnHelper } from '@tanstack/react-table';
import useTableState from '../../../hooks/useTableState';
import _ from 'lodash';
import FilterAltTwoToneIcon from '@mui/icons-material/FilterAltTwoTone';
import EnumFilter from '../WorkOrders/Filters/EnumFilter';
import SignalCellularAltTwoToneIcon from '@mui/icons-material/SignalCellularAltTwoTone';
import CircleTwoToneIcon from '@mui/icons-material/CircleTwoTone';
import SearchInput from '../components/SearchInput';
import * as React from 'react';
import WorkOrder from '../../../models/owns/workOrder';

function Requests() {
  const { t }: { t: any } = useTranslation();
  const { setTitle } = useContext(TitleContext);
  const [openAddModal, setOpenAddModal] = useState<boolean>(false);
  const [openUpdateModal, setOpenUpdateModal] = useState<boolean>(false);
  const [openDrawer, setOpenDrawer] = useState<boolean>(false);
  const {
    companySettings,
    hasViewPermission,
    hasCreatePermission,
    getFilteredFields
  } = useAuth();
  const { workOrderRequestConfiguration } = companySettings;
  const [currentRequest, setCurrentRequest] = useState<Request>();
  const { uploadFiles, getFormattedDate } = useContext(CompanySettingsContext);
  const { requestId } = useParams();
  const dispatch = useDispatch();
  const [openDelete, setOpenDelete] = useState<boolean>(false);
  const { requests, loadingGet, singleRequest } = useSelector(
    (state) => state.requests
  );
  const [openDrawerFromUrl, setOpenDrawerFromUrl] = useState<boolean>(false);
  const defaultFilterFields: FilterField[] = [
    {
      field: 'priority',
      operation: 'in',
      values: [],
      value: '',
      enumName: 'PRIORITY'
    },
    {
      field: 'status',
      operation: 'in',
      values: ['APPROVED', 'CANCELLED', 'PENDING'],
      value: '',
      enumName: 'STATUS'
    }
  ];
  const [criteria, setCriteria] = useState<SearchCriteria>({
    filterFields: defaultFilterFields,
    pageSize: 10,
    pageNum: 0,
    direction: 'DESC'
  });

  // Mapping for column fields to API field names for sorting
  const fieldMapping: Record<string, string> = {
    customId: 'customId',
    title: 'title',
    description: 'description',
    priority: 'priority',
    createdAt: 'createdAt',
    dueDate: 'dueDate'
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
    prefix: 'requests',
    initialSorting: [],
    initialPagination: {
      pageSize: criteria.pageSize,
      pageIndex: criteria.pageNum
    },
    setCriteria,
    fieldMapping
  });
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const navigate = useNavigate();
  const handleOpenDrawer = (request: Request) => {
    setCurrentRequest(request);
    window.history.replaceState(
      null,
      'Request details',
      `/app/requests/${request.id}`
    );
    setOpenDrawer(true);
  };
  useEffect(() => {
    setTitle(t('requests'));
  }, []);
  useEffect(() => {
    if (requestId && isNumeric(requestId)) {
      dispatch(getSingleRequest(Number(requestId)));
    }
  }, [requestId]);

  useEffect(() => {
    if (hasViewPermission(PermissionEntity.REQUESTS))
      dispatch(getRequests(criteria));
  }, [criteria]);

  //see changes in ui on edit
  useEffect(() => {
    if (singleRequest || requests.content.length) {
      const currentInContent = requests.content.find(
        (request) => request.id === currentRequest?.id
      );
      const updatedRequest = currentInContent ?? singleRequest;
      if (updatedRequest) {
        if (openDrawerFromUrl) {
          setCurrentRequest(updatedRequest);
        } else {
          handleOpenDrawer(updatedRequest);
          setOpenDrawerFromUrl(true);
        }
      }
    }
    return () => {
      dispatch(clearSingleRequest());
    };
  }, [singleRequest, requests]);

  const handleDelete = (id: number) => {
    handleCloseDetails();
    dispatch(deleteRequest(id)).then(onDeleteSuccess).catch(onDeleteFailure);
    setOpenDelete(false);
  };
  const handleOpenUpdate = () => {
    setOpenUpdateModal(true);
  };
  const onCreationSuccess = () => {
    setOpenAddModal(false);
    showSnackBar(t('request_create_success'), 'success');
  };
  const onCreationFailure = (err) =>
    showSnackBar(t('request_create_failure'), 'error');
  const onEditSuccess = () => {
    setOpenUpdateModal(false);
    showSnackBar(t('changes_saved_success'), 'success');
  };
  const onEditFailure = (err) =>
    showSnackBar(t('request_edit_failure'), 'error');
  const onDeleteSuccess = () => {
    showSnackBar(t('request_delete_success'), 'success');
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('request_delete_failure'), 'error');

  const handleOpenDetails = (id: number) => {
    const foundRequest = requests.content.find((request) => request.id === id);
    if (foundRequest) {
      if (foundRequest.workOrder) {
        navigate(`/app/work-orders/${foundRequest.workOrder.id}`);
      } else {
        handleOpenDrawer(foundRequest);
      }
    }
  };
  const handleCloseDetails = () => {
    window.history.replaceState(null, 'Request', `/app/requests`);
    setOpenDrawer(false);
  };
  const formatValues = (values) => {
    const newValues = { ...values };
    newValues.primaryUser = formatSelect(newValues.primaryUser);
    newValues.location = formatSelect(newValues.location);
    newValues.team = formatSelect(newValues.team);
    newValues.asset = formatSelect(newValues.asset);
    newValues.assignedTo = formatSelectMultiple(newValues.assignedTo);
    newValues.priority = newValues.priority?.value;
    newValues.category = formatSelect(newValues.category);
    return newValues;
  };

  const columnHelper = createColumnHelper<Request>();

  const columns: CustomDatagridColumn2<Request>[] = [
    columnHelper.accessor('customId', {
      id: 'customId',
      header: () => t('id'),
      cell: (info) => info.getValue(),
      size: 80
    }),
    columnHelper.accessor('title', {
      id: 'title',
      header: () => t('title'),
      cell: (info) => (
        <Box sx={{ fontWeight: 'bold' }}>{info.getValue()}</Box>
      ),
      size: 150
    }),
    columnHelper.accessor('description', {
      id: 'description',
      header: () => t('description'),
      cell: (info) => info.getValue(),
      size: 300
    }),
    columnHelper.accessor('priority', {
      id: 'priority',
      header: () => t('priority'),
      cell: (info) => <PriorityWrapper priority={info.getValue()} />,
      size: 150
    }),
    columnHelper.display({
      id: 'status',
      header: () => t('status'),
      cell: (info) => {
        const row = info.row.original;
        return row.cancelled
          ? t('rejected')
          : row.workOrder
          ? t('approved')
          : t('pending');
      },
      size: 150
    }),
    columnHelper.accessor('dueDate', {
      id: 'dueDate',
      header: () => t('due_date'),
      cell: (info) => getFormattedDate(info.getValue()),
      size: 150
    }),
    columnHelper.accessor('createdAt', {
      id: 'createdAt',
      header: () => t('created_at'),
      cell: (info) => getFormattedDate(info.getValue()),
      size: 150
    })
  ];
  const defaultFields: Array<IField> = [...getWOBaseFields(t)];
  const defaultShape = {
    title: Yup.string().required(t('required_request_name'))
  };
  const getFieldsAndShapes = (): [Array<IField>, { [key: string]: any }] => {
    let fields = [...getFilteredFields(defaultFields)];
    let shape = { ...defaultShape };
    const fieldsToConfigure = [
      'asset',
      'location',
      'primaryUser',
      'category',
      'dueDate',
      'team'
    ];
    fieldsToConfigure.forEach((name) => {
      const fieldConfig =
        workOrderRequestConfiguration.fieldConfigurations.find(
          (fc) => fc.fieldName === name
        );
      const fieldIndexInFields = fields.findIndex(
        (field) => field.name === name
      );
      if (fieldConfig.fieldType === 'REQUIRED') {
        fields[fieldIndexInFields] = {
          ...fields[fieldIndexInFields],
          required: true
        };
        const requiredMessage = t('required_field');
        let yupSchema;
        switch (fields[fieldIndexInFields].type) {
          case 'text':
            yupSchema = Yup.string().required(requiredMessage);
            break;
          case 'date':
            yupSchema = Yup.string().required(requiredMessage);
            break;
          case 'number':
            yupSchema = Yup.number().required(requiredMessage);
            break;
          default:
            yupSchema = Yup.object().required(requiredMessage).nullable();
            break;
        }
        shape[name] = yupSchema;
      } else if (fieldConfig.fieldType === 'HIDDEN') {
        fields.splice(fieldIndexInFields, 1);
      }
    });

    return [fields, shape];
  };
  const onQueryChange = (event) => {
    onSearchQueryChange<WorkOrder>(event, criteria, setCriteria, [
      'title',
      'description'
    ]);
  };
  const debouncedQueryChange = useMemo(() => debounce(onQueryChange, 1300), []);

  const onFilterChange = (newFilters: FilterField[]) => {
    const newCriteria = { ...criteria };
    newCriteria.filterFields = newFilters;
    setCriteria(newCriteria);
  };
  const renderAddModal = () => (
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
          {t('add_request')}
        </Typography>
        <Typography variant="subtitle2">
          {t('add_request_description')}
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
            fields={getFieldsAndShapes()[0]}
            validation={Yup.object().shape(getFieldsAndShapes()[1])}
            submitText={t('add')}
            values={{}}
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              let formattedValues = formatValues(values);
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

                await dispatch(addRequest(formattedValues));
                onCreationSuccess();
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
  const renderUpdateModal = () => (
    <Dialog
      fullWidth
      maxWidth="md"
      open={openUpdateModal}
      onClose={() => setOpenUpdateModal(false)}
    >
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {t('edit_request')}
        </Typography>
        <Typography variant="subtitle2">
          {t('edit_request_description')}
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
            fields={getFieldsAndShapes()[0]}
            validation={Yup.object().shape(getFieldsAndShapes()[1])}
            submitText={t('save')}
            values={{
              ...currentRequest,
              ...getWOBaseValues(t, currentRequest)
            }}
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              let formattedValues = formatValues(values);
              try {
                const imageAndFiles = await handleFileUpload(
                  {
                    files: formattedValues.files,
                    image: formattedValues.image
                  },
                  uploadFiles
                );

                formattedValues = {
                  ...formattedValues,
                  image: imageAndFiles.image,
                  files: imageAndFiles.files
                };

                await dispatch(
                  editRequest(currentRequest?.id, formattedValues)
                );
                await onEditSuccess();
              } catch (err) {
                onEditFailure(err);
                throw err;
              }
            }}
          />
        </Box>
      </DialogContent>
    </Dialog>
  );
  if (hasViewPermission(PermissionEntity.REQUESTS))
    return (
      <>
        <Helmet>
          <title>{t('requests')}</title>
        </Helmet>
        {renderAddModal()}
        {renderUpdateModal()}
        <Box justifyContent="center" alignItems="stretch" paddingX={4}>
          {hasCreatePermission(PermissionEntity.REQUESTS) && (
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
                onClick={() => setOpenAddModal(true)}
              >
                {t('request')}
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
            <Stack
              sx={{ ml: 1 }}
              direction="row"
              spacing={1}
              justifyContent={'flex-start'}
              width={'95%'}
            >
              <EnumFilter
                filterFields={criteria.filterFields}
                onChange={onFilterChange}
                completeOptions={['NONE', 'LOW', 'MEDIUM', 'HIGH']}
                fieldName="priority"
                icon={<SignalCellularAltTwoToneIcon />}
              />
              <EnumFilter
                filterFields={criteria.filterFields}
                onChange={onFilterChange}
                completeOptions={['APPROVED', 'CANCELLED', 'PENDING']}
                fieldName="status"
                icon={<CircleTwoToneIcon />}
              />
              <SearchInput onChange={debouncedQueryChange} />
            </Stack>
            <Divider sx={{ mt: 1 }} />
            <Box sx={{ width: '95%' }}>
              <CustomDatagrid2
                columns={columns}
                data={requests.content}
                loading={loadingGet}
                pagination={pagination}
                onPaginationChange={setPagination}
                totalRows={requests.totalElements}
                pageSizeOptions={[10, 20, 50]}
                sorting={sorting}
                onSortingChange={setSorting}
                columnOrder={columnOrder}
                onColumnOrderChange={setColumnOrder}
                columnSizing={columnSizing}
                onColumnSizingChange={setColumnSizing}
                columnVisibility={columnVisibility}
                onColumnVisibilityChange={setColumnVisibility}
                onRowClick={(row) => handleOpenDetails(row.id)}
                noRowsMessage={t('noRows.request.message')}
                noRowsAction={t('noRows.request.action')}
                enableColumnReordering
                enableColumnResizing
                pinnedColumns={pinnedColumns}
                onPinnedColumnsChange={setPinnedColumns}
              />
            </Box>
          </Card>
        </Box>
        <Drawer
          anchor="right"
          open={openDrawer}
          onClose={handleCloseDetails}
          PaperProps={{
            sx: { width: { xs: '90%', sm: '70%', md: '50%' } }
          }}
        >
          <RequestDetails
            onClose={handleCloseDetails}
            request={currentRequest}
            handleOpenUpdate={handleOpenUpdate}
            handleOpenDelete={() => setOpenDelete(true)}
          />
        </Drawer>
        <ConfirmDialog
          open={openDelete}
          onCancel={() => {
            setOpenDelete(false);
            setOpenDrawer(true);
          }}
          onConfirm={() => handleDelete(currentRequest?.id)}
          confirmText={t('to_delete')}
          question={t('confirm_delete_request')}
        />
      </>
    );
  else return <PermissionErrorMessage message={'no_access_requests'} />;
}

export default Requests;
