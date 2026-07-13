import { useContext, useEffect } from 'react';

import { Helmet } from 'react-helmet-async';

import { Grid } from '@mui/material';
import useRefMounted from 'src/hooks/useRefMounted';
import { useTranslation } from 'react-i18next';
import ProfileCover from './ProfileCover';
import RecentActivity from './RecentActivity';
import ProfileDetails from './ProfileDetails';
import { TitleContext } from '../../../contexts/TitleContext';
import useAuth from '../../../hooks/useAuth';

function UserProfile() {
  const isMountedRef = useRefMounted();
  const { user } = useAuth();
  // @ts-ignore
  const { t }: { t: any } = useTranslation();
  const { setTitle } = useContext(TitleContext);
  useEffect(() => {
    setTitle(t('profile'));
  }, []);

  if (!user) {
    return null;
  }

  return (
    <>
      <Helmet>
        <title>{user.firstName} - Profile Details</title>
      </Helmet>
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
        {/*<Grid item xs={12} md={12}>*/}
        {/*  <MyCards />*/}
        {/*</Grid>*/}
      </Grid>
    </>
  );
}

export default UserProfile;
