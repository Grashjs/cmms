import { useContext } from 'react';
import Scrollbar from 'src/components/Scrollbar';
import { SidebarContext } from 'src/contexts/SidebarContext';

import {
  alpha,
  Box,
  Button,
  darken,
  Divider,
  Drawer,
  lighten,
  Link,
  Stack,
  styled,
  Typography,
  useTheme
} from '@mui/material';
import SidebarMenu from './SidebarMenu';
import SidebarFooter from './SidebarFooter';
import Logo from 'src/components/LogoSign';
import { isCloudVersion, isWhiteLabeled } from '../../../config';
import useAuth from '../../../hooks/useAuth';
import dayjs from 'dayjs';

const SidebarWrapper = styled(Box)(
  ({ theme }) => `
        width: ${theme.sidebar.width};
        min-width: ${theme.sidebar.width};
        color: ${theme.colors.alpha.trueWhite[70]};
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
  const { user, company } = useAuth();
  const TRIAL_DAYS = 15;

  const daysPassed = dayjs().diff(dayjs(company.createdAt), 'day');
  const daysLeft = TRIAL_DAYS - daysPassed;

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
          background:
            theme.palette.mode === 'dark'
              ? alpha(lighten(theme.header.background, 0.1), 0.5)
              : darken(theme.colors.alpha.black[100], 0.5),
          boxShadow:
            theme.palette.mode === 'dark' ? theme.sidebar.boxShadow : 'none'
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
                <Logo white />
                {!isWhiteLabeled && (
                  <Typography
                    style={{ cursor: 'pointer', color: 'white' }}
                    fontSize={13}
                    onClick={() => {
                      window.open('https://www.intel-loop.com/', '_blank');
                    }}
                  >
                    Powered by Intelloop
                  </Typography>
                )}
              </Box>
            </Box>
          </Box>
          <Divider
            sx={{
              mt: theme.spacing(1),
              mx: theme.spacing(2),
              background: theme.colors.alpha.trueWhite[10]
            }}
          />
          {isCloudVersion &&
            !company.demo &&
            user.ownsCompany &&
            !company.subscription.activated && (
              <Stack
                sx={{
                  backgroundColor: 'rgb(51, 194, 255)',
                  p: 2,
                  mx: 2,
                  mt: 2,
                  borderRadius: 2
                }}
                spacing={1}
              >
                <Typography
                  color={'white'}
                  fontSize={'16px'}
                  fontWeight={'bold'}
                >
                  {daysLeft > 0
                    ? `Your trial ends in ${daysLeft} days`
                    : `Your trial has ended`}
                </Typography>
                <Typography color={'white'} fontSize={'14px'}>
                  You are on the {company.subscription.subscriptionPlan.name}{' '}
                  plan
                </Typography>
                <Button
                  component={Link}
                  href={'/app/subscription/plans'}
                  variant="contained"
                  color="primary"
                  sx={{ mt: 1 }}
                >
                  Upgrade
                </Button>
              </Stack>
            )}
          <SidebarMenu />
        </Scrollbar>
        <Divider
          sx={{
            background: theme.colors.alpha.trueWhite[10]
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
            background:
              theme.palette.mode === 'dark'
                ? theme.colors.alpha.white[100]
                : darken(theme.colors.alpha.black[100], 0.5)
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
                  <Logo white />
                  {!isWhiteLabeled && (
                    <Typography
                      style={{ cursor: 'pointer', color: 'white' }}
                      fontSize={13}
                      onClick={() => {
                        window.open('https://www.intel-loop.com/', '_blank');
                      }}
                    >
                      Powered by Intelloop
                    </Typography>
                  )}
                </Box>
              </Box>
            </Box>
            <Divider
              sx={{
                mt: theme.spacing(1),
                mx: theme.spacing(2),
                background: theme.colors.alpha.trueWhite[10]
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
