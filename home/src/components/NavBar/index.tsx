"use client";
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
  ListItemButton,
  ListItemIcon,
  Divider,
  useTheme,
  useMediaQuery,
  Slide,
  Menu,
  Grid,
  Collapse,
  Link as MuiLink,
} from "@mui/material";
import Logo from "../LogoSign";
import { GitHub, ExpandLess, ExpandMore } from "@mui/icons-material";
import MenuIcon from "@mui/icons-material/Menu";
import LanguageSwitcher from "src/components/LanguageSwitcher";
import { Link } from "src/i18n/routing";
import { useTranslations } from "next-intl";
import { useState } from "react";
import { demoLink, isWhiteLabeled } from "src/config";
import { getIndustriesLinks, getFeaturesLinks, signupUrl, workOrdersUrl } from "src/utils/urlPaths";

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
  `,
);

// Spacer to prevent content from going under fixed navbar
const NavbarSpacer = styled(Box)(
  ({ theme }) => `
    height: ${theme.spacing(10)};
    margin-bottom: ${theme.spacing(10)};
  `,
);

export default function NavBar() {
  const t = useTranslations();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("md"));
  const featuresLinks = getFeaturesLinks(t);
  const industriesLinks = getIndustriesLinks(t);

  // State for hamburger menu
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const open = Boolean(anchorEl);

  // State for Features menu (Desktop)
  const [featuresAnchorEl, setFeaturesAnchorEl] = useState<null | HTMLElement>(null);
  const featuresOpen = Boolean(featuresAnchorEl);

  // State for Industries menu (Desktop)
  const [industriesAnchorEl, setIndustriesAnchorEl] = useState<null | HTMLElement>(null);
  const industriesOpen = Boolean(industriesAnchorEl);

  // State for Features collapse (Mobile)
  const [featuresMobileOpen, setFeaturesMobileOpen] = useState(false);

  // State for Industries collapse (Mobile)
  const [industriesMobileOpen, setIndustriesMobileOpen] = useState(false);

  // Handlers for Features menu
  const handleFeaturesOpen = (event: React.MouseEvent<HTMLElement>) => {
    setFeaturesAnchorEl(event.currentTarget);
  };

  const handleFeaturesClose = () => {
    setFeaturesAnchorEl(null);
  };

  const handleFeaturesMobileToggle = () => {
    setFeaturesMobileOpen(!featuresMobileOpen);
  };

  // Handlers for Industries menu
  const handleIndustriesOpen = (event: React.MouseEvent<HTMLElement>) => {
    setIndustriesAnchorEl(event.currentTarget);
  };

  const handleIndustriesClose = () => {
    setIndustriesAnchorEl(null);
  };

  const handleIndustriesMobileToggle = () => {
    setIndustriesMobileOpen(!industriesMobileOpen);
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
            <Box alignItems={"center"}>
              <Logo />
              {!isWhiteLabeled && (
                <Typography
                  style={{ cursor: "pointer" }}
                  fontSize={13}
                  onClick={() => {
                    window.open("https://www.intel-loop.com/", "_blank");
                  }}
                >
                  Powered by Intelloop
                </Typography>
              )}
            </Box>
            <Stack direction="row" alignItems="center" justifyContent="space-between" flex={1}>
              <Box />
              {/* Desktop Menu */}
              <Stack
                direction="row"
                spacing={{ xs: 1, md: 2 }}
                alignItems={"center"}
                sx={{
                  display: { xs: "none", md: "flex" },
                }}
              >
                <Button
                  onClick={handleFeaturesOpen}
                  onMouseEnter={handleFeaturesOpen}
                  endIcon={
                    <ExpandMore
                      sx={{
                        transition: "transform 0.2s",
                        transform: featuresOpen ? "rotate(180deg)" : "rotate(0deg)",
                      }}
                    />
                  }
                >
                  {t("features")}
                </Button>
                <Menu
                  id="features-menu"
                  anchorEl={featuresAnchorEl}
                  open={featuresOpen}
                  onClose={handleFeaturesClose}
                  MenuListProps={{
                    onMouseLeave: handleFeaturesClose,
                    sx: { p: 0 },
                  }}
                  PaperProps={{
                    sx: {
                      mt: 1.5,
                      boxShadow: theme.shadows[5],
                      borderRadius: 1,
                      minWidth: 250,
                    },
                  }}
                >
                  <Box sx={{ p: 0 }}>
                    <Typography
                      variant="h6"
                      sx={{
                        mb: 1,
                        ml: 2,
                        fontWeight: "bold",
                        color: theme.palette.primary.main,
                        textTransform: "uppercase",
                        fontSize: 12,
                        letterSpacing: 1,
                      }}
                    >
                      {t("features")}
                    </Typography>
                    <List dense>
                      {featuresLinks.map((link) => (
                        <ListItem key={link.title} disablePadding>
                          <ListItemButton
                            component={Link}
                            href={link.href}
                            onClick={handleFeaturesClose}
                            disableRipple
                            sx={{
                              px: 1,
                              py: 1,
                              color: "inherit",
                              textDecoration: "none",
                            }}
                          >
                            <ListItemText
                              primary={link.title}
                              primaryTypographyProps={{
                                variant: "body2",
                                sx: { fontWeight: 500 },
                              }}
                            />
                          </ListItemButton>
                        </ListItem>
                      ))}
                    </List>
                  </Box>
                </Menu>

                <Button
                  onClick={handleIndustriesOpen}
                  onMouseEnter={handleIndustriesOpen}
                  endIcon={industriesOpen ? <ExpandLess /> : <ExpandMore />}
                >
                  {t("industries")}
                </Button>
                <Menu
                  id="industries-menu"
                  anchorEl={industriesAnchorEl}
                  open={industriesOpen}
                  onClose={handleIndustriesClose}
                  MenuListProps={{
                    onMouseLeave: handleIndustriesClose,
                    sx: { p: 0 },
                  }}
                  PaperProps={{
                    sx: {
                      mt: 1.5,
                      boxShadow: theme.shadows[5],
                      borderRadius: 1,
                      minWidth: 500,
                    },
                  }}
                >
                  <Box sx={{ p: 2 }}>
                    <Typography
                      variant="h6"
                      sx={{
                        mb: 2,
                        fontWeight: "bold",
                        color: theme.palette.primary.main,
                        textTransform: "uppercase",
                        fontSize: 12,
                        letterSpacing: 1,
                      }}
                    >
                      {t("industries")}
                    </Typography>
                    <Grid container spacing={2}>
                      <Grid item xs={6}>
                        <List dense disablePadding>
                          {industriesLinks.slice(0, Math.ceil(industriesLinks.length / 2)).map((link) => (
                            <ListItem key={link.title} disablePadding>
                              <ListItemButton
                                component={Link}
                                href={link.href}
                                onClick={handleIndustriesClose}
                                sx={{
                                  px: 0,
                                  py: 1,
                                  color: "inherit",
                                  textDecoration: "none",
                                  "&:hover": {
                                    color: theme.palette.primary.main,
                                    backgroundColor: "transparent",
                                  },
                                }}
                              >
                                <ListItemIcon sx={{ minWidth: 36, color: "inherit" }}>
                                  <link.icon fontSize="small" />
                                </ListItemIcon>

                                <ListItemText
                                  primary={link.title}
                                  primaryTypographyProps={{
                                    variant: "body2",
                                    sx: { fontWeight: 500 },
                                  }}
                                />
                              </ListItemButton>
                            </ListItem>
                          ))}
                        </List>
                      </Grid>
                      <Grid item xs={6}>
                        <List dense disablePadding>
                          {industriesLinks.slice(Math.ceil(industriesLinks.length / 2)).map((link) => (
                            <ListItem
                              key={link.title}
                              component={Link}
                              href={link.href}
                              onClick={handleIndustriesClose}
                              sx={{
                                px: 0,
                                py: 1,
                                color: "inherit",
                                textDecoration: "none",
                                "&:hover": {
                                  color: theme.palette.primary.main,
                                  backgroundColor: "transparent",
                                },
                              }}
                            >
                              <ListItemIcon sx={{ minWidth: 36, color: "inherit" }}>
                                <link.icon fontSize="small" />
                              </ListItemIcon>
                              <ListItemText
                                primary={link.title}
                                primaryTypographyProps={{
                                  variant: "body2",
                                  sx: { fontWeight: 500 },
                                }}
                              />
                            </ListItem>
                          ))}
                        </List>
                      </Grid>
                    </Grid>
                  </Box>
                </Menu>
                <Button
                  component={Link}
                  href="/pricing"
                  sx={{
                    ml: 2,
                    size: { xs: "small", md: "medium" },
                  }}
                >
                  {t("pricing")}
                </Button>
                {!isWhiteLabeled && (
                  <Button
                    component={"a"}
                    target={"_blank"}
                    href={"https://github.com/Grashjs/cmms"}
                    startIcon={<GitHub />}
                  >
                    GitHub
                  </Button>
                )}
                <LanguageSwitcher />
                <Button
                  component={Link}
                  href={workOrdersUrl}
                  variant="text"
                  sx={{
                    ml: 2,
                    size: { xs: "small", md: "medium" },
                  }}
                >
                  {t("login")}
                </Button>
                <Button
                  component={Link}
                  href={signupUrl}
                  variant="contained"
                  sx={{
                    ml: 2,
                    size: { xs: "small", md: "medium" },
                  }}
                >
                  {t("register")}
                </Button>
                <Button
                  href={demoLink}
                  variant="outlined"
                  sx={{
                    ml: 2,
                    size: { xs: "small", md: "medium" },
                  }}
                >
                  {t("book_demo")}
                </Button>
              </Stack>

              {/* Mobile Menu Icon */}
              <Box sx={{ display: { xs: "block", md: "none" } }}>
                <IconButton
                  onClick={handleMenuOpen}
                  size="large"
                  aria-controls={open ? "mobile-menu" : undefined}
                  aria-haspopup="true"
                  aria-expanded={open ? "true" : undefined}
                >
                  <MenuIcon />
                </IconButton>
                <Drawer
                  anchor="right"
                  open={open}
                  onClose={handleMenuClose}
                  sx={{
                    "& .MuiDrawer-paper": {
                      width: "100%",
                      background: theme.palette.background.default,
                    },
                  }}
                  transitionDuration={300}
                >
                  <Box
                    sx={{
                      height: "100%",
                      display: "flex",
                      flexDirection: "column",
                    }}
                  >
                    {/* Close button at top */}
                    <Box
                      sx={{
                        display: "flex",
                        justifyContent: "flex-end",
                        p: 2,
                      }}
                    >
                      <IconButton onClick={handleMenuClose}>
                        <MenuIcon />
                      </IconButton>
                    </Box>

                    {/* Main menu items */}
                    <List sx={{ flexGrow: 1, pt: 2 }}>
                      <Slide direction="left" in={open} mountOnEnter unmountOnExit>
                        <Box>
                          <ListItem onClick={handleFeaturesMobileToggle} sx={{ py: 2, cursor: "pointer" }}>
                            <ListItemText
                              primary={t("features")}
                              primaryTypographyProps={{
                                variant: "h3",
                                sx: { fontWeight: "bold" },
                              }}
                            />
                            {featuresMobileOpen ? <ExpandLess /> : <ExpandMore />}
                          </ListItem>
                          <Collapse in={featuresMobileOpen} timeout="auto" unmountOnExit>
                            <List component="div" disablePadding sx={{ pl: 4 }}>
                              {featuresLinks.map((link) => (
                                <ListItem key={link.title} disablePadding>
                                  <ListItemButton
                                    component={Link}
                                    href={link.href}
                                    onClick={handleMenuClose}
                                    disableRipple
                                    sx={{
                                      py: 1,
                                    }}
                                  >
                                    <ListItemText primary={link.title} />
                                  </ListItemButton>
                                </ListItem>
                              ))}
                            </List>
                          </Collapse>
                        </Box>
                      </Slide>

                      <Slide direction="left" in={open} mountOnEnter unmountOnExit>
                        <Box>
                          <ListItem onClick={handleIndustriesMobileToggle} sx={{ py: 2, cursor: "pointer" }}>
                            <ListItemText
                              primary={t("industries")}
                              primaryTypographyProps={{
                                variant: "h3",
                                sx: { fontWeight: "bold" },
                              }}
                            />
                            {industriesMobileOpen ? <ExpandLess /> : <ExpandMore />}
                          </ListItem>
                          <Collapse in={industriesMobileOpen} timeout="auto" unmountOnExit>
                            <List component="div" disablePadding sx={{ pl: 4 }}>
                              {industriesLinks.map((link) => (
                                <ListItem key={link.title} disablePadding>
                                  <ListItemButton
                                    component={Link}
                                    href={link.href}
                                    onClick={handleMenuClose}
                                    disableRipple
                                    sx={{
                                      py: 1,
                                    }}
                                  >
                                    <ListItemIcon sx={{ minWidth: 40, color: "inherit" }}>
                                      <link.icon />
                                    </ListItemIcon>

                                    <ListItemText primary={link.title} />
                                  </ListItemButton>
                                </ListItem>
                              ))}
                            </List>
                          </Collapse>
                        </Box>
                      </Slide>
                      <Slide direction="left" in={open} mountOnEnter unmountOnExit>
                        <ListItem component={Link} href="/pricing" onClick={handleMenuClose} sx={{ py: 2 }}>
                          <ListItemText
                            primary={t("pricing")}
                            primaryTypographyProps={{
                              variant: "h3",
                              sx: { fontWeight: "bold" },
                            }}
                          />
                        </ListItem>
                      </Slide>

                      <Slide direction="left" in={open} mountOnEnter unmountOnExit timeout={{ enter: 400 }}>
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
                              variant: "h3",
                              sx: { fontWeight: "bold" },
                            }}
                          />
                        </ListItem>
                      </Slide>

                      <Slide direction="left" in={open} mountOnEnter unmountOnExit timeout={{ enter: 500 }}>
                        <ListItem sx={{ py: 2 }}>
                          <LanguageSwitcher onSwitch={() => setAnchorEl(null)} />
                        </ListItem>
                      </Slide>
                    </List>

                    {/* Bottom buttons */}
                    <Divider />
                    <Box sx={{ p: 2 }}>
                      <Stack spacing={2}>
                        <Button
                          component={Link}
                          href={workOrdersUrl}
                          variant="text"
                          fullWidth
                          size="large"
                          onClick={handleMenuClose}
                        >
                          {t("login")}
                        </Button>
                        <Button
                          component={Link}
                          href={signupUrl}
                          variant="contained"
                          fullWidth
                          size="large"
                          onClick={handleMenuClose}
                        >
                          {t("register")}
                        </Button>
                        <Button href={demoLink} variant="outlined" fullWidth size="large">
                          {t("book_demo")}
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
