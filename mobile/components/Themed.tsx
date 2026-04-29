/**
 * Learn more about Light and Dark modes:
 * https://docs.expo.io/guides/color-schemes/
 */

import { Text as DefaultText, View as DefaultView } from 'react-native';
import { useTheme } from 'react-native-paper';

import Colors from '../constants/Colors';

export function useThemeColor(
  props: { light?: string; dark?: string },
  colorName: keyof typeof Colors.light & keyof typeof Colors.dark
) {
  const paperTheme = useTheme();
  const colorScheme = paperTheme.dark ? 'dark' : 'light';
  const colorFromProps = props[colorScheme];

  if (colorFromProps) {
    return colorFromProps;
  }

  const themeColor = paperTheme.colors[
    colorName as keyof typeof paperTheme.colors
  ] as string | undefined;

  if (themeColor) {
    return themeColor;
  }

  return Colors[colorScheme][colorName];
}

type ThemeProps = {
  lightColor?: string;
  darkColor?: string;
};

export type TextProps = ThemeProps & DefaultText['props'];
export type ViewProps = ThemeProps & DefaultView['props'];

export function Text(props: TextProps) {
  const { style, lightColor, darkColor, ...otherProps } = props;
  const color = useThemeColor({ light: lightColor, dark: darkColor }, 'text');

  return <DefaultText style={[{ color }, style]} {...otherProps} />;
}

export function View(props: ViewProps) {
  const { style, lightColor, darkColor, ...otherProps } = props;
  const backgroundColor = useThemeColor(
    { light: lightColor, dark: darkColor },
    'background'
  );
  const paperTheme = useTheme();
  const colorScheme = paperTheme.dark ? 'dark' : 'light';
  const defaultBackgroundColor =
    colorScheme === 'light' ? Colors.light.background : Colors.dark.background;
  const resolvedBackgroundColor =
    backgroundColor ?? paperTheme.colors.background ?? defaultBackgroundColor;

  return (
    <DefaultView
      style={[{ backgroundColor: resolvedBackgroundColor }, style]}
      {...otherProps}
    />
  );
}
