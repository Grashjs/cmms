'use client';
import { ReactNode, useEffect } from 'react';
import { initI18n } from './i18n';

export default function I18nProvider({
                                       lang,
                                       children
                                     }: {
  lang: string;
  children: ReactNode;
}) {
  // Initialize synchronously with the server-detected lang
  initI18n(lang);

  // Sync if language drifts (e.g. user had a different lang in localStorage)
  useEffect(() => {
    const i18n = initI18n(lang);
    if (i18n.language !== lang) {
      i18n.changeLanguage(lang);
    }
  }, [lang]);

  return <>{children}</>;
}
