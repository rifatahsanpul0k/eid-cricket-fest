import { NextResponse } from "next/server";

import { getLiveCentreMatches } from "@/lib/api/matches";

export async function GET() {
  const result = await getLiveCentreMatches();

  if (!result.ok) {
    return NextResponse.json(
      {
        detail: result.error.detail,
        title: result.error.title,
      },
      { status: result.error.status ?? 502 }
    );
  }

  return NextResponse.json(result.data);
}
