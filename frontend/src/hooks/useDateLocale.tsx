import { useEffect, useState } from 'react';
import { Locale as DateLocale } from 'date-fns';
import { enUS } from 'date-fns/locale';
import { getDateLocale } from '../i18n/i18n';
import { useTranslation } from 'react-i18next';

const useDateLocale = (): DateLocale => {
  const { i18n } = useTranslation();
  const [dateLocale, setDateLocale] = useState<DateLocale>(enUS);

  useEffect(() => {
    getDateLocale(i18n.language).then(setDateLocale);
  }, [i18n.language]);

  return dateLocale;
};

export default useDateLocale;
