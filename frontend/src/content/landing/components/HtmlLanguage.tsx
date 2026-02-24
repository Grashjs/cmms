import { Helmet } from 'react-helmet-async';
import { useTranslation } from 'react-i18next';

export default function HtmlLanguage() {
  const { i18n } = useTranslation();

  return (
    <Helmet>
      <html lang={i18n.language} />
    </Helmet>
  );
}
