/**
 * Design tokens.
 *
 * The scales below were derived from the values already used across the app so
 * that adopting them is a substitution rather than a redesign. Prefer a token
 * over a literal in new code; screens still using raw numbers are migrated
 * opportunistically as they are touched.
 */

export const spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 24,
  xxxl: 32
} as const;

export const radius = {
  sm: 4,
  md: 8,
  lg: 12,
  xl: 16,
  pill: 9999
} as const;

/**
 * For text that does not go through a react-native-paper `variant`. Paper's
 * MD3 type scale remains the default for anything rendered with its `Text`.
 */
export const fontSize = {
  xs: 11,
  sm: 13,
  md: 15,
  lg: 17,
  xl: 20,
  xxl: 24
} as const;

export const fontWeight = {
  regular: '400',
  medium: '500',
  semibold: '600',
  bold: '700'
} as const;

/**
 * iOS Human Interface Guidelines and Material both put the minimum touch target
 * at 44pt. Several controls in this app are currently smaller; use this with
 * `hitSlop` when a control cannot be made physically larger.
 */
export const touchTarget = {
  min: 44
} as const;

export const elevation = {
  card: {
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2,
    shadowRadius: 2,
    elevation: 5
  }
} as const;

export type PaletteColor =
  | 'primary'
  | 'primaryAlt'
  | 'primaryContainer'
  | 'secondary'
  | 'secondaryContainer'
  | 'tertiary'
  | 'tertiaryContainer'
  | 'background'
  | 'success'
  | 'warning'
  | 'error'
  | 'info'
  | 'black'
  | 'white'
  | 'grey'
  | 'card'
  | 'border'
  | 'text'
  | 'textInverse';

/**
 * Brand and semantic colors. These feed `custom-theme.ts`; components should
 * read them from the theme rather than importing this palette directly, so a
 * dark variant can override them in one place. Values are typed as `string`
 * rather than literals so an alternate palette can be substituted wholesale.
 */
export const palette: Record<PaletteColor, string> = {
  primary: '#5569ff',
  primaryAlt: '#000C57',
  primaryContainer: '#333586',
  secondary: '#959be0',
  secondaryContainer: '#7b7d93',
  tertiary: '#9DA1A1',
  tertiaryContainer: 'black',
  background: '#ebecf6',
  success: '#57CA22',
  warning: '#FFA319',
  error: '#FF1943',
  info: '#33C2FF',
  black: '#223354',
  white: '#ffffff',
  grey: '#676b6b',
  // Raised surfaces (cards, sheets, list rows) sitting on `background`.
  card: '#ffffff',
  // Hairline separator. Dark surfaces cannot rely on a drop shadow to read as
  // raised, so cards outline themselves instead.
  border: '#e0e2ee',
  // `black` above is a brand navy rather than true black. These two are the
  // plain foreground pair used by the `Themed` primitives, flipped by scheme.
  text: '#000000',
  textInverse: '#ffffff'
};

/**
 * Dark counterpart. Brand hues are lightened rather than reused directly:
 * the light `primary` fails contrast against a dark surface, and the status
 * colors need to stay distinguishable at low luminance since they are the
 * primary signal in work order lists.
 */
export const darkPalette: Record<PaletteColor, string> = {
  primary: '#8a99ff',
  primaryAlt: '#c7ceff',
  primaryContainer: '#3a3d8f',
  secondary: '#a8adea',
  secondaryContainer: '#4a4c5e',
  tertiary: '#b6baba',
  tertiaryContainer: '#e8eaea',
  background: '#121318',
  success: '#7BDC4B',
  warning: '#FFB84D',
  error: '#FF5C7A',
  info: '#5FD0FF',
  // `black` is the strong-foreground role rather than the literal color, so it
  // inverts. `white` stays literal white; use `card` for raised surfaces.
  black: '#e6e9f2',
  white: '#ffffff',
  grey: '#9ba0a0',
  // Deliberately lighter than `background` so cards stay distinguishable.
  card: '#1e2029',
  border: '#2c2f3a',
  text: '#ffffff',
  textInverse: '#000000'
};
