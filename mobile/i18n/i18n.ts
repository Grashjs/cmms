import i18n from 'i18next';

import { initReactI18next } from 'react-i18next';
// import LanguageDetector from 'i18next-browser-languagedetector';
import deJSON from './translations/de';
import arJSON from './translations/ar';
import locale from './translations/en';
import esJSON from './translations/es';
import frJSON from './translations/fr';
import trJSON from './translations/tr';
import plJSON from './translations/pl';
import itJSON from './translations/it';
import ptBRJSON from './translations/pt_BR';
import svJSON from './translations/sv';
import ruJSON from './translations/ru';
import huJSON from './translations/hu';
import nlJSON from './translations/nl';
import zhCnJSON from './translations/zh_cn';
import baJSON from './translations/ba';
import jaJSON from './translations/ja';

const resources = {
  de: { translation: deJSON },
  en: { translation: locale },
  es: { translation: esJSON },
  fr: { translation: frJSON },
  tr: { translation: trJSON },
  pl: { translation: plJSON },
  pt_br: { translation: ptBRJSON },
  ar: { translation: arJSON },
  it: { translation: itJSON },
  sv: { translation: svJSON },
  ru: { translation: ruJSON },
  hu: { translation: huJSON },
  nl: { translation: nlJSON },
  zh_cn: { translation: zhCnJSON },
  ba: { translation: baJSON },
  ja: { translation: jaJSON }
};

i18n
  // .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    compatibilityJSON: 'v3',
    resources,
    keySeparator: false,
    lng: 'en',
    fallbackLng: 'en',
    react: {
      useSuspense: true
    },
    interpolation: {
      escapeValue: false
    }
  });

export type SupportedLanguage =
  | 'DE'
  | 'EN'
  | 'FR'
  | 'TR'
  | 'ES'
  | 'PT_BR'
  | 'PL'
  | 'IT'
  | 'SV'
  | 'RU'
  | 'AR'
  | 'HU'
  | 'NL'
  | 'ZH_CN'
  | 'BA'
  | 'JA';

export const supportedLanguages: {
  code: Lowercase<SupportedLanguage>;
  label: string;
}[] = [
  { code: 'en', label: 'English' },
  { code: 'fr', label: 'French' },
  { code: 'es', label: 'Spanish' },
  { code: 'de', label: 'German' },
  { code: 'tr', label: 'Turkish' },
  { code: 'pt_br', label: 'Portuguese (Brazil)' },
  { code: 'pl', label: 'Polish' },
  { code: 'ar', label: 'Arabic' },
  { code: 'it', label: 'Italian' },
  { code: 'sv', label: 'Swedish' },
  { code: 'ru', label: 'Russian' },
  { code: 'hu', label: 'Hungarian' },
  { code: 'nl', label: 'Dutch' },
  { code: 'zh_cn', label: 'Chinese (Simplified)' },
  { code: 'ba', label: 'Bosnian' },
  { code: 'ja', label: 'Japanese' }
];

export default i18n;
