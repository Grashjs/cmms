import { useRef, useState } from 'react';
import useAuth from 'src/hooks/useAuth';
import { useNavigate } from 'react-router-dom';

import {
  alpha,
  Avatar,
  Box,
  Button,
  Divider,
  IconButton,
  ListItemText,
  MenuItem,
  MenuList,
  Popover,
  styled,
  Typography,
  useTheme,
  CircularProgress, ListItemIcon
} from '@mui/material';
import { useTranslation } from 'react-i18next';
import LockOpenTwoToneIcon from '@mui/icons-material/LockOpenTwoTone';
import ChevronRightTwoToneIcon from '@mui/icons-material/ChevronRightTwoTone';
import type { ApexOptions } from 'apexcharts';
import PersonOutlineTwoToneIcon from '@mui/icons-material/PersonOutlineTwoTone';
import BusinessTwoToneIcon from '@mui/icons-material/BusinessTwoTone';
import SwitchLeftTwoToneIcon from '@mui/icons-material/SwitchLeftTwoTone';
import HelpTwoToneIcon from '@mui/icons-material/HelpTwoTone';

const DotLegend = styled('span')(
  ({ theme }) => `
    border-radius: 22px;
    width: ${theme.spacing(1.38)};
    height: ${theme.spacing(1.4)};
    display: inline-block;
    border: ${theme.colors.alpha.white[100]} solid 2px;
`
);

const UserBoxButton = styled(IconButton)(
  ({ theme }) => `
  width: ${theme.spacing(4)};
  padding: 0;
  height: ${theme.spacing(4)};
  margin-left: ${theme.spacing(1)};
  border-radius: ${theme.general.borderRadiusLg};
  
  &:hover {
    background: ${theme.colors.primary.main};
  }
`
);

const UserAvatar = styled(Avatar)(
  ({ theme }) => `
        height: 90%;
        width: 90%;
        border-radius: ${theme.general.borderRadiusLg};
`
);

const MenuListWrapperPrimary = styled(MenuList)(
  ({ theme }) => `
  padding: ${theme.spacing(2)};

  & .MuiMenuItem-root {
      border-radius: 50px;
      padding: ${theme.spacing(1, 1, 1, 2.5)};
      min-width: 200px;
      margin-bottom: 2px;
      position: relative;
      color: ${theme.colors.alpha.black[100]};

      &.Mui-selected,
      &:hover,
      &.MuiButtonBase-root:active {
          background: ${theme.colors.primary.lighter};
          color: ${theme.colors.primary.main};
      }

      &:last-child {
          margin-bottom: 0;
      }
    }
`
);

const MenuUserBox = styled(Box)(
  ({ theme }) => `
        background: ${alpha(theme.colors.alpha.black[100], 0.08)};
        padding: ${theme.spacing(2)};
`
);

const UserBoxText = styled(Box)(
  ({ theme }) => `
        text-align: left;
        padding-left: ${theme.spacing(1)};
`
);

const UserBoxLabel = styled(Typography)(
  ({ theme }) => `
        font-weight: ${theme.typography.fontWeightBold};
        color: ${theme.palette.secondary.main};
        display: block;
`
);

const UserBoxDescription = styled(Typography)(
  ({ theme }) => `
        color: ${theme.palette.secondary.light}
`
);

function HeaderUserbox() {
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();

  const navigate = useNavigate();

  const { user, logout, switchAccount } = useAuth();
  const [switchingAccount, setSwitchingAccount] = useState<boolean>(false);

  const ref = useRef<any>(null);
  const [isOpen, setOpen] = useState<boolean>(false);

  const handleOpen = (): void => {
    setOpen(true);
  };

  const handleClose = (): void => {
    setOpen(false);
  };

  const handleLogout = async (): Promise<void> => {
    try {
      handleClose();
      await logout();
      navigate('/');
    } catch (err) {
      console.error(err);
    }
  };

  const Box1Options: ApexOptions = {
    chart: {
      background: 'transparent',
      toolbar: {
        show: false
      },
      sparkline: {
        enabled: true
      },
      zoom: {
        enabled: false
      }
    },
    labels: [
      'Monday',
      'Tuesday',
      'Wednesday',
      'Thursday',
      'Friday',
      'Saturday',
      'Sunday',
      'Last Week',
      'Last Month',
      'Last Year',
      'Last Decade'
    ],
    theme: {
      mode: theme.palette.mode === 'dark' ? 'light' : 'dark'
    },
    stroke: {
      colors: [theme.colors.error.main],
      curve: 'smooth',
      width: 3
    },
    grid: {
      padding: {
        right: 5,
        left: 5,
        bottom: 5
      }
    },
    tooltip: {
      fixed: {
        enabled: true
      },
      x: {
        show: false
      },
      y: {
        title: {
          formatter: function() {
            return 'Orders:';
          }
        }
      },
      marker: {
        show: true
      }
    },
    colors: [theme.colors.error.main]
  };
  const Box1Data = [
    {
      name: 'Revenue',
      data: [465, 546, 234, 576, 554, 338, 427, 348, 586, 254, 348]
    }
  ];

  return (
    <>
      <UserBoxButton color="primary" ref={ref} onClick={handleOpen}>
        <UserAvatar alt={user.firstName} src={user.image?.url} />
      </UserBoxButton>
      <Popover
        disableScrollLock
        anchorEl={ref.current}
        onClose={handleClose}
        open={isOpen}
        anchorOrigin={{
          vertical: 'top',
          horizontal: 'right'
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right'
        }}
      >
        <MenuUserBox
          sx={{
            minWidth: 210
          }}
          display="flex"
        >
          <Avatar
            variant="rounded"
            alt={user.firstName}
            src={user.image?.url}
          />
          <UserBoxText>
            <UserBoxLabel variant="body1">{user.firstName}</UserBoxLabel>
            <UserBoxDescription variant="body2">
              {user.jobTitle ?? user.role.code === 'USER_CREATED'
                ? user.role.name
                : t(`${user.role.code}_name`)}
            </UserBoxDescription>
          </UserBoxText>
        </MenuUserBox>
        <Divider
          sx={{
            mb: 0
          }}
        />
        <MenuListWrapperPrimary disablePadding>
          <MenuItem
            onClick={() => {
              handleClose();
              navigate('/app/account/profile');
            }}
          >
            <ListItemIcon>
              <PersonOutlineTwoToneIcon />
            </ListItemIcon>
            <ListItemText
              primaryTypographyProps={{
                variant: 'h5'
              }}
              primary={t('my_account')}
            />
            <ChevronRightTwoToneIcon
              sx={{
                color: `${theme.colors.alpha.black[30]}`,
                opacity: 0.8
              }}
            />
          </MenuItem>{
          user.parentSuperAccount && (<MenuItem
            onClick={() => {
              setSwitchingAccount(true);
              switchAccount(user.parentSuperAccount.superUserId).then(() => {
                handleClose();
                navigate('/app/switch-account');
              })
                .finally(() => setSwitchingAccount(false));
            }}
          >
            <ListItemIcon>
              <SwitchLeftTwoToneIcon />
            </ListItemIcon>
            <ListItemText
              primaryTypographyProps={{
                variant: 'h5'
              }}
              primary={t('switch_to_super_user')}
            />
            {switchingAccount ? <CircularProgress size="1rem" /> : <ChevronRightTwoToneIcon
              sx={{
                color: `${theme.colors.alpha.black[30]}`,
                opacity: 0.8
              }}
            />}
          </MenuItem>)
        }
          <MenuItem
            onClick={() => {
              handleClose();
              navigate('/app/account/company-profile');
            }}
          >
            <ListItemIcon>
              <BusinessTwoToneIcon />
            </ListItemIcon>
            <ListItemText
              primaryTypographyProps={{
                variant: 'h5'
              }}
              primary={t('my_company')}
            />
            <Box display="flex" alignItems="center">
              <ChevronRightTwoToneIcon
                sx={{
                  ml: 1,
                  color: `${theme.colors.alpha.black[30]}`,
                  opacity: 0.8
                }}
              />
            </Box>
          </MenuItem>
          <MenuItem
            onClick={() => {
              window.open('https://grashjs.github.io/user-guide');
            }}
          >
            <ListItemIcon>
              <HelpTwoToneIcon />
            </ListItemIcon>
            <ListItemText
              primaryTypographyProps={{
                variant: 'h5'
              }}
              primary={t('documentation')}
            />
            <Box display="flex" alignItems="center">
              <ChevronRightTwoToneIcon
                sx={{
                  ml: 1,
                  color: `${theme.colors.alpha.black[30]}`,
                  opacity: 0.8
                }}
              />
            </Box>
          </MenuItem>
        </MenuListWrapperPrimary>
        <Divider />
        {/*<Box m={1}>*/}
        {/*  <Box px={2} pt={1} pb={0.5} display="flex" alignItems="flex-start">*/}
        {/*    <Text color="warning">*/}
        {/*      <MonetizationOnTwoToneIcon fontSize="large" />*/}
        {/*    </Text>*/}
        {/*    <Box ml={1}>*/}
        {/*      <Typography variant="h3">$14,264</Typography>*/}
        {/*      <Typography noWrap variant="subtitle2">*/}
        {/*        {t('total value')}*/}
        {/*      </Typography>*/}
        {/*    </Box>*/}
        {/*  </Box>*/}
        {/*  <Chart*/}
        {/*    options={Box1Options}*/}
        {/*    series={Box1Data}*/}
        {/*    type="line"*/}
        {/*    height={60}*/}
        {/*  />*/}
        {/*</Box>*/}
        {/*<Divider />*/}
        <Box m={1}>
          <Button color="primary" fullWidth onClick={handleLogout}>
            <LockOpenTwoToneIcon
              sx={{
                mr: 1
              }}
            />
            {t('Sign out')}
          </Button>
        </Box>
      </Popover>
    </>
  );
}

export default HeaderUserbox;
