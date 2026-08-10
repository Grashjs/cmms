import {
  MD3DarkTheme,
  MD3LightTheme as DefaultTheme,
  useTheme
} from 'react-native-paper';
import {
  DarkTheme as NavigationDarkTheme,
  DefaultTheme as NavigationDefaultTheme,
  Theme as NavigationTheme
} from '@react-navigation/native';
import { darkPalette, palette, PaletteColor } from './theme/tokens';

const buildColors = (source: Record<PaletteColor, string>) => ({
  primary: source.primary,
  secondary: source.secondary,
  tertiary: source.tertiary,
  background: source.background,
  secondaryContainer: source.secondaryContainer,
  success: source.success,
  warning: source.warning,
  error: source.error,
  info: source.info,
  black: source.black,
  white: source.white,
  primaryAlt: source.primaryAlt,
  primaryContainer: source.primaryContainer,
  tertiaryContainer: source.tertiaryContainer,
  grey: source.grey,
  card: source.card,
  border: source.border,
  text: source.text,
  textInverse: source.textInverse
});

export const customTheme = {
  ...DefaultTheme,
  colors: {
    ...DefaultTheme.colors,
    ...buildColors(palette)
  }
};

export const darkTheme = {
  ...MD3DarkTheme,
  colors: {
    ...MD3DarkTheme.colors,
    ...buildColors(darkPalette)
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

/**
 * React Navigation keeps its own theme for the container background and
 * header, so it has to be kept in step with Paper's or screens flash the
 * wrong color during transitions.
 */
export const getNavigationTheme = (dark: boolean): NavigationTheme => {
  const base = dark ? NavigationDarkTheme : NavigationDefaultTheme;
  const source = dark ? darkPalette : palette;

  return {
    ...base,
    dark,
    colors: {
      ...base.colors,
      primary: source.primary,
      background: source.background,
      card: source.card,
      text: source.text,
      border: source.border,
      notification: source.error
    }
  };
};
