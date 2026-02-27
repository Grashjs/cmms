import { Metadata } from "next";
import Overview from "src/content/overview";
import { getTranslations } from "next-intl/server";
import { getBrandServer } from "src/utils/serverBrand";
import { IS_ORIGINAL_CLOUD } from "src/config";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale });
  const brand = await getBrandServer();

  return {
    title: IS_ORIGINAL_CLOUD ? t("main.title") : brand.name,
    description: t("overview_1.description"),
    keywords: t("overview_1.keywords"),
  };
}

export default function Home() {
  return <Overview />;
}
