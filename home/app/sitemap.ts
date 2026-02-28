import { MetadataRoute } from "next";
import { getBrandServer } from "src/utils/serverBrand";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  // const brand = await getBrandServer();
  const baseUrl = "https://atlas-cmms.com";

  const staticPaths = [
    "/",
    "/free-cmms",
    "/pricing",
    "/privacy",
    "/terms-of-service",
    "/features/work-orders",
    "/features/assets",
    "/features/preventive-maintenance",
    "/features/inventory",
    "/features/analytics",
    "/industries/open-source-manufacturing-maintenance-software",
    "/industries/open-source-facility-management-software",
    "/industries/open-source-food-and-beverage-maintenance-software",
    "/industries/open-source-healthcare-maintenance-software",
    "/industries/open-source-energy-utilities-maintenance-software",
    "/industries/open-source-education-maintenance-software",
    "/industries/open-source-hospitality-maintenance-software",
    "/industries/open-source-construction-maintenance-software",
  ];

  const locales = ["en", "es", "fr", "de", "tr", "pt-br", "pl", "ar", "it", "sv", "ru", "hu", "nl", "zh-cn"];
  const defaultLocale = "en";

  const getUrl = (path: string, locale: string) => {
    const prefix = locale === defaultLocale ? "" : `/${locale}`;
    const urlPath = path === "/" ? prefix : `${prefix}${path}`;
    return `${baseUrl}${urlPath || "/"}`;
  };

  return staticPaths.map((path) => {
    const languages: Record<string, string> = {};

    locales.forEach((locale) => {
      languages[locale] = getUrl(path, locale);
    });

    return {
      url: getUrl(path, defaultLocale),
      alternates: {
        languages,
      },
    };
  });
}
