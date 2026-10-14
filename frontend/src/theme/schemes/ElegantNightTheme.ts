import { alpha, createTheme, lighten, darken } from '@mui/material';
import '@mui/lab/themeAugmentation';
import { customColors } from '../../config';

const themeColors = {
  primary: '#06B6D4',
  secondary: '#6366F1',
  success: '#10B981',
  warning: '#F59E0B',
  error: '#EF4444',
  info: '#3B82F6',
  black: '#0A0E27',
  white: '#ffffff',
  primaryAlt: '#0891B2',
  ...customColors
};

const colors = {
  gradients: {
    blue1: 'linear-gradient(135deg, #667EEA 0%, #764BA2 100%)',
    blue2: 'linear-gradient(135deg, #00D4FF 0%, #0099FF 100%)',
    blue3: 'linear-gradient(127.55deg, #1A2847 3.73%, #2D3F5C 92.26%)',
    blue4: 'linear-gradient(-20deg, #2d4a6e 0%, #3d3a5c 100%)',
    blue5: 'linear-gradient(135deg, #7DD3FC 10%, #1E40AF 100%)',
    orange1: 'linear-gradient(135deg, #FBBF24 0%, #F87171 100%)',
    orange2: 'linear-gradient(135deg, #FCD34D 0%, #FB7185 100%)',
    orange3: 'linear-gradient(120deg, #FDE047 0%, #FB923C 100%)',
    purple1: 'linear-gradient(135deg, #67E8F9 0%, #A78BFA 100%)',
    purple3: 'linear-gradient(135deg, #818CF8 0%, #A855F7 100%)',
    pink1: 'linear-gradient(135deg, #F5D0FE 0%, #D946EF 100%)',
    pink2: 'linear-gradient(135deg, #FCA5A5 0%, #EC4899 100%)',
    green1: 'linear-gradient(135deg, #BBF7D0 0%, #059669 100%)',
    green2: 'linear-gradient(to bottom, #14B8A6, #6EE7B7)',
    black1: 'linear-gradient(100.66deg, #2D3E5F 6.56%, #0A0E27 93.57%)',
    black2: 'linear-gradient(60deg, #111629 0%, #1A2239 100%)'
  },
  shadows: {
    success:
      '0px 1px 4px rgba(16, 185, 129, 0.25), 0px 3px 12px 2px rgba(16, 185, 129, 0.35)',
    error:
      '0px 1px 4px rgba(239, 68, 68, 0.25), 0px 3px 12px 2px rgba(239, 68, 68, 0.35)',
    info: '0px 1px 4px rgba(59, 130, 246, 0.25), 0px 3px 12px 2px rgba(59, 130, 246, 0.35)',
    primary:
      '0px 1px 4px rgba(6, 182, 212, 0.25), 0px 3px 12px 2px rgba(6, 182, 212, 0.35)',
    warning:
      '0px 1px 4px rgba(245, 158, 11, 0.25), 0px 3px 12px 2px rgba(245, 158, 11, 0.35)',
    card: '0px 9px 16px rgba(0, 0, 0, 0.6), 0px 2px 2px rgba(0, 0, 0, 0.7)',
    cardSm:
      '0px 2px 3px rgba(0, 0, 0, 0.5), 0px 1px 1px rgba(0, 0, 0, 0.6)',
    cardLg:
      '0 5rem 14rem 0 rgb(0 0 0 / 70%), 0 0.8rem 2.3rem rgb(0 0 0 / 85%), 0 0.2rem 0.3rem rgb(0 0 0 / 80%)'
  },
  layout: {
    general: {
      bodyBg: '#0A0E27'
    },
    sidebar: {
      background: '#0A0E27',
      textColor: '#CBD5E1',
      dividerBg: '#1E293B',
      menuItemColor: '#E2E8F0',
      menuItemColorActive: themeColors.primary,
      menuItemBg: '#0A0E27',
      menuItemBgActive: '#0F3947',
      menuItemIconColor: '#94A3B8',
      menuItemIconColorActive: themeColors.primary,
      menuItemHeadingColor: '#F1F5F9'
    }
  },
  alpha: {
    white: {
      5: alpha(themeColors.white, 0.02),
      10: alpha(themeColors.white, 0.1),
      30: alpha(themeColors.white, 0.3),
      50: alpha(themeColors.white, 0.5),
      70: alpha(themeColors.white, 0.7),
      100: themeColors.white
    },
    trueWhite: {
      5: alpha(themeColors.white, 0.02),
      10: alpha(themeColors.white, 0.1),
      30: alpha(themeColors.white, 0.3),
      50: alpha(themeColors.white, 0.5),
      70: alpha(themeColors.white, 0.7),
      100: themeColors.white
    },
    black: {
      5: alpha(themeColors.black, 0.02),
      10: alpha(themeColors.black, 0.1),
      30: alpha(themeColors.black, 0.3),
      50: alpha(themeColors.black, 0.5),
      70: alpha(themeColors.black, 0.7),
      100: themeColors.black
    }
  },
  secondary: {
    lighter: lighten(themeColors.secondary, 0.85),
    light: lighten(themeColors.secondary, 0.25),
    main: themeColors.secondary,
    dark: darken(themeColors.secondary, 0.2)
  },
  primary: {
    lighter: lighten(themeColors.primary, 0.85),
    light: lighten(themeColors.primary, 0.3),
    main: themeColors.primary,
    dark: darken(themeColors.primary, 0.2)
  },
  success: {
    lighter: lighten(themeColors.success, 0.85),
    light: lighten(themeColors.success, 0.3),
    main: themeColors.success,
    dark: darken(themeColors.success, 0.2)
  },
  warning: {
    lighter: lighten(themeColors.warning, 0.85),
    light: lighten(themeColors.warning, 0.3),
    main: themeColors.warning,
    dark: darken(themeColors.warning, 0.2)
  },
  error: {
    lighter: lighten(themeColors.error, 0.85),
    light: lighten(themeColors.error, 0.3),
    main: themeColors.error,
    dark: darken(themeColors.error, 0.2)
  },
  info: {
    lighter: lighten(themeColors.info, 0.85),
    light: lighten(themeColors.info, 0.3),
    main: themeColors.info,
    dark: darken(themeColors.info, 0.2)
  }
};

export const ElegantNightTheme = createTheme({
  direction: 'ltr',
  colors: {
    gradients: {
      blue1: colors.gradients.blue1,
      blue2: colors.gradients.blue2,
      blue3: colors.gradients.blue3,
      blue4: colors.gradients.blue4,
      blue5: colors.gradients.blue5,
      orange1: colors.gradients.orange1,
      orange2: colors.gradients.orange2,
      orange3: colors.gradients.orange3,
      purple1: colors.gradients.purple1,
      purple3: colors.gradients.purple3,
      pink1: colors.gradients.pink1,
      pink2: colors.gradients.pink2,
      green1: colors.gradients.green1,
      green2: colors.gradients.green2,
      black1: colors.gradients.black1,
      black2: colors.gradients.black2
    },
    shadows: {
      success: colors.shadows.success,
      error: colors.shadows.error,
      primary: colors.shadows.primary,
      info: colors.shadows.info,
      warning: colors.shadows.warning
    },
    alpha: {
      white: {
        5: alpha(themeColors.white, 0.02),
        10: alpha(themeColors.white, 0.1),
        30: alpha(themeColors.white, 0.3),
        50: alpha(themeColors.white, 0.5),
        70: alpha(themeColors.white, 0.7),
        100: themeColors.white
      },
      trueWhite: {
        5: alpha(themeColors.white, 0.02),
        10: alpha(themeColors.white, 0.1),
        30: alpha(themeColors.white, 0.3),
        50: alpha(themeColors.white, 0.5),
        70: alpha(themeColors.white, 0.7),
        100: themeColors.white
      },
      black: {
        5: alpha(themeColors.black, 0.02),
        10: alpha(themeColors.black, 0.1),
        30: alpha(themeColors.black, 0.3),
        50: alpha(themeColors.black, 0.5),
        70: alpha(themeColors.black, 0.7),
        100: themeColors.black
      }
    },
    secondary: {
      lighter: alpha(themeColors.secondary, 0.1),
      light: lighten(themeColors.secondary, 0.3),
      main: themeColors.secondary,
      dark: darken(themeColors.secondary, 0.2)
    },
    primary: {
      lighter: alpha(themeColors.primary, 0.1),
      light: lighten(themeColors.primary, 0.3),
      main: themeColors.primary,
      dark: darken(themeColors.primary, 0.2)
    },
    success: {
      lighter: alpha(themeColors.success, 0.1),
      light: lighten(themeColors.success, 0.3),
      main: themeColors.success,
      dark: darken(themeColors.success, 0.2)
    },
    warning: {
      lighter: alpha(themeColors.warning, 0.1),
      light: lighten(themeColors.warning, 0.3),
      main: themeColors.warning,
      dark: darken(themeColors.warning, 0.2)
    },
    error: {
      lighter: alpha(themeColors.error, 0.1),
      light: lighten(themeColors.error, 0.3),
      main: themeColors.error,
      dark: darken(themeColors.error, 0.2)
    },
    info: {
      lighter: alpha(themeColors.info, 0.1),
      light: lighten(themeColors.info, 0.3),
      main: themeColors.info,
      dark: darken(themeColors.info, 0.2)
    }
  },
  general: {
    reactFrameworkColor: '#00D8FF',
    borderRadiusSm: '6px',
    borderRadius: '10px',
    borderRadiusLg: '12px',
    borderRadiusXl: '16px'
  },
  sidebar: {
    background: colors.layout.sidebar.background,
    textColor: colors.layout.sidebar.textColor,
    dividerBg: colors.layout.sidebar.dividerBg,
    menuItemColor: colors.layout.sidebar.menuItemColor,
    menuItemColorActive: colors.layout.sidebar.menuItemColorActive,
    menuItemBg: colors.layout.sidebar.menuItemBg,
    menuItemBgActive: colors.layout.sidebar.menuItemBgActive,
    menuItemIconColor: colors.layout.sidebar.menuItemIconColor,
    menuItemIconColorActive: colors.layout.sidebar.menuItemIconColorActive,
    menuItemHeadingColor: colors.layout.sidebar.menuItemHeadingColor,
    boxShadow:
      '2px 0 3px rgba(15, 23, 42, .28), 1px 0 1px rgba(15, 23, 42, 0.52)',
    width: '290px'
  },
  header: {
    height: '80px',
    background: '#111629',
    boxShadow: colors.shadows.cardSm,
    textColor: '#F1F5F9'
  },
  spacing: 9,
  palette: {
    common: {
      black: colors.alpha.black[100],
      white: colors.alpha.white[100]
    },
    mode: 'dark',
    primary: {
      light: colors.primary.light,
      main: colors.primary.main,
      dark: colors.primary.dark
    },
    secondary: {
      light: colors.secondary.light,
      main: colors.secondary.main,
      dark: colors.secondary.dark
    },
    error: {
      light: colors.error.light,
      main: colors.error.main,
      dark: colors.error.dark,
      contrastText: colors.alpha.white[100]
    },
    success: {
      light: colors.success.light,
      main: colors.success.main,
      dark: colors.success.dark,
      contrastText: colors.alpha.white[100]
    },
    info: {
      light: colors.info.light,
      main: colors.info.main,
      dark: colors.info.dark,
      contrastText: colors.alpha.white[100]
    },
    warning: {
      light: colors.warning.light,
      main: colors.warning.main,
      dark: colors.warning.dark,
      contrastText: colors.alpha.white[100]
    },
    text: {
      primary: '#F1F5F9',
      secondary: '#CBD5E1',
      disabled: '#64748B'
    },
    background: {
      paper: '#111629',
      default: '#0A0E27'
    },
    action: {
      active: '#06B6D4',
      hover: alpha('#06B6D4', 0.1),
      hoverOpacity: 0.1,
      selected: alpha('#06B6D4', 0.1),
      selectedOpacity: 0.1,
      disabled: '#64748B',
      disabledBackground: '#1E293B',
      disabledOpacity: 0.38,
      focus: alpha('#06B6D4', 0.2),
      focusOpacity: 0.05,
      activatedOpacity: 0.12
    },
    tonalOffset: 0.5
  },
  breakpoints: {
    values: {
      xs: 0,
      sm: 600,
      md: 960,
      lg: 1280,
      xl: 1840
    }
  },
  components: {
    MuiBackdrop: {
      styleOverrides: {
        root: {
          backgroundColor: alpha(darken(themeColors.primaryAlt, 0.4), 0.2),
          backdropFilter: 'blur(2px)',

          '&.MuiBackdrop-invisible': {
            backgroundColor: 'transparent',
            backdropFilter: 'blur(2px)'
          }
        }
      }
    },
    MuiFormHelperText: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          marginLeft: 8,
          marginRight: 8,
          fontWeight: 'bold'
        }
      }
    },
    MuiCssBaseline: {
      styleOverrides: {
        'html, body': {
          width: '100%',
          height: '100%'
        },
        body: {
          display: 'flex',
          flexDirection: 'column',
          minHeight: '100%',
          width: '100%',
          flex: 1
        },
        '#root': {
          width: '100%',
          height: '100%',
          display: 'flex',
          flex: 1,
          flexDirection: 'column'
        },
        html: {
          display: 'flex',
          flexDirection: 'column',
          minHeight: '100%',
          width: '100%',
          MozOsxFontSmoothing: 'grayscale',
          WebkitFontSmoothing: 'antialiased'
        },
        '.child-popover .MuiPaper-root .MuiList-root': {
          flexDirection: 'column'
        },
        '#nprogress': {
          pointerEvents: 'none'
        },
        '#nprogress .bar': {
          background: colors.primary.lighter
        },
        '#nprogress .spinner-icon': {
          borderTopColor: colors.primary.lighter,
          borderLeftColor: colors.primary.lighter
        },
        '#nprogress .peg': {
          boxShadow:
            '0 0 15px ' +
            colors.primary.lighter +
            ', 0 0 8px' +
            colors.primary.light
        },
        ':root': {
          '--swiper-theme-color': colors.primary.main
        },
        code: {
          background: colors.info.lighter,
          color: colors.info.dark,
          borderRadius: 4,
          padding: 4
        },
        '@keyframes pulse': {
          '0%': {
            transform: 'scale(.75)'
          },
          '20%': {
            transform: 'scale(1.1)'
          },
          '40%': {
            transform: 'scale(.75)'
          },
          '60%': {
            transform: 'scale(1.05)'
          },
          '80%': {
            transform: 'scale(.75)'
          },
          '100%': {
            transform: 'scale(.75)'
          }
        },
        '@keyframes ripple': {
          '0%': {
            transform: 'scale(.8)',
            opacity: 1
          },
          '100%': {
            transform: 'scale(2.8)',
            opacity: 0
          }
        },
        '@keyframes float': {
          '0%': {
            transform: 'translate(0%, 0%)'
          },
          '100%': {
            transform: 'translate(3%, 3%)'
          }
        },
        '::-webkit-scrollbar': {
          width: '8px',
          height: '8px'
        },
        '::-webkit-scrollbar-track': {
          backgroundColor: '#111629'
        },
        '::-webkit-scrollbar-thumb': {
          backgroundColor: '#475569',
          borderRadius: '4px',
          '&:hover': {
            backgroundColor: '#64748B'
          }
        },
        'scrollbar-color': '#475569 #111629 !important',
        'scrollbar-width': 'thin !important'
      }
    },
    MuiSelect: {
      styleOverrides: {
        select: {
          color: '#F1F5F9',
          backgroundColor: '#111629',
          '&:focus': {
            backgroundColor: '#1E293B'
          }
        },
        iconOutlined: {
          color: '#CBD5E1'
        },
        icon: {
          top: 'calc(50% - 14px)',
          color: '#CBD5E1'
        }
      }
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          color: '#F1F5F9',
          backgroundColor: 'transparent',
          '& .MuiInputAdornment-positionEnd.MuiInputAdornment-outlined': {
            paddingRight: 6
          },
          '&:hover .MuiOutlinedInput-notchedOutline': {
            borderColor: '#06B6D4'
          },
          '&.Mui-focused:hover .MuiOutlinedInput-notchedOutline': {
            borderColor: '#06B6D4'
          },
          '& input': {
            color: '#F1F5F9'
          }
        }
      }
    },
    MuiListSubheader: {
      styleOverrides: {
        colorPrimary: {
          fontWeight: 'bold',
          lineHeight: '40px',
          fontSize: 13,
          background: colors.alpha.black[5],
          color: colors.alpha.black[70]
        }
      }
    },
    MuiCardHeader: {
      styleOverrides: {
        action: {
          marginTop: -5,
          marginBottom: -5
        },
        title: {
          fontSize: 15
        }
      }
    },
    MuiRadio: {
      styleOverrides: {
        root: {
          borderRadius: '50px'
        }
      }
    },
    MuiChip: {
      styleOverrides: {
        colorSecondary: {
          background: colors.alpha.black[5],
          color: colors.alpha.black[100],

          '&:hover': {
            background: colors.alpha.black[10]
          }
        },
        deleteIcon: {
          color: colors.error.light,

          '&:hover': {
            color: colors.error.main
          }
        }
      }
    },
    MuiAccordion: {
      styleOverrides: {
        root: {
          boxShadow: 'none',

          '&.Mui-expanded': {
            margin: 0
          },
          '&::before': {
            display: 'none'
          }
        }
      }
    },
    MuiAvatar: {
      styleOverrides: {
        root: {
          fontSize: 14,
          fontWeight: 'bold'
        },
        colorDefault: {
          background: colors.alpha.black[30],
          color: colors.alpha.white[100]
        }
      }
    },
    MuiAvatarGroup: {
      styleOverrides: {
        root: {
          alignItems: 'center'
        },
        avatar: {
          background: colors.alpha.black[10],
          fontSize: 13,
          color: colors.alpha.black[70],
          fontWeight: 'bold',

          '&:first-of-type': {
            border: 0,
            background: 'transparent'
          }
        }
      }
    },
    MuiListItemAvatar: {
      styleOverrides: {
        alignItemsFlexStart: {
          marginTop: 0
        }
      }
    },
    MuiPaginationItem: {
      styleOverrides: {
        page: {
          fontSize: 13,
          fontWeight: 'bold',
          transition: 'all .2s'
        },
        textPrimary: {
          '&.Mui-selected': {
            boxShadow: colors.shadows.primary
          },
          '&.MuiButtonBase-root:hover': {
            background: colors.alpha.black[5]
          },
          '&.Mui-selected.MuiButtonBase-root:hover': {
            background: colors.primary.main
          }
        }
      }
    },
    MuiButton: {
      defaultProps: {
        disableRipple: true
      },
      styleOverrides: {
        root: {
          fontWeight: 'bold',
          textTransform: 'none',
          paddingLeft: 16,
          paddingRight: 16,

          '.MuiSvgIcon-root': {
            transition: 'all .2s'
          }
        },
        endIcon: {
          marginRight: -8
        },
        containedSecondary: {
          backgroundColor: colors.secondary.main,
          color: colors.alpha.white[100],
          border: '1px solid ' + colors.alpha.black[30]
        },
        outlinedSecondary: {
          backgroundColor: colors.alpha.white[100],

          '&:hover, &.MuiSelected': {
            backgroundColor: colors.alpha.black[5],
            color: colors.alpha.black[100]
          }
        },
        sizeSmall: {
          padding: '6px 16px',
          lineHeight: 1.5
        },
        sizeMedium: {
          padding: '8px 20px'
        },
        sizeLarge: {
          padding: '11px 24px'
        },
        textSizeSmall: {
          padding: '7px 12px'
        },
        textSizeMedium: {
          padding: '9px 16px'
        },
        textSizeLarge: {
          padding: '12px 16px'
        }
      }
    },
    MuiButtonBase: {
      defaultProps: {
        disableRipple: false
      },
      styleOverrides: {
        root: {
          borderRadius: 6
        }
      }
    },
    MuiToggleButton: {
      defaultProps: {
        disableRipple: true
      },
      styleOverrides: {
        root: {
          color: colors.primary.main,
          background: colors.alpha.white[100],
          transition: 'all .2s',

          '&:hover, &.Mui-selected, &.Mui-selected:hover': {
            color: colors.alpha.white[100],
            background: colors.primary.main
          }
        }
      }
    },
    MuiIconButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          padding: 8,

          '& .MuiTouchRipple-root': {
            borderRadius: 8
          }
        },
        sizeSmall: {
          padding: 4
        }
      }
    },
    MuiListItemText: {
      styleOverrides: {
        root: {
          margin: 0,
          color: '#F1F5F9',
          transition: 'all .2s'
        },
        primary: {
          color: '#F1F5F9',
          fontWeight: 500,
          fontSize: '0.95rem',
          transition: 'all .2s'
        },
        secondary: {
          color: '#94A3B8',
          fontSize: '0.85rem'
        }
      }
    },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          backgroundColor: '#111629',
          color: '#F1F5F9',
          transition: 'all .2s cubic-bezier(0.4, 0, 0.2, 1)',
          '& .MuiTouchRipple-root': {
            opacity: 0.15
          },
          '&:hover': {
            backgroundColor: 'rgba(6, 182, 212, 0.08)',
            paddingLeft: 18
          },
          '&.Mui-selected': {
            backgroundColor: 'rgba(6, 182, 212, 0.15)',
            color: '#06B6D4',
            fontWeight: 600,
            '&:hover': {
              backgroundColor: 'rgba(6, 182, 212, 0.20)',
              paddingLeft: 18
            }
          },
          '& .MuiTypography-root': {
            color: '#F1F5F9',
            transition: 'all .2s'
          },
          '&:hover .MuiTypography-root': {
            color: '#06B6D4'
          },
          '&.Mui-selected .MuiTypography-root': {
            color: '#06B6D4'
          }
        }
      }
    },
    MuiDivider: {
      styleOverrides: {
        root: {
          background: colors.alpha.black[10],
          border: 0,
          height: 1
        },
        vertical: {
          height: 'auto',
          width: 1,

          '&.MuiDivider-flexItem.MuiDivider-fullWidth': {
            height: 'auto'
          },
          '&.MuiDivider-absolute.MuiDivider-fullWidth': {
            height: '100%'
          }
        },
        withChildren: {
          '&:before, &:after': {
            border: 0
          }
        },
        wrapper: {
          background: colors.alpha.white[100],
          fontWeight: 'bold',
          height: 24,
          lineHeight: '24px',
          marginTop: -12,
          color: 'inherit',
          textTransform: 'uppercase'
        }
      }
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          padding: 0,
          backgroundColor: '#111629'
        },
        elevation0: {
          boxShadow: 'none'
        },
        elevation: {
          boxShadow: colors.shadows.card,
          backgroundColor: '#111629'
        },
        elevation2: {
          boxShadow: colors.shadows.cardSm,
          backgroundColor: '#111629'
        },
        elevation24: {
          boxShadow: colors.shadows.cardLg,
          backgroundColor: '#111629'
        },
        outlined: {
          boxShadow: colors.shadows.card,
          backgroundColor: '#111629'
        }
      }
    },
    MuiLink: {
      defaultProps: {
        underline: 'hover'
      }
    },
    MuiLinearProgress: {
      styleOverrides: {
        root: {
          borderRadius: 6,
          height: 6
        }
      }
    },
    MuiSlider: {
      styleOverrides: {
        root: {
          '& .MuiSlider-valueLabelCircle, .MuiSlider-valueLabelLabel': {
            transform: 'none'
          },
          '& .MuiSlider-valueLabel': {
            borderRadius: 6,
            background: colors.alpha.black[100],
            color: colors.alpha.white[100]
          }
        }
      }
    },
    MuiList: {
      styleOverrides: {
        root: {
          padding: 0,
          color: '#F1F5F9',

          '& .MuiListItem-button': {
            transition: 'all .2s cubic-bezier(0.4, 0, 0.2, 1)',
            color: '#F1F5F9',

            '& > .MuiSvgIcon-root': {
              minWidth: 34,
              color: '#94A3B8',
              transition: 'all .2s'
            },

            '& .MuiTouchRipple-root': {
              opacity: 0.15
            },

            '&:hover': {
              backgroundColor: 'rgba(6, 182, 212, 0.08)',

              '& > .MuiSvgIcon-root': {
                color: '#06B6D4'
              }
            }
          },
          '& .MuiListItem-root.MuiButtonBase-root.Mui-selected': {
            backgroundColor: 'rgba(6, 182, 212, 0.15)',
            color: '#06B6D4',
            fontWeight: 600,

            '& > .MuiSvgIcon-root': {
              color: '#06B6D4'
            },

            '&:hover': {
              backgroundColor: 'rgba(6, 182, 212, 0.20)'
            }
          },
          '& .MuiMenuItem-root.MuiButtonBase-root:active': {
            backgroundColor: 'rgba(6, 182, 212, 0.15)',
            color: '#06B6D4'
          },
          '& .MuiMenuItem-root.MuiButtonBase-root .MuiTouchRipple-root': {
            opacity: 0.15
          }
        },
        padding: {
          padding: '8px 4px',

          '& .MuiListItem-button': {
            borderRadius: 6,
            margin: '4px 0',
            paddingLeft: 16,
            paddingRight: 16,

            '&:hover': {
              backgroundColor: 'rgba(6, 182, 212, 0.08)'
            }
          }
        }
      }
    },
    MuiTabs: {
      styleOverrides: {
        root: {
          height: 38,
          minHeight: 38,
          overflow: 'visible'
        },
        indicator: {
          height: 38,
          minHeight: 38,
          borderRadius: 6,
          border: '1px solid ' + colors.primary.dark,
          boxShadow: '0px 2px 10px ' + colors.primary.light
        },
        scrollableX: {
          overflow: 'visible !important'
        }
      }
    },
    MuiTab: {
      styleOverrides: {
        root: {
          padding: 0,
          height: 38,
          minHeight: 38,
          borderRadius: 6,
          transition: 'color .2s',
          textTransform: 'capitalize',

          '&.MuiButtonBase-root': {
            minWidth: 'auto',
            paddingLeft: 20,
            paddingRight: 20,
            marginRight: 4
          },
          '&.Mui-selected, &.Mui-selected:hover': {
            color: colors.alpha.white[100],
            zIndex: 5
          },
          '&:hover': {
            color: colors.alpha.black[100]
          }
        }
      }
    },
    MuiMenu: {
      styleOverrides: {
        paper: {
          padding: 0,
          backgroundColor: '#111629',
          backgroundImage: 'none',
          border: '1px solid #1E293B',
          boxShadow: '0 10px 40px rgba(0, 0, 0, 0.5)',
          borderRadius: '8px'
        },
        list: {
          padding: '8px 4px',

          '& .MuiMenuItem-root.MuiButtonBase-root': {
            fontSize: 14,
            marginTop: 0,
            marginBottom: 0,
            marginX: 1,
            padding: '10px 16px',
            marginY: '3px',
            transition: 'all .2s cubic-bezier(0.4, 0, 0.2, 1)',
            color: '#CBD5E1',
            borderRadius: '6px',
            position: 'relative',

            '& .MuiTouchRipple-root': {
              opacity: 0.1
            },

            '&:hover': {
              color: '#0A0E27',
              background: 'linear-gradient(135deg, #06B6D4 0%, #0891B2 100%)',
              transform: 'translateX(4px)'
            },

            '&:active, &.active, &.Mui-selected': {
              color: '#0A0E27',
              background: 'linear-gradient(135deg, #06B6D4 0%, #0891B2 100%)',
              fontWeight: 600
            },

            '&.Mui-selected:hover': {
              background: 'linear-gradient(135deg, #06B6D4 0%, #0891B2 100%)'
            }
          }
        }
      }
    },
    MuiMenuItem: {
      styleOverrides: {
        root: {
          background: 'transparent',
          color: '#CBD5E1',
          transition: 'all .2s cubic-bezier(0.4, 0, 0.2, 1)',

          '&:hover': {
            color: '#0A0E27',
            background: 'linear-gradient(135deg, #06B6D4 0%, #0891B2 100%)',
            paddingLeft: 20
          },

          '&:active, &.active, &.Mui-selected': {
            color: '#0A0E27',
            background: 'linear-gradient(135deg, #06B6D4 0%, #0891B2 100%)',
            fontWeight: 600
          },

          '&.Mui-selected:hover': {
            background: 'linear-gradient(135deg, #06B6D4 0%, #0891B2 100%)'
          }
        }
      }
    },
    MuiListItem: {
      styleOverrides: {
        root: {
          '&.MuiButtonBase-root': {
            color: colors.secondary.main,

            '&:hover, &:active, &.active, &.Mui-selected': {
              color: colors.alpha.black[100],
              background: lighten(colors.primary.lighter, 0.5)
            }
          }
        }
      }
    },
    MuiAutocomplete: {
      styleOverrides: {
        tag: {
          margin: 1
        },
        root: {
          '.MuiAutocomplete-inputRoot.MuiOutlinedInput-root .MuiAutocomplete-endAdornment':
            {
              right: 14
            }
        },
        clearIndicator: {
          background: colors.error.lighter,
          color: colors.error.main,
          marginRight: 8,

          '&:hover': {
            background: colors.error.lighter,
            color: colors.error.dark
          }
        },
        popupIndicator: {
          color: colors.alpha.black[50],

          '&:hover': {
            background: colors.primary.lighter,
            color: colors.primary.main
          }
        }
      }
    },
    MuiTablePagination: {
      styleOverrides: {
        toolbar: {
          '& .MuiIconButton-root': {
            padding: 8
          }
        },
        select: {
          '&:focus': {
            backgroundColor: 'transparent'
          }
        }
      }
    },
    MuiToolbar: {
      styleOverrides: {
        root: {
          minHeight: '0 !important',
          padding: '0 !important'
        }
      }
    },
    MuiTableRow: {
      styleOverrides: {
        head: {
          background: '#1E293B'
        },
        root: {
          transition: 'background-color .2s',

          '&.MuiTableRow-hover:hover': {
            backgroundColor: '#111629'
          }
        }
      }
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderBottomColor: '#2D3E5F',
          fontSize: 14,
          color: '#F1F5F9',
          '&.MuiTableCell-head': {
            backgroundColor: '#111629 !important',
            color: '#F1F5F9 !important'
          }
        },
        head: {
          textTransform: 'uppercase',
          fontSize: 13,
          fontWeight: 'bold',
          color: '#F1F5F9',
          backgroundColor: '#111629'
        },
        stickyHeader: {
          backgroundColor: '#111629 !important',
          color: '#F1F5F9 !important'
        }
      }
    },
    MuiAlert: {
      styleOverrides: {
        message: {
          lineHeight: 1.5,
          fontSize: 14
        },
        standardInfo: {
          color: colors.info.main
        },
        action: {
          color: colors.alpha.black[70]
        }
      }
    },
    MuiTimelineDot: {
      styleOverrides: {
        root: {
          margin: 0,
          zIndex: 5,
          position: 'absolute',
          top: '50%',
          marginTop: -6,
          left: -6
        },
        outlined: {
          backgroundColor: colors.alpha.white[100],
          boxShadow: '0 0 0 6px ' + colors.alpha.white[100]
        },
        outlinedPrimary: {
          backgroundColor: colors.alpha.white[100],
          boxShadow: '0 0 0 6px ' + colors.alpha.white[100]
        }
      }
    },
    MuiTimelineConnector: {
      styleOverrides: {
        root: {
          position: 'absolute',
          height: '100%',
          top: 0,
          borderRadius: 50,
          backgroundColor: colors.alpha.black[10]
        }
      }
    },
    MuiTimelineItem: {
      styleOverrides: {
        root: {
          minHeight: 0,
          padding: '8px 0',

          '&:before': {
            display: 'none'
          }
        },
        missingOppositeContent: {
          '&:before': {
            display: 'none'
          }
        }
      }
    },
    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          backgroundColor: alpha(colors.alpha.black['100'], 0.95),
          padding: '8px 16px',
          fontSize: 13
        },
        arrow: {
          color: alpha(colors.alpha.black['100'], 0.95)
        }
      }
    },
    MuiSwitch: {
      styleOverrides: {
        root: {
          height: 33,
          overflow: 'visible',

          '& .MuiButtonBase-root': {
            position: 'absolute',
            padding: 6,
            transition:
              'left 150ms cubic-bezier(0.4, 0, 0.2, 1) 0ms,transform 150ms cubic-bezier(0.4, 0, 0.2, 1) 0ms'
          },
          '& .MuiIconButton-root': {
            borderRadius: 100
          },
          '& .MuiSwitch-switchBase.Mui-checked + .MuiSwitch-track': {
            opacity: 0.3
          }
        },
        thumb: {
          border: '1px solid ' + colors.alpha.black[30],
          boxShadow:
            '0px 9px 14px ' +
            colors.alpha.black[10] +
            ', 0px 2px 2px ' +
            colors.alpha.black[10]
        },
        track: {
          backgroundColor: colors.alpha.black[5],
          border: '1px solid ' + colors.alpha.black[10],
          boxShadow: 'inset 0px 1px 1px ' + colors.alpha.black[10],
          opacity: 1
        },
        colorPrimary: {
          '& .MuiSwitch-thumb': {
            backgroundColor: colors.alpha.white[100]
          },

          '&.Mui-checked .MuiSwitch-thumb': {
            backgroundColor: colors.primary.main
          }
        }
      }
    },
    MuiStepper: {
      styleOverrides: {
        root: {
          paddingTop: 20,
          paddingBottom: 20,
          background: colors.alpha.black[5]
        }
      }
    },
    MuiStepIcon: {
      styleOverrides: {
        root: {
          '&.MuiStepIcon-completed': {
            color: colors.success.main
          }
        }
      }
    },
    MuiDrawer: {
      styleOverrides: {
        paper: {
          backgroundColor: '#0A0E27 !important',
          backgroundImage: 'none !important',
          color: '#F1F5F9',
          '& .MuiButton-textPrimary': {
            color: '#F1F5F9',
            '&:hover': {
              backgroundColor: '#1E293B'
            }
          },
          '& .MuiSvgIcon-root': {
            color: '#CBD5E1'
          }
        }
      }
    },
    MuiTypography: {
      defaultProps: {
        variantMapping: {
          h1: 'h1',
          h2: 'h2',
          h3: 'div',
          h4: 'div',
          h5: 'div',
          h6: 'div',
          subtitle1: 'div',
          subtitle2: 'div',
          body1: 'div',
          body2: 'div'
        }
      },
      styleOverrides: {
        gutterBottom: {
          marginBottom: 4
        },
        paragraph: {
          fontSize: 17,
          lineHeight: 1.7
        },
        h1: {
          color: '#F1F5F9'
        },
        h2: {
          color: '#F1F5F9'
        },
        h3: {
          color: '#F1F5F9'
        },
        h4: {
          color: '#E2E8F0'
        },
        h5: {
          color: '#E2E8F0'
        },
        h6: {
          color: '#E2E8F0 !important'
        },
        subtitle1: {
          color: '#CBD5E1'
        },
        subtitle2: {
          color: '#CBD5E1'
        },
        body1: {
          color: '#F1F5F9 !important'
        },
        body2: {
          color: '#CBD5E1 !important'
        },
        caption: {
          color: '#94A3B8'
        }
      }
    }
  },
  shape: {
    borderRadius: 10
  },
  typography: {
    fontFamily:
      '"Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji"',
    h1: {
      fontWeight: 700,
      fontSize: 35,
      color: '#F1F5F9'
    },
    h2: {
      fontWeight: 700,
      fontSize: 30,
      color: '#F1F5F9'
    },
    h3: {
      fontWeight: 700,
      fontSize: 25,
      lineHeight: 1.4,
      color: '#F1F5F9'
    },
    h4: {
      fontWeight: 700,
      fontSize: 16,
      color: '#E2E8F0'
    },
    h5: {
      fontWeight: 700,
      fontSize: 14,
      color: '#E2E8F0'
    },
    h6: {
      fontSize: 15,
      color: '#E2E8F0'
    },
    body1: {
      fontSize: 14,
      color: '#F1F5F9'
    },
    body2: {
      fontSize: 14,
      color: '#CBD5E1'
    },
    button: {
      fontWeight: 600
    },
    caption: {
      fontSize: 13,
      textTransform: 'uppercase',
      color: colors.alpha.black[50]
    },
    subtitle1: {
      fontSize: 14,
      color: colors.alpha.black[70]
    },
    subtitle2: {
      fontWeight: 400,
      fontSize: 15,
      color: colors.alpha.black[70]
    },
    overline: {
      fontSize: 13,
      fontWeight: 700,
      textTransform: 'uppercase'
    }
  },
  shadows: [
    'none',
    'none',
    'none',
    'none',
    'none',
    'none',
    'none',
    'none',
    'none',
    'none',
    'none',
    'none',
    'none',
    'none',
    'none',
    'none',
    'none',
    'none',
    'none',
    'none',
    'none',
    'none',
    'none',
    'none',
    'none'
  ]
});
