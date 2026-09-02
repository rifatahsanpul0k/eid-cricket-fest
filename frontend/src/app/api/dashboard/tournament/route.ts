import { revalidatePath } from "next/cache";
import { NextRequest, NextResponse } from "next/server";

import type { UpdateTournamentEditionStatusRequest } from "@/lib/api/schema-helpers";
import { formValue, problemMessage } from "@/lib/auth/forms";
import { assertSameOriginRequest } from "@/lib/auth/origin";
import {
  assignTournamentAward,
  createTournament,
  createTournamentEdition,
  transitionTournamentEditionStatus,
  updateTournamentEdition,
} from "@/lib/dashboard/tournament-admin-api";

export async function POST(request: NextRequest) {
  try {
    await assertSameOriginRequest(request);
  } catch {
    return NextResponse.json({ error: "Invalid request origin" }, { status: 403 });
  }

  const formData = await request.formData();
  const action = formValue(formData, "action");
  const returnTo = safeReturn(formValue(formData, "returnTo"));
  const result = await handleTournamentAction(action, formData);

  revalidatePath("/");
  revalidatePath("/dashboard");
  revalidatePath("/dashboard/tournament");
  revalidatePath("/register");
  revalidatePath("/awards");
  revalidatePath("/history");
  revalidatePath("/account/registration");

  if (!result.ok) {
    return redirectWithMessage(
      request,
      returnTo,
      problemMessage(result.error) ?? "Tournament operation failed."
    );
  }

  return NextResponse.redirect(new URL(returnTo, request.url), { status: 303 });
}

async function handleTournamentAction(action: string, formData: FormData) {
  if (action === "create-tournament") {
    return createTournament({
      description: optionalValue(formData, "description"),
      logoUrl: optionalValue(formData, "logoUrl"),
      name: formValue(formData, "name"),
    });
  }

  if (action === "create-edition") {
    return createTournamentEdition(
      numberValue(formData, "tournamentId"),
      editionPayload(formData)
    );
  }

  if (action === "update-edition") {
    return updateTournamentEdition(
      numberValue(formData, "tournamentId"),
      numberValue(formData, "editionId"),
      editionPayload(formData)
    );
  }

  if (action === "transition-status") {
    return transitionTournamentEditionStatus(
      numberValue(formData, "tournamentId"),
      numberValue(formData, "editionId"),
      {
        status:
          formValue(formData, "status") as UpdateTournamentEditionStatusRequest["status"],
      }
    );
  }

  if (action === "assign-award") {
    return assignTournamentAward(numberValue(formData, "editionId"), {
      awardType: formValue(formData, "awardType") as never,
      notes: optionalValue(formData, "notes"),
      registrationId: numberValue(formData, "registrationId"),
      title: optionalValue(formData, "title"),
    });
  }

  return {
    ok: false as const,
    status: 400,
    error: {
      title: "Unsupported tournament action",
      detail: "The requested tournament operation is not supported.",
      status: 400,
    },
  };
}

function editionPayload(formData: FormData) {
  return {
    endDate: optionalValue(formData, "endDate"),
    lossPoints: decimalValue(formData, "lossPoints"),
    name: formValue(formData, "name"),
    noResultPoints: decimalValue(formData, "noResultPoints"),
    oversPerInnings: numberValue(formData, "oversPerInnings"),
    playingXiSize: numberValue(formData, "playingXiSize"),
    registrationCurrency:
      optionalValue(formData, "registrationCurrency")?.toUpperCase(),
    registrationEndAt: optionalDateTimeValue(formData, "registrationEndAt"),
    registrationFee: decimalValue(formData, "registrationFee"),
    registrationStartAt: optionalDateTimeValue(formData, "registrationStartAt"),
    squadSize: numberValue(formData, "squadSize"),
    startDate: optionalValue(formData, "startDate"),
    tiePoints: decimalValue(formData, "tiePoints"),
    winPoints: decimalValue(formData, "winPoints"),
  };
}

function numberValue(formData: FormData, name: string) {
  return Number(formValue(formData, name));
}

function decimalValue(formData: FormData, name: string) {
  const value = formValue(formData, name);

  return value ? Number(value) : undefined;
}

function optionalValue(formData: FormData, name: string) {
  return formValue(formData, name) || undefined;
}

function optionalDateTimeValue(formData: FormData, name: string) {
  const value = optionalValue(formData, name);

  return value ? new Date(value).toISOString() : undefined;
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
  return value?.startsWith("/dashboard/tournament")
    ? value
    : "/dashboard/tournament";
}
