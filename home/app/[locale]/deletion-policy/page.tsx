import { Metadata } from "next";
import DeletionPolicy from "src/content/deletion-policy";
import { getTranslations } from "next-intl/server";
import Footer from "@/src/components/Footer";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }): Promise<Metadata> {
  // const { locale } = await params;
  // const t = await getTranslations({ locale });

  return {
    title: "Account deletion",
    description:
      "Information on how to delete your account in Atlas CMMS. We provide clear steps for data removal and account closure.",
  };
}

export default function Page() {
  return (
    <>
      <DeletionPolicy />
      <Footer />
    </>
  );
}
