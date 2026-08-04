import { forwardRef, ReactElement, Ref, useContext, useState } from 'react';
import * as Yup from 'yup';
import { Formik } from 'formik';
import { Link as RouterLink } from 'react-router-dom';
import {
  Alert,
  Avatar,
  Box,
  Button,
  Card,
  CircularProgress,
  Collapse,
  Container,
  Dialog,
  IconButton,
  Link,
  Slide,
  styled,
  TextField,
  Typography
} from '@mui/material';
import { TransitionProps } from '@mui/material/transitions';
import useRefMounted from 'src/hooks/useRefMounted';
import CloseIcon from '@mui/icons-material/Close';

import { useTranslation } from 'react-i18next';
import Logo from 'src/components/LogoSign';
import CheckTwoToneIcon from '@mui/icons-material/CheckTwoTone';
import useAuth from '../../../../hooks/useAuth';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import { emailRegExp } from '../../../../utils/validators';

const Transition = forwardRef(function Transition(
  props: TransitionProps & { children: ReactElement<any, any> },
  ref: Ref<unknown>
) {
  return <Slide direction="down" ref={ref} {...props} />;
});

const MainContent = styled(Box)(
  () => `
    height: 100%;
    display: flex;
    flex: 1;
    flex-direction: column;
    align-items: center;
    justify-content: center;
`
);

const DialogWrapper = styled(Dialog)(
  () => `
      .MuiDialog-paper {
        overflow: visible;
      }
`
);

const AvatarSuccess = styled(Avatar)(
  ({ theme }) => `
      background-color: ${theme.colors.success.main};
      color: ${theme.palette.success.contrastText};
      width: ${theme.spacing(12)};
      height: ${theme.spacing(12)};
      box-shadow: ${theme.colors.shadows.success};
      top: -${theme.spacing(6)};
      position: absolute;
      left: 50%;
      margin-left: -${theme.spacing(6)};

      .MuiSvgIcon-root {
        font-size: ${theme.typography.pxToRem(45)};
      }
`
);

function RecoverPasswordBasic() {
  const { t }: { t: any } = useTranslation();
  const isMountedRef = useRefMounted();
  const { resetPassword } = useAuth();
  const [openAlert, setOpenAlert] = useState(true);
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [openDialog, setOpenDialog] = useState(false);

  const handleOpenDialog = () => {
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
  };

  return (
    <>
<MainContent>
        <Container maxWidth="sm">
          <Logo />
          <Card
            sx={{
              mt: 3,
              p: 4
            }}
          >
            <Box>
              <Typography
                variant="h2"
                sx={{
                  mb: 1
                }}
              >
                {t('Recover Password')}
              </Typography>
              <Typography
                variant="h4"
                color="text.secondary"
                fontWeight="normal"
                sx={{
                  mb: 3
                }}
              >
                {t('recover_password_description')}
              </Typography>
            </Box>

            <Formik
              initialValues={{
                email: '',
                submit: null
              }}
              validationSchema={Yup.object().shape({
                email: Yup.string()
                  .matches(emailRegExp, t('invalid_email'))
                  .max(255)
                  .required(t('required_email'))
              })}
              onSubmit={async (
                values,
                { setErrors, setStatus, setSubmitting }
              ) => {
                setSubmitting(true);
                return resetPassword(values.email)
                  .then((success) => {
                    if (success) {
                      handleOpenDialog();
                    } else {
                      showSnackBar(t("The operation didn't succeed"), 'error');
                    }
                  })
                  .catch((err) =>
                    showSnackBar(t("The operation didn't succeed"), 'error')
                  )
                  .finally(() => setSubmitting(false));
              }}
            >
              {({
                errors,
                handleBlur,
                handleChange,
                handleSubmit,
                touched,
                values,
                isSubmitting
              }) => (
                <form noValidate onSubmit={handleSubmit}>
                  <TextField
                    error={Boolean(touched.email && errors.email)}
                    fullWidth
                    helperText={touched.email && errors.email}
                    label={t('email')}
                    margin="normal"
                    name="email"
                    onBlur={handleBlur}
                    onChange={handleChange}
                    type="email"
                    value={values.email}
                    variant="outlined"
                  />

                  <Button
                    sx={{
                      mt: 3
                    }}
                    disabled={Boolean(
                      (touched.email && errors.email) || isSubmitting
                    )}
                    //onClick={handleResetPassword}
                    type="submit"
                    fullWidth
                    size="large"
                    variant="contained"
                    startIcon={
                      isSubmitting ? <CircularProgress size="1rem" /> : null
                    }
                  >
                    {t('send_me_new_password')}
                  </Button>
                </form>
              )}
            </Formik>
          </Card>
          <Box mt={3} textAlign="right">
            <Typography
              component="span"
              variant="subtitle2"
              color="text.primary"
              fontWeight="bold"
            >
              {t('want_to_sign_in_again')}
            </Typography>{' '}
            <Link component={RouterLink} to="/account/login">
              <b>{t('click_here')}</b>
            </Link>
          </Box>
        </Container>
      </MainContent>

      <DialogWrapper
        open={openDialog}
        maxWidth="sm"
        fullWidth
        TransitionComponent={Transition}
        keepMounted
        onClose={handleCloseDialog}
      >
        <Box
          sx={{
            px: 4,
            pb: 4,
            pt: 10
          }}
        >
          <AvatarSuccess>
            <CheckTwoToneIcon />
          </AvatarSuccess>

          <Collapse in={openAlert}>
            <Alert
              action={
                <IconButton
                  aria-label="close"
                  color="inherit"
                  size="small"
                  onClick={() => {
                    setOpenAlert(false);
                  }}
                >
                  <CloseIcon fontSize="inherit" />
                </IconButton>
              }
              severity="info"
            >
              {t(
                'The password reset instructions have been sent to your email'
              )}
            </Alert>
          </Collapse>

          <Typography
            align="center"
            sx={{
              py: 4,
              px: 10
            }}
            variant="h3"
          >
            {t('check_mails_for_instructions')}
          </Typography>

          <Button
            fullWidth
            size="large"
            variant="contained"
            onClick={handleCloseDialog}
            href="/account/login"
          >
            {t('login')}
          </Button>
        </Box>
      </DialogWrapper>
    </>
  );
}

export default RecoverPasswordBasic;
