import { cookies } from "next/headers";
import { NextRequest, NextResponse } from "next/server";

import { login, setAuthCookies } from "@/lib/auth/backend";
import { formValue, problemMessage } from "@/lib/auth/forms";
import { assertSameOriginRequest } from "@/lib/auth/origin";
import { safeReturnTo } from "@/lib/auth/return-url";

export async function POST(request: NextRequest) {
  try {
    await assertSameOriginRequest(request);
  } catch {
    return NextResponse.json({ error: "Invalid request origin" }, { status: 403 });
  }

  const formData = await request.formData();
  const returnTo = safeReturnTo(formValue(formData, "returnTo"));
  const result = await login({
    identifier: formValue(formData, "identifier"),
    password: formValue(formData, "password"),
  });

  if (!result.ok) {
    const url = new URL("/login", request.url);
    url.searchParams.set("error", problemMessage(result.error) ?? "Login failed");
    url.searchParams.set("returnTo", returnTo);

    return NextResponse.redirect(url, { status: 303 });
  }

  setAuthCookies(await cookies(), result.data);

  return NextResponse.redirect(new URL(returnTo, request.url), { status: 303 });
}
