import {
  Box,
  debounce,
  Dialog,
  DialogContent,
  DialogTitle,
  IconButton,
  Stack,
  Typography
} from '@mui/material';
import {
  addVendor,
  clearSingleVendor,
  deleteVendor,
  editVendor,
  getSingleVendor,
  getVendors
} from '../../../slices/vendor';
import { useDispatch, useSelector } from '../../../store';
import { useTranslation } from 'react-i18next';
import Form from '../components/form';
import * as Yup from 'yup';
import { IField } from '../type';
import ConfirmDialog from '../components/ConfirmDialog';
import * as React from 'react';
import { useContext, useEffect, useMemo, useState } from 'react';
import CustomDatagrid2, {
  CustomDatagridColumn2
} from '../components/CustomDatagrid2';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import {
  emailRegExp,
  isNumeric,
  phoneRegExp,
  websiteRegExp
} from '../../../utils/validators';
import { Close } from '@mui/icons-material';
import { Vendor } from '../../../models/owns/vendor';
import { useParams } from 'react-router-dom';
import useAuth from '../../../hooks/useAuth';
import { PermissionEntity } from '../../../models/owns/role';
import NoRowsMessageWrapper from '../components/NoRowsMessageWrapper';
import { SearchCriteria, SortDirection } from '../../../models/owns/page';
import { onSearchQueryChange } from '../../../utils/overall';
import SearchInput from '../components/SearchInput';
import { createColumnHelper } from '@tanstack/react-table';
import useTableState from '../../../hooks/useTableState';
import { useBrand } from '../../../hooks/useBrand';
import { getErrorMessage } from '../../../utils/api';

interface PropsType {
  values?: any;
  openModal: boolean;
  handleCloseModal: () => void;
}

const fieldMapping: Record<string, string> = {
  companyName: 'companyName',
  name: 'name',
  vendorType: 'vendorType',
  email: 'email',
  phone: 'phone',
  website: 'website',
  rate: 'rate'
};

const Vendors = ({ openModal, handleCloseModal }: PropsType) => {
  const { t }: { t: any } = useTranslation();
  const brandConfig = useBrand();
  const [isVendorDetailsOpen, setIsVendorDetailsOpen] =
    useState<boolean>(false);
  const { vendorId } = useParams();
  const dispatch = useDispatch();
  const [openDelete, setOpenDelete] = useState<boolean>(false);
  const { vendors, loadingGet, singleVendor } = useSelector(
    (state) => state.vendors
  );
  const [openDrawerFromUrl, setOpenDrawerFromUrl] = useState<boolean>(false);
  const [criteria, setCriteria] = useState<SearchCriteria>({
    filterFields: [],
    pageSize: 10,
    pageNum: 0,
    direction: 'DESC'
  });

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
    prefix: 'vendors',
    initialSorting: [],
    initialPagination: {
      pageSize: criteria.pageSize,
      pageIndex: criteria.pageNum
    },
    setCriteria,
    fieldMapping
  });
  const [currentVendor, setCurrentVendor] = useState<Vendor>();
  const [viewOrUpdate, setViewOrUpdate] = useState<'view' | 'update'>('view');
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { hasEditPermission, hasDeletePermission } = useAuth();

  const onQueryChange = (event) => {
    onSearchQueryChange<Vendor>(event, criteria, setCriteria, [
      'name',
      'vendorType',
      'companyName',
      'description'
    ]);
  };
  const debouncedQueryChange = useMemo(() => debounce(onQueryChange, 1300), []);

  const handleDelete = (id: number) => {
    handleCloseDetails();
    dispatch(deleteVendor(id)).then(onDeleteSuccess).catch(onDeleteFailure);
    setOpenDelete(false);
  };
  const onCreationSuccess = () => {
    handleCloseModal();
    showSnackBar(t('vendor_create_success'), 'success');
  };
  const onCreationFailure = (err) =>
    showSnackBar(getErrorMessage(err, t('vendor_create_failure')), 'error');
  const onEditSuccess = () => {
    setViewOrUpdate('view');
    showSnackBar(t('changes_saved_success'), 'success');
  };
  const onEditFailure = (err) =>
    showSnackBar(t('vendor_edit_failure'), 'error');
  const onDeleteSuccess = () => {
    showSnackBar(t('vendor_delete_success'), 'success');
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('vendor_delete_failure'), 'error');

  const handleOpenModal = (vendor: Vendor) => {
    setCurrentVendor(vendor);
    window.history.replaceState(
      null,
      'Vendor details',
      `/app/vendors-customers/vendors/${vendor.id}`
    );
    setIsVendorDetailsOpen(true);
  };
  const handleOpenDetails = (id: number) => {
    const foundVendor = vendors.content.find((vendor) => vendor.id === id);
    if (foundVendor) {
      handleOpenModal(foundVendor);
    }
  };
  const handleCloseDetails = () => {
    window.history.replaceState(
      null,
      'Vendors',
      `/app/vendors-customers/vendors`
    );
    setIsVendorDetailsOpen(false);
  };
  useEffect(() => {
    if (vendorId && isNumeric(vendorId)) {
      dispatch(getSingleVendor(Number(vendorId)));
    }
  }, [vendorId]);

  useEffect(() => {
    dispatch(getVendors(criteria));
  }, [criteria]);

  //see changes in ui on edit
  useEffect(() => {
    if (singleVendor || vendors.content.length) {
      const currentInContent = vendors.content.find(
        (vendor) => vendor.id === currentVendor?.id
      );
      const updatedVendor = currentInContent ?? singleVendor;
      if (updatedVendor) {
        if (openDrawerFromUrl) {
          setCurrentVendor(updatedVendor);
        } else {
          handleOpenModal(updatedVendor);
          setOpenDrawerFromUrl(true);
        }
      }
    }
    return () => {
      dispatch(clearSingleVendor());
    };
  }, [singleVendor, vendors]);

  const onPageSizeChange = (size: number) => {
    setCriteria({ ...criteria, pageSize: size });
  };
  const onPageChange = (number: number) => {
    setCriteria({ ...criteria, pageNum: number });
  };

  let fields: Array<IField> = [
    {
      name: 'companyName',
      type: 'text',
      label: t('company_name'),
      placeholder: brandConfig.shortName,
      required: true
    },
    {
      name: 'address',
      type: 'text',
      label: t('address'),
      placeholder: t('address')
    },
    {
      name: 'phone',
      type: 'text',
      label: t('phone'),
      placeholder: '+00212611223344'
    },
    {
      name: 'website',
      type: 'text',
      label: t('website'),
      placeholder: 'https://web-site.com'
    },
    {
      name: 'name',
      type: 'text',
      label: t('name'),
      placeholder: 'John Doe',
      required: true
    },
    {
      name: 'email',
      type: 'text',
      label: t('email'),
      placeholder: 'john.doe@gmail.com'
    },
    {
      name: 'vendorType',
      type: 'text',
      label: t('vendor_type'),
      placeholder: t('vendor_type_description')
    },
    {
      name: 'description',
      type: 'text',
      label: t('Description'),
      multiple: true,
      placeholder: t('description')
    },
    {
      name: 'rate',
      type: 'number',
      label: t('hourly_rate'),
      placeholder: t('hourly_rate'),
      icon: '$'
    }
  ];

  const shape = {
    companyName: Yup.string().required(t('required_company_name')),
    rate: Yup.number(),
    phone: Yup.string().matches(phoneRegExp, t('invalid_phone')).nullable(),
    name: Yup.string().required(t('required_name')),
    website: Yup.string()
      .matches(websiteRegExp, t('invalid_website'))
      .nullable(),
    email: Yup.string().matches(emailRegExp, t('invalid_email')).nullable()
  };

  const columnHelper = createColumnHelper<Vendor>();

  const columns: CustomDatagridColumn2<Vendor>[] = [
    columnHelper.accessor('companyName', {
      id: 'companyName',
      header: () => t('company_name'),
      cell: (info) => (
        <Box sx={{ fontWeight: 'bold' }}>{info.getValue()}</Box>
      ),
      size: 150
    }),
    columnHelper.accessor('address', {
      id: 'address',
      header: () => t('address'),
      cell: (info) => info.getValue(),
      size: 150
    }),
    columnHelper.accessor('phone', {
      id: 'phone',
      header: () => t('phone'),
      cell: (info) => info.getValue(),
      size: 150
    }),
    columnHelper.accessor('website', {
      id: 'website',
      header: () => t('website'),
      cell: (info) => info.getValue(),
      size: 150
    }),
    columnHelper.accessor('name', {
      id: 'name',
      header: () => t('name'),
      cell: (info) => info.getValue(),
      size: 150
    }),
    columnHelper.accessor('email', {
      id: 'email',
      header: () => t('email'),
      cell: (info) => info.getValue(),
      size: 150
    }),
    columnHelper.accessor('vendorType', {
      id: 'vendorType',
      header: () => t('vendor_type'),
      cell: (info) => info.getValue(),
      size: 150
    }),
    columnHelper.accessor('description', {
      id: 'description',
      header: () => t('description'),
      cell: (info) => info.getValue(),
      size: 300
    }),
    columnHelper.accessor('rate', {
      id: 'rate',
      header: () => t('hourly_rate'),
      cell: (info) => info.getValue(),
      size: 150
    })
  ];

  // const searchFilterProperties = ['name', 'companyName', 'vendorType', 'email'];
  const fieldsToRender = [
    {
      label: t('address'),
      value: currentVendor?.address
    },
    {
      label: t('phone'),
      value: currentVendor?.phone
    },
    {
      label: t('email'),
      value: currentVendor?.email
    },
    {
      label: t('type'),
      value: currentVendor?.vendorType
    },
    {
      label: t('contact_name'),
      value: currentVendor?.name
    }
  ];
  const renderKeyAndValue = (key: string, value: string) => {
    if (value)
      return (
        <>
          <Typography variant="subtitle1">{key}</Typography>
          <Typography variant="h5" sx={{ mb: 1 }}>
            {value}
          </Typography>
        </>
      );
  };
  const RenderVendorsAddModal = () => (
    <Dialog fullWidth maxWidth="md" open={openModal} onClose={handleCloseModal}>
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {t('add_vendor')}
        </Typography>
        <Typography variant="subtitle2">
          {t('add_vendor_description')}
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
            fields={fields}
            validation={Yup.object().shape(shape)}
            submitText={t('add')}
            values={{}}
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              const formattedValues = {
                ...values,
                rate: Number(values.rate)
              };
              return dispatch(addVendor(formattedValues))
                .then(onCreationSuccess)
                .catch(onCreationFailure);
            }}
          />
        </Box>
      </DialogContent>
    </Dialog>
  );

  const RenderVendorsList = () => (
    <Box
      sx={{
        width: '95%'
      }}
    >
      <CustomDatagrid2
        columns={columns}
        data={vendors.content}
        loading={loadingGet}
        pagination={pagination}
        onPaginationChange={setPagination}
        totalRows={vendors.totalElements}
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
        noRowsMessage={t('noRows.vendor.message')}
        noRowsAction={t('noRows.vendor.action')}
        enableColumnReordering
        enableColumnResizing
        pinnedColumns={pinnedColumns}
        onPinnedColumnsChange={setPinnedColumns}
      />
    </Box>
  );

  const ModalVendorDetails = () => (
    <Dialog
      fullWidth
      maxWidth="sm"
      open={isVendorDetailsOpen}
      onClose={handleCloseDetails}
    >
      <DialogTitle
        sx={{
          p: 3,
          display: 'flex',
          flexDirection: 'row',
          justifyContent: 'space-between'
        }}
      >
        <Box sx={{ display: 'flex', flexDirection: 'row' }}>
          {viewOrUpdate === 'view' ? (
            hasEditPermission(
              PermissionEntity.VENDORS_AND_CUSTOMERS,
              currentVendor
            ) && (
              <Typography
                onClick={() => setViewOrUpdate('update')}
                style={{ cursor: 'pointer' }}
                variant="subtitle1"
                mr={2}
              >
                {t('edit')}
              </Typography>
            )
          ) : (
            <Typography
              onClick={() => setViewOrUpdate('view')}
              style={{ cursor: 'pointer' }}
              variant="subtitle1"
              mr={2}
            >
              {t('go_back')}
            </Typography>
          )}
          {hasDeletePermission(
            PermissionEntity.VENDORS_AND_CUSTOMERS,
            currentVendor
          ) && (
            <Typography
              onClick={() => {
                setIsVendorDetailsOpen(false);
                setOpenDelete(true);
              }}
              variant="subtitle1"
              style={{ cursor: 'pointer' }}
            >
              {t('to_delete')}
            </Typography>
          )}
        </Box>
        <IconButton
          aria-label="close"
          onClick={() => {
            setIsVendorDetailsOpen(false);
          }}
          sx={{
            position: 'absolute',
            right: 8,
            top: 8,
            color: (theme) => theme.palette.grey[500]
          }}
        >
          <Close />
        </IconButton>
      </DialogTitle>

      <DialogContent
        dividers
        sx={{
          p: 3
        }}
      >
        {viewOrUpdate === 'view' ? (
          <Box>
            <Typography variant="h4" sx={{ textAlign: 'center' }} gutterBottom>
              {currentVendor?.companyName}
            </Typography>
            <Typography variant="subtitle1" sx={{ textAlign: 'center', mb: 3 }}>
              {currentVendor?.description}
            </Typography>
            {fieldsToRender.map((field) =>
              renderKeyAndValue(field.label, field.value)
            )}
            {currentVendor?.website && (
              <>
                <Typography variant="subtitle1">{t('website')}</Typography>
                <Typography variant="h5" sx={{ mb: 1 }}>
                  <a
                    href={
                      currentVendor.website.toLowerCase().startsWith('https')
                        ? currentVendor.website
                        : `https://${currentVendor.website}`
                    }
                  >
                    {currentVendor.website}
                  </a>
                </Typography>
              </>
            )}
          </Box>
        ) : (
          <Box>
            <Form
              fields={fields}
              validation={Yup.object().shape(shape)}
              submitText={t('save')}
              values={currentVendor || {}}
              onChange={({ field, e }) => {}}
              onSubmit={async (values) => {
                const formattedValues = values.rate
                  ? {
                      ...values,
                      rate: Number(values.rate)
                    }
                  : values;
                return dispatch(editVendor(currentVendor.id, formattedValues))
                  .then(onEditSuccess)
                  .catch(onEditFailure);
              }}
            />
          </Box>
        )}
      </DialogContent>
    </Dialog>
  );

  return (
    <Box
      sx={{
        py: 2,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        width: '100%'
      }}
    >
      <ModalVendorDetails />
      <RenderVendorsAddModal />
      <Stack direction="row" width="95%">
        <Box sx={{ my: 0.5 }}>
          <SearchInput onChange={debouncedQueryChange} />
        </Box>
      </Stack>
      {RenderVendorsList()}
      <ConfirmDialog
        open={openDelete}
        onCancel={() => {
          setOpenDelete(false);
          setIsVendorDetailsOpen(true);
        }}
        onConfirm={() => handleDelete(currentVendor?.id)}
        confirmText={t('to_delete')}
        question={t('confirm_delete_vendor')}
      />
    </Box>
  );
};

export default Vendors;
