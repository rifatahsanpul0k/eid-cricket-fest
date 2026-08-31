import { revalidatePath } from "next/cache";
import { NextRequest, NextResponse } from "next/server";

import { formValue, problemMessage } from "@/lib/auth/forms";
import { assertSameOriginRequest } from "@/lib/auth/origin";
import {
  createDraft,
  generateDraftLottery,
  makeDraftPick,
  startDraft,
} from "@/lib/dashboard/team-draft-api";

export async function POST(request: NextRequest) {
  try {
    await assertSameOriginRequest(request);
  } catch {
    return NextResponse.json({ error: "Invalid request origin" }, { status: 403 });
  }

  const formData = await request.formData();
  const action = formValue(formData, "action");
  const returnTo = safeReturn(formValue(formData, "returnTo"));
  const result = await handleDraftAction(action, formData);

  revalidatePath("/dashboard/draft");
  revalidatePath("/dashboard/teams");

  if (!result.ok) {
    return redirectWithMessage(
      request,
      returnTo,
      problemMessage(result.error) ?? "Draft operation failed."
    );
  }

  return NextResponse.redirect(new URL(returnTo, request.url), { status: 303 });
}

async function handleDraftAction(action: string, formData: FormData) {
  if (action === "create") {
    const pickMode = formValue(formData, "pickMode") === "LINEAR" ? "LINEAR" : "SNAKE";

    return createDraft(numberValue(formData, "editionId"), pickMode);
  }

  if (action === "lottery") {
    return generateDraftLottery(numberValue(formData, "draftId"));
  }

  if (action === "start") {
    return startDraft(numberValue(formData, "draftId"));
  }

  return makeDraftPick(
    numberValue(formData, "draftId"),
    numberValue(formData, "registrationId")
  );
}

function numberValue(formData: FormData, name: string) {
  return Number(formValue(formData, name));
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
  return value?.startsWith("/dashboard/draft") ? value : "/dashboard/draft";
}
