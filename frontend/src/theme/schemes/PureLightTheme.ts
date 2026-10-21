import { alpha, createTheme, lighten, darken } from '@mui/material';
import '@mui/lab/themeAugmentation';
import { customColors } from '../../config';
import {
  brand,
  controlHeight,
  elevation,
  fontFamily,
  layout,
  neutral,
  radius,
  status
} from '../tokens';
import i18n from 'src/i18n/i18n';

const themeColors = {
  primary: brand[500],
  secondary: neutral[500],
  success: status.success,
  warning: status.warning,
  error: status.error,
  info: status.info,
  black: neutral[900],
  white: neutral[0],
  primaryAlt: brand[900],
  ...customColors
};

const colors = {
  gradients: {
    blue1: 'linear-gradient(135deg, #38bdf8 0%, #1a87c8 100%)',
    blue2: 'linear-gradient(135deg, #bae6fd 0%, #0284c7 100%)',
    blue3: 'linear-gradient(127.55deg, #0f172a 3.73%, #1e293b 92.26%)',
    blue4: 'linear-gradient(-20deg, #075985 0%, #0c4a6e 100%)',
    blue5: 'linear-gradient(135deg, #7dd3fc 10%, #0369a1 100%)',
    orange1: 'linear-gradient(135deg, #fde047 0%, #f97316 100%)',
    orange2: 'linear-gradient(135deg, #fed7aa 0%, #fb7185 100%)',
    orange3: 'linear-gradient(120deg, #fde68a 0%, #f87171 100%)',
    purple1: 'linear-gradient(135deg, #38bdf8 0%, #6366f1 100%)',
    purple3: 'linear-gradient(135deg, #818cf8 0%, #4f46e5 100%)',
    pink1: 'linear-gradient(135deg, #fbcfe8 0%, #ec4899 100%)',
    pink2: 'linear-gradient(135deg, #f472b6 0%, #db2777 100%)',
    green1: 'linear-gradient(135deg, #a7f3d0 0%, #10b981 100%)',
    green2: 'linear-gradient(to bottom, #059669, #34d399)',
    black1: 'linear-gradient(100.66deg, #334155 6.56%, #0f172a 93.57%)',
    black2: 'linear-gradient(60deg, #1e293b 0%, #334155 100%)'
  },
  shadows: {
    success:
      '0 4px 14px 0 rgba(16, 185, 129, 0.25)',
    error:
      '0 4px 14px 0 rgba(239, 68, 68, 0.25)',
    info: '0 4px 14px 0 rgba(6, 182, 212, 0.25)',
    primary:
      '0 4px 14px 0 rgba(26, 135, 200, 0.3)',
    warning:
      '0 4px 14px 0 rgba(245, 158, 11, 0.25)',
    card: elevation.sm,
    cardSm: elevation.xs,
    cardLg: elevation.xl
  },
  layout: {
    general: {
      bodyBg: '#f4f6f9'
    },
    sidebar: {
      background: themeColors.white,
      textColor: themeColors.secondary,
      dividerBg: neutral[200],
      menuItemColor: '#1e293b',
      menuItemColorActive: themeColors.primary,
      menuItemBg: themeColors.white,
      menuItemBgActive: '#f0f9ff',
      menuItemIconColor: lighten(themeColors.secondary, 0.3),
      menuItemIconColorActive: themeColors.primary,
      menuItemHeadingColor: darken(themeColors.secondary, 0.3)
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

// @mui/x-data-grid's theme augmentation conflicts with @mui/lab's, so the
// DataGrid overrides are kept untyped and spread into `components`.
const dataGridOverrides: Record<string, any> = {
  MuiDataGrid: {
    styleOverrides: {
      root: {
        border: `1px solid ${neutral[200]}`,
        borderRadius: radius.md,
        fontSize: 14,
        '& .MuiDataGrid-columnHeaders': {
          backgroundColor: neutral[50],
          borderBottom: `1px solid ${neutral[200]}`
        },
        '& .MuiDataGrid-columnHeaderTitle': {
          fontSize: 12,
          fontWeight: 600,
          letterSpacing: '0.04em',
          textTransform: 'uppercase',
          color: neutral[500]
        },
        '& .MuiDataGrid-columnSeparator': {
          color: neutral[200]
        },
        '& .MuiDataGrid-cell': {
          borderBottomColor: neutral[100]
        },
        '& .MuiDataGrid-row:hover': {
          backgroundColor: alpha(brand[500], 0.04)
        },
        '& .MuiDataGrid-row.Mui-selected, & .MuiDataGrid-row.Mui-selected:hover': {
          backgroundColor: alpha(brand[500], 0.08)
        },
        '& .MuiDataGrid-cell:focus, & .MuiDataGrid-cell:focus-within, & .MuiDataGrid-columnHeader:focus, & .MuiDataGrid-columnHeader:focus-within':
          {
            outline: 'none'
          },
        '& .MuiDataGrid-footerContainer': {
          borderTop: `1px solid ${neutral[200]}`
        }
      }
    }
  }
};

export const PureLightTheme = createTheme({
  direction: i18n.dir(),
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
    reactFrameworkColor: brand[400],
    borderRadiusSm: `${radius.sm}px`,
    borderRadius: `${radius.md}px`,
    borderRadiusLg: `${radius.lg}px`,
    borderRadiusXl: `${radius.xl}px`
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
    boxShadow: '1px 0 0 rgba(15, 23, 42, 0.06)',
    width: `${layout.sidebarWidth}px`,
    collapsedWidth: `${layout.sidebarCollapsedWidth}px`
  },
  header: {
    height: `${layout.headerHeight}px`,
    background: colors.alpha.white[100],
    boxShadow: colors.shadows.cardSm,
    textColor: colors.secondary.main
  },
  spacing: 8,
  palette: {
    common: {
      black: colors.alpha.black[100],
      white: colors.alpha.white[100]
    },
    mode: 'light',
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
      primary: neutral[900],
      secondary: neutral[600],
      disabled: neutral[400]
    },
    grey: {
      50: neutral[50],
      100: neutral[100],
      200: neutral[200],
      300: neutral[300],
      400: neutral[400],
      500: neutral[500],
      600: neutral[600],
      700: neutral[700],
      800: neutral[800],
      900: neutral[900]
    },
    divider: neutral[200],
    background: {
      paper: colors.alpha.white[100],
      default: colors.layout.general.bodyBg
    },
    action: {
      active: neutral[600],
      hover: neutral[100],
      hoverOpacity: 0.08,
      selected: alpha(brand[500], 0.08),
      selectedOpacity: 0.1,
      disabled: colors.alpha.black[50],
      disabledBackground: neutral[100],
      disabledOpacity: 0.38,
      focus: colors.alpha.black[10],
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
    ...dataGridOverrides,
    MuiCheckbox: {
      styleOverrides: {
        root: {
          color: neutral[400],
          borderRadius: radius.sm
        }
      }
    },
    MuiDialog: {
      styleOverrides: {
        paper: {
          borderRadius: radius.xl,
          boxShadow: elevation.xl
        },
        paperFullScreen: {
          borderRadius: 0
        }
      }
    },
    MuiDialogTitle: {
      styleOverrides: {
        root: {
          padding: '20px 24px',
          fontSize: 18,
          fontWeight: 600,
          lineHeight: 1.4
        }
      }
    },
    MuiDialogContent: {
      styleOverrides: {
        root: {
          padding: '20px 24px'
        },
        dividers: {
          borderColor: neutral[200]
        }
      }
    },
    MuiDialogActions: {
      styleOverrides: {
        root: {
          padding: '16px 24px',
          gap: 8,
          '& > :not(:first-of-type)': {
            marginLeft: 0
          }
        }
      }
    },
    MuiPopover: {
      styleOverrides: {
        paper: {
          border: `1px solid ${neutral[200]}`,
          boxShadow: elevation.lg,
          borderRadius: radius.md + 2
        }
      }
    },
    MuiCardContent: {
      styleOverrides: {
        root: {
          padding: 20,
          '&:last-child': {
            paddingBottom: 20
          }
        }
      }
    },
    MuiFormLabel: {
      styleOverrides: {
        root: {
          fontSize: 14,
          fontWeight: 500,
          color: neutral[700]
        }
      }
    },
    MuiInputLabel: {
      styleOverrides: {
        root: {
          fontSize: 14,
          color: neutral[500],
          '&.Mui-focused': {
            color: brand[600]
          }
        },
        outlined: {
          transform: 'translate(14px, 10px) scale(1)',
          '&.MuiInputLabel-sizeSmall': {
            transform: 'translate(12px, 7px) scale(1)'
          },
          '&.MuiInputLabel-shrink': {
            transform: 'translate(14px, -8px) scale(0.8)',
            fontWeight: 500
          }
        }
      }
    },
    MuiBackdrop: {
      styleOverrides: {
        root: {
          backgroundColor: alpha(neutral[900], 0.45),
          backdropFilter: 'blur(2px)',

          '&.MuiBackdrop-invisible': {
            backgroundColor: 'transparent',
            backdropFilter: 'none'
          }
        }
      }
    },
    MuiFormHelperText: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          marginTop: 6,
          marginLeft: 2,
          marginRight: 2,
          fontSize: 12,
          fontWeight: 500,
          lineHeight: 1.45
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
          backgroundColor: colors.layout.general.bodyBg,
          color: neutral[900],
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
          background: brand[500],
          height: 3
        },
        '#nprogress .spinner-icon': {
          borderTopColor: brand[500],
          borderLeftColor: brand[500]
        },
        '::selection': {
          backgroundColor: brand[100],
          color: neutral[900]
        },
        '*::-webkit-scrollbar': {
          width: 10,
          height: 10
        },
        '*::-webkit-scrollbar-thumb': {
          backgroundColor: neutral[300],
          borderRadius: 10,
          border: '2px solid transparent',
          backgroundClip: 'content-box'
        },
        '*::-webkit-scrollbar-thumb:hover': {
          backgroundColor: neutral[400]
        },
        '*::-webkit-scrollbar-track': {
          backgroundColor: 'transparent'
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
        }
      }
    },
    MuiSelect: {
      styleOverrides: {
        iconOutlined: {
          color: neutral[400]
        },
        icon: {
          top: 'calc(50% - 12px)'
        }
      }
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: radius.md,
          backgroundColor: neutral[0],
          transition: 'box-shadow 0.15s ease-in-out',
          '& .MuiInputAdornment-positionEnd.MuiInputAdornment-outlined': {
            paddingRight: 6
          },
          '& .MuiOutlinedInput-notchedOutline': {
            borderColor: neutral[300],
            transition: 'border-color 0.15s ease-in-out'
          },
          '&:hover .MuiOutlinedInput-notchedOutline': {
            borderColor: neutral[400]
          },
          '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
            borderColor: brand[500],
            borderWidth: 1
          },
          '&.Mui-focused': {
            boxShadow: elevation.focus
          },
          '&.Mui-error .MuiOutlinedInput-notchedOutline': {
            borderColor: status.error
          },
          '&.Mui-error.Mui-focused': {
            boxShadow: `0 0 0 3px ${alpha(status.error, 0.15)}`
          },
          '&.Mui-disabled': {
            backgroundColor: neutral[50]
          },
          '&.Mui-disabled .MuiOutlinedInput-notchedOutline': {
            borderColor: neutral[200]
          }
        },
        input: {
          padding: '10px 14px',
          '&::placeholder': {
            color: neutral[400],
            opacity: 1
          }
        },
        inputSizeSmall: {
          padding: '7px 12px'
        },
        multiline: {
          padding: '10px 14px'
        }
      }
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: radius.lg,
          border: `1px solid ${neutral[200]}`,
          boxShadow: elevation.xs,
          backgroundImage: 'none'
        }
      }
    },
    MuiListSubheader: {
      styleOverrides: {
        colorPrimary: {
          fontWeight: 600,
          lineHeight: '36px',
          fontSize: 12,
          letterSpacing: '0.04em',
          textTransform: 'uppercase',
          background: neutral[50],
          color: neutral[500]
        }
      }
    },
    MuiCardHeader: {
      styleOverrides: {
        root: {
          padding: '16px 20px'
        },
        action: {
          marginTop: -4,
          marginBottom: -4,
          alignSelf: 'center'
        },
        title: {
          fontSize: 16,
          fontWeight: 600,
          color: neutral[900]
        },
        subheader: {
          fontSize: 13,
          color: neutral[500]
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
        root: {
          fontWeight: 500,
          borderRadius: radius.sm
        },
        sizeSmall: {
          height: 24,
          fontSize: 12
        },
        colorSecondary: {
          background: neutral[100],
          color: neutral[700],

          '&:hover': {
            background: neutral[200]
          }
        },
        deleteIcon: {
          color: neutral[400],

          '&:hover': {
            color: status.error
          }
        }
      }
    },
    MuiAccordion: {
      styleOverrides: {
        root: {
          boxShadow: 'none',
          border: `1px solid ${neutral[200]}`,
          borderRadius: radius.md,

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
          fontWeight: 600
        },
        colorDefault: {
          background: brand[100],
          color: brand[700]
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
          fontWeight: 600,
          borderRadius: radius.sm,
          transition: 'all .15s'
        },
        textPrimary: {
          '&.Mui-selected': {
            boxShadow: 'none'
          },
          '&.MuiButtonBase-root:hover': {
            background: neutral[100]
          },
          '&.Mui-selected.MuiButtonBase-root:hover': {
            background: brand[600]
          }
        }
      }
    },
    MuiButton: {
      defaultProps: {
        disableElevation: true
      },
      styleOverrides: {
        root: {
          fontWeight: 600,
          textTransform: 'none',
          borderRadius: radius.md,
          letterSpacing: '-0.005em',
          whiteSpace: 'nowrap',
          transition:
            'background-color .15s ease-in-out, border-color .15s ease-in-out, color .15s ease-in-out, box-shadow .15s ease-in-out',

          '&.Mui-focusVisible': {
            boxShadow: elevation.focus
          },
          '.MuiSvgIcon-root': {
            transition: 'all .2s'
          }
        },
        contained: {
          '&.Mui-disabled': {
            backgroundColor: neutral[200],
            color: neutral[400]
          }
        },
        containedPrimary: {
          backgroundColor: brand[500],
          color: neutral[0],
          '&:hover': {
            backgroundColor: brand[600]
          },
          '&:active': {
            backgroundColor: brand[700]
          }
        },
        outlined: {
          borderColor: neutral[300],
          '&.Mui-disabled': {
            borderColor: neutral[200]
          }
        },
        outlinedPrimary: {
          borderColor: alpha(brand[500], 0.5),
          color: brand[600],
          backgroundColor: neutral[0],
          '&:hover': {
            borderColor: brand[500],
            backgroundColor: brand[50]
          }
        },
        textPrimary: {
          color: brand[600],
          '&:hover': {
            backgroundColor: brand[50]
          }
        },
        startIcon: {
          marginRight: 6
        },
        endIcon: {
          marginLeft: 6,
          marginRight: -4
        },
        containedSecondary: {
          backgroundColor: neutral[800],
          color: neutral[0],
          '&:hover': {
            backgroundColor: neutral[900]
          }
        },
        outlinedSecondary: {
          backgroundColor: neutral[0],
          borderColor: neutral[300],
          color: neutral[700],

          '&:hover, &.MuiSelected': {
            backgroundColor: neutral[50],
            borderColor: neutral[400],
            color: neutral[900]
          }
        },
        sizeSmall: {
          minHeight: controlHeight.sm,
          padding: '4px 12px',
          fontSize: 13,
          lineHeight: 1.5
        },
        sizeMedium: {
          minHeight: controlHeight.md,
          padding: '8px 16px'
        },
        sizeLarge: {
          minHeight: controlHeight.lg,
          padding: '11px 22px',
          fontSize: 15
        },
        textSizeSmall: {
          padding: '4px 10px'
        },
        textSizeMedium: {
          padding: '8px 12px'
        },
        textSizeLarge: {
          padding: '11px 16px'
        }
      }
    },
    MuiButtonBase: {
      defaultProps: {
        disableRipple: false
      },
      styleOverrides: {
        root: {
          borderRadius: radius.sm
        }
      }
    },
    MuiToggleButton: {
      defaultProps: {
        disableRipple: true
      },
      styleOverrides: {
        root: {
          color: neutral[600],
          background: neutral[0],
          borderColor: neutral[300],
          fontWeight: 600,
          textTransform: 'none',
          transition: 'all .15s',

          '&:hover': {
            background: neutral[50],
            color: neutral[900]
          },
          '&.Mui-selected, &.Mui-selected:hover': {
            color: brand[600],
            background: brand[50]
          }
        }
      }
    },
    MuiIconButton: {
      styleOverrides: {
        root: {
          borderRadius: radius.md,
          padding: 8,
          transition: 'background-color .15s, color .15s',

          '& .MuiTouchRipple-root': {
            borderRadius: radius.md
          },
          '&.Mui-focusVisible': {
            boxShadow: elevation.focus
          }
        },
        sizeSmall: {
          padding: 5
        }
      }
    },
    MuiListItemText: {
      styleOverrides: {
        root: {
          margin: 0
        }
      }
    },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          '& .MuiTouchRipple-root': {
            opacity: 0.3
          }
        }
      }
    },
    MuiDivider: {
      styleOverrides: {
        root: {
          background: neutral[200],
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
          background: neutral[0],
          fontWeight: 600,
          fontSize: 12,
          height: 24,
          lineHeight: '24px',
          marginTop: -12,
          color: neutral[500],
          textTransform: 'uppercase',
          letterSpacing: '0.04em'
        }
      }
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          padding: 0,
          backgroundImage: 'none'
        },
        rounded: {
          borderRadius: radius.lg
        },
        elevation0: {
          boxShadow: 'none'
        },
        elevation: {
          boxShadow: elevation.sm
        },
        elevation1: {
          boxShadow: elevation.xs
        },
        elevation2: {
          boxShadow: elevation.sm
        },
        elevation24: {
          boxShadow: elevation.xl
        },
        outlined: {
          borderColor: neutral[200],
          boxShadow: 'none'
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

          '& .MuiListItem-button': {
            transition: 'all .2s',

            '& > .MuiSvgIcon-root': {
              minWidth: 34
            },

            '& .MuiTouchRipple-root': {
              opacity: 0.2
            }
          },
          '& .MuiListItem-root.MuiButtonBase-root.Mui-selected': {
            backgroundColor: brand[50]
          },
          '& .MuiMenuItem-root.MuiButtonBase-root:active': {
            backgroundColor: neutral[100]
          },
          '& .MuiMenuItem-root.MuiButtonBase-root .MuiTouchRipple-root': {
            opacity: 0.2
          }
        },
        padding: {
          padding: '12px',

          '& .MuiListItem-button': {
            borderRadius: 6,
            margin: '1px 0'
          }
        }
      }
    },
    MuiTabs: {
      styleOverrides: {
        root: {
          minHeight: 44,
          minWidth: 0,
          maxWidth: '100%'
        },
        indicator: {
          height: 2,
          borderRadius: '2px 2px 0 0',
          backgroundColor: brand[500]
        }
      }
    },
    MuiTab: {
      styleOverrides: {
        root: {
          padding: 0,
          minHeight: 44,
          fontSize: 14,
          fontWeight: 500,
          color: neutral[600],
          whiteSpace: 'nowrap',
          transition: 'color .15s',
          textTransform: 'none',

          '&.MuiButtonBase-root': {
            minWidth: 'auto',
            paddingLeft: 14,
            paddingRight: 14,
            marginRight: 4
          },
          '&.Mui-selected, &.Mui-selected:hover': {
            color: brand[600],
            fontWeight: 600
          },
          '&:hover': {
            color: neutral[900]
          },
          '&.Mui-disabled': {
            color: neutral[400]
          }
        }
      }
    },
    MuiMenu: {
      styleOverrides: {
        paper: {
          padding: 0
        },
        list: {
          padding: 6,

          '& .MuiMenuItem-root.MuiButtonBase-root': {
            fontSize: 14,
            borderRadius: radius.sm,
            marginTop: 1,
            marginBottom: 1,
            minHeight: 36,
            transition: 'background-color .15s, color .15s',
            color: neutral[700],

            '& .MuiTouchRipple-root': {
              opacity: 0.2
            },

            '&:hover, &:active, &.active': {
              color: neutral[900],
              background: neutral[100]
            },
            '&.Mui-selected, &.Mui-selected:hover': {
              color: brand[700],
              background: brand[50]
            }
          }
        }
      }
    },
    MuiMenuItem: {
      styleOverrides: {
        root: {
          background: 'transparent',
          transition: 'background-color .15s, color .15s',

          '&:hover, &:active, &.active': {
            color: neutral[900],
            background: neutral[100]
          },
          '&.Mui-selected, &.Mui-selected:hover': {
            color: brand[700],
            background: brand[50]
          }
        }
      }
    },
    MuiListItem: {
      styleOverrides: {
        root: {
          '&.MuiButtonBase-root': {
            color: neutral[700],

            '&:hover, &:active, &.active, &.Mui-selected': {
              color: neutral[900],
              background: brand[50]
            }
          }
        }
      }
    },
    MuiAutocomplete: {
      styleOverrides: {
        tag: {
          margin: 2
        },
        root: {
          '.MuiAutocomplete-inputRoot.MuiOutlinedInput-root': {
            paddingTop: 4,
            paddingBottom: 4
          },
          '.MuiAutocomplete-inputRoot.MuiOutlinedInput-root .MuiAutocomplete-endAdornment':
            {
              right: 10
            }
        },
        clearIndicator: {
          color: neutral[400],
          marginRight: 2,

          '&:hover': {
            background: neutral[100],
            color: neutral[700]
          }
        },
        popupIndicator: {
          color: neutral[400],

          '&:hover': {
            background: neutral[100],
            color: brand[600]
          }
        },
        paper: {
          marginTop: 4
        },
        option: {
          fontSize: 14,
          borderRadius: radius.sm,
          margin: '1px 6px',
          '&.Mui-focused': {
            backgroundColor: `${neutral[100]} !important`
          },
          '&[aria-selected="true"]': {
            backgroundColor: `${brand[50]} !important`,
            color: brand[700]
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
          background: neutral[50]
        },
        root: {
          transition: 'background-color .15s ease-in-out',

          '&.MuiTableRow-hover:hover': {
            backgroundColor: alpha(brand[500], 0.04)
          }
        }
      }
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderBottomColor: neutral[200],
          fontSize: 14,
          padding: '12px 16px'
        },
        head: {
          textTransform: 'uppercase',
          fontSize: 12,
          fontWeight: 600,
          letterSpacing: '0.04em',
          color: neutral[500],
          backgroundColor: neutral[50]
        },
        sizeSmall: {
          padding: '8px 12px'
        }
      }
    },
    MuiAlert: {
      styleOverrides: {
        root: {
          borderRadius: radius.md,
          alignItems: 'flex-start'
        },
        message: {
          lineHeight: 1.5,
          fontSize: 14
        },
        icon: {
          paddingTop: 8
        },
        standardInfo: {
          color: brand[800],
          backgroundColor: brand[50],
          border: `1px solid ${brand[100]}`
        },
        standardSuccess: {
          border: `1px solid ${alpha(status.success, 0.2)}`
        },
        standardWarning: {
          border: `1px solid ${alpha(status.warning, 0.25)}`
        },
        standardError: {
          border: `1px solid ${alpha(status.error, 0.2)}`
        },
        action: {
          color: neutral[500]
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
          backgroundColor: neutral[900],
          padding: '6px 10px',
          fontSize: 12,
          fontWeight: 500,
          lineHeight: 1.45,
          borderRadius: radius.sm
        },
        arrow: {
          color: neutral[900]
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
            opacity: 1
          }
        },
        thumb: {
          backgroundColor: neutral[0],
          boxShadow: '0 1px 3px rgba(15, 23, 42, 0.25)'
        },
        track: {
          backgroundColor: neutral[300],
          borderRadius: 20,
          opacity: 1
        },
        colorPrimary: {
          '& .MuiSwitch-thumb': {
            backgroundColor: neutral[0]
          },
          '&.Mui-checked .MuiSwitch-thumb': {
            backgroundColor: neutral[0]
          },
          '&.Mui-checked + .MuiSwitch-track': {
            backgroundColor: brand[500]
          }
        }
      }
    },
    MuiStepper: {
      styleOverrides: {
        root: {
          paddingTop: 20,
          paddingBottom: 20,
          background: 'transparent'
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
        }
      }
    }
  },
  shape: {
    borderRadius: radius.md
  },
  typography: {
    fontFamily,
    fontWeightLight: 300,
    fontWeightRegular: 400,
    fontWeightMedium: 500,
    fontWeightBold: 700,
    h1: {
      fontWeight: 700,
      fontSize: 32,
      lineHeight: 1.25,
      letterSpacing: '-0.02em'
    },
    h2: {
      fontWeight: 700,
      fontSize: 26,
      lineHeight: 1.3,
      letterSpacing: '-0.02em'
    },
    h3: {
      fontWeight: 600,
      fontSize: 20,
      lineHeight: 1.4,
      letterSpacing: '-0.01em',
      color: neutral[900]
    },
    h4: {
      fontWeight: 600,
      fontSize: 16,
      lineHeight: 1.5
    },
    h5: {
      fontWeight: 600,
      fontSize: 14,
      lineHeight: 1.5
    },
    h6: {
      fontWeight: 600,
      fontSize: 14,
      lineHeight: 1.5
    },
    body1: {
      fontSize: 14,
      lineHeight: 1.6
    },
    body2: {
      fontSize: 13,
      lineHeight: 1.55
    },
    button: {
      fontWeight: 600,
      fontSize: 14
    },
    caption: {
      fontSize: 12,
      lineHeight: 1.5,
      letterSpacing: '0.02em',
      textTransform: 'uppercase',
      color: neutral[500]
    },
    subtitle1: {
      fontSize: 14,
      lineHeight: 1.55,
      color: neutral[600]
    },
    subtitle2: {
      fontWeight: 400,
      fontSize: 14,
      lineHeight: 1.55,
      color: neutral[600]
    },
    overline: {
      fontSize: 12,
      fontWeight: 600,
      letterSpacing: '0.08em',
      textTransform: 'uppercase'
    }
  },
  shadows: [
    'none',
    '0 1px 2px 0 rgba(15, 23, 42, 0.05)',
    '0 1px 3px 0 rgba(15, 23, 42, 0.07), 0 1px 2px -1px rgba(15, 23, 42, 0.05)',
    '0 4px 6px -1px rgba(15, 23, 42, 0.08), 0 2px 4px -2px rgba(15, 23, 42, 0.05)',
    '0 10px 15px -3px rgba(15, 23, 42, 0.08), 0 4px 6px -4px rgba(15, 23, 42, 0.04)',
    '0 20px 25px -5px rgba(15, 23, 42, 0.08), 0 8px 10px -6px rgba(15, 23, 42, 0.04)',
    '0 25px 50px -12px rgba(15, 23, 42, 0.18)',
    '0 2px 4px rgba(15, 23, 42, 0.05)',
    '0 4px 8px rgba(15, 23, 42, 0.06)',
    '0 6px 12px rgba(15, 23, 42, 0.07)',
    '0 8px 16px rgba(15, 23, 42, 0.08)',
    '0 10px 20px rgba(15, 23, 42, 0.08)',
    '0 12px 24px rgba(15, 23, 42, 0.09)',
    '0 14px 28px rgba(15, 23, 42, 0.09)',
    '0 16px 32px rgba(15, 23, 42, 0.1)',
    '0 18px 36px rgba(15, 23, 42, 0.1)',
    '0 20px 40px rgba(15, 23, 42, 0.11)',
    '0 22px 44px rgba(15, 23, 42, 0.11)',
    '0 24px 48px rgba(15, 23, 42, 0.12)',
    '0 26px 52px rgba(15, 23, 42, 0.12)',
    '0 28px 56px rgba(15, 23, 42, 0.13)',
    '0 30px 60px rgba(15, 23, 42, 0.13)',
    '0 32px 64px rgba(15, 23, 42, 0.14)',
    '0 34px 68px rgba(15, 23, 42, 0.14)',
    '0 36px 72px rgba(15, 23, 42, 0.15)'
  ]
});
