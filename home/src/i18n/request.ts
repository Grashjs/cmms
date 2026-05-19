import { getRequestConfig } from "next-intl/server";

export const locales = [
  "en",
  "es",
  "fr",
  "de",
  "tr",
  "pt-br",
  "pl",
  "ar",
  "it",
  "sv",
  "ru",
  "hu",
  "nl",
  "zh-cn",
  "ba",
  "ja",
];

function deepmerge(target: object, source: object): object {
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

// Cache is populated once per locale, per process
const messagesCache = new Map<string, object>();

async function getMessages(locale: string): Promise<object> {
  if (messagesCache.has(locale)) {
    return messagesCache.get(locale)!;
  }

  const enRaw = (await import(`./translations/en`)).default;

  if (locale === "en") {
    messagesCache.set(locale, enRaw);
    return enRaw;
  }

  const localeRaw = (await import(`./translations/${locale}`)).default;
  const merged = deepmerge(enRaw, localeRaw);

  messagesCache.set(locale, merged);
  return merged;
}

export default getRequestConfig(async ({ requestLocale }) => {
  let locale = await requestLocale;

  if (!locale || !locales.includes(locale)) {
    locale = "en";
  }

  return {
    locale,
    messages: await getMessages(locale),
  };
});
