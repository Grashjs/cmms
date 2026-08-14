import { StyleSheet, ViewStyle } from 'react-native';
import type { AppTheme } from '../custom-theme';
import { elevation } from './tokens';

/**
 * Styling for a card sitting above the screen background.
 *
 * A drop shadow is how a light surface reads as raised, but shadows are
 * essentially invisible against a dark background, which left cards
 * indistinguishable from the page. Dark uses a hairline outline instead.
 */
export const raisedSurface = (theme: AppTheme): ViewStyle => ({
  backgroundColor: theme.colors.card,
  ...(theme.dark
    ? {
        borderWidth: StyleSheet.hairlineWidth,
        borderColor: theme.colors.border
      }
    : elevation.card)
});
