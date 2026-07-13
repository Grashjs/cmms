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
import ptBRJSON from './translations/pt_BR';

const resources = {
  de: { translation: deJSON },
  en: { translation: locale },
  es: { translation: esJSON },
  fr: { translation: frJSON },
  tr: { translation: trJSON },
  pl: { translation: plJSON },
  pt_br: { translation: ptBRJSON },
  ar: { translation: arJSON }
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

export default i18n;
