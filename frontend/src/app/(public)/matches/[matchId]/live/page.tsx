import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { connection } from "next/server";

import { DataUnavailable } from "@/components/cricket/data-unavailable";
import { LiveMatchClient } from "@/components/cricket/live/live-match-client";
import { getLiveMatch } from "@/lib/api/matches";

export const metadata: Metadata = {
  title: "Live Match | Eid Cricket Fest",
  description: "Realtime score updates for an Eid Cricket Fest match.",
};

type LiveMatchPageProps = {
  params: Promise<{
    matchId: string;
  }>;
};

export default async function LiveMatchPage({ params }: LiveMatchPageProps) {
  await connection();

  const matchId = parseMatchId((await params).matchId);

  if (!matchId) {
    notFound();
  }

  const liveMatch = await getLiveMatch(matchId);

  if (!liveMatch.ok) {
    if (liveMatch.error.status === 404) {
      notFound();
    }

    return (
      <main>
        <DataUnavailable
          message="Live match data is temporarily unavailable."
        />
      </main>
    );
  }

  return (
    <main>
      <LiveMatchClient initialMatch={liveMatch.data} />
    </main>
  );
}

function parseMatchId(value: string) {
  const parsed = Number.parseInt(value, 10);

  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined;
}
