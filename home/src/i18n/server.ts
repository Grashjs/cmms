// src/i18n/server.ts
import { initI18n } from './i18n';

export default async function initTranslations(lang: string) {
  const i18n = initI18n(lang);

  if (i18n.language !== lang) {
    await i18n.changeLanguage(lang);
  }

  return i18n;
}