import {
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  DialogActions,
  DialogContent,
  Divider,
  Grid,
  Switch,
  TextField,
  Typography
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import LockTwoToneIcon from '@mui/icons-material/LockTwoTone';
import Text from 'src/components/Text';
import { Formik } from 'formik';
import * as Yup from 'yup';
import { useContext, useEffect, useState } from 'react';
import { phoneRegExp } from '../../../utils/validators';
import CustomDialog from '../components/CustomDialog';
import useAuth from '../../../hooks/useAuth';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';

function ProfileDetails() {
  const { t }: { t: any } = useTranslation();
  const {
    user,
    userSettings,
    fetchUserSettings,
    patchUserSettings,
    patchUser,
    updatePassword
  } = useAuth();
  const [openEditModal, setOpenEditModal] = useState<boolean>(false);
  const handleOpenEditModal = () => setOpenEditModal(true);
  const handleCloseEditModal = () => setOpenEditModal(false);
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [openPasswordModal, setOpenPasswordModal] = useState<boolean>(false);
  const handleOpenPasswordModal = () => setOpenPasswordModal(true);
  const handleClosePasswordModal = () => setOpenPasswordModal(false);

  useEffect(() => {
    fetchUserSettings();
  }, []);

  const userConfig = {
    firstName: { value: user.firstName, title: t('first_name') },
    lastName: { value: user.lastName, title: t('last_name') },
    email: { value: user.email, title: t('email') },
    phone: { value: user.phone, title: t('phone') },
    jobTitle: { value: user.jobTitle, title: t('job_title') },
    settings: {
      emailNotified: {
        value: userSettings?.emailNotified,
        title: t('email_notifications')
      },
      emailUpdatesForWorkOrders: {
        value: userSettings?.emailUpdatesForWorkOrders,
        title: t('email_updates_wo')
      },
      emailUpdatesForRequests: {
        value: userSettings?.emailUpdatesForRequests,
        title: t('email_updates_requests')
      }
      // dailyEmailSummary: { value: userSettings?., title: t('Daily Summary Emails') },
      // emailUpdatesForPurchaseOrders: {
      //   value: userSettings?.emailUpdatesForPurchaseOrders,
      //   title: t('po_emails')
      // }
    }
  };

  const renderKeyAndValue = (key: string, value: string) => {
    return (
      <>
        <Grid item xs={12} sm={4} md={3} textAlign={{ sm: 'right' }}>
          <Box pr={3} pb={2}>
            {t(key)}:
          </Box>
        </Grid>
        <Grid item xs={12} sm={8} md={9}>
          <Text color="black">
            <b>{value ?? t('N/A')}</b>
          </Text>
        </Grid>
      </>
    );
  };
  const renderKeyAndSwitch = (key: string, value: boolean, setting: string) => {
    return (
      <>
        <Grid item xs={12} sm={4} md={3} textAlign={{ sm: 'right' }}>
          <Box pr={3} pb={2}>
            {t(key)}:
          </Box>
        </Grid>
        <Grid item xs={12} sm={8} md={9}>
          <Switch
            checked={value}
            onChange={(event, value) => {
              patchUserSettings({
                ...userSettings,
                [setting]: value
              });
            }}
          />
        </Grid>
      </>
    );
  };
  const renderEditModal = () => (
    <CustomDialog
      onClose={handleCloseEditModal}
      open={openEditModal}
      title="Edit profile"
      subtitle="Fill in the fields below"
    >
      <Formik
        initialValues={{
          firstName: userConfig.firstName.value,
          lastName: userConfig.lastName.value,
          phone: userConfig.phone.value,
          jobTitle: userConfig.jobTitle.value
        }}
        validationSchema={Yup.object().shape({
          firstName: Yup.string().max(100).required(t('required_firstName')),
          lastName: Yup.string().max(100).required(t('required_lastName')),
          phone: Yup.string().matches(phoneRegExp, t('invalid_phone')),
          jobTitle: Yup.string()
            .max(100)
            .required(t('required_job_title'))
            .nullable()
        })}
        onSubmit={async (
          _values,
          { resetForm, setErrors, setStatus, setSubmitting }
        ) => {
          setSubmitting(true);
          return patchUser(_values)
            .then(handleCloseEditModal)
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
                <Grid item xs={12} lg={12}>
                  <Grid container spacing={3}>
                    <Grid item xs={12}>
                      <TextField
                        error={Boolean(touched.firstName && errors.firstName)}
                        fullWidth
                        helperText={touched.firstName && errors.firstName}
                        label={t('first_name')}
                        name="firstName"
                        onBlur={handleBlur}
                        onChange={handleChange}
                        value={values.firstName}
                        variant="outlined"
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        error={Boolean(touched.lastName && errors.lastName)}
                        fullWidth
                        helperText={touched.lastName && errors.lastName}
                        label={t('last_name')}
                        name="lastName"
                        onBlur={handleBlur}
                        onChange={handleChange}
                        value={values.lastName}
                        variant="outlined"
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        error={Boolean(touched.phone && errors.phone)}
                        fullWidth
                        helperText={touched.phone && errors.phone}
                        label={t('phone')}
                        name="phone"
                        onBlur={handleBlur}
                        onChange={handleChange}
                        value={values.phone}
                        variant="outlined"
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        error={Boolean(touched.jobTitle && errors.jobTitle)}
                        fullWidth
                        helperText={touched.jobTitle && errors.jobTitle}
                        label={t('job_title')}
                        name="jobTitle"
                        onBlur={handleBlur}
                        onChange={handleChange}
                        value={values.jobTitle}
                        variant="outlined"
                      />
                    </Grid>
                  </Grid>
                </Grid>
              </Grid>
            </DialogContent>
            <DialogActions
              sx={{
                p: 3
              }}
            >
              <Button color="secondary" onClick={handleCloseEditModal}>
                {t('cancel')}
              </Button>
              <Button
                type="submit"
                startIcon={
                  isSubmitting ? <CircularProgress size="1rem" /> : null
                }
                disabled={isSubmitting}
                variant="contained"
              >
                {t('save')}
              </Button>
            </DialogActions>
          </form>
        )}
      </Formik>
    </CustomDialog>
  );
  const renderPasswordModal = () => (
    <CustomDialog
      onClose={handleClosePasswordModal}
      open={openPasswordModal}
      title="Change password"
      subtitle="Fill in the fields below"
    >
      <Formik
        initialValues={{
          oldPassword: '',
          newPassword: '',
          confirmPassword: ''
        }}
        validationSchema={Yup.object().shape({
          oldPassword: Yup.string()
            .required(t('required_old_password'))
            .min(8, t('invalid_password')),
          newPassword: Yup.string()
            .required(t('required_new_password'))
            .min(8, t('invalid_password')),
          confirmPassword: Yup.string().oneOf(
            [Yup.ref('newPassword'), null],
            t('passwords_must_match')
          )
        })}
        onSubmit={async (
          _values,
          { resetForm, setErrors, setStatus, setSubmitting }
        ) => {
          setSubmitting(true);
          return updatePassword(_values)
            .then(() => {
              handleClosePasswordModal();
              showSnackBar(t('password_change_success'), 'success');
            })
            .catch((err) => showSnackBar(t('wrong_password'), 'error'))
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
                <Grid item xs={12} lg={12}>
                  <Grid container spacing={3}>
                    <Grid item xs={12}>
                      <TextField
                        error={Boolean(
                          touched.oldPassword && errors.oldPassword
                        )}
                        fullWidth
                        helperText={touched.oldPassword && errors.oldPassword}
                        label={t('current_password')}
                        type="password"
                        name="oldPassword"
                        onBlur={handleBlur}
                        onChange={handleChange}
                        value={values.oldPassword}
                        variant="outlined"
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        error={Boolean(
                          touched.newPassword && errors.newPassword
                        )}
                        fullWidth
                        helperText={touched.newPassword && errors.newPassword}
                        label={t('new_password')}
                        type="password"
                        name="newPassword"
                        onBlur={handleBlur}
                        onChange={handleChange}
                        value={values.newPassword}
                        variant="outlined"
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        error={Boolean(
                          touched.confirmPassword && errors.confirmPassword
                        )}
                        fullWidth
                        helperText={
                          touched.confirmPassword && errors.confirmPassword
                        }
                        type="password"
                        label={t('confirm_password')}
                        name="confirmPassword"
                        onBlur={handleBlur}
                        onChange={handleChange}
                        value={values.confirmPassword}
                        variant="outlined"
                      />
                    </Grid>
                  </Grid>
                </Grid>
              </Grid>
            </DialogContent>
            <DialogActions
              sx={{
                p: 3
              }}
            >
              <Button color="secondary" onClick={handleClosePasswordModal}>
                {t('cancel')}
              </Button>
              <Button
                type="submit"
                startIcon={
                  isSubmitting ? <CircularProgress size="1rem" /> : null
                }
                disabled={isSubmitting}
                variant="contained"
              >
                {t('save')}
              </Button>
            </DialogActions>
          </form>
        )}
      </Formik>
    </CustomDialog>
  );
  return (
    <Grid container spacing={2}>
      {renderEditModal()}
      {renderPasswordModal()}
      <Grid item xs={12}>
        <Card>
          <Box
            p={3}
            display="flex"
            alignItems="center"
            justifyContent="space-between"
          >
            <Box>
              <Typography variant="h4" gutterBottom>
                {t('personal_details')}
              </Typography>
              <Typography variant="subtitle2">
                {t('personal_details_description')}
              </Typography>
            </Box>
            <Box>
              <Button
                onClick={handleOpenEditModal}
                variant="text"
                startIcon={<EditTwoToneIcon />}
              >
                {t('edit')}
              </Button>
              <Button
                onClick={handleOpenPasswordModal}
                variant="text"
                startIcon={<LockTwoToneIcon />}
              >
                {t('change_password')}
              </Button>
            </Box>
          </Box>
          <Divider />
          <CardContent
            sx={{
              p: 4
            }}
          >
            <Typography variant="subtitle2">
              <Grid container spacing={0}>
                {Object.keys(userConfig).map((key) => {
                  if (key !== 'settings')
                    return renderKeyAndValue(
                      userConfig[key].title,
                      userConfig[key].value
                    );
                })}
              </Grid>
            </Typography>
          </CardContent>
        </Card>
      </Grid>
      <Grid item xs={12}>
        <Card>
          <Box
            p={3}
            display="flex"
            alignItems="center"
            justifyContent="space-between"
          >
            <Box>
              <Typography variant="h4" gutterBottom>
                {t('notification_settings')}
              </Typography>
              <Typography variant="subtitle2">
                {t('notification_settings_description')}
              </Typography>
            </Box>
          </Box>
          <Divider />
          <CardContent
            sx={{
              p: 4
            }}
          >
            <Typography variant="subtitle2">
              <Grid container spacing={0}>
                {Object.keys(userConfig.settings).map((key) =>
                  renderKeyAndSwitch(
                    userConfig.settings[key].title,
                    userConfig.settings[key].value,
                    key
                  )
                )}
              </Grid>
            </Typography>
          </CardContent>
        </Card>
      </Grid>
    </Grid>
  );
}

export default ProfileDetails;
