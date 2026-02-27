import { Helmet } from "react-helmet-async";
import { useLocale } from "next-intl";
import { locales } from "src/i18n/request";

interface HreflangHelmetProps {
  path: string;
  title: string;
  description: string;
  keywords?: string;
  children?: React.ReactNode;
}

const baseUrl = "https://atlas-cmms.com";

function SharedHelmet({ path, title, description, keywords, children }: HreflangHelmetProps) {
  const locale = useLocale();
  const langPrefix = locale && locale !== "en" ? `/${locale}` : "";

  const slug = path ? `/${path}` : "";
  const canonicalLink = `${baseUrl}${langPrefix}${slug}`;
  return (
    <Helmet>
      <title>{title}</title>
      <meta name="description" content={description} />
      {keywords && <meta name="keywords" content={keywords} />}
      <link rel="canonical" href={canonicalLink} />
      <meta property="og:url" content={canonicalLink} />
      <meta property="og:description" content={description} />
      {/* x-default + all hreflang alternates */}
      <link rel="alternate" hrefLang="x-default" href={`${baseUrl}${slug}`} />
      {locales.map((code) => {
        const isEnglish = code === "en";
        const urlPrefix = isEnglish ? "" : `/${code}`;
        const hrefLang = code;

        return <link key={code} rel="alternate" hrefLang={hrefLang} href={`${baseUrl}${urlPrefix}${slug}`} />;
      })}

      {children}
    </Helmet>
  );
}

export default SharedHelmet;
