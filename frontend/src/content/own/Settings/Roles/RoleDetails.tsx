import {
  Box,
  Grid,
  IconButton,
  Tooltip,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import { Role } from '../../../../models/owns/role';
import PermissionsMatrix from './PermissionsMatrix';
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

      <Grid item xs={12}>
        <Typography sx={{ mt: 2, mb: 2 }} variant="h4">
          {t('permissions')}
        </Typography>
        <PermissionsMatrix role={role} />
      </Grid>
    </Grid>
  );
}
