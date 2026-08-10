/**
 * Thin wrappers around React Native's `Text` and `View` that resolve their
 * colors from the app theme, with an optional per-scheme override.
 *
 * Dark mode is not wired up yet, so `useColorScheme` currently only selects
 * between the caller's `lightColor` and `darkColor` overrides. Once the dark
 * theme lands these should read a scheme-aware surface color instead of
 * `colors.white`.
 */

import { Text as DefaultText, View as DefaultView } from 'react-native';

import useColorScheme from '../hooks/useColorScheme';
import { useAppTheme } from '../custom-theme';

type ThemeProps = {
  lightColor?: string;
  darkColor?: string;
};

export type TextProps = ThemeProps & DefaultText['props'];
export type ViewProps = ThemeProps & DefaultView['props'];

/** The caller's override for the active color scheme, if they supplied one. */
function useSchemeOverride({ lightColor, darkColor }: ThemeProps) {
  return useColorScheme() === 'dark' ? darkColor : lightColor;
}

export function Text(props: TextProps) {
  const { style, lightColor, darkColor, ...otherProps } = props;
  const theme = useAppTheme();
  const override = useSchemeOverride({ lightColor, darkColor });
  const color = override ?? theme.colors.text;

  return <DefaultText style={[{ color }, style]} {...otherProps} />;
}

export function View(props: ViewProps) {
  const { style, lightColor, darkColor, ...otherProps } = props;
  const theme = useAppTheme();
  const override = useSchemeOverride({ lightColor, darkColor });
  const backgroundColor = override ?? theme.colors.white;

  return (
    <DefaultView style={[{ backgroundColor }, style]} {...otherProps} />
  );
}
