import { FC, ReactNode, useContext, useEffect } from 'react';
import { Box, useTheme } from '@mui/material';
import { SidebarContext } from 'src/contexts/SidebarContext';
import { Outlet } from 'react-router-dom';

import Sidebar from './Sidebar';
import Header from './Header';
import Intercom from '@intercom/messenger-js-sdk';
import useAuth from '../../hooks/useAuth';
import { intercomId, isCloudVersion } from '../../config';

interface ExtendedSidebarLayoutProps {
  children?: ReactNode;
}

const ExtendedSidebarLayout: FC<ExtendedSidebarLayoutProps> = () => {
  const theme = useTheme();
  const { user } = useAuth();
  const { sidebarCollapsed } = useContext(SidebarContext);

  useEffect(() => {
    if (user && user.ownsCompany && isCloudVersion && intercomId)
      Intercom({
        app_id: intercomId,
        user_id: user.email,
        name: `${user.firstName} ${user.lastName}`,
        // intercom_user_jwt: localStorage.getItem('intercomAccessToken'),
        email: user.email,
        created_at: Math.floor(new Date(user.createdAt).getTime() / 1000)
      });
  }, [user?.id]);

  return (
    <>
      <Box
        sx={{
          flex: 1,
          height: '100%',

          '.MuiPageTitle-wrapper': {
            background: theme.colors.alpha.white[100],
            marginBottom: theme.spacing(3),
            borderBottom: `1px solid ${theme.palette.divider}`
          }
        }}
      >
        <Header />
        <Sidebar />
        <Box
          sx={{
            position: 'relative',
            zIndex: 5,
            display: 'block',
            flex: 1,
            pt: `${theme.header.height}`,
            minWidth: 0,
            transition: theme.transitions.create('margin', {
              duration: theme.transitions.duration.shorter
            }),
            [theme.breakpoints.up('lg')]: {
              ml: sidebarCollapsed
                ? theme.sidebar.collapsedWidth
                : theme.sidebar.width
            }
          }}
        >
          <Box display="block">
            <Outlet />
          </Box>
          {/*<ThemeSettings />*/}
        </Box>
      </Box>
    </>
  );
};

export default ExtendedSidebarLayout;
