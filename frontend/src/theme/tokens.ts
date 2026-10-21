/**
 * Webkorps design tokens.
 *
 * Single source of truth for brand colors, neutrals, radii, control sizes,
 * elevation and layout dimensions. Theme schemes and layout components read
 * from here instead of hard-coding values.
 */

// Sampled from the official Webkorps logo (#1A87C8).
export const brand = {
  50: '#eef7fc',
  100: '#d7ecf8',
  200: '#b0d9f1',
  300: '#7cbfe5',
  400: '#3fa0d8',
  500: '#1a87c8',
  600: '#156fa6',
  700: '#125a86',
  800: '#114a6d',
  900: '#0d3a55'
};

// Slate neutral scale – used for text, borders and surfaces.
export const neutral = {
  0: '#ffffff',
  50: '#f8fafc',
  100: '#f1f5f9',
  200: '#e2e8f0',
  300: '#cbd5e1',
  400: '#94a3b8',
  500: '#64748b',
  600: '#475569',
  700: '#334155',
  800: '#1e293b',
  900: '#0f172a'
};

export const status = {
  success: '#16a34a',
  warning: '#f59e0b',
  error: '#dc2626',
  info: '#0ea5e9'
};

export const radius = {
  xs: 4,
  sm: 6,
  md: 8,
  lg: 12,
  xl: 16
};

// Shared heights so buttons, inputs and selects line up in a row.
export const controlHeight = {
  sm: 32,
  md: 40,
  lg: 48
};

export const elevation = {
  xs: '0 1px 2px 0 rgba(15, 23, 42, 0.05)',
  sm: '0 1px 3px 0 rgba(15, 23, 42, 0.06), 0 1px 2px -1px rgba(15, 23, 42, 0.06)',
  md: '0 4px 8px -2px rgba(15, 23, 42, 0.08), 0 2px 4px -2px rgba(15, 23, 42, 0.04)',
  lg: '0 12px 24px -6px rgba(15, 23, 42, 0.12), 0 4px 8px -4px rgba(15, 23, 42, 0.06)',
  xl: '0 24px 48px -12px rgba(15, 23, 42, 0.22)',
  focus: `0 0 0 3px rgba(26, 135, 200, 0.18)`
};

export const layout = {
  sidebarWidth: 264,
  sidebarCollapsedWidth: 76,
  headerHeight: 64,
  // Dark navigation surface; brand-tinted navy rather than pure black.
  sidebarBg: '#0f1b2d',
  sidebarBgElevated: '#15253b'
};

export const fontFamily =
  '"Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif, "Apple Color Emoji", "Segoe UI Emoji"';
