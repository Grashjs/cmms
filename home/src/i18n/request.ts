import { getRequestConfig } from "next-intl/server";

export const locales = ["en", "es", "fr", "de", "tr", "pt-br", "pl", "ar", "it", "sv", "ru", "hu", "nl", "zh-cn"];

function deepmerge(target: any, source: any): any {
  const result = { ...target };
  for (const key in source) {
    if (source[key] && typeof source[key] === "object" && !Array.isArray(source[key])) {
      result[key] = deepmerge(target[key] ?? {}, source[key]);
    } else {
      result[key] = source[key];
    }
  }
  return result;
}

export default getRequestConfig(async ({ requestLocale }) => {
  let locale = await requestLocale;

  if (!locale || !locales.includes(locale as any)) {
    locale = "en";
  }

  const enRaw = (await import(`./translations/en`)).default;

  if (locale === "en") {
    return { locale, messages: enRaw };
  }

  const localeRaw = (await import(`./translations/${locale}.ts`)).default;

  return {
    locale,
    messages: deepmerge(enRaw, localeRaw),
  };
});
