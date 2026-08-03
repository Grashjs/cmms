import { useContext } from 'react';
import Scrollbar from 'src/components/Scrollbar';
import { SidebarContext } from 'src/contexts/SidebarContext';

import {
  Box,
  Button,
  Divider,
  Drawer,
  Link,
  Stack,
  styled,
  useTheme
} from '@mui/material';
import SidebarMenu from './SidebarMenu';
import SidebarFooter from './SidebarFooter';
import Logo from 'src/components/LogoSign';
import { isCloudVersion } from '../../../config';
import useAuth from '../../../hooks/useAuth';
import dayjs from 'dayjs';

// Colours come from theme.sidebar.* throughout. The Tokyo template this is forked
// from painted the sidebar dark even in light mode, ignoring its own theme tokens —
// every scheme here defines a light sidebar, so the tokens were simply unused.
const SidebarWrapper = styled(Box)(
  ({ theme }) => `
        width: ${theme.sidebar.width};
        min-width: ${theme.sidebar.width};
        color: ${theme.sidebar.textColor};
        position: relative;
        z-index: 7;
        height: 100%;
        padding-bottom: 61px;
`
);

function Sidebar() {
  const { sidebarToggle, toggleSidebar } = useContext(SidebarContext);
  const closeSidebar = () => toggleSidebar();
  const theme = useTheme();

  return (
    <>
      <SidebarWrapper
        sx={{
          display: {
            xs: 'none',
            lg: 'inline-block'
          },
          position: 'fixed',
          left: 0,
          top: 0,
          background: theme.sidebar.background,
          boxShadow: theme.sidebar.boxShadow
        }}
      >
        <Scrollbar>
          <Box mt={3}>
            <Box
              sx={{
                display: 'flex',
                justifyContent: 'center',
                flexDirection: 'row'
              }}
            >
              <Box>
                <Logo />
              </Box>
            </Box>
          </Box>
          <Divider
            sx={{
              mt: theme.spacing(1),
              mx: theme.spacing(2),
              background: theme.sidebar.dividerBg
            }}
          />
          <SidebarMenu />
        </Scrollbar>
        <Divider
          sx={{
            background: theme.sidebar.dividerBg
          }}
        />
        <SidebarFooter />
      </SidebarWrapper>
      <Drawer
        sx={{
          boxShadow: `${theme.sidebar.boxShadow}`
        }}
        anchor={theme.direction === 'rtl' ? 'right' : 'left'}
        open={sidebarToggle}
        onClose={closeSidebar}
        variant="temporary"
        elevation={9}
      >
        <SidebarWrapper
          sx={{
            background: theme.sidebar.background
          }}
        >
          <Scrollbar>
            <Box mt={3}>
              <Box
                sx={{
                  display: 'flex',
                  justifyContent: 'center',
                  flexDirection: 'row'
                }}
              >
                <Box>
                  <Logo />
                </Box>
              </Box>
            </Box>
            <Divider
              sx={{
                mt: theme.spacing(1),
                mx: theme.spacing(2),
                background: theme.sidebar.dividerBg
              }}
            />
            <SidebarMenu />
          </Scrollbar>
          <SidebarFooter />
        </SidebarWrapper>
      </Drawer>
    </>
  );
}

export default Sidebar;
