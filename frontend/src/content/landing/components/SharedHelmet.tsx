// components/HreflangHelmet.tsx
import { Helmet } from 'react-helmet-async';
import { supportedLanguages } from '../../../i18n/i18n';
import { useTranslation } from 'react-i18next';
import { useBrand } from '../../../hooks/useBrand';

interface HreflangHelmetProps {
  path: string; // e.g. 'free-cmms', 'pricing', ''
  title: string;
  description: string;
  keywords?: string;
  children?: React.ReactNode;
}
const baseUrl = 'https://atlas-cmms.com';

function SharedHelmet({
  path,
  title,
  description,
  keywords,
  children
}: HreflangHelmetProps) {
  const { t, i18n }: { t: any; i18n: any } = useTranslation();
  // const { website } = useBrand();
  const langPrefix =
    i18n.language && i18n.language !== 'en' ? `/${i18n.language}` : '';

  const slug = path ? `/${path}` : '';

  return (
    <Helmet>
      <title>{title}</title>
      <meta name="description" content={description} />
      {keywords && <meta name="keywords" content={keywords} />}
      <link rel="canonical" href={`${baseUrl}${langPrefix}${slug}`} />
      <link rel="alternate" hrefLang="x-default" href={`${baseUrl}${slug}`} />
      {supportedLanguages.map((supportedLanguage) => {
        const code = supportedLanguage.code.split('_')[0];
        const prefix = code !== 'en' ? `${code}` : '';
        return (
          <link
            key={supportedLanguage.code}
            rel="alternate"
            hrefLang={code}
            href={`${baseUrl}${prefix ? `/${prefix}` : ''}${slug}`}
          />
        );
      })}
      {children}
    </Helmet>
  );
}

export default SharedHelmet;
