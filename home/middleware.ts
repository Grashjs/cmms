import createMiddleware from 'next-intl/middleware';

export const locales = ['en', 'es', 'fr', 'de', 'tr', 'pt_br', 'pl', 'ar', 'it', 'sv', 'ru', 'hu', 'nl', 'zh_cn'];

export default createMiddleware({
  locales,
  defaultLocale: 'en',
  localePrefix: 'always'
});

export const config = {
  // Match only internationalized pathnames
  matcher: ['/', '/(en|es|fr|de|tr|pt_br|pl|ar|it|sv|ru|hu|nl|zh_cn)/:path*']
};
