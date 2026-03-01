import { Metadata } from "next";
import Pricing from "src/content/pricing";
import { getTranslations } from "next-intl/server";
import Footer from "@/src/components/Footer";
import { getLocalizedMetadata } from "src/utils/metadata";

const ldJson = {
  "@context": "https://schema.org",
  "@type": "Product",
  name: "Atlas CMMS",
  description:
    "Flexible pricing plans for Atlas CMMS. Choose between Cloud and Self-Hosted versions of our open-source CMMS to optimize your maintenance operations.",
  url: "https://atlas-cmms.com/pricing",
  image: "https://atlas-cmms.com/static/images/logo/logo.png",
  offers: [
    {
      "@type": "Offer",
      name: "Basic",
      price: "0",
      priceCurrency: "USD",
      description: "For small teams getting started with maintenance management.",
    },
    {
      "@type": "Offer",
      name: "Starter",
      price: "10",
      priceCurrency: "USD",
      description: "For growing teams that need more advanced features.",
    },
    {
      "@type": "Offer",
      name: "Professional",
      price: "15",
      priceCurrency: "USD",
      description: "For established teams that require more customization and support.",
    },
    {
      "@type": "Offer",
      name: "Business",
      price: "40",
      priceCurrency: "USD",
      description: "For large organizations with complex needs and integrations.",
    },
  ],
  publisher: {
    "@type": "Organization",
    name: "Atlas CMMS",
  },
};
export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale });

  return {
    title: t("pricing_1.title"),
    description: t("pricing_1.description"),
    alternates: getLocalizedMetadata(locale, "/pricing"),
  };
}

export default function Home() {
  return (
    <>
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(ldJson) }} />
      <Pricing />
      <Footer />
    </>
  );
}
