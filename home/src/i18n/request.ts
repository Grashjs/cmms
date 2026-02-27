import { getRequestConfig } from 'next-intl/server';

export const locales = ['en', 'es', 'fr', 'de', 'tr', 'pt_br', 'pl', 'ar', 'it', 'sv', 'ru', 'hu', 'nl', 'zh_cn'];

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

export default getRequestConfig(async ({ requestLocale }) => {
  let locale = await requestLocale;
  
  if (!locale || !locales.includes(locale as any)) {
    locale = 'en';
  }

  const rawMessages = (await import(`./translations/${locale}.ts`)).default;
  const messages = unflatten(rawMessages);

  return {
    locale,
    messages
  };
});
