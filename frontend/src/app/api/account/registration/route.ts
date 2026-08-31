import { NextRequest, NextResponse } from "next/server";

import { backendRequest, jsonInit } from "@/lib/auth/backend";
import { formValue, problemMessage } from "@/lib/auth/forms";
import { assertSameOriginRequest } from "@/lib/auth/origin";

export async function POST(request: NextRequest) {
  try {
    await assertSameOriginRequest(request);
  } catch {
    return NextResponse.json({ error: "Invalid request origin" }, { status: 403 });
  }

  const formData = await request.formData();
  const editionId = Number(formValue(formData, "editionId"));
  const categoryId = Number(formValue(formData, "categoryId"));

  if (!Number.isFinite(editionId) || !Number.isFinite(categoryId)) {
    const url = new URL("/account/registration", request.url);
    url.searchParams.set("error", "Registration information is incomplete.");

    return NextResponse.redirect(url, { status: 303 });
  }

  const result = await backendRequest(
    `/api/v1/tournament-editions/${editionId}/registrations/me`,
    jsonInit("POST", { categoryId }),
    { authenticated: true }
  );

  if (!result.ok) {
    const url = new URL("/account/registration", request.url);
    url.searchParams.set(
      "error",
      problemMessage(result.error) ?? "Registration failed"
    );

    return NextResponse.redirect(url, { status: 303 });
  }

  return NextResponse.redirect(new URL("/account/registration", request.url), {
    status: 303,
  });
}
