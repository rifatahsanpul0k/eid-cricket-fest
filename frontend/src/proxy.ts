import { NextRequest, NextResponse } from "next/server";

const ACCESS_TOKEN_COOKIE = "ecf_access_token";

export function proxy(request: NextRequest) {
  if (!request.nextUrl.pathname.startsWith("/account")) {
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
  matcher: ["/account/:path*"],
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
