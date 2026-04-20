import { Metadata } from "next";
import { getLocalizedMetadata } from "src/utils/metadata";
import Overview from "src/content/overview";
import { getTranslations } from "next-intl/server";
import { getBrandServer } from "src/utils/serverBrand";
import { IS_ORIGINAL_CLOUD } from "src/config";
const ldJson = [
  {
    "@context": "https://schema.org",
    "@type": "SoftwareApplication",
    name: "Atlas CMMS",
    description:
      "Atlas CMMS is a free, open-source CMMS to manage work orders, preventive maintenance, assets, and facilities.",
    applicationCategory: "BusinessApplication",
    operatingSystem: "Web",
    url: "https://atlas-cmms.com/",
    screenshot: "https://atlas-cmms.com/static/images/overview/work_orders_screenshot.png",
    // aggregateRating: {
    //   '@type': 'AggregateRating',
    //   ratingValue: '4.5',
    //   reviewCount: '5',
    //   bestRating: '5',
    //   worstRating: '1'
    // },
    publisher: {
      "@type": "Organization",
      name: "Atlas CMMS",
      url: "https://atlas-cmms.com/",
    },
    offers: {
      "@type": "Offer",
      price: "0",
      priceCurrency: "USD",
    },
  },
  {
    "@context": "https://schema.org",
    "@type": "MobileApplication",
    name: "Atlas CMMS for iOS",
    operatingSystem: "iOS",
    applicationCategory: "BusinessApplication",
    downloadUrl: "https://apps.apple.com/us/app/atlas-cmms/id6751547284",
    offers: {
      "@type": "Offer",
      price: "0",
      priceCurrency: "USD",
    },
  },
  {
    "@context": "https://schema.org",
    "@type": "MobileApplication",
    name: "Atlas CMMS for Android",
    operatingSystem: "Android",
    applicationCategory: "BusinessApplication",
    downloadUrl: "https://play.google.com/store/apps/details?id=com.atlas.cmms",
    offers: {
      "@type": "Offer",
      price: "0",
      priceCurrency: "USD",
    },
  },
];
export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale });
  const brand = await getBrandServer();

  const title = IS_ORIGINAL_CLOUD ? t("main.title") : brand.name;
  const description = t("overview_1.description");
  return {
    title,
    description,
    keywords: t("overview_1.keywords"),
    openGraph: { title, description },
    alternates: getLocalizedMetadata(locale, ""),
  };
}

export default function Home() {
  return (
    <>
      {ldJson.map((item, i) => (
        <script key={i} type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(item) }} />
      ))}
      <Overview />
    </>
  );
}
