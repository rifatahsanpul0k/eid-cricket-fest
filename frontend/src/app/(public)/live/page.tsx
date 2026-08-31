import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { connection } from "next/server";

import { DataUnavailable } from "@/components/cricket/data-unavailable";
import { getMatches } from "@/lib/api/matches";
import { matchStageLabel } from "@/lib/cricket/match-labels";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";

export const metadata: Metadata = {
  title: "Live | Eid Cricket Fest",
  description: "Follow active Eid Cricket Fest matches in real time.",
};

export default async function LivePage() {
  await connection();

  const currentEdition = await getCurrentEditionData();

  if (currentEdition.status === "unavailable") {
    return (
      <main>
        <LiveHero />
        <DataUnavailable message={currentEdition.message} />
      </main>
    );
  }

  const [liveMatches, inningsBreakMatches] = await Promise.all([
    getMatches(currentEdition.edition.id, {
      status: "LIVE",
      size: 20,
      sortBy: "matchNumber",
      direction: "asc",
    }),
    getMatches(currentEdition.edition.id, {
      status: "INNINGS_BREAK",
      size: 20,
      sortBy: "matchNumber",
      direction: "asc",
    }),
  ]);

  if (!liveMatches.ok || !inningsBreakMatches.ok) {
    return (
      <main>
        <LiveHero />
        <DataUnavailable message="Live match data is temporarily unavailable." />
      </main>
    );
  }

  const activeMatches = [...liveMatches.data, ...inningsBreakMatches.data].filter(
    (match) => match.id
  );

  if (activeMatches.length === 1 && activeMatches[0]?.id) {
    redirect(`/matches/${activeMatches[0].id}/live`);
  }

  return (
    <main>
      <LiveHero />
      <section className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        {activeMatches.length > 0 ? (
          <div className="grid gap-4 md:grid-cols-2">
            {activeMatches.map((match) => (
              <Link
                className="rounded-sm border border-white/10 bg-card p-5 transition-colors hover:bg-surface"
                href={`/matches/${match.id}/live`}
                key={match.id}
              >
                <p className="font-mono text-xs uppercase text-live">
                  {match.status === "INNINGS_BREAK" ? "Innings Break" : "Live"}
                </p>
                <h2 className="mt-3 font-heading text-xl font-semibold uppercase tracking-normal">
                  {match.teamA?.name ?? "Team A"} vs{" "}
                  {match.teamB?.name ?? "Team B"}
                </h2>
                <p className="mt-2 font-mono text-xs uppercase text-muted-foreground">
                  Match {match.matchNumber ?? "-"} · {matchStageLabel(match.stage)}
                </p>
              </Link>
            ))}
          </div>
        ) : (
          <div className="rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground">
            No match is live.
          </div>
        )}
      </section>
    </main>
  );
}

function LiveHero() {
  return (
    <section className="border-b border-white/10 bg-background">
      <div className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <p className="font-mono text-xs uppercase text-live">Live Centre</p>
        <h1 className="mt-3 font-heading text-4xl font-bold uppercase tracking-normal">
          Live Matches
        </h1>
      </div>
    </section>
  );
}
