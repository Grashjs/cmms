import {
  Box,
  debounce,
  Dialog,
  DialogContent,
  DialogTitle,
  Drawer,
  Stack,
  Switch,
  Typography,
  useTheme
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import CustomDatagrid2, {
  CustomDatagridColumn2
} from '../components/CustomDatagrid2';
import * as React from 'react';
import { useContext, useEffect, useMemo, useState } from 'react';
import UserDetailsDrawer from './UserDetailsDrawer';
import User from '../../../models/owns/user';
import { useParams } from 'react-router-dom';
import { isNumeric } from 'src/utils/validators';
import { useDispatch, useSelector } from '../../../store';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import {
  clearSingleUser,
  disableUser,
  editUser,
  editUserRole,
  getSingleUser,
  getUsers
} from '../../../slices/user';
import { OwnUser } from '../../../models/user';
import { PermissionEntity, Role } from '../../../models/owns/role';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import useAuth from '../../../hooks/useAuth';
import Form from '../components/form';
import * as Yup from 'yup';
import { IField } from '../type';
import { formatSelect } from '../../../utils/formatters';
import { CompanySettingsContext } from '../../../contexts/CompanySettingsContext';
import { SearchCriteria, SortDirection } from '../../../models/owns/page';
import { onSearchQueryChange } from '../../../utils/overall';
import SearchInput from '../components/SearchInput';
import CancelIcon from '@mui/icons-material/Cancel';
import ConfirmDialog from '../components/ConfirmDialog';
import InviteUserDialog from './components/InviteUserDialog';
import { isEmailVerificationEnabled } from '../../../config';
import { createColumnHelper } from '@tanstack/react-table';
import useTableState from '../../../hooks/useTableState';

const fieldMapping: Record<string, string> = {
  name: 'firstName',
  email: 'email',
  phone: 'phone',
  jobTitle: 'jobTitle',
  role: 'role.name',
  rate: 'rate',
  lastLogin: 'lastLogin'
};

interface PropsType {
  values?: any;
  openModal: boolean;
  handleCloseModal: () => void;
}

const People = ({ openModal, handleCloseModal }: PropsType) => {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const [currentUser, setCurrentUser] = useState<OwnUser>();
  const [detailDrawerOpen, setDetailDrawerOpen] = useState(false);
  const { peopleId } = useParams();
  const { hasEditPermission, user } = useAuth();
  const [enabledOnly, setEnabledOnly] = useState<boolean>(true);
  const { users, loadingGet, singleUser } = useSelector((state) => state.users);
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
    prefix: 'people',
    initialSorting: [],
    initialPagination: {
      pageSize: criteria.pageSize,
      pageIndex: criteria.pageNum
    },
    setCriteria,
    fieldMapping
  });

  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { getFormattedCurrency, getFormattedDate } = useContext(
    CompanySettingsContext
  );
  const [openUpdateModal, setOpenUpdateModal] = useState<boolean>(false);
  const [openDisableModal, setOpenDisableModal] = useState<boolean>(false);

  const onQueryChange = (event) => {
    onSearchQueryChange<User>(event, criteria, setCriteria, [
      'firstName',
      'lastName',
      'email',
      'phone',
      'jobTitle'
    ]);
  };
  const debouncedQueryChange = useMemo(() => debounce(onQueryChange, 1300), []);

  const onEditSuccess = () => {
    setOpenUpdateModal(false);
    showSnackBar(t('changes_saved_success'), 'success');
  };
  const onEditFailure = (err) =>
    showSnackBar(t("The User couldn't be edited"), 'error');

  const handleOpenDrawer = (user: OwnUser) => {
    setCurrentUser(user);
    window.history.replaceState(
      null,
      'User details',
      `/app/people-teams/people/${user.id}`
    );
    setDetailDrawerOpen(true);
  };
  const handleOpenDetails = (id: number) => {
    const foundUser = users.content.find((user) => user.id === id);
    if (foundUser) {
      handleOpenDrawer(foundUser);
    }
  };
  const handleOpenUpdate = (id: number) => {
    const foundUser = users.content.find((user) => user.id === id);
    if (foundUser) {
      setCurrentUser(foundUser);
      setOpenUpdateModal(true);
    }
  };
  const handleOpenDisable = (id: number) => {
    const foundUser = users.content.find((user) => user.id === id);
    if (foundUser) {
      setCurrentUser(foundUser);
      setOpenDisableModal(true);
    }
  };
  const handleCloseDetails = () => {
    window.history.replaceState(null, 'User', `/app/people-teams/people`);
    setDetailDrawerOpen(false);
  };
  const defautfields: Array<IField> = [
    {
      name: 'rate',
      type: 'number',
      label: t('hourly_rate')
    },
    {
      name: 'role',
      type: 'select',
      type2: 'role',
      label: t('role')
    },
    ...(isEmailVerificationEnabled
      ? []
      : [
          {
            name: 'password',
            type: 'text',
            label: t('password_leave_empty_if_you_dont_want_to_change')
          } as IField
        ])
  ];
  const getFields = () => {
    let fields = [...defautfields];
    if (currentUser?.ownsCompany || currentUser?.id === user?.id) {
      fields = fields.filter(
        (field) => !['role', 'password'].includes(field.name)
      );
    }
    return fields;
  };
  const renderEditUserModal = () => (
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
          {t('edit_user')}
        </Typography>
        <Typography variant="subtitle2">
          {t('edit_user_description')}
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
            fields={getFields()}
            validation={Yup.object().shape({
              password: Yup.string().min(8, t('invalid_password')).nullable()
            })}
            submitText={t('save')}
            values={{
              rate: currentUser?.rate,
              role: currentUser
                ? {
                    label:
                      currentUser.role.code === 'USER_CREATED'
                        ? currentUser.role.name
                        : t(`${currentUser.role.code}_name`),
                    value: currentUser.role.id
                  }
                : null,
              password: null
            }}
            onChange={({ field, e }) => {}}
            onSubmit={async (values) => {
              return dispatch(
                editUser(currentUser.id, {
                  ...currentUser,
                  rate: values.rate ?? currentUser.rate,
                  newPassword: values.password ?? null
                })
              )
                .then(
                  () =>
                    formatSelect(values.role).id !== currentUser.role.id &&
                    dispatch(
                      editUserRole(currentUser.id, formatSelect(values.role).id)
                    )
                )
                .then(onEditSuccess)
                .catch(onEditFailure);
            }}
          />
        </Box>
      </DialogContent>
    </Dialog>
  );
  // if reload with peopleId
  useEffect(() => {
    if (peopleId && isNumeric(peopleId)) {
      dispatch(getSingleUser(Number(peopleId)));
    }
  }, [peopleId]);

  useEffect(() => {
    dispatch(getUsers(criteria, enabledOnly));
  }, [criteria, enabledOnly]);

  //see changes in ui on edit
  useEffect(() => {
    if (singleUser || users.content.length) {
      const currentInContent = users.content.find(
        (user) => user.id === currentUser?.id
      );
      const updatedUser = currentInContent ?? singleUser;
      if (updatedUser) {
        if (openDrawerFromUrl) {
          setCurrentUser(updatedUser);
        } else {
          handleOpenDrawer(updatedUser);
          setOpenDrawerFromUrl(true);
        }
      }
    }
    return () => {
      dispatch(clearSingleUser());
    };
  }, [singleUser, users]);

  const columnHelper = createColumnHelper<OwnUser>();

  const columns: CustomDatagridColumn2<OwnUser>[] = [
    columnHelper.accessor(
      (row) =>
        `${row.firstName} ${row.lastName}${
          row.enabled ? '' : ` (${t('disabled')})`
        }`,
      {
        id: 'name',
        header: () => t('name'),
        cell: (info) => (
          <Box
            sx={{
              fontWeight: 'bold',
              color: info.row.original.enabled ? 'inherit' : 'gray'
            }}
          >
            {info.getValue()}
          </Box>
        ),
        size: 150
      }
    ),
    columnHelper.accessor('email', {
      id: 'email',
      header: () => t('email'),
      cell: (info) => info.getValue(),
      size: 150
    }),
    columnHelper.accessor('phone', {
      id: 'phone',
      header: () => t('phone'),
      cell: (info) => info.getValue() || '',
      size: 150
    }),
    columnHelper.accessor('jobTitle', {
      id: 'jobTitle',
      header: () => t('job_title'),
      cell: (info) => info.getValue() || '',
      size: 150
    }),
    columnHelper.accessor('role', {
      id: 'role',
      header: () => t('role'),
      cell: (info) => {
        const role = info.getValue();
        return role.code === 'USER_CREATED'
          ? role.name
          : t(`${role.code}_name`);
      },
      size: 150
    }),
    columnHelper.accessor('rate', {
      id: 'rate',
      header: () => t('hourly_rate'),
      cell: (info) => getFormattedCurrency(info.getValue()),
      size: 150
    }),
    columnHelper.accessor('lastLogin', {
      id: 'lastLogin',
      header: () => t('last_login'),
      cell: (info) => getFormattedDate(info.getValue()),
      size: 150
    }),
    columnHelper.display({
      id: 'actions',
      header: () => t('actions'),
      cell: (info) => {
        const user = info.row.original;

        if (!hasEditPermission(PermissionEntity.PEOPLE_AND_TEAMS, user))
          return null;

        return (
          <Stack direction="row" spacing={1}>
            <EditTwoToneIcon
              fontSize="small"
              color="primary"
              sx={{ cursor: 'pointer' }}
              onClick={(e) => {
                e.stopPropagation();
                handleOpenUpdate(user.id);
              }}
            />
            {user.enabled && !user.ownsCompany && (
              <CancelIcon
                fontSize="small"
                color="error"
                sx={{ cursor: 'pointer' }}
                onClick={(e) => {
                  e.stopPropagation();
                  handleOpenDisable(user.id);
                }}
              />
            )}
          </Stack>
        );
      },
      size: 100
    })
  ];
  const RenderPeopleList = () => (
    <CustomDatagrid2
      columns={columns}
      data={users.content}
      loading={loadingGet}
      pagination={pagination}
      onPaginationChange={setPagination}
      totalRows={users.totalElements}
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
      enableColumnReordering
      enableColumnResizing
      pinnedColumns={pinnedColumns}
      onPinnedColumnsChange={setPinnedColumns}
    />
  );

  return (
    <Box
      sx={{
        p: 2
      }}
    >
      <Stack direction="row" width="100%" justifyContent="space-between">
        <Box sx={{ my: 0.5 }}>
          <SearchInput onChange={debouncedQueryChange} />
        </Box>
        <Stack direction="row" spacing={1} alignItems="center">
          <Typography>Enabled Only</Typography>
          <Switch
            checked={enabledOnly}
            onChange={() => setEnabledOnly((prevState) => !prevState)}
          />
        </Stack>
      </Stack>
      {RenderPeopleList()}

      <Drawer
        variant="temporary"
        anchor={theme.direction === 'rtl' ? 'left' : 'right'}
        open={detailDrawerOpen}
        onClose={handleCloseDetails}
        elevation={9}
      >
        <UserDetailsDrawer user={currentUser} />
      </Drawer>

      <InviteUserDialog
        open={openModal}
        onClose={handleCloseModal}
        onRefreshUsers={() => {
          dispatch(getUsers(criteria));
        }}
      />
      <ConfirmDialog
        open={openDisableModal}
        onCancel={() => {
          setOpenDisableModal(false);
        }}
        onConfirm={() => {
          dispatch(disableUser(currentUser.id)).then(() => {
            setOpenDisableModal(false);
            showSnackBar(t('user_disabled_success'), 'success');
          });
        }}
        confirmText={t('disable')}
        question={t('confirm_disable_user', {
          user: `${currentUser?.firstName} ${currentUser?.lastName}`
        })}
      />
      {renderEditUserModal()}
    </Box>
  );
};

export default People;
