import { Metadata } from "next";
import FreeCMMS from "src/content/landing/FreeCMMS";
import { getTranslations } from "next-intl/server";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale });

  return {
    title: t("free_cmms.title"),
    description: t("free_cmms.description"),
    keywords: t("free_cmms.keywords"),
  };
}
export default function Page() {
  return <FreeCMMS />;
}
