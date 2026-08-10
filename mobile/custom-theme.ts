import { MD3LightTheme as DefaultTheme, useTheme } from 'react-native-paper';
import { palette } from './theme/tokens';

export const customTheme = {
  ...DefaultTheme,
  colors: {
    ...DefaultTheme.colors,
    primary: palette.primary,
    secondary: palette.secondary,
    tertiary: palette.tertiary,
    background: palette.background,
    secondaryContainer: palette.secondaryContainer,
    success: palette.success,
    warning: palette.warning,
    error: palette.error,
    info: palette.info,
    black: palette.black,
    white: palette.white,
    primaryAlt: palette.primaryAlt,
    primaryContainer: palette.primaryContainer,
    tertiaryContainer: palette.tertiaryContainer,
    grey: palette.grey,
    text: palette.text,
    textInverse: palette.textInverse
  }
};

/**
 * The app theme extends MD3 with the semantic colors above. Paper's own
 * `useTheme()` resolves to `MD3Theme`, which does not know about them, so
 * anything reading `colors.success` and friends must go through `useAppTheme`
 * or accept an `AppTheme`. `MD3Colors` is a type alias rather than an
 * interface, so it cannot be widened by declaration merging.
 */
export type AppTheme = typeof customTheme;

export const useAppTheme = () => useTheme<AppTheme>();
