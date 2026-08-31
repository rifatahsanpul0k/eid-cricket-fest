import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

import { clearAuthCookies, logout } from "@/lib/auth/backend";
import { REFRESH_TOKEN_COOKIE } from "@/lib/auth/cookies";
import { assertSameOriginRequest } from "@/lib/auth/origin";

export async function POST(request: NextRequest) {
  try {
    await assertSameOriginRequest(request);
  } catch {
    return NextResponse.json({ error: "Invalid request origin" }, { status: 403 });
  }

  const store = await cookies();
  const refreshToken = store.get(REFRESH_TOKEN_COOKIE)?.value;

  if (refreshToken) {
    await logout(refreshToken);
  }

  clearAuthCookies(store);

  return NextResponse.redirect(new URL("/", request.url), { status: 303 });
}
