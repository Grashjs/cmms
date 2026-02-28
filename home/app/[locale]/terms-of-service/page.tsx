import { Metadata } from "next";
import TermsOfService from "src/content/terms-of-service";
import { getTranslations } from "next-intl/server";
import Footer from "@/src/components/Footer";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale });

  return {
    title: `${t("terms_of_service")}`,
    description:
      "Terms of Service for Atlas CMMS. Review the guidelines, responsibilities, and legal agreements for using our open-source CMMS software.",
    alternates: {
      canonical: "terms-of-service",
    },
  };
}

export default function Page() {
  return (
    <>
      <TermsOfService />
      <Footer />
    </>
  );
}
