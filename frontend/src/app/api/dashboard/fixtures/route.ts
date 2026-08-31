import { revalidatePath } from "next/cache";
import { NextRequest, NextResponse } from "next/server";

import { formValue, problemMessage } from "@/lib/auth/forms";
import { assertSameOriginRequest } from "@/lib/auth/origin";
import {
  createVenue,
  generateRoundRobin,
  generateSemiFinals,
} from "@/lib/dashboard/match-admin-api";

export async function POST(request: NextRequest) {
  try {
    await assertSameOriginRequest(request);
  } catch {
    return NextResponse.json({ error: "Invalid request origin" }, { status: 403 });
  }

  const formData = await request.formData();
  const action = formValue(formData, "action");
  const returnTo = safeReturn(formValue(formData, "returnTo"));
  const result = await handleFixtureAction(action, formData);

  revalidatePath("/dashboard/fixtures");
  revalidatePath("/dashboard/matches");

  if (!result.ok) {
    return redirectWithMessage(
      request,
      returnTo,
      problemMessage(result.error) ?? "Fixture operation failed."
    );
  }

  return NextResponse.redirect(new URL(returnTo, request.url), { status: 303 });
}

async function handleFixtureAction(action: string, formData: FormData) {
  if (action === "venue") {
    return createVenue({
      address: formValue(formData, "address") || undefined,
      name: formValue(formData, "name"),
    });
  }

  if (action === "knockout") {
    return generateSemiFinals(numberValue(formData, "editionId"));
  }

  return generateRoundRobin(
    numberValue(formData, "editionId"),
    optionalNumber(formData, "venueId")
  );
}

function numberValue(formData: FormData, name: string) {
  return Number(formValue(formData, name));
}

function optionalNumber(formData: FormData, name: string) {
  const value = Number(formValue(formData, name));

  return Number.isFinite(value) && value > 0 ? value : undefined;
}

function redirectWithMessage(
  request: NextRequest,
  returnTo: string,
  message: string
) {
  const url = new URL(returnTo, request.url);
  url.searchParams.set("error", message);

  return NextResponse.redirect(url, { status: 303 });
}

function safeReturn(value?: string) {
  return value?.startsWith("/dashboard/fixtures")
    ? value
    : "/dashboard/fixtures";
}
