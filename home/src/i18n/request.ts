import { getRequestConfig } from 'next-intl/server';

export const locales = ['en', 'es', 'fr', 'de', 'tr', 'pt-br', 'pl', 'ar', 'it', 'sv', 'ru', 'hu', 'nl', 'zh-cn'];

// Helper to convert flat keys with dots into nested objects
function unflatten(data: Record<string, string>) {
  const result: any = {};
  for (const p in data) {
    const keys = p.split('.');
    keys.reduce((acc, key, index) => {
      if (index === keys.length - 1) {
        acc[key] = data[p];
      } else {
        acc[key] = acc[key] || {};
      }
      return acc[key];
    }, result);
  }
  return result;
}

function deepmerge(target: any, source: any): any {
  const result = { ...target };
  for (const key in source) {
    if (source[key] && typeof source[key] === 'object' && !Array.isArray(source[key])) {
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
    locale = 'en';
  }

  const enRaw = (await import(`./translations/en`)).default;
  const enMessages = unflatten(enRaw);

  if (locale === 'en') {
    return { locale, messages: enMessages };
  }

  const localeRaw = (await import(`./translations/${locale}.ts`)).default;
  const localeMessages = unflatten(localeRaw);

  return {
    locale,
    messages: deepmerge(enMessages, localeMessages),
  };
});