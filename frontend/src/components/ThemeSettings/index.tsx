import { FC, useContext, useRef, useState } from 'react';
import {
  Popover,
  styled,
  Button,
  Typography,
  Stack,
  Divider,
  Box,
  Tooltip
} from '@mui/material';
import { ThemeContext } from 'src/theme/ThemeProvider';
import CheckTwoToneIcon from '@mui/icons-material/CheckTwoTone';
import SettingsTwoToneIcon from '@mui/icons-material/SettingsTwoTone';
import Fab from '@mui/material/Fab';
import { useTranslation } from 'react-i18next';

const ThemeSettingsButton = styled(Box)(
  ({ theme }) => `
          position: fixed;
          z-index: 9999;
          right: ${theme.spacing(4)};
          bottom: ${theme.spacing(4)};

          &::before {
              width: 30px;
              height: 30px;
              content: ' ';
              position: absolute;
              border-radius: 100px;
              left: 13px;
              top: 13px;
              background: ${theme.colors.primary.main};
              animation: ripple 1s infinite;
              transition: ${theme.transitions.create(['all'])};
          }

          .MuiSvgIcon-root {
            animation: pulse 1s infinite;
            transition: ${theme.transitions.create(['all'])};
          }
  `
);

const ThemeToggleWrapper = styled(Box)(
  ({ theme }) => `
          padding: ${theme.spacing(2)};
          min-width: 220px;
  `
);

const ButtonWrapper = styled(Box)(
  ({ theme }) => `
        cursor: pointer;
        position: relative;
        border-radius: ${theme.general.borderRadiusXl};
        padding: ${theme.spacing(0.8)};
        display: flex;
        flex-direction: row;
        align-items: stretch;
        min-width: 80px;
        box-shadow: 0 0 0 2px ${theme.colors.primary.lighter};

        &:hover {
            box-shadow: 0 0 0 2px ${theme.colors.primary.light};
        }

        &.active {
            box-shadow: 0 0 0 2px ${theme.palette.primary.main};

            .colorSchemeWrapper {
                opacity: .6;
            }
        }
  `
);

const ColorSchemeWrapper = styled(Box)(
  ({ theme }) => `

    position: relative;

    border-radius: ${theme.general.borderRadiusXl};
    height: 28px;

    &.colorSchemeWrapper {
        display: flex;
        align-items: stretch;
        width: 100%;

        .primary {
            border-top-left-radius: ${theme.general.borderRadiusXl};
            border-bottom-left-radius: ${theme.general.borderRadiusXl};
        }

        .secondary {
            border-top-right-radius: ${theme.general.borderRadiusXl};
            border-bottom-right-radius: ${theme.general.borderRadiusXl};
        }

        .primary,
        .secondary,
        .alternate {
            flex: 1;
        }
    }

    &.pureLight {
        .primary {
            background: #5569ff;
        }

        .secondary {
            background: #f2f5f9;
        }
    }

    &.greyGoose {
        .primary {
            background: #2442AF;
        }

        .secondary {
            background: #F8F8F8;
        }
    }

    &.purpleFlow {
        .primary {
            background: #9b52e1;
        }

        .secondary {
            background: #00b795;
        }
    }

    &.theme1 {
        .primary {
            background: #0EA5E9;
        }

        .secondary {
            background: #06B6D4;
        }
    }

    &.theme2 {
        .primary {
            background: #EC4899;
        }

        .secondary {
            background: #8B5CF6;
        }
    }

    &.theme3 {
        .primary {
            background: #06B6D4;
        }

        .secondary {
            background: #6366F1;
        }
    }
  `
);

const CheckSelected = styled(Box)(
  ({ theme }) => `
    background: ${theme.palette.success.main};
    border-radius: 50px;
    height: 26px;
    width: 26px;
    color: ${theme.palette.success.contrastText};
    display: flex;
    align-items: center;
    justify-content: center;
    position: absolute;
    left: 50%;
    top: 50%;
    margin: -13px 0 0 -13px;
    z-index: 5;

    .MuiSvgIcon-root {
        height: 16px;
        width: 16px;
    }

  `
);

const ThemeSettings: FC = () => {
  const { t }: { t: any } = useTranslation();

  const ref = useRef<any>(null);
  const [isOpen, setOpen] = useState<boolean>(false);

  const handleOpen = (): void => {
    setOpen(true);
  };

  const handleClose = (): void => {
    setOpen(false);
  };

  const setThemeName = useContext(ThemeContext);
  const curThemeName = localStorage.getItem('appTheme') || 'PureLightTheme';

  const [theme, setTheme] = useState(curThemeName);

  const changeTheme = (theme): void => {
    setTheme(theme);
    setThemeName(theme);
  };

  return (
    <>
      <ThemeSettingsButton>
        <Tooltip arrow title={t('Theme Settings')}>
          <Fab ref={ref} onClick={handleOpen} color="primary" aria-label="add">
            <SettingsTwoToneIcon />
          </Fab>
        </Tooltip>
        <Popover
          disableScrollLock
          anchorEl={ref.current}
          onClose={handleClose}
          open={isOpen}
          anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'left'
          }}
          transformOrigin={{
            vertical: 'bottom',
            horizontal: 'right'
          }}
        >
          <ThemeToggleWrapper>
            <Typography
              sx={{
                mt: 1,
                mb: 3,
                textAlign: 'center',
                fontWeight: 'bold',
                textTransform: 'uppercase'
              }}
              variant="body1"
            >
              Light color schemes
            </Typography>
            <Stack alignItems="center" spacing={2}>
              <Tooltip placement="left" title="Pure Light" arrow>
                <ButtonWrapper
                  className={theme === 'PureLightTheme' ? 'active' : ''}
                  onClick={() => {
                    changeTheme('PureLightTheme');
                  }}
                >
                  {theme === 'PureLightTheme' && (
                    <CheckSelected>
                      <CheckTwoToneIcon />
                    </CheckSelected>
                  )}
                  <ColorSchemeWrapper className="colorSchemeWrapper pureLight">
                    <Box className="primary" />
                    <Box className="secondary" />
                  </ColorSchemeWrapper>
                </ButtonWrapper>
              </Tooltip>
              <Tooltip placement="left" title="Grey Goose" arrow>
                <ButtonWrapper
                  className={theme === 'GreyGooseTheme' ? 'active' : ''}
                  onClick={() => {
                    changeTheme('GreyGooseTheme');
                  }}
                >
                  {theme === 'GreyGooseTheme' && (
                    <CheckSelected>
                      <CheckTwoToneIcon />
                    </CheckSelected>
                  )}
                  <ColorSchemeWrapper className="colorSchemeWrapper greyGoose">
                    <Box className="primary" />
                    <Box className="secondary" />
                  </ColorSchemeWrapper>
                </ButtonWrapper>
              </Tooltip>
              <Tooltip placement="left" title="Purple Flow" arrow>
                <ButtonWrapper
                  className={theme === 'PurpleFlowTheme' ? 'active' : ''}
                  onClick={() => {
                    changeTheme('PurpleFlowTheme');
                  }}
                >
                  {theme === 'PurpleFlowTheme' && (
                    <CheckSelected>
                      <CheckTwoToneIcon />
                    </CheckSelected>
                  )}
                  <ColorSchemeWrapper className="colorSchemeWrapper purpleFlow">
                    <Box className="primary" />
                    <Box className="secondary" />
                  </ColorSchemeWrapper>
                </ButtonWrapper>
              </Tooltip>
              <Tooltip placement="left" title="SkyView" arrow>
                <ButtonWrapper
                  className={theme === 'SkyView' ? 'active' : ''}
                  onClick={() => {
                    changeTheme('SkyView');
                  }}
                >
                  {theme === 'SkyView' && (
                    <CheckSelected>
                      <CheckTwoToneIcon />
                    </CheckSelected>
                  )}
                  <ColorSchemeWrapper className="colorSchemeWrapper theme1">
                    <Box className="primary" />
                    <Box className="secondary" />
                  </ColorSchemeWrapper>
                </ButtonWrapper>
              </Tooltip>
            </Stack>
            <Divider sx={{ my: 2 }} />
            <Typography
              sx={{
                mb: 3,
                textAlign: 'center',
                fontWeight: 'bold',
                textTransform: 'uppercase'
              }}
              variant="body1"
            >
              Dark color schemes
            </Typography>
            <Stack alignItems="center" spacing={2}>
              <Tooltip placement="left" title="PureDarkTheme" arrow>
                <ButtonWrapper
                  className={theme === 'PureDarkTheme' ? 'active' : ''}
                  onClick={() => {
                    changeTheme('PureDarkTheme');
                  }}
                >
                  {theme === 'PureDarkTheme' && (
                    <CheckSelected>
                      <CheckTwoToneIcon />
                    </CheckSelected>
                  )}
                  <ColorSchemeWrapper className="colorSchemeWrapper theme2">
                    <Box className="primary" />
                    <Box className="secondary" />
                  </ColorSchemeWrapper>
                </ButtonWrapper>
              </Tooltip>
              <Tooltip placement="left" title="Elegant Night" arrow>
                <ButtonWrapper
                  className={theme === 'ElegantNightTheme' ? 'active' : ''}
                  onClick={() => {
                    changeTheme('ElegantNightTheme');
                  }}
                >
                  {theme === 'ElegantNightTheme' && (
                    <CheckSelected>
                      <CheckTwoToneIcon />
                    </CheckSelected>
                  )}
                  <ColorSchemeWrapper className="colorSchemeWrapper theme3">
                    <Box className="primary" />
                    <Box className="secondary" />
                  </ColorSchemeWrapper>
                </ButtonWrapper>
              </Tooltip>
            </Stack>
          </ThemeToggleWrapper>
        </Popover>
      </ThemeSettingsButton>
    </>
  );
};

export default ThemeSettings;
