import React, {
  createContext,
  ReactNode,
  useContext,
  useEffect,
  useMemo,
  useState
} from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  ColorSchemeName,
  useColorScheme as _useColorScheme
} from 'react-native';
import { Theme } from 'react-native-paper';
import { darkTheme, lightTheme } from './custom-theme';

export type ThemeMode = 'system' | 'light' | 'dark';
const STORAGE_KEY = 'themeMode';

export interface ThemeModeContextValue {
  themeMode: ThemeMode;
  setThemeMode: (mode: ThemeMode) => void;
  resolvedScheme: Exclude<ColorSchemeName, null>;
  theme: Theme;
}

const ThemeModeContext = createContext<ThemeModeContextValue>({
  themeMode: 'system',
  setThemeMode: () => {},
  resolvedScheme: 'light',
  theme: lightTheme
});

export const ThemeModeProvider = ({ children }: { children: ReactNode }) => {
  const systemScheme = (_useColorScheme() || 'light') as Exclude<ColorSchemeName, null>;
  const [themeMode, setThemeModeState] = useState<ThemeMode>('system');
  const [isLoaded, setIsLoaded] = useState(false);

  useEffect(() => {
    AsyncStorage.getItem(STORAGE_KEY)
      .then((value) => {
        if (value === 'system' || value === 'light' || value === 'dark') {
          setThemeModeState(value);
        }
      })
      .finally(() => setIsLoaded(true));
  }, []);

  useEffect(() => {
    if (!isLoaded) {
      return;
    }
    AsyncStorage.setItem(STORAGE_KEY, themeMode).catch(() => {
      // ignore storage failures
    });
  }, [themeMode, isLoaded]);

  const resolvedScheme = themeMode === 'system' ? systemScheme : themeMode;
  const theme = resolvedScheme === 'dark' ? darkTheme : lightTheme;

  const value = useMemo(
    () => ({
      themeMode,
      setThemeMode: setThemeModeState,
      resolvedScheme,
      theme
    }),
    [themeMode, resolvedScheme, theme]
  );

  if (!isLoaded) {
    return null;
  }

  return (
    <ThemeModeContext.Provider value={value}>
      {children}
    </ThemeModeContext.Provider>
  );
};

export const useThemeMode = () => useContext(ThemeModeContext);
