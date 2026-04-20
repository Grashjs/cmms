// src/utils/metadata.ts
import { Metadata } from "next";
import { locales } from "src/i18n/request";

const baseUrl = "https://atlas-cmms.com";

const getLocalePrefix = (locale: string) => (locale === "en" ? "" : `/${locale}`);
export function getLocalizedMetadata(
  locale: string,
  path: string, // e.g., "/pricing" or "" for home
): Metadata["alternates"] {
  // 1. Construct the canonical (usually points to the current locale version)
  const canonical = `${baseUrl}${getLocalePrefix(locale)}${path}`;

  // 2. Construct the hreflang object for all supported languages
  const languages = Object.fromEntries(locales.map((l) => [l, `${baseUrl}${getLocalePrefix(l)}${path}`]));

  return {
    canonical,
    languages: {
      ...languages,
      "x-default": `${baseUrl}${path}`, // Or point to /en specifically
    },
  };
}
