import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import deJSON from './translations/de';
import arJSON from './translations/ar';
import locale from './translations/en';
import esJSON from './translations/es';
import frJSON from './translations/fr';
import trJSON from './translations/tr';
import plJSON from './translations/pl';
import ptBRJSON from './translations/pt_BR';
import itJSON from './translations/it';
import svJSON from './translations/sv';
import ruJSON from './translations/ru';
import huJSON from './translations/hu';
import nlJSON from './translations/nl';
import zhCNJSON from './translations/zh_cn';
import { FlagComponent } from 'country-flag-icons/react/1x1';
import {
  BR,
  DE,
  ES,
  FR,
  PL,
  TR,
  US,
  SA,
  IT,
  SE,
  RU,
  HU,
  NL,
  CN
} from 'country-flag-icons/react/3x2';
import { LocaleSingularArg } from '@fullcalendar/react';
import deLocale from '@fullcalendar/core/locales/de';
import esLocale from '@fullcalendar/core/locales/es';
import arLocale from '@fullcalendar/core/locales/ar';
import trLocale from '@fullcalendar/core/locales/tr';
import ptBRLocale from '@fullcalendar/core/locales/pt-br';
import frLocale from '@fullcalendar/core/locales/fr';
import plLocale from '@fullcalendar/core/locales/pl';
import enLocale from '@fullcalendar/core/locales/en-gb';
import itLocale from '@fullcalendar/core/locales/it';
import svLocale from '@fullcalendar/core/locales/sv';
import ruLocale from '@fullcalendar/core/locales/ru';
import huLocale from '@fullcalendar/core/locales/hu';
import nlLocale from '@fullcalendar/core/locales/nl';
import zhCNLocale from '@fullcalendar/core/locales/zh-cn';
import { Locale as DateLocale } from 'date-fns';
import {
  fr,
  de,
  es,
  ar,
  tr,
  ptBR,
  pl,
  enUS,
  it,
  sv,
  ru,
  hu,
  nl,
  zhCN
} from 'date-fns/locale';

const resources = {
  de: { translation: deJSON },
  en: { translation: locale },
  es: { translation: esJSON },
  fr: { translation: frJSON },
  pl: { translation: plJSON },
  tr: { translation: trJSON },
  pt_br: { translation: ptBRJSON },
  ar: { translation: arJSON },
  it: { translation: itJSON },
  sv: { translation: svJSON },
  ru: { translation: ruJSON },
  hu: { translation: huJSON },
  nl: { translation: nlJSON },
  zh_cn: { translation: zhCNJSON }
};

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources,
    // supportedLngs: Object.keys(resources),
    keySeparator: false,
    fallbackLng: 'en',
    react: {
      useSuspense: true
    },
    interpolation: {
      escapeValue: false
    },
    detection: {
      lookupLocalStorage: 'lang',
      caches: ['localStorage'],
      convertDetectedLanguage: (lng) => lng.split('-')[0].toLowerCase()
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
  | 'ZH_CN';

export const supportedLanguages: {
  code: Lowercase<SupportedLanguage>;
  label: string;
  Icon: FlagComponent;
  calendarLocale: LocaleSingularArg;
  dateLocale: DateLocale;
}[] = [
  {
    code: 'en',
    label: 'English',
    Icon: US,
    calendarLocale: enLocale,
    dateLocale: enUS
  },
  {
    code: 'fr',
    label: 'French',
    Icon: FR,
    calendarLocale: frLocale,
    dateLocale: fr
  },
  {
    code: 'es',
    label: 'Spanish',
    Icon: ES,
    calendarLocale: esLocale,
    dateLocale: es
  },
  {
    code: 'de',
    label: 'German',
    Icon: DE,
    calendarLocale: deLocale,
    dateLocale: de
  },
  {
    code: 'tr',
    label: 'Turkish',
    Icon: TR,
    calendarLocale: trLocale,
    dateLocale: tr
  },
  {
    code: 'pt_br',
    label: 'Portuguese (Brazil)',
    Icon: BR,
    calendarLocale: ptBRLocale,
    dateLocale: ptBR
  },
  {
    code: 'pl',
    label: 'Polish',
    Icon: PL,
    calendarLocale: plLocale,
    dateLocale: pl
  },
  {
    code: 'ar',
    label: 'Arabic',
    Icon: SA,
    calendarLocale: arLocale,
    dateLocale: ar
  },
  {
    code: 'it',
    label: 'Italian',
    Icon: IT,
    calendarLocale: itLocale,
    dateLocale: it
  },
  {
    code: 'sv',
    label: 'Swedish',
    Icon: SE,
    calendarLocale: svLocale,
    dateLocale: sv
  },
  {
    code: 'ru',
    label: 'Russian',
    Icon: RU,
    calendarLocale: ruLocale,
    dateLocale: ru
  },
  {
    code: 'hu',
    label: 'Hungarian',
    Icon: HU,
    calendarLocale: huLocale,
    dateLocale: hu
  },
  {
    code: 'nl',
    label: 'Dutch',
    Icon: NL,
    calendarLocale: nlLocale,
    dateLocale: nl
  },
  {
    code: 'zh_cn',
    label: 'Chinese (Simplified)',
    Icon: CN,
    calendarLocale: zhCNLocale,
    dateLocale: zhCN
  }
];

export default i18n;
