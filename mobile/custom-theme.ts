import { MD3DarkTheme, MD3LightTheme, useTheme } from 'react-native-paper';

const baseColors = {
  primary: '#5569ff',
  secondary: '#959be0',
  tertiary: '#9DA1A1',
  success: '#57CA22',
  warning: '#FFA319',
  error: '#FF1943',
  info: '#33C2FF',
  black: '#223354',
  white: '#ffffff',
  primaryAlt: '#000C57',
  primaryContainer: '#333586',
  tertiaryContainer: 'black'
};

export const lightTheme = {
  ...MD3LightTheme,
  colors: {
    ...MD3LightTheme.colors,
    ...baseColors,
    background: '#ebecf6',
    surface: '#ffffff',
    onSurface: '#1f2937',
    surfaceVariant: '#e5e7eb',
    onSurfaceVariant: '#4b5563',
    outline: '#9ca3af',
    grey: '#6b7280'
  }
};

export const darkTheme = {
  ...MD3DarkTheme,
  colors: {
    ...MD3DarkTheme.colors,
    ...baseColors,
    background: '#121212',
    surface: '#1e1e1e',
    onSurface: '#f9fafb',
    surfaceVariant: '#2c2c2c',
    onSurfaceVariant: '#d1d5db',
    outline: '#4b5563',
    grey: '#d1d5db'
  }
};

export const useAppTheme = () => useTheme();
