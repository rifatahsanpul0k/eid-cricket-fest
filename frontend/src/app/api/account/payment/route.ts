import { NextRequest, NextResponse } from "next/server";

import { backendRequest, jsonInit, type SubmitPaymentBody } from "@/lib/auth/backend";
import { formValue, problemMessage } from "@/lib/auth/forms";
import { assertSameOriginRequest } from "@/lib/auth/origin";

export async function POST(request: NextRequest) {
  try {
    await assertSameOriginRequest(request);
  } catch {
    return NextResponse.json({ error: "Invalid request origin" }, { status: 403 });
  }

  const formData = await request.formData();
  const registrationId = Number(formValue(formData, "registrationId"));
  const amount = Number(formValue(formData, "amount"));
  const paymentMethod = formValue(formData, "paymentMethod") as SubmitPaymentBody["paymentMethod"];

  if (!Number.isFinite(registrationId) || !Number.isFinite(amount)) {
    const url = new URL("/account/registration", request.url);
    url.searchParams.set("error", "Payment information is incomplete.");

    return NextResponse.redirect(url, { status: 303 });
  }

  const result = await backendRequest(
    `/api/v1/registrations/${registrationId}/payments/me`,
    jsonInit("POST", {
      amount,
      paymentMethod,
      transactionReference:
        formValue(formData, "transactionReference") || undefined,
    } satisfies SubmitPaymentBody),
    { authenticated: true }
  );

  if (!result.ok) {
    const url = new URL("/account/registration", request.url);
    url.searchParams.set(
      "error",
      problemMessage(result.error) ?? "Payment submission failed"
    );

    return NextResponse.redirect(url, { status: 303 });
  }

  return NextResponse.redirect(new URL("/account/registration", request.url), {
    status: 303,
  });
}
