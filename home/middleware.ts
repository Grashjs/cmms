import createMiddleware from "next-intl/middleware";
import { NextRequest, NextResponse } from "next/server";
import { mainAppUrl } from "src/config";
import { locales } from "src/i18n/request";

const intlMiddleware = createMiddleware({
  locales,
  defaultLocale: "en",
  localePrefix: "as-needed",
});

export default function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  if (pathname === "/app" || pathname.startsWith("/app/")) {
    const normalizedMainAppUrl = mainAppUrl.endsWith("/") ? mainAppUrl : mainAppUrl + "/";
    const targetUrl = normalizedMainAppUrl + pathname.slice(1);
    return NextResponse.redirect(targetUrl);
  }

  return intlMiddleware(request);
}
export const config = {
  matcher: [
    // Root only (needs locale detection)
    "/",

    // /app redirects
    "/app/:path*",

    // /mb-app route
    "/mb-app/:path*",

    // Non-localized paths that need locale prefix added
    // Excludes: api, _next, _vercel, files with extensions, and already-localized paths
    "/((?!en|es|fr|de|tr|pt-br|pl|ar|it|sv|ru|hu|nl|zh-cn|ba|api|_next|_vercel|.*\\..*).*)",
  ],
};
