import { Metadata } from "next";
import { getLocalizedMetadata } from "src/utils/metadata";
import PrivacyPolicy from "src/content/privacyPolicy";
import { getTranslations } from "next-intl/server";
import Footer from "@/src/components/Footer";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale });

  return {
    title: t("privacy_policy"),
    description:
      "Read the Privacy Policy for Atlas CMMS. Learn how we protect your data and ensure security in our open-source maintenance management platform.",
    alternates: getLocalizedMetadata(locale, "/privacy"),
  };
}

export default function Page() {
  return (
    <>
      <PrivacyPolicy />
      <Footer />
    </>
  );
}
