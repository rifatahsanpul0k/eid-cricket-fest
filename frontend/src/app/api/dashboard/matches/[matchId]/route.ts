import { revalidatePath } from "next/cache";
import { NextRequest, NextResponse } from "next/server";

import { formValue, problemMessage } from "@/lib/auth/forms";
import { assertSameOriginRequest } from "@/lib/auth/origin";
import {
  assignMatchScorer,
  markNoResult,
  orderRematch,
  recordToss,
  reasonMatchOperation,
  rescheduleMatchOperation,
  resolveKnockoutWinner,
  scheduleMatch,
  submitPlayingXi,
} from "@/lib/dashboard/match-admin-api";

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ matchId: string }> }
) {
  try {
    await assertSameOriginRequest(request);
  } catch {
    return NextResponse.json({ error: "Invalid request origin" }, { status: 403 });
  }

  const { matchId } = await params;
  const id = Number(matchId);
  const formData = await request.formData();
  const returnTo = safeReturn(id, formValue(formData, "returnTo"));
  const result = await handleMatchAction(id, formData);

  revalidatePath("/dashboard/matches");
  revalidatePath(`/dashboard/matches/${id}`);

  if (!result.ok) {
    return redirectWithMessage(
      request,
      returnTo,
        problemMessage(result.error) ?? "Match operation failed."
    );
  }

  const redirectTo =
    result.data?.id && result.data.id !== id
      ? `/dashboard/matches/${result.data.id}`
      : returnTo;

  return NextResponse.redirect(new URL(redirectTo, request.url), { status: 303 });
}

async function handleMatchAction(matchId: number, formData: FormData) {
  const action = formValue(formData, "action");

  if (action === "schedule") {
    const scheduledAt = instantValue(formData, "scheduledAt");

    if (!scheduledAt) {
      return {
        ok: false as const,
        error: {
          title: "Invalid schedule",
          detail: "Choose a valid date and time before scheduling the match.",
          status: 400,
        },
        status: 400,
      };
    }

    return scheduleMatch(matchId, {
      scheduledAt,
      venueId: numberValue(formData, "venueId"),
    });
  }

  if (action === "operation-reschedule") {
    const scheduledAt = instantValue(formData, "scheduledAt");

    if (!scheduledAt) {
      return {
        ok: false as const,
        error: {
          title: "Invalid reschedule",
          detail: "Choose a valid date and time before rescheduling the match.",
          status: 400,
        },
        status: 400,
      };
    }

    return rescheduleMatchOperation(matchId, {
      scheduledAt,
      venueId: numberValue(formData, "venueId"),
      oversPerInnings: optionalNumber(formData, "oversPerInnings"),
      reason: formValue(formData, "reason"),
    });
  }

  if (action === "operation-rematch") {
    const scheduledAt = instantValue(formData, "scheduledAt");

    return orderRematch(matchId, {
      reason: formValue(formData, "reason"),
      scheduledAt,
      venueId: optionalNumber(formData, "venueId"),
      oversPerInnings: optionalNumber(formData, "oversPerInnings"),
    });
  }

  const operation = operationSlug(action);

  if (operation) {
    return reasonMatchOperation(
      matchId,
      operation,
      formValue(formData, "reason")
    );
  }

  if (action === "scorer") {
    return assignMatchScorer(matchId, {
      primary: formValue(formData, "primary") === "on",
      scorerUserId: numberValue(formData, "scorerUserId"),
    });
  }

  if (action === "playing-xi") {
    const registrationIds = formData
      .getAll("registrationIds")
      .map((value) => Number(value))
      .filter((value) => Number.isFinite(value));

    return submitPlayingXi(
      matchId,
      numberValue(formData, "tournamentTeamId"),
      {
        registrationIds,
        wicketkeeperRegistrationId:
          optionalNumber(formData, "wicketkeeperRegistrationId"),
      }
    );
  }

  if (action === "toss") {
    return recordToss(matchId, {
      decision: formValue(formData, "decision") === "BOWL" ? "BOWL" : "BAT",
      winnerMatchSideId: optionalNumber(formData, "winnerMatchSideId"),
      winnerTournamentTeamId: optionalNumber(formData, "winnerTournamentTeamId"),
    });
  }

  if (action === "no-result") {
    return markNoResult(matchId, {
      reason: formValue(formData, "reason"),
    });
  }

  return resolveKnockoutWinner(matchId, {
    reason: formValue(formData, "reason"),
    resolutionType:
      formValue(formData, "resolutionType") === "FORFEIT"
        ? "FORFEIT"
        : "TIEBREAKER",
    winnerTournamentTeamId: numberValue(formData, "winnerTournamentTeamId"),
  });
}

function operationSlug(action?: string) {
  if (action === "operation-postpone") return "postpone";
  if (action === "operation-suspend") return "suspend";
  if (action === "operation-resume") return "resume";
  if (action === "operation-abandon") return "abandon";
  if (action === "operation-cancel") return "cancel";
  if (action === "operation-reset-toss") return "reset-toss";
  if (action === "operation-review") return "review";
  if (action === "operation-restore-result") return "restore-result";
  if (action === "operation-void-result") return "void-result";

  return undefined;
}

function numberValue(formData: FormData, name: string) {
  return Number(formValue(formData, name));
}

function optionalNumber(formData: FormData, name: string) {
  const value = Number(formValue(formData, name));

  return Number.isFinite(value) && value > 0 ? value : undefined;
}

function instantValue(formData: FormData, name: string) {
  const date = new Date(formValue(formData, name));

  return Number.isNaN(date.getTime()) ? undefined : date.toISOString();
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

function safeReturn(matchId: number, value?: string) {
  const fallback = `/dashboard/matches/${matchId}`;

  return value?.startsWith(fallback) ? value : fallback;
}
