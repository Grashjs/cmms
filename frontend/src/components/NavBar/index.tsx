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
  Slide,
  Menu,
  Grid,
  Collapse
} from '@mui/material';
import Logo from '../LogoSign';
import { GitHub, ExpandLess, ExpandMore } from '@mui/icons-material';
import MenuIcon from '@mui/icons-material/Menu';
import LanguageSwitcher from '../../layouts/ExtendedSidebarLayout/Header/Buttons/LanguageSwitcher';
import { Link as RouterLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useState } from 'react';
import { demoLink, isWhiteLabeled } from '../../config';
import { industriesLinks, useCaseLinks } from '../../utils/urlPaths';

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

  // State for Solutions menu (Desktop)
  const [solutionsAnchorEl, setSolutionsAnchorEl] =
    useState<null | HTMLElement>(null);
  const solutionsOpen = Boolean(solutionsAnchorEl);

  // State for Solutions collapse (Mobile)
  const [solutionsMobileOpen, setSolutionsMobileOpen] = useState(false);

  // Handlers for Solutions menu
  const handleSolutionsOpen = (event: React.MouseEvent<HTMLElement>) => {
    setSolutionsAnchorEl(event.currentTarget);
  };

  const handleSolutionsClose = () => {
    setSolutionsAnchorEl(null);
  };

  const handleSolutionsMobileToggle = () => {
    setSolutionsMobileOpen(!solutionsMobileOpen);
  };

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
                  onClick={handleSolutionsOpen}
                  onMouseEnter={handleSolutionsOpen}
                  endIcon={solutionsOpen ? <ExpandLess /> : <ExpandMore />}
                >
                  {t('Solutions')}
                </Button>
                <Menu
                  id="solutions-menu"
                  anchorEl={solutionsAnchorEl}
                  open={solutionsOpen}
                  onClose={handleSolutionsClose}
                  MenuListProps={{
                    onMouseLeave: handleSolutionsClose,
                    sx: { p: 0 }
                  }}
                  PaperProps={{
                    sx: {
                      mt: 1.5,
                      boxShadow: theme.shadows[5],
                      borderRadius: 1,
                      minWidth: 600,
                      maxWidth: 800
                    }
                  }}
                >
                  <Box sx={{ p: 3 }}>
                    <Grid container spacing={4}>
                      {/* Use Cases Column */}
                      <Grid item xs={12} md={4}>
                        <Typography
                          variant="h6"
                          sx={{
                            mb: 2,
                            fontWeight: 'bold',
                            color: theme.palette.primary.main,
                            textTransform: 'uppercase',
                            fontSize: 12,
                            letterSpacing: 1
                          }}
                        >
                          {t('Use cases')}
                        </Typography>
                        <List dense disablePadding>
                          {useCaseLinks.map((link) => (
                            <ListItem
                              key={link.title}
                              component={RouterLink}
                              to={link.href}
                              onClick={handleSolutionsClose}
                              sx={{
                                px: 0,
                                py: 1,
                                color: 'inherit',
                                textDecoration: 'none',
                                '&:hover': {
                                  color: theme.palette.primary.main,
                                  backgroundColor: 'transparent'
                                }
                              }}
                            >
                              <ListItemText
                                primary={link.title}
                                primaryTypographyProps={{
                                  variant: 'body2',
                                  sx: { fontWeight: 500 }
                                }}
                              />
                            </ListItem>
                          ))}
                        </List>
                      </Grid>

                      {/* Industries Section (2 columns) */}
                      <Grid item xs={12} md={8}>
                        <Typography
                          variant="h6"
                          sx={{
                            mb: 2,
                            fontWeight: 'bold',
                            color: theme.palette.primary.main,
                            textTransform: 'uppercase',
                            fontSize: 12,
                            letterSpacing: 1
                          }}
                        >
                          {t('Industries')}
                        </Typography>
                        <Grid container spacing={2}>
                          {/* We want 2 columns for industries */}
                          <Grid item xs={6}>
                            <List dense disablePadding>
                              {industriesLinks
                                .slice(0, Math.ceil(industriesLinks.length / 2))
                                .map((link) => (
                                  <ListItem
                                    key={link.title}
                                    component={RouterLink}
                                    to={link.href}
                                    onClick={handleSolutionsClose}
                                    sx={{
                                      px: 0,
                                      py: 1,
                                      color: 'inherit',
                                      textDecoration: 'none',
                                      '&:hover': {
                                        color: theme.palette.primary.main,
                                        backgroundColor: 'transparent'
                                      }
                                    }}
                                  >
                                    <ListItemIcon
                                      sx={{ minWidth: 36, color: 'inherit' }}
                                    >
                                      <link.icon fontSize="small" />
                                    </ListItemIcon>
                                    <ListItemText
                                      primary={link.title}
                                      primaryTypographyProps={{
                                        variant: 'body2',
                                        sx: { fontWeight: 500 }
                                      }}
                                    />
                                  </ListItem>
                                ))}
                            </List>
                          </Grid>
                          <Grid item xs={6}>
                            <List dense disablePadding>
                              {industriesLinks
                                .slice(Math.ceil(industriesLinks.length / 2))
                                .map((link) => (
                                  <ListItem
                                    key={link.title}
                                    component={RouterLink}
                                    to={link.href}
                                    onClick={handleSolutionsClose}
                                    sx={{
                                      px: 0,
                                      py: 1,
                                      color: 'inherit',
                                      textDecoration: 'none',
                                      '&:hover': {
                                        color: theme.palette.primary.main,
                                        backgroundColor: 'transparent'
                                      }
                                    }}
                                  >
                                    <ListItemIcon
                                      sx={{ minWidth: 36, color: 'inherit' }}
                                    >
                                      <link.icon fontSize="small" />
                                    </ListItemIcon>
                                    <ListItemText
                                      primary={link.title}
                                      primaryTypographyProps={{
                                        variant: 'body2',
                                        sx: { fontWeight: 500 }
                                      }}
                                    />
                                  </ListItem>
                                ))}
                            </List>
                          </Grid>
                        </Grid>
                      </Grid>
                    </Grid>
                  </Box>
                </Menu>
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
                        <>
                          <ListItem
                            button
                            onClick={handleSolutionsMobileToggle}
                            sx={{ py: 2 }}
                          >
                            <ListItemText
                              primary={t('Solutions')}
                              primaryTypographyProps={{
                                variant: 'h3',
                                sx: { fontWeight: 'bold' }
                              }}
                            />
                            {solutionsMobileOpen ? (
                              <ExpandLess />
                            ) : (
                              <ExpandMore />
                            )}
                          </ListItem>
                          <Collapse
                            in={solutionsMobileOpen}
                            timeout="auto"
                            unmountOnExit
                          >
                            <List component="div" disablePadding sx={{ pl: 4 }}>
                              <Typography
                                variant="overline"
                                sx={{
                                  mt: 2,
                                  display: 'block',
                                  color: theme.palette.text.secondary
                                }}
                              >
                                {t('Use cases')}
                              </Typography>
                              {useCaseLinks.map((link) => (
                                <ListItem
                                  key={link.title}
                                  component={RouterLink}
                                  to={link.href}
                                  onClick={handleMenuClose}
                                  sx={{ py: 1 }}
                                >
                                  <ListItemText primary={link.title} />
                                </ListItem>
                              ))}
                              <Typography
                                variant="overline"
                                sx={{
                                  mt: 2,
                                  display: 'block',
                                  color: theme.palette.text.secondary
                                }}
                              >
                                {t('Industries')}
                              </Typography>
                              {industriesLinks.map((link) => (
                                <ListItem
                                  key={link.title}
                                  component={RouterLink}
                                  to={link.href}
                                  onClick={handleMenuClose}
                                  sx={{ py: 1 }}
                                >
                                  <ListItemIcon sx={{ minWidth: 40 }}>
                                    <link.icon />
                                  </ListItemIcon>
                                  <ListItemText primary={link.title} />
                                </ListItem>
                              ))}
                            </List>
                          </Collapse>
                        </>
                      </Slide>
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
