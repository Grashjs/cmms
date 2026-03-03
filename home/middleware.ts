import createMiddleware from "next-intl/middleware";

export const locales = ["en", "es", "fr", "de", "tr", "pt-br", "pl", "ar", "it", "sv", "ru", "hu", "nl", "zh-cn"];

export default createMiddleware({
  locales,
  defaultLocale: "en",
  localePrefix: "as-needed",
});

export const config = {
  // Match only internationalized pathnames
  matcher: [
    "/",
    "/(en|es|fr|de|tr|pt-br|pl|ar|it|sv|ru|hu|nl|zh-cn)/:path*",
    // Match all pathnames except for API routes, static files, Next.js internals
    "/((?!api|_next|_vercel|.*\\..*).*)",
  ],
};
