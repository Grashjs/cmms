import { NextRequest, NextResponse } from "next/server";
import { mainAppUrl } from "src/config";

export default function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  if (pathname === "/app" || pathname.startsWith("/app/")) {
    const normalizedMainAppUrl = mainAppUrl.endsWith("/") ? mainAppUrl : mainAppUrl + "/";
    return NextResponse.redirect(normalizedMainAppUrl + pathname.slice(1));
  }

  const response = NextResponse.next();
  response.headers.set("Netlify-CDN-Cache-Control", "public, durable, s-maxage=3600, stale-while-revalidate=86400");
  return response;
}

export const config = {
  matcher: ["/app/:path*", "/mb-app/:path*", "/((?!_next/static|_next/image|favicon.ico|.*\\..*).*)"],
};
