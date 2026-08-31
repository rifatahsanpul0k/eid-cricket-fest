import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { connection } from "next/server";

import { DataUnavailable } from "@/components/cricket/data-unavailable";
import { ScorecardView } from "@/components/cricket/scorecard/scorecard-view";
import { getScorecard } from "@/lib/api/scorecard";

export const metadata: Metadata = {
  title: "Scorecard | Eid Cricket Fest",
  description: "Full innings scorecard for an Eid Cricket Fest match.",
};

type ScorecardPageProps = {
  params: Promise<{
    matchId: string;
  }>;
};

export default async function ScorecardPage({ params }: ScorecardPageProps) {
  await connection();

  const matchId = parseMatchId((await params).matchId);

  if (!matchId) {
    notFound();
  }

  const scorecard = await getScorecard(matchId);

  if (!scorecard.ok) {
    if (scorecard.error.status === 404) {
      notFound();
    }

    return (
      <main>
        <DataUnavailable
          message="Scorecard data is temporarily unavailable."
        />
      </main>
    );
  }

  return (
    <main>
      <ScorecardView scorecard={scorecard.data} />
    </main>
  );
}

function parseMatchId(value: string) {
  const parsed = Number.parseInt(value, 10);

  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined;
}
