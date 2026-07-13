import * as Yup from 'yup';
import type { FC } from 'react';
import { useContext, useState } from 'react';
import { Formik } from 'formik';
import { Link as RouterLink, useSearchParams } from 'react-router-dom';

import {
  Box,
  Button,
  CircularProgress,
  Divider,
  IconButton,
  InputAdornment,
  Link,
  TextField
} from '@mui/material';
import useAuth from 'src/hooks/useAuth';
import useRefMounted from 'src/hooks/useRefMounted';
import { useTranslation } from 'react-i18next';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import VisibilityIcon from '@mui/icons-material/Visibility';
import { apiUrl, isSSOEnabled, oauth2Provider } from '../../../../config';

const LoginJWT: FC = () => {
  const { login } = useAuth() as any;
  const isMountedRef = useRefMounted();
  const { t }: { t: any } = useTranslation();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [searchParams, setSearchParams] = useSearchParams();
  const [showPassword, setShowPassword] = useState<boolean>(false);

  return (
    <Formik
      initialValues={{
        email: searchParams.get('email') ?? '',
        password: '',
        submit: null
      }}
      validationSchema={Yup.object().shape({
        email: Yup.string()
          .email(t('invalid_email'))
          .max(255)
          .required(t('required_email')),
        password: Yup.string().max(255).required(t('required_password'))
      })}
      onSubmit={async (
        values,
        { setErrors, setStatus, setSubmitting }
      ): Promise<void> => {
        setSubmitting(true);
        return login(values.email, values.password)
          .catch((err) => {
            showSnackBar(t('wrong_credentials'), 'error');
            setStatus({ success: false });
          })
          .finally(() => {
            if (isMountedRef.current) {
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
        values
      }): JSX.Element => (
        <form noValidate onSubmit={handleSubmit}>
          <TextField
            error={Boolean(touched.email && errors.email)}
            fullWidth
            margin="normal"
            autoFocus
            helperText={touched.email && errors.email}
            label={t('email')}
            name="email"
            onBlur={handleBlur}
            onChange={handleChange}
            type="email"
            value={values.email}
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
              startAdornment: (
                <InputAdornment position="start">
                  <IconButton onClick={() => setShowPassword(!showPassword)}>
                    <VisibilityIcon />
                  </IconButton>
                </InputAdornment>
              )
            }}
          />
          <Box
            alignItems="center"
            display={{ xs: 'block', md: 'flex' }}
            justifyContent="space-between"
          >
            <Link component={RouterLink} to="/account/recover-password">
              <b>{t('lost_password')}</b>
            </Link>
          </Box>

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
            {t('login')}
          </Button>
          {isSSOEnabled && (
            <Box>
              <Box display="flex" alignItems="center" my={3}>
                <Divider sx={{ flexGrow: 1 }} />
                <Box px={2} color="text.secondary">
                  {t('or')}
                </Box>
                <Divider sx={{ flexGrow: 1 }} />
              </Box>

              <Button
                onClick={() => {
                  window.location.href = `${apiUrl}oauth2/authorize/${oauth2Provider.toLowerCase()}`;
                }}
                fullWidth
                size="large"
                variant="outlined"
              >
                {t('continue_with_sso')}
              </Button>
            </Box>
          )}
        </form>
      )}
    </Formik>
  );
};

export default LoginJWT;
