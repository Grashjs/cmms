import * as Yup from 'yup';
import { Formik } from 'formik';
import {
  Autocomplete,
  Box,
  Button,
  Checkbox,
  CircularProgress,
  FormControlLabel,
  FormHelperText,
  Grid,
  IconButton,
  InputAdornment,
  Stack,
  TextField,
  Typography
} from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import useAuth from 'src/hooks/useAuth';
import useRefMounted from 'src/hooks/useRefMounted';
import { useTranslation } from 'react-i18next';
import { phoneRegExp } from '../../../../utils/validators';
import { useContext, useState } from 'react';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { useNavigate } from 'react-router-dom';
import i18n from 'i18next';
import countries from '../../../../i18n/countries';
import { verify } from '../../../../utils/jwt';
import { useUtmTracker } from '@nik0di3m/utm-tracker-hook';
import { inviteUsers } from '../../../../slices/user';
import { useDispatch } from '../../../../store';
import { homeUrl } from '../../../../config';
import { getLocalizedHomeUrl } from '../../../../utils/urlPaths';

function RegisterJWT({
  email,
  role,
  invitationMode,
  onInvitationSuccess,
  subscriptionPlanId
}: {
  email?: string | undefined;
  role?: number | undefined;
  invitationMode?: boolean;
  onInvitationSuccess?: () => void;
  subscriptionPlanId?: string;
}) {
  const { register, loginInternal, user } = useAuth();
  const isMountedRef = useRefMounted();
  const { t, i18n } = useTranslation();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const navigate = useNavigate();
  const getLanguage = i18n.language;
  const [showPassword, setShowPassword] = useState(false);
  const dispatch = useDispatch();
  const getFieldsAndShapes = (): [
    { [key: string]: any },
    { [key: string]: any }
  ] => {
    let fields = {
      email: email ?? '',
      firstName: '',
      lastName: '',
      phone: '',
      password: '',
      companyName: '',
      submit: null
    };
    let shape = {
      email: Yup.string()
        .email(t('invalid_email'))
        .max(255)
        .required(t('required_email')),
      firstName: Yup.string().max(255).required(t('required_firstName')),
      lastName: Yup.string().max(255).required(t('required_lastName')),
      companyName: Yup.string().max(255).required(t('required_company')),
      phone: Yup.string().matches(phoneRegExp, t('invalid_phone')),
      password: Yup.string().min(8).max(255).required(t('required_password'))
    };
    if (role) {
      const keysToDelete = ['companyName'];
      keysToDelete.forEach((key) => {
        delete fields[key];
        delete shape[key];
      });
    }
    return [fields, shape];
  };
  return (
    <Formik
      initialValues={getFieldsAndShapes()[0]}
      validationSchema={Yup.object().shape(getFieldsAndShapes()[1])}
      onSubmit={async (values, { setErrors, setStatus, setSubmitting }) => {
        setSubmitting(true);
        const valuesClone = { ...values };
        valuesClone.language = getLanguage.toUpperCase();
        valuesClone.subscriptionPlanId = subscriptionPlanId;
        valuesClone.phone =
          (values.countryCode ? `+${values.countryCode.phone}` : '') +
          `${values.phone}`;
        if (invitationMode)
          await dispatch(inviteUsers(role, [valuesClone.email], true));
        return register(
          role ? { ...valuesClone, role: { id: role } } : valuesClone,
          invitationMode
        )
          .then(async (res) => {
            if (invitationMode) {
              onInvitationSuccess();
            } else {
              if (!(res && (await verify(res.message)))) {
                if (!role) showSnackBar(t('verify_email'), 'success');
                navigate(role ? '/account/login' : '/account/verify');
              }
            }
          })
          .catch((err) => {
            let errorMessage = 'An unknown error occurred';

            // Check if the error message contains a JSON string
            if (typeof err.message === 'string') {
              try {
                const parsedError = JSON.parse(err.message);
                // If the JSON contains a 'message' field, use it
                if (parsedError && parsedError.message) {
                  errorMessage = parsedError.message;
                }
              } catch (e) {
                // If parsing fails, fallback to the original error message
                errorMessage = err.message;
              }
            } else {
              // In case err.message is not a string, just use the general error
              errorMessage = 'An unknown error occurred';
            }
            showSnackBar(errorMessage, 'error');
            console.error(err);
          })
          .finally(() => {
            if (isMountedRef.current) {
              setStatus({ success: true });
              setSubmitting(false);
            }
          });
      }}
    >
      {({
        errors,
        handleBlur,
        handleChange,
        handleSubmit,
        isSubmitting,
        touched,
        values,
        setFieldValue
      }) => (
        <form noValidate onSubmit={handleSubmit}>
          <Grid container spacing={1}>
            <Grid item xs={12} lg={6}>
              <TextField
                error={Boolean(touched.firstName && errors.firstName)}
                fullWidth
                margin="normal"
                helperText={touched.firstName && errors.firstName}
                label={t('first_name')}
                name="firstName"
                onBlur={handleBlur}
                onChange={handleChange}
                value={values.firstName}
                variant="outlined"
              />
            </Grid>
            <Grid item xs={12} lg={6}>
              <TextField
                error={Boolean(touched.lastName && errors.lastName)}
                fullWidth
                margin="normal"
                helperText={touched.lastName && errors.lastName}
                label={t('last_name')}
                name="lastName"
                onBlur={handleBlur}
                onChange={handleChange}
                value={values.lastName}
                variant="outlined"
              />
            </Grid>
          </Grid>
          <TextField
            error={Boolean(touched.email && errors.email)}
            fullWidth
            margin="normal"
            helperText={touched.email && errors.email}
            label={t('email')}
            name="email"
            disabled={!!email}
            onBlur={handleBlur}
            onChange={handleChange}
            type="email"
            value={values.email}
            variant="outlined"
          />
          <TextField
            error={Boolean(touched.phone && errors.phone)}
            fullWidth
            margin="normal"
            helperText={touched.phone && errors.phone}
            label={t('phone')}
            name="phone"
            onBlur={handleBlur}
            onChange={handleChange}
            value={values.phone}
            variant="outlined"
          />
          <TextField
            error={Boolean(touched.password && errors.password)}
            fullWidth
            margin="normal"
            helperText={touched.password && errors.password}
            label={t('password')}
            name="password"
            onBlur={handleBlur}
            onChange={handleChange}
            type={showPassword ? 'text' : 'password'}
            value={values.password}
            variant="outlined"
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton onClick={() => setShowPassword(!showPassword)}>
                    <VisibilityIcon />
                  </IconButton>
                </InputAdornment>
              )
            }}
          />
          {!role && !invitationMode && (
            <>
              <Grid item xs={12} lg={6}>
                <TextField
                  error={Boolean(touched.companyName && errors.companyName)}
                  fullWidth
                  margin="normal"
                  helperText={touched.companyName && errors.companyName}
                  label={t('companyName')}
                  name="companyName"
                  onBlur={handleBlur}
                  onChange={handleChange}
                  value={values.companyName}
                  variant="outlined"
                />
              </Grid>
            </>
          )}
          <Button
            sx={{
              mt: 3
            }}
            color="primary"
            startIcon={isSubmitting ? <CircularProgress size="1rem" /> : null}
            disabled={isSubmitting}
            type="submit"
            fullWidth
            size="large"
            variant="contained"
          >
            {t(invitationMode ? 'invite' : 'create_your_account')}
          </Button>
          {!invitationMode && (
            <Typography mt={2} variant="body2">
              {t('i_accept')}{' '}
              <Typography
                color={'primary'}
                href={getLocalizedHomeUrl('terms-of-service', i18n.language)}
                target={'_blank'}
                component="a"
                style={{ cursor: 'pointer' }}
              >
                {t('terms_conditions')}
              </Typography>
              .
            </Typography>
          )}
        </form>
      )}
    </Formik>
  );
}

export default RegisterJWT;
