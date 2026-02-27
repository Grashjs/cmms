"use client";
import React, { useEffect, useState } from 'react';
import { ThemeProvider } from '@mui/material';
import { themeCreator } from './base';
import { StylesProvider } from '@mui/styles';
import { CacheProvider } from '@emotion/react';
import createCache from '@emotion/cache';
import stylisRTLPlugin from 'stylis-plugin-rtl';
import { useLocale } from 'next-intl'; // 👈 replace useTranslation

const cacheRtl = createCache({
  key: 'bloom-ui',
  prepend: true,
  // @ts-ignore
  stylisPlugins: [stylisRTLPlugin]
});

export const ThemeContext = React.createContext(
    (themeName: string): void => {}
);

const RTL_LOCALES = ['ar', 'he', 'fa', 'ur']; // 👈 define your RTL locales

const ThemeProviderWrapper: React.FC<{children?: React.ReactNode}> = (props) => {
  const [themeName, _setThemeName] = useState('PureLightTheme');

  useEffect(() => {
    const curThemeName = localStorage.getItem('appTheme') || 'PureLightTheme';
    _setThemeName(curThemeName);
  }, []);

  const theme = themeCreator(themeName);
  const locale = useLocale(); // 👈 get current locale
  const rtl = RTL_LOCALES.includes(locale); // 👈 derive direction

  const setThemeName = (themeName: string): void => {
    localStorage.setItem('appTheme', themeName);
    _setThemeName(themeName);
  };

  useEffect(() => {
    if (rtl) document.documentElement.setAttribute('dir', 'rtl');
    else document.documentElement.removeAttribute('dir');
  }, [rtl]);

  const providers = (
      <ThemeContext.Provider value={setThemeName}>
        <ThemeProvider theme={theme}>{props.children}</ThemeProvider>
      </ThemeContext.Provider>
  );
  return (
      <StylesProvider injectFirst>
        {rtl ? (
            <CacheProvider value={cacheRtl}>{providers}</CacheProvider>
        ) : (
            providers
        )}
      </StylesProvider>
  );
};

export default ThemeProviderWrapper;
