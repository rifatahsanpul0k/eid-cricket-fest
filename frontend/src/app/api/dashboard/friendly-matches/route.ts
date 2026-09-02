import { revalidatePath } from "next/cache";
import { NextRequest, NextResponse } from "next/server";

import { formValue, problemMessage } from "@/lib/auth/forms";
import { assertSameOriginRequest } from "@/lib/auth/origin";
import { createFriendlyMatch } from "@/lib/dashboard/match-admin-api";

export async function POST(request: NextRequest) {
  try {
    await assertSameOriginRequest(request);
  } catch {
    return NextResponse.json({ error: "Invalid request origin" }, { status: 403 });
  }

  const formData = await request.formData();
  const teamAPlayerIds = playerIds(formData, "teamAPlayerIds");
  const teamBPlayerIds = playerIds(formData, "teamBPlayerIds");

  const result = await createFriendlyMatch({
    oversPerInnings: numberValue(formData, "oversPerInnings"),
    scheduledAt: instantValue(formData, "scheduledAt"),
    teamAName: formValue(formData, "teamAName"),
    teamAPlayerIds,
    teamBName: formValue(formData, "teamBName"),
    teamBPlayerIds,
    venueId: numberValue(formData, "venueId"),
  });

  revalidatePath("/dashboard/matches");

  if (!result.ok) {
    return redirectWithMessage(
      request,
      problemMessage(result.error) ?? "Friendly match could not be created."
    );
  }

  const matchId = result.data.id;

  return NextResponse.redirect(
    new URL(matchId ? `/dashboard/matches/${matchId}` : "/dashboard/matches", request.url),
    { status: 303 }
  );
}

function playerIds(formData: FormData, name: string) {
  return formData
    .getAll(name)
    .map((value) => Number(value))
    .filter((value) => Number.isFinite(value));
}

function numberValue(formData: FormData, name: string) {
  return Number(formValue(formData, name));
}

function instantValue(formData: FormData, name: string) {
  const value = formValue(formData, name);

  if (!value) {
    return undefined;
  }

  const date = new Date(value);

  return Number.isNaN(date.getTime()) ? undefined : date.toISOString();
}

function redirectWithMessage(request: NextRequest, message: string) {
  const url = new URL("/dashboard/matches/friendly/new", request.url);
  url.searchParams.set("error", message);

  return NextResponse.redirect(url, { status: 303 });
}
