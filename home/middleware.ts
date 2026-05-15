import { NextRequest, NextResponse } from "next/server";
import { mainAppUrl } from "src/config";

export default function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  if (pathname === "/app" || pathname.startsWith("/app/")) {
    const normalizedMainAppUrl = mainAppUrl.endsWith("/") ? mainAppUrl : mainAppUrl + "/";
    return NextResponse.redirect(normalizedMainAppUrl + pathname.slice(1));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/app/:path*", "/mb-app/:path*"],
};
