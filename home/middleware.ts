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
  // Match only internationalized pathnames
  matcher: [
    "/",
    "/(en|es|fr|de|tr|pt-br|pl|ar|it|sv|ru|hu|nl|zh-cn|ba)/:path*",
    // Match all pathnames except for API routes, static files, Next.js internals
    "/((?!api|_next|_vercel|.*\\..*).*)",
    // Include /mb-app route
    "/mb-app/:path*",
  ],
};
