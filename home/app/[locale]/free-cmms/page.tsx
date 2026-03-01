import { Metadata } from "next";
import { getLocalizedMetadata } from "src/utils/metadata";
import FreeCMMS from "src/content/landing/FreeCMMS";
import { getTranslations } from "next-intl/server";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale });
  const title = t("free_cmms.title");
  const description = t("free_cmms.description");
  return {
    title,
    description,
    openGraph: { title, description },
    keywords: t("free_cmms.keywords"),
    alternates: getLocalizedMetadata(locale, "/free-cmms"),
  };
}
export default function Page() {
  return <FreeCMMS />;
}
