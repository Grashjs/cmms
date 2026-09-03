import { useContext, useEffect } from 'react';


import { Button, Grid } from '@mui/material';
import { useTranslation } from 'react-i18next';
import ProfileCover from './ProfileCover';
import RecentActivity from './RecentActivity';
import ProfileDetails from './ProfileDetails';
import { TitleContext } from '../../../contexts/TitleContext';
import useAuth from '../../../hooks/useAuth';
import api from '../../../utils/api';
import { useNavigate } from 'react-router-dom';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';

function UserProfile() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { t } = useTranslation();
  const { setTitle } = useContext(TitleContext);
  useEffect(() => {
    setTitle(t('profile'));
  }, []);

  if (!user) {
    return null;
  }

  const onDeleteAccount = async () => {
    if (window.confirm('Are you sure you want to delete your account?')) {
      const { success } = await api.post<{ success: boolean }>(
        `auth/delete-account-request`,
        {}
      );
      if (success) {
        showSnackBar(t('delete_account_email_confirmation'), 'warning');
      }
    }
  };

  return (
    <>
<Grid
        sx={{
          px: 4
        }}
        container
        direction="row"
        justifyContent="center"
        alignItems="stretch"
        spacing={2}
      >
        <Grid item xs={12} md={4}>
          <ProfileCover user={user} />
        </Grid>
        <Grid item xs={12} md={8}>
          <RecentActivity />
        </Grid>
        <Grid item xs={12}>
          <ProfileDetails />
        </Grid>
        <Grid item xs={12}>
          <Button
            sx={{ mb: 2 }}
            onClick={onDeleteAccount}
            variant="contained"
            color="error"
          >
            {t('delete_account')}
          </Button>
        </Grid>
        {/*<Grid item xs={12} md={12}>*/}
        {/*  <MyCards />*/}
        {/*</Grid>*/}
      </Grid>
    </>
  );
}

export default UserProfile;
