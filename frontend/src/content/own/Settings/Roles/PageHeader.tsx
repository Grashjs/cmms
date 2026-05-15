import { useContext, useState } from 'react';
import * as Yup from 'yup';
import { Formik } from 'formik';
import { useTranslation } from 'react-i18next';

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
  Tooltip,
  Typography
} from '@mui/material';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import { useDispatch } from '../../../../store';
import { addRole } from '../../../../slices/role';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import useAuth from '../../../../hooks/useAuth';
import { PlanFeature } from '../../../../models/owns/subscriptionPlan';
import { getErrorMessage } from '../../../../utils/api';
import { PermissionEntity, PermissionRoot } from '../../../../models/owns/role';
import { defaultPermissions } from '../../../../utils/roles';
import PermissionsMatrix from './PermissionsMatrix';

interface PageHeaderProps {
  rolesNumber: number;
  formatValues: (values, defaultPermissions: boolean) => any;
}

function PageHeader({ rolesNumber, formatValues }: PageHeaderProps) {
  const { t }: { t: any } = useTranslation();
  const { hasFeature } = useAuth();
  const [open, setOpen] = useState(false);
  const dispatch = useDispatch();
  const { showSnackBar } = useContext(CustomSnackBarContext);

  const handleCreateRoleOpen = () => {
    setOpen(true);
  };

  const handleCreateRoleClose = () => {
    setOpen(false);
  };
  const onCreationSuccess = () => {
    handleCreateRoleClose();
    showSnackBar(t('role_create_success'), 'success');
  };
  const onCreationFailure = (err) =>
    showSnackBar(getErrorMessage(err, t('role_create_failure')), 'error');

  const permissionRoots: PermissionRoot[] = [
    'viewPermissions',
    'viewOtherPermissions',
    'createPermissions',
    'editOtherPermissions',
    'deleteOtherPermissions'
  ];

  return (
    <>
      <Grid container justifyContent="space-between" alignItems="center">
        <Grid item>
          <Typography variant="h3" component="h3" gutterBottom>
            {t('roles_management')}
          </Typography>
          <Typography variant="subtitle2">
            {t('roles_number', { count: rolesNumber })}
          </Typography>
        </Grid>

        <Grid item>
          <Tooltip
            title={
              hasFeature(PlanFeature.ROLE)
                ? t('create_role')
                : t('upgrade_role')
            }
          >
            <span>
              <Button
                sx={{
                  mt: { xs: 2, sm: 0 }
                }}
                disabled={!hasFeature(PlanFeature.ROLE)}
                onClick={handleCreateRoleOpen}
                variant="contained"
                startIcon={<AddTwoToneIcon fontSize="small" />}
              >
                {t('create_role')}
              </Button>
            </span>
          </Tooltip>
        </Grid>
      </Grid>

      <Dialog
        fullWidth
        maxWidth="md"
        open={open}
        onClose={handleCreateRoleClose}
      >
        <DialogTitle
          sx={{
            p: 3
          }}
        >
          <Typography variant="h4" gutterBottom>
            {t('add_role')}
          </Typography>
          <Typography variant="subtitle2">
            {t('add_role_description')}
          </Typography>
        </DialogTitle>
        <Formik
          initialValues={{
            name: '',
            description: '',
            externalId: '',
            ...Object.fromEntries(
              Object.values(PermissionEntity).flatMap((entity) =>
                permissionRoots.map((root) => [
                  `${root}_${entity}`,
                  defaultPermissions[root].includes(entity)
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
            setSubmitting(true);
            const formattedValues = formatValues(_values, true);
            return dispatch(addRole(formattedValues))
              .then(onCreationSuccess)
              .catch(onCreationFailure)
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
                      error={Boolean(touched.description && errors.description)}
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
                <Button color="secondary" onClick={handleCreateRoleClose}>
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
                  {t('add_role')}
                </Button>
              </DialogActions>
            </form>
          )}
        </Formik>
      </Dialog>
    </>
  );
}

export default PageHeader;
