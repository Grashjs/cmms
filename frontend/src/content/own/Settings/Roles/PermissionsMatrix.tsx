import {
  Checkbox,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import {
  PermissionEntity,
  PermissionRoot,
  Role
} from '../../../../models/owns/role';

const permissionRoots: PermissionRoot[] = [
  'viewPermissions',
  'viewOtherPermissions',
  'createPermissions',
  'editOtherPermissions',
  'deleteOtherPermissions'
];

interface PermissionsMatrixProps {
  values?: Record<string, any>;
  handleChange?: (e: any) => void;
  role?: Role;
}

function PermissionsMatrix({
  values,
  handleChange,
  role
}: PermissionsMatrixProps) {
  const { t }: { t: any } = useTranslation();

  const entityLabel = (entity: PermissionEntity): string => {
    const labels: Record<string, string> = {
      PEOPLE_AND_TEAMS: t('people_teams'),
      CATEGORIES: t('categories'),
      WORK_ORDERS: t('work_orders'),
      PREVENTIVE_MAINTENANCES: t('pm_trigger'),
      ASSETS: t('assets'),
      PARTS_AND_MULTIPARTS: t('parts_and_sets'),
      PURCHASE_ORDERS: t('purchase_orders'),
      METERS: t('meters'),
      VENDORS_AND_CUSTOMERS: t('vendors_customers'),
      FILES: t('files'),
      LOCATIONS: t('locations'),
      SETTINGS: t('settings'),
      REQUESTS: 'Requests',
      ANALYTICS: 'Analytics'
    };
    return labels[entity] || entity;
  };

  const isEditable = !!values && !!handleChange;

  const isChecked = (
    root: PermissionRoot,
    entity: PermissionEntity
  ): boolean => {
    if (isEditable) {
      return values[`${root}_${entity}`] ?? false;
    }
    return role?.[root]?.includes(entity) ?? false;
  };

  return (
    <>
      <Typography mb={1} variant="subtitle2">
        {t('view_other_hint')}
      </Typography>
      <TableContainer component={Paper} sx={{ maxHeight: 400 }}>
        <Table size="small" stickyHeader>
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 'bold' }}>{''}</TableCell>
              <TableCell align="center" sx={{ fontWeight: 'bold' }}>
                {t('view')}
              </TableCell>
              <TableCell align="center" sx={{ fontWeight: 'bold' }}>
                {t('view_other')}
              </TableCell>
              <TableCell align="center" sx={{ fontWeight: 'bold' }}>
                {t('create')}
              </TableCell>
              <TableCell align="center" sx={{ fontWeight: 'bold' }}>
                {t('edit')}
              </TableCell>
              <TableCell align="center" sx={{ fontWeight: 'bold' }}>
                {t('delete')}
              </TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {Object.values(PermissionEntity).map((entity) => (
              <TableRow key={entity}>
                <TableCell sx={{ fontWeight: 'bold' }}>
                  {entityLabel(entity)}
                </TableCell>
                {permissionRoots.map((root) => (
                  <TableCell key={root} align="center">
                    <Checkbox
                      name={isEditable ? `${root}_${entity}` : undefined}
                      onChange={isEditable ? handleChange : undefined}
                      checked={isChecked(root, entity)}
                    />
                  </TableCell>
                ))}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
}

export default PermissionsMatrix;
