import {
  Box,
  Checkbox,
  Divider,
  FormControlLabel,
  Grid,
  IconButton,
  Tooltip,
  Typography,
  useTheme
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import { PermissionEntity, Role } from '../../../../models/owns/role';
import useAuth from '../../../../hooks/useAuth';
import { PlanFeature } from '../../../../models/owns/subscriptionPlan';

interface RoleDetailsProps {
  role: Role;
  handleOpenUpdate: () => void;
  handleOpenDelete: () => void;
}
export default function RoleDetails(props: RoleDetailsProps) {
  const { role, handleOpenUpdate, handleOpenDelete } = props;
  const { hasFeature } = useAuth();
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const allPermissions: {
    [key: string]: {
      label: string;
      condition: (role: Role) => boolean;
    }[];
  } = {
    create: [
      {
        label: t('categories'),
        condition: (role: Role) =>
          role.createPermissions.includes(PermissionEntity.CATEGORIES)
      },
      {
        label: t('people_teams'),
        condition: (role: Role) =>
          role.createPermissions.includes(PermissionEntity.PEOPLE_AND_TEAMS)
      }
    ],
    delete: [
      {
        label: t('people_teams'),
        condition: (role: Role) =>
          role.deleteOtherPermissions.includes(
            PermissionEntity.PEOPLE_AND_TEAMS
          )
      },
      {
        label: t('files'),
        condition: (role: Role) =>
          role.deleteOtherPermissions.includes(PermissionEntity.FILES)
      },
      {
        label: t('categories'),
        condition: (role: Role) =>
          role.deleteOtherPermissions.includes(PermissionEntity.CATEGORIES)
      },
      {
        label: t('meters'),
        condition: (role: Role) =>
          role.deleteOtherPermissions.includes(PermissionEntity.METERS)
      },
      {
        label: t('locations'),
        condition: (role: Role) =>
          role.deleteOtherPermissions.includes(PermissionEntity.LOCATIONS)
      },
      {
        label: t('assets'),
        condition: (role: Role) =>
          role.deleteOtherPermissions.includes(PermissionEntity.ASSETS)
      },
      {
        label: t('purchase_orders'),
        condition: (role: Role) =>
          role.deleteOtherPermissions.includes(PermissionEntity.PURCHASE_ORDERS)
      },
      {
        label: t('vendors_customers'),
        condition: (role: Role) =>
          role.deleteOtherPermissions.includes(
            PermissionEntity.VENDORS_AND_CUSTOMERS
          )
      },
      {
        label: t('parts_and_sets'),
        condition: (role: Role) =>
          role.deleteOtherPermissions.includes(
            PermissionEntity.PARTS_AND_MULTIPARTS
          )
      },
      {
        label: t('work_orders'),
        condition: (role: Role) =>
          role.deleteOtherPermissions.includes(PermissionEntity.WORK_ORDERS)
      },
      {
        label: t('preventive_maintenance'),
        condition: (role: Role) =>
          role.deleteOtherPermissions.includes(
            PermissionEntity.PREVENTIVE_MAINTENANCES
          )
      }
    ],
    access: [
      {
        label: t('settings'),
        condition: (role: Role) =>
          role.viewPermissions.includes(PermissionEntity.SETTINGS)
      }
    ]
  };

  const PermissionsGroup = ({
    name,
    title
  }: {
    name: string;
    title: string;
  }) => (
    <Grid item xs={12} lg={6}>
      <Grid item>
        <Typography sx={{ mt: 2, mb: 1 }} variant="h5">
          {title}
        </Typography>
      </Grid>
      {allPermissions[name].map(({ label, condition }, index) => (
        <Grid item key={index} xs={12} lg={12}>
          <FormControlLabel
            control={<Checkbox checked={condition(role)} />}
            label={label}
          />
        </Grid>
      ))}
    </Grid>
  );
  return (
    <Grid
      container
      justifyContent="center"
      alignItems="stretch"
      spacing={2}
      padding={4}
    >
      <Grid
        item
        xs={12}
        display="flex"
        flexDirection="row"
        justifyContent="space-between"
      >
        <Box>
          <Typography variant="h2">{role?.name}</Typography>
          <Typography variant="subtitle1">{role?.description}</Typography>
        </Box>
        {role.code === 'USER_CREATED' && (
          <Box>
            <Tooltip
              title={
                hasFeature(PlanFeature.ROLE)
                  ? t('edit_role')
                  : t('upgrade_role_edit')
              }
            >
              <span>
                <IconButton
                  disabled={!hasFeature(PlanFeature.ROLE)}
                  style={{ marginRight: 10 }}
                  onClick={handleOpenUpdate}
                >
                  <EditTwoToneIcon
                    color={
                      hasFeature(PlanFeature.ROLE) ? 'primary' : 'disabled'
                    }
                  />
                </IconButton>
              </span>
            </Tooltip>
            <Tooltip
              title={
                hasFeature(PlanFeature.ROLE)
                  ? t('delete_role')
                  : t('upgrade_role_delete')
              }
            >
              <span>
                <IconButton
                  disabled={!hasFeature(PlanFeature.ROLE)}
                  onClick={handleOpenDelete}
                >
                  <DeleteTwoToneIcon
                    color={hasFeature(PlanFeature.ROLE) ? 'error' : 'disabled'}
                  />
                </IconButton>
              </span>
            </Tooltip>
          </Box>
        )}
      </Grid>
      <Divider />

      <Grid item xs={12}>
        <Box>
          <Typography sx={{ mt: 2, mb: 1 }} variant="h4">
            Permissions
          </Typography>
          <Grid container spacing={2}>
            <PermissionsGroup name={'create'} title={t('create')} />
            <PermissionsGroup name={'delete'} title={t('to_delete')} />
            <PermissionsGroup name={'access'} title={t('to_access')} />
          </Grid>
        </Box>
      </Grid>
    </Grid>
  );
}
