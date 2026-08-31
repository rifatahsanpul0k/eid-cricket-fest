import { revalidatePath } from "next/cache";
import { NextRequest, NextResponse } from "next/server";

import {
  approveRegistration,
  rejectRegistration,
} from "@/lib/dashboard/api";
import { formValue, problemMessage } from "@/lib/auth/forms";
import { assertSameOriginRequest } from "@/lib/auth/origin";

export async function POST(request: NextRequest) {
  try {
    await assertSameOriginRequest(request);
  } catch {
    return NextResponse.json({ error: "Invalid request origin" }, { status: 403 });
  }

  const formData = await request.formData();
  const action = formValue(formData, "action");
  const registrationId = Number(formValue(formData, "registrationId"));
  const reason = formValue(formData, "reason");
  const returnTo = safeDashboardReturn(formValue(formData, "returnTo"));

  if (!Number.isFinite(registrationId)) {
    return redirectWithMessage(request, returnTo, "Registration is missing.");
  }

  const result =
    action === "approve"
      ? await approveRegistration(registrationId)
      : await rejectRegistration(registrationId, reason);

  revalidatePath("/dashboard/registrations");

  if (!result.ok) {
    return redirectWithMessage(
      request,
      returnTo,
      problemMessage(result.error) ?? "Registration review failed."
    );
  }

  return NextResponse.redirect(new URL(returnTo, request.url), { status: 303 });
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

function safeDashboardReturn(value?: string) {
  if (!value || !value.startsWith("/dashboard/registrations")) {
    return "/dashboard/registrations";
  }

  return value;
}
