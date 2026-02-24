// components/HreflangHelmet.tsx
import { Helmet } from 'react-helmet-async';
import { supportedLanguages } from '../../../i18n/i18n';
import { useTranslation } from 'react-i18next';

interface HreflangHelmetProps {
  path: string;
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
      {/* x-default + all hreflang alternates */}
      <link rel="alternate" hrefLang="x-default" href={`${baseUrl}${slug}`} />
      {supportedLanguages.map((supportedLanguage) => {
        const code = supportedLanguage.code; // 'pt_br', 'zh_cn', 'en', etc.
        const isEnglish = code === 'en';
        const urlPrefix = isEnglish ? '' : `/${code}`;
        const hrefLang = code.replace('_', '-'); // 'pt_br' → 'pt-br', 'zh_cn' → 'zh-cn'

        return (
          <link
            key={code}
            rel="alternate"
            hrefLang={hrefLang}
            href={`${baseUrl}${urlPrefix}${slug}`}
          />
        );
      })}

      {children}
    </Helmet>
  );
}

export default SharedHelmet;
