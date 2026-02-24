import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';

export default function HtmlLanguage() {
  const { i18n } = useTranslation();
  const hrefLang = i18n.language.replace('_', '-');

  return (
    <Helmet>
      <html lang={hrefLang} />
    </Helmet>
  );
}
