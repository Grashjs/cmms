import {
  Box,
  Button,
  Checkbox,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography
} from '@mui/material';
import { Formik } from 'formik';
import * as Yup from 'yup';
import { editRole } from '../../../../slices/role';
import {
  PermissionEntity,
  PermissionRoot,
  Role
} from '../../../../models/owns/role';
import { useTranslation } from 'react-i18next';
import { useContext } from 'react';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { useDispatch } from '../../../../store';
import { useBrand } from '../../../../hooks/useBrand';
import { getErrorMessage } from '../../../../utils/api';

interface EditRoleProps {
  role: Role;
  formatValues: (values, defaultPermissions: boolean) => any;
  open: boolean;
  onClose: () => void;
}
function EditRole({ role, open, onClose, formatValues }: EditRoleProps) {
  const { t }: { t: any } = useTranslation();
  const dispatch = useDispatch();
  const brandConfig = useBrand();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const onEditSuccess = () => {
    onClose();
    showSnackBar(t('changes_saved_success'), 'success');
  };
  const onEditFailure = (err) =>
    showSnackBar(getErrorMessage(err, t('role_edit_failure')), 'error');

  const permissionRoots: PermissionRoot[] = [
    'viewPermissions',
    'viewOtherPermissions',
    'createPermissions',
    'editOtherPermissions',
    'deleteOtherPermissions'
  ];

  const entityLabel = (entity: PermissionEntity): string => {
    const labels: Record<string, string> = {
      PEOPLE_AND_TEAMS: t('people_teams'),
      CATEGORIES: t('categories'),
      CATEGORIES_WEB: 'Categories Web',
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

  return (
    <Dialog fullWidth maxWidth="md" open={open} onClose={onClose}>
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {t('edit_role')}
        </Typography>
        <Typography variant="subtitle2">
          {t('edit_role_description')}
        </Typography>
      </DialogTitle>
      <Formik
        initialValues={{
          ...role,
          ...Object.fromEntries(
            Object.values(PermissionEntity).flatMap((entity) =>
              permissionRoots.map((root) => [
                `${root}_${entity}`,
                role?.[root]?.includes(entity) ?? false
              ])
            )
          ),
          submit: null
        }}
        validationSchema={Yup.object().shape({
          name: Yup.string().max(255).required(t('required_name')),
          description: Yup.string().max(255).nullable(),
          externalId: Yup.string().max(255).nullable()
        })}
        onSubmit={async (
          _values,
          { resetForm, setErrors, setStatus, setSubmitting }
        ) => {
          const formattedValues = formatValues({ ...role, ..._values }, false);
          setSubmitting(true);
          return dispatch(editRole(role.id, formattedValues))
            .then(onEditSuccess)
            .catch(onEditFailure)
            .finally(() => setSubmitting(false));
        }}
      >
        {({
          errors,
          handleBlur,
          handleChange,
          handleSubmit,
          isSubmitting,
          touched,
          values
        }) => (
          <form onSubmit={handleSubmit}>
            <DialogContent
              dividers
              sx={{
                p: 3
              }}
            >
              <Grid container spacing={3}>
                <Grid item xs={12}>
                  <TextField
                    error={Boolean(touched.name && errors.name)}
                    fullWidth
                    helperText={touched.name && errors.name}
                    label={t('name')}
                    name="name"
                    onBlur={handleBlur}
                    onChange={handleChange}
                    value={values.name}
                    variant="outlined"
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    error={Boolean(
                      touched.description && errors.description
                    )}
                    fullWidth
                    helperText={touched.description && errors.description}
                    label={t('description')}
                    name="description"
                    onBlur={handleBlur}
                    onChange={handleChange}
                    value={values.description}
                    variant="outlined"
                  />
                </Grid>
                <Grid item xs={12}>
                  <TextField
                    error={Boolean(touched.externalId && errors.externalId)}
                    fullWidth
                    helperText={touched.externalId && errors.externalId}
                    label={t('external_id')}
                    name="externalId"
                    onBlur={handleBlur}
                    onChange={handleChange}
                    value={values.externalId}
                    variant="outlined"
                  />
                </Grid>
                <Grid item xs={12}>
                  <Typography variant="h4" sx={{ pb: 2 }}>
                    {t('permissions')}
                  </Typography>
                  <TableContainer component={Paper} sx={{ maxHeight: 400 }}>
                    <Table size="small" stickyHeader>
                      <TableHead>
                        <TableRow>
                          <TableCell sx={{ fontWeight: 'bold' }}>
                            {t('entity')}
                          </TableCell>
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
                                  name={`${root}_${entity}`}
                                  onChange={handleChange}
                                  checked={values[`${root}_${entity}`]}
                                />
                              </TableCell>
                            ))}
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                </Grid>
              </Grid>
            </DialogContent>
            <DialogActions
              sx={{
                p: 3
              }}
            >
              <Button color="secondary" onClick={onClose}>
                {t('cancel')}
              </Button>
              <Button
                type="submit"
                startIcon={
                  isSubmitting ? <CircularProgress size="1rem" /> : null
                }
                disabled={Boolean(errors.submit) || isSubmitting}
                variant="contained"
              >
                {t('save')}
              </Button>
            </DialogActions>
          </form>
        )}
      </Formik>
    </Dialog>
  );
}
export default EditRole;
