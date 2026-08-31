import { NextRequest, NextResponse } from "next/server";

import { backendRequest, jsonInit, type CreatePlayerBody } from "@/lib/auth/backend";
import { formValue, problemMessage } from "@/lib/auth/forms";
import { assertSameOriginRequest } from "@/lib/auth/origin";

export async function POST(request: NextRequest) {
  try {
    await assertSameOriginRequest(request);
  } catch {
    return NextResponse.json({ error: "Invalid request origin" }, { status: 403 });
  }

  const formData = await request.formData();
  const categoryId = Number(formValue(formData, "primaryCategoryId"));
  const body: CreatePlayerBody = {
    fullName: formValue(formData, "fullName"),
    dateOfBirth: formValue(formData, "dateOfBirth") || undefined,
    primaryCategoryId: Number.isFinite(categoryId) ? categoryId : undefined,
    battingStyle: formValue(formData, "battingStyle") || undefined,
    bowlingStyle: formValue(formData, "bowlingStyle") || undefined,
  };
  const result = await backendRequest(
    "/api/v1/players/me",
    jsonInit("POST", body),
    { authenticated: true }
  );

  if (!result.ok) {
    const url = new URL("/account/profile", request.url);
    url.searchParams.set(
      "error",
      problemMessage(result.error) ?? "Profile creation failed"
    );

    return NextResponse.redirect(url, { status: 303 });
  }

  return NextResponse.redirect(new URL("/account/profile", request.url), {
    status: 303,
  });
}
