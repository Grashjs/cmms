import {
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  TextField,
  Typography
} from '@mui/material';
import { Formik } from 'formik';
import * as Yup from 'yup';
import { editRole } from '../../../../slices/role';
import { PermissionEntity, PermissionRoot, Role } from '../../../../models/owns/role';
import PermissionsMatrix from './PermissionsMatrix';
import { useTranslation } from 'react-i18next';
import { useContext } from 'react';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { useDispatch } from '../../../../store';

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
                    <PermissionsMatrix values={values} handleChange={handleChange} />
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
