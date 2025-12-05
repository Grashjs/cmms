import {
  Box,
  Button,
  Card,
  Container,
  Stack,
  styled,
  Typography,
  IconButton,
  Drawer,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Divider,
  useTheme,
  useMediaQuery,
  Slide
} from '@mui/material';
import Logo from '../LogoSign';
import { GitHub } from '@mui/icons-material';
import MenuIcon from '@mui/icons-material/Menu';
import LanguageSwitcher from '../../layouts/ExtendedSidebarLayout/Header/Buttons/LanguageSwitcher';
import { Link as RouterLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useState } from 'react';
import { demoLink, isWhiteLabeled } from '../../config';

const HeaderWrapper = styled(Card)(
  ({ theme }) => `
    width: 100%;
    display: flex;
    align-items: center;
    height: ${theme.spacing(10)};
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: ${theme.zIndex.appBar};
    margin-bottom: 0;
    box-shadow: ${theme.shadows[2]};
    border-radius: 0;
  `
);

// Spacer to prevent content from going under fixed navbar
const NavbarSpacer = styled(Box)(
  ({ theme }) => `
    height: ${theme.spacing(10)};
    margin-bottom: ${theme.spacing(10)};
  `
);

export default function NavBar() {
  const { t } = useTranslation();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  // State for hamburger menu
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const open = Boolean(anchorEl);

  // Handlers for hamburger menu
  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  return (
    <>
      <HeaderWrapper>
        <Container maxWidth="lg">
          <Stack direction="row" alignItems="center">
            <Box alignItems={'center'}>
              <Logo />
              {!isWhiteLabeled && (
                <Typography
                  style={{ cursor: 'pointer' }}
                  fontSize={13}
                  onClick={() => {
                    window.open('https://www.intel-loop.com/', '_blank');
                  }}
                >
                  Powered by Intelloop
                </Typography>
              )}
            </Box>
            <Stack
              direction="row"
              alignItems="center"
              justifyContent="space-between"
              flex={1}
            >
              <Box />
              {/* Desktop Menu */}
              <Stack
                direction="row"
                spacing={{ xs: 1, md: 2 }}
                alignItems={'center'}
                sx={{
                  display: { xs: 'none', md: 'flex' }
                }}
              >
                <Button
                  component={RouterLink}
                  to="/pricing"
                  sx={{
                    ml: 2,
                    size: { xs: 'small', md: 'medium' }
                  }}
                >
                  {t('Pricing')}
                </Button>
                {!isWhiteLabeled && (
                  <Button
                    component={'a'}
                    target={'_blank'}
                    href={'https://github.com/Grashjs/cmms'}
                    startIcon={<GitHub />}
                  >
                    GitHub
                  </Button>
                )}
                <LanguageSwitcher />
                <Button
                  component={RouterLink}
                  to="/app/work-orders"
                  variant="text"
                  sx={{
                    ml: 2,
                    size: { xs: 'small', md: 'medium' }
                  }}
                >
                  {t('login')}
                </Button>
                <Button
                  component={RouterLink}
                  to="/account/register"
                  variant="contained"
                  sx={{
                    ml: 2,
                    size: { xs: 'small', md: 'medium' }
                  }}
                >
                  {t('register')}
                </Button>
                <Button
                  href={demoLink}
                  variant="outlined"
                  sx={{
                    ml: 2,
                    size: { xs: 'small', md: 'medium' }
                  }}
                >
                  {t('book_demo')}
                </Button>
              </Stack>

              {/* Mobile Menu Icon */}
              <Box sx={{ display: { xs: 'block', md: 'none' } }}>
                <IconButton
                  onClick={handleMenuOpen}
                  size="large"
                  aria-controls={open ? 'mobile-menu' : undefined}
                  aria-haspopup="true"
                  aria-expanded={open ? 'true' : undefined}
                >
                  <MenuIcon />
                </IconButton>
                <Drawer
                  anchor="right"
                  open={open}
                  onClose={handleMenuClose}
                  sx={{
                    '& .MuiDrawer-paper': {
                      width: '100%',
                      background: theme.palette.background.default
                    }
                  }}
                  transitionDuration={300}
                >
                  <Box
                    sx={{
                      height: '100%',
                      display: 'flex',
                      flexDirection: 'column'
                    }}
                  >
                    {/* Close button at top */}
                    <Box
                      sx={{
                        display: 'flex',
                        justifyContent: 'flex-end',
                        p: 2
                      }}
                    >
                      <IconButton onClick={handleMenuClose}>
                        <MenuIcon />
                      </IconButton>
                    </Box>

                    {/* Main menu items */}
                    <List sx={{ flexGrow: 1, pt: 2 }}>
                      <Slide
                        direction="left"
                        in={open}
                        mountOnEnter
                        unmountOnExit
                      >
                        <ListItem
                          component={RouterLink}
                          to="/pricing"
                          onClick={handleMenuClose}
                          sx={{ py: 2 }}
                        >
                          <ListItemText
                            primary={t('Pricing')}
                            primaryTypographyProps={{
                              variant: 'h3',
                              sx: { fontWeight: 'bold' }
                            }}
                          />
                        </ListItem>
                      </Slide>

                      <Slide
                        direction="left"
                        in={open}
                        mountOnEnter
                        unmountOnExit
                        timeout={{ enter: 400 }}
                      >
                        <ListItem
                          component="a"
                          href="https://github.com/Grashjs/cmms"
                          target="_blank"
                          onClick={handleMenuClose}
                          sx={{ py: 2 }}
                        >
                          <ListItemIcon>
                            <GitHub />
                          </ListItemIcon>
                          <ListItemText
                            primary="GitHub"
                            primaryTypographyProps={{
                              variant: 'h3',
                              sx: { fontWeight: 'bold' }
                            }}
                          />
                        </ListItem>
                      </Slide>

                      <Slide
                        direction="left"
                        in={open}
                        mountOnEnter
                        unmountOnExit
                        timeout={{ enter: 500 }}
                      >
                        <ListItem sx={{ py: 2 }}>
                          <LanguageSwitcher
                            onSwitch={() => setAnchorEl(null)}
                          />
                        </ListItem>
                      </Slide>
                    </List>

                    {/* Bottom buttons */}
                    <Divider />
                    <Box sx={{ p: 2 }}>
                      <Stack spacing={2}>
                        <Button
                          component={RouterLink}
                          to="/app/work-orders"
                          variant="text"
                          fullWidth
                          size="large"
                          onClick={handleMenuClose}
                        >
                          {t('login')}
                        </Button>
                        <Button
                          component={RouterLink}
                          to="/account/register"
                          variant="contained"
                          fullWidth
                          size="large"
                          onClick={handleMenuClose}
                        >
                          {t('register')}
                        </Button>
                        <Button
                          href={demoLink}
                          variant="outlined"
                          fullWidth
                          size="large"
                        >
                          {t('book_demo')}
                        </Button>
                      </Stack>
                    </Box>
                  </Box>
                </Drawer>
              </Box>
            </Stack>
          </Stack>
        </Container>
      </HeaderWrapper>
      <NavbarSpacer />
    </>
  );
}
