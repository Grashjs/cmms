import { useContext, useState } from 'react';
import * as Yup from 'yup';
import { Formik } from 'formik';
import { useTranslation } from 'react-i18next';

import {
  Box,
  Button,
  Checkbox,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControlLabel,
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

// const roles = [
//   { label: 'Free', value: 'free' },
//   { label: 'Paid', value: 'paid' }
// ];

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
    showSnackBar(t('role_create_failure'), 'error');

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
                  <Grid item xs={12} lg={6}>
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
                          error={Boolean(
                            touched.externalId && errors.externalId
                          )}
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
                    </Grid>
                  </Grid>
                  <Grid item xs={12} lg={6}>
                    <Box
                      display="flex"
                      justifyContent="space-between"
                      flexDirection="column"
                    >
                      <Box>
                        <Typography variant="h2" sx={{ pb: 1 }}>
                          {t('permissions')}
                        </Typography>
                        <Typography variant="subtitle2">
                          {t('create_role_description')}
                        </Typography>
                      </Box>

                      <Divider flexItem sx={{ m: 4 }} />

                      <Box
                        display="flex"
                        flexDirection="column"
                        justifyContent="space-between"
                        mb={3}
                      >
                        <Typography variant="h4" sx={{ pb: 1 }}>
                          {t('create_and_edit')}
                        </Typography>
                        <FormControlLabel
                          onChange={handleChange}
                          name={'createPeopleTeams'}
                          control={<Checkbox />}
                          label={t('people_teams')}
                        />
                        <FormControlLabel
                          onChange={handleChange}
                          name={'createCategories'}
                          control={<Checkbox />}
                          label={t('categories')}
                        />
                      </Box>

                      <Box
                        display="flex"
                        flexDirection="column"
                        justifyContent="space-between"
                        mb={3}
                      >
                        <Typography variant="h4" sx={{ pb: 1 }}>
                          {t('to_delete')}
                        </Typography>
                        <FormControlLabel
                          onChange={handleChange}
                          name={'deleteWorkOrders'}
                          control={<Checkbox />}
                          label={t('work_orders')}
                        />
                        <FormControlLabel
                          onChange={handleChange}
                          name={'deletePreventiveMaintenanceTrigger'}
                          control={<Checkbox />}
                          label={t('pm_trigger')}
                        />
                        <FormControlLabel
                          onChange={handleChange}
                          name={'deleteLocations'}
                          control={<Checkbox />}
                          label={t('locations')}
                        />
                        <FormControlLabel
                          onChange={handleChange}
                          name={'deleteAssets'}
                          control={<Checkbox />}
                          label={t('assets')}
                        />
                        <FormControlLabel
                          onChange={handleChange}
                          name={'deletePartsAndSets'}
                          control={<Checkbox />}
                          label={t('parts_and_sets')}
                        />
                        <FormControlLabel
                          onChange={handleChange}
                          name={'deletePurchaseOrders'}
                          control={<Checkbox />}
                          label={t('purchase_orders')}
                        />
                        <FormControlLabel
                          onChange={handleChange}
                          name={'deleteMeters'}
                          control={<Checkbox />}
                          label={t('meters')}
                        />
                        <FormControlLabel
                          onChange={handleChange}
                          name={'deleteVendorsCustomers'}
                          control={<Checkbox />}
                          label={t('vendors_customers')}
                        />
                        <FormControlLabel
                          onChange={handleChange}
                          name={'deleteCategories'}
                          control={<Checkbox />}
                          label={t('categories')}
                        />
                        <FormControlLabel
                          onChange={handleChange}
                          name={'deleteFiles'}
                          control={<Checkbox />}
                          label={t('files')}
                        />
                        <FormControlLabel
                          onChange={handleChange}
                          name={'deletePeopleTeams'}
                          control={<Checkbox />}
                          label={t('people_teams')}
                        />
                      </Box>

                      <Box
                        display="flex"
                        flexDirection="column"
                        justifyContent="space-between"
                      >
                        <Typography variant="h4" sx={{ pb: 1 }}>
                          {t('to_access')}
                        </Typography>
                        <FormControlLabel
                          onChange={handleChange}
                          name={'accessSettings'}
                          control={<Checkbox />}
                          label={t('settings')}
                        />
                      </Box>
                    </Box>
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
