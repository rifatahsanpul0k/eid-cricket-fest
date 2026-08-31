import { revalidatePath } from "next/cache";
import { NextRequest, NextResponse } from "next/server";

import { rejectPayment, verifyPayment } from "@/lib/dashboard/api";
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
  const paymentId = Number(formValue(formData, "paymentId"));
  const reason = formValue(formData, "reason");
  const returnTo = safeDashboardReturn(formValue(formData, "returnTo"));

  if (!Number.isFinite(paymentId)) {
    return redirectWithMessage(request, returnTo, "Payment is missing.");
  }

  const result =
    action === "verify"
      ? await verifyPayment(paymentId)
      : await rejectPayment(paymentId, reason);

  revalidatePath("/dashboard/payments");

  if (!result.ok) {
    return redirectWithMessage(
      request,
      returnTo,
      problemMessage(result.error) ?? "Payment review failed."
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
  if (!value || !value.startsWith("/dashboard/payments")) {
    return "/dashboard/payments";
  }

  return value;
}
