import { NextRequest, NextResponse } from "next/server";

const ACCESS_TOKEN_COOKIE = "ecf_access_token";

export function proxy(request: NextRequest) {
  if (!isProtectedPath(request.nextUrl.pathname)) {
    return NextResponse.next();
  }

  if (request.cookies.has(ACCESS_TOKEN_COOKIE)) {
    return NextResponse.next();
  }

  const loginUrl = new URL("/login", request.url);
  loginUrl.searchParams.set(
    "returnTo",
    safeReturnTo(`${request.nextUrl.pathname}${request.nextUrl.search}`)
  );

  return NextResponse.redirect(loginUrl);
}

export const config = {
  matcher: ["/account/:path*", "/dashboard/:path*"],
};

function safeReturnTo(value?: string | null) {
  if (!value || !value.startsWith("/") || value.startsWith("//")) {
    return "/account";
  }

  if (value.startsWith("/api/")) {
    return "/account";
  }

  return value;
}

function isProtectedPath(pathname: string) {
  return pathname.startsWith("/account") || pathname.startsWith("/dashboard");
}
