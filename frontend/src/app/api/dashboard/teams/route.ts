import { revalidatePath } from "next/cache";
import { NextRequest, NextResponse } from "next/server";

import { formValue, problemMessage } from "@/lib/auth/forms";
import { assertSameOriginRequest } from "@/lib/auth/origin";
import {
  addTeamToEdition,
  assignCaptain,
  createPermanentTeam,
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
  const result = await handleTeamAction(action, formData);

  revalidatePath("/dashboard/teams");
  revalidatePath("/dashboard/draft");

  if (!result.ok) {
    return redirectWithMessage(
      request,
      returnTo,
      problemMessage(result.error) ?? "Team operation failed."
    );
  }

  return NextResponse.redirect(new URL(returnTo, request.url), { status: 303 });
}

async function handleTeamAction(action: string, formData: FormData) {
  if (action === "create") {
    return createPermanentTeam({
      logoUrl: formValue(formData, "logoUrl") || undefined,
      name: formValue(formData, "name"),
      shortName: formValue(formData, "shortName") || undefined,
    });
  }

  if (action === "add-to-edition") {
    return addTeamToEdition(
      numberValue(formData, "editionId"),
      numberValue(formData, "teamId")
    );
  }

  return assignCaptain(
    numberValue(formData, "tournamentTeamId"),
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
  return value?.startsWith("/dashboard/teams") ? value : "/dashboard/teams";
}
