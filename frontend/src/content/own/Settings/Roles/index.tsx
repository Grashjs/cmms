import SettingsLayout from '../SettingsLayout';

import {
  Avatar,
  Box,
  Button,
  Dialog,
  Drawer,
  Grid,
  Slide,
  styled,
  Typography,
  useTheme
} from '@mui/material';
import { deleteRole, getRoles } from '../../../../slices/role';
import { useDispatch, useSelector } from '../../../../store';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import PageHeader from './PageHeader';
import { useTranslation } from 'react-i18next';
import {
  PermissionEntity,
  PermissionRoot,
  Role
} from '../../../../models/owns/role';
import CloseIcon from '@mui/icons-material/Close';
import {
  forwardRef,
  ReactElement,
  Ref,
  useContext,
  useEffect,
  useState
} from 'react';
import { TransitionProps } from '@mui/material/transitions';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import {
  GridActionsCellItem,
  GridRenderCellParams,
  GridRowParams,
  GridToolbar
} from '@mui/x-data-grid';
import CustomDatagrid from '../../components/CustomDatagrid';
import { GridEnrichedColDef } from '@mui/x-data-grid/models/colDef/gridColDef';
import RoleDetails from './RoleDetails';
import EditRole from './EditRole';
import useAuth from '../../../../hooks/useAuth';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { defaultPermissions } from '../../../../utils/roles';
import { PlanFeature } from '../../../../models/owns/subscriptionPlan';

const DialogWrapper = styled(Dialog)(
  () => `
        .MuiDialog-paper {
          overflow: visible;
        }
  `
);
const Transition = forwardRef(function Transition(
  props: TransitionProps & { children: ReactElement<any, any> },
  ref: Ref<unknown>
) {
  return <Slide direction="down" ref={ref} {...props} />;
});
const AvatarError = styled(Avatar)(
  ({ theme }) => `
        background-color: ${theme.colors.error.lighter};
        color: ${theme.colors.error.main};
        width: ${theme.spacing(12)};
        height: ${theme.spacing(12)};
  
        .MuiSvgIcon-root {
          font-size: ${theme.typography.pxToRem(45)};
        }
  `
);

const ButtonError = styled(Button)(
  ({ theme }) => `
       background: ${theme.colors.error.main};
       color: ${theme.palette.error.contrastText};
  
       &:hover {
          background: ${theme.colors.error.dark};
       }
      `
);
const LabelWrapper = styled(Box)(
  ({ theme }) => `
    font-size: ${theme.typography.pxToRem(10)};
    font-weight: bold;
    text-transform: uppercase;
    border-radius: ${theme.general.borderRadiusSm};
    padding: ${theme.spacing(0.9, 1.5, 0.7)};
    line-height: 1;
  `
);
function Roles() {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const { companySettings } = useAuth();
  const [openDelete, setOpenDelete] = useState(false);
  const [openUpdateModal, setOpenUpdateModal] = useState(false);
  const [openDrawer, setOpenDrawer] = useState<boolean>(false);
  const [currentRole, setCurrentRole] = useState<Role>();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const dispatch = useDispatch();
  const { roles, loadingGet } = useSelector((state) => state.roles);
  const { hasFeature } = useAuth();

  useEffect(() => {
    if (currentRole) {
      setCurrentRole(roles.find((role) => role.id == currentRole.id));
    }
  }, [roles]);
  const permissionRoots: PermissionRoot[] = [
    'viewPermissions',
    'viewOtherPermissions',
    'createPermissions',
    'editOtherPermissions',
    'deleteOtherPermissions'
  ];

  const formatValues = (values, useDefaultPermissions: boolean) => {
    let newValues = { ...values };

    newValues.companySettings = { id: companySettings.id };
    newValues.roleType = 'ROLE_CLIENT';

    permissionRoots.forEach((root) => {
      if (useDefaultPermissions) {
        newValues[root] = [...defaultPermissions[root]];
      } else if (!newValues[root]) {
        newValues[root] = [];
      }
    });

    Object.keys(values).forEach((key) => {
      const idx = key.indexOf('_');
      if (idx > 0) {
        const root = key.slice(0, idx) as PermissionRoot;
        const entity = key.slice(idx + 1) as PermissionEntity;
        if (
          permissionRoots.includes(root) &&
          Object.values(PermissionEntity).includes(entity)
        ) {
          if (values[key]) {
            if (!newValues[root].includes(entity)) {
              newValues[root].push(entity);
            }
          } else if (newValues[root]) {
            newValues[root] = newValues[root].filter((e) => e !== entity);
          }
          delete newValues[key];
        }
      }
    });
    return newValues;
  };
  const onDeleteSuccess = () => {
    showSnackBar(t('role_delete_success'), 'success');
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('role_delete_failure'), 'error');

  const handleOpenDetails = (id: number) => {
    const foundRole = roles.find((role) => role.id === id);
    if (foundRole) {
      setCurrentRole(foundRole);
      setOpenDrawer(true);
    }
  };
  const handleOpenDelete = (id: number) => {
    changeCurrentRole(id);
    setOpenDelete(true);
  };
  const changeCurrentRole = (id: number) => {
    const foundRole = roles.find((role) => role.id === id);
    setCurrentRole(foundRole);
  };
  const handleOpenUpdate = (id: number) => {
    changeCurrentRole(id);
    setOpenUpdateModal(true);
  };
  const closeConfirmDelete = () => setOpenDelete(false);

  const handleDelete = (id: number) => {
    setOpenDrawer(false);
    dispatch(deleteRole(id)).then(onDeleteSuccess).catch(onDeleteFailure);
    setOpenDelete(false);
  };
  useEffect(() => {
    dispatch(getRoles());
  }, []);

  const renderDeleteModal = () => (
    <DialogWrapper
      open={openDelete}
      maxWidth="sm"
      fullWidth
      TransitionComponent={Transition}
      keepMounted
      onClose={closeConfirmDelete}
    >
      <Box
        display="flex"
        alignItems="center"
        justifyContent="center"
        flexDirection="column"
        p={5}
      >
        <AvatarError>
          <CloseIcon />
        </AvatarError>

        <Typography
          align="center"
          sx={{
            py: 4,
            px: 6
          }}
          variant="h3"
        >
          {t('confirm_delete_role')}?
        </Typography>

        <Box>
          <Button
            variant="text"
            size="large"
            sx={{
              mx: 1
            }}
            onClick={closeConfirmDelete}
          >
            {t('cancel')}
          </Button>
          <ButtonError
            onClick={() => handleDelete(currentRole.id)}
            size="large"
            sx={{
              mx: 1,
              px: 3
            }}
            variant="contained"
          >
            {t('to_delete')}
          </ButtonError>
        </Box>
      </Box>
    </DialogWrapper>
  );

  const columns: GridEnrichedColDef[] = [
    {
      field: 'name',
      headerName: t('name'),
      description: t('name'),
      width: 150,
      renderCell: (params: GridRenderCellParams<string, Role>) => (
        <Box sx={{ fontWeight: 'bold' }}>
          {params.row.code === 'USER_CREATED'
            ? params.value
            : t(`${params.row.code}_name`)}
        </Box>
      )
    },
    {
      field: 'users',
      headerName: t('users'),
      description: t('users'),
      width: 150
    },
    {
      field: 'externalId',
      headerName: t('external_id'),
      description: t('external_id'),
      width: 150
    },
    {
      field: 'paid',
      headerName: t('type'),
      description: t('type'),
      width: 150,
      renderCell: (params: GridRenderCellParams<string>) => (
        <LabelWrapper
          sx={{
            background: params.value
              ? `${theme.colors.info.main}`
              : `${theme.colors.success.main}`,
            color: `${theme.palette.getContrastText(
              params.value ? theme.colors.info.dark : theme.colors.success.dark
            )}`
          }}
        >
          {params.value ? t('paid') : t('free')}
        </LabelWrapper>
      )
    },
    {
      field: 'actions',
      type: 'actions',
      headerName: t('actions'),
      description: t('actions'),
      getActions: (params: GridRowParams<Role>) => {
        let actions = [
          <GridActionsCellItem
            key="edit"
            disabled={!hasFeature(PlanFeature.ROLE)}
            icon={
              <EditTwoToneIcon
                fontSize="small"
                color={hasFeature(PlanFeature.ROLE) ? 'primary' : 'disabled'}
              />
            }
            onClick={() => handleOpenUpdate(Number(params.id))}
            label={t('edit')}
          />,
          <GridActionsCellItem
            key="delete"
            disabled={!hasFeature(PlanFeature.ROLE)}
            icon={
              <DeleteTwoToneIcon
                fontSize="small"
                color={hasFeature(PlanFeature.ROLE) ? 'error' : 'disabled'}
              />
            }
            onClick={() => handleOpenDelete(Number(params.id))}
            label={t('to_delete')}
          />
        ];
        if (params.row.code !== 'USER_CREATED') actions = [];
        return actions;
      }
    }
  ];

  return (
    <>
      <Grid item xs={12}>
        <Box p={4}>
          <PageHeader rolesNumber={roles.length} formatValues={formatValues} />
          <EditRole
            open={openUpdateModal}
            role={currentRole}
            onClose={() => setOpenUpdateModal(false)}
            formatValues={formatValues}
          />
          {renderDeleteModal()}
          <Box sx={{ mt: 4, width: '95%' }}>
            <CustomDatagrid
              rows={roles}
              columns={columns}
              loading={loadingGet}
              components={{
                Toolbar: GridToolbar
                // Toolbar: GridToolbarColumnsButton,
                // Toolbar: GridToolbarDensitySelector
              }}
              onRowClick={(params) => handleOpenDetails(Number(params.id))}
              initialState={{
                columns: {
                  columnVisibilityModel: {}
                }
              }}
            />
          </Box>
        </Box>
      </Grid>
      <Drawer
        anchor="right"
        open={openDrawer}
        onClose={() => setOpenDrawer(false)}
        PaperProps={{
          sx: { width: { xs: '90%', sm: '70%', md: '50%' } }
        }}
      >
        <RoleDetails
          role={currentRole}
          handleOpenUpdate={() => setOpenUpdateModal(true)}
          handleOpenDelete={() => setOpenDelete(true)}
        />
      </Drawer>
    </>
  );
}

export default Roles;
