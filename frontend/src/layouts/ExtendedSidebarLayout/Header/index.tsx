import { useContext } from 'react';

import {
  alpha,
  Box,
  Divider,
  IconButton,
  Stack,
  styled,
  Tooltip,
  Typography,
  useTheme
} from '@mui/material';
import MenuTwoToneIcon from '@mui/icons-material/MenuTwoTone';
import MenuOpenTwoToneIcon from '@mui/icons-material/MenuOpenTwoTone';
import { SidebarContext } from 'src/contexts/SidebarContext';
import ArrowBackTwoToneIcon from '@mui/icons-material/ArrowBackTwoTone';
import CloseTwoToneIcon from '@mui/icons-material/CloseTwoTone';

import HeaderButtons from './Buttons';
import HeaderUserbox from './Userbox';
import { useTranslation } from 'react-i18next';
import { TitleContext } from '../../../contexts/TitleContext';
import { useNavigate, useLocation } from 'react-router-dom';

const HeaderWrapper = styled(Box)(
  ({ theme }) => `
        height: ${theme.header.height};
        color: ${theme.header.textColor};
        right: 0;
        z-index: 6;
        background-color: ${alpha(theme.header.background, 0.92)};
        backdrop-filter: saturate(180%) blur(8px);
        border-bottom: 1px solid ${theme.palette.divider};
        position: fixed;
        justify-content: space-between;
        width: 100%;
        transition: ${theme.transitions.create('left', {
          duration: theme.transitions.duration.shorter
        })};
`
);

function Header() {
  const {
    sidebarToggle,
    toggleSidebar,
    sidebarCollapsed,
    toggleSidebarCollapsed
  } = useContext(SidebarContext);
  const { title } = useContext(TitleContext);
  const theme = useTheme();
  const { t }: { t: any } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <HeaderWrapper
      component="header"
      display="flex"
      alignItems="center"
      sx={{
        px: { xs: 1.5, sm: 2, lg: 3 },
        [theme.breakpoints.up('lg')]: {
          left: sidebarCollapsed
            ? theme.sidebar.collapsedWidth
            : theme.sidebar.width,
          width: 'auto'
        }
      }}
    >
      <Stack
        direction="row"
        divider={
          <Divider orientation="vertical" flexItem sx={{ my: 1.25 }} />
        }
        alignItems="center"
        spacing={1.5}
        sx={{ minWidth: 0 }}
      >
        <Stack direction="row" alignItems="center" spacing={0.5}>
          <Tooltip
            arrow
            title={t(sidebarCollapsed ? 'expand_menu' : 'collapse_menu', {
              defaultValue: sidebarCollapsed ? 'Expand menu' : 'Collapse menu'
            })}
          >
            <IconButton
              onClick={toggleSidebarCollapsed}
              aria-label={sidebarCollapsed ? 'Expand menu' : 'Collapse menu'}
              sx={{ display: { xs: 'none', lg: 'inline-flex' } }}
            >
              {sidebarCollapsed ? (
                <MenuTwoToneIcon fontSize="small" />
              ) : (
                <MenuOpenTwoToneIcon fontSize="small" />
              )}
            </IconButton>
          </Tooltip>
          <IconButton
            onClick={() => navigate(-1)}
            disabled={location.key === 'default'}
            aria-label={t('go_back', { defaultValue: 'Go back' })}
          >
            <ArrowBackTwoToneIcon fontSize="small" />
          </IconButton>
        </Stack>
        <Typography
          variant="h3"
          component="h1"
          noWrap
          sx={{
            fontWeight: 600,
            fontSize: { xs: '1rem', sm: '1.125rem' },
            letterSpacing: '-0.01em',
            color: 'text.primary',
            minWidth: 0
          }}
        >
          {title}
        </Typography>
      </Stack>
      <Stack direction="row" alignItems="center" spacing={1}>
        <HeaderButtons />
        <HeaderUserbox />
        <Box
          component="span"
          sx={{
            display: { lg: 'none', xs: 'inline-flex' }
          }}
        >
          <Tooltip arrow title={t('toggle_menu')}>
            <IconButton color="primary" onClick={toggleSidebar}>
              {!sidebarToggle ? (
                <MenuTwoToneIcon fontSize="small" />
              ) : (
                <CloseTwoToneIcon fontSize="small" />
              )}
            </IconButton>
          </Tooltip>
        </Box>
      </Stack>
    </HeaderWrapper>
  );
}

export default Header;
