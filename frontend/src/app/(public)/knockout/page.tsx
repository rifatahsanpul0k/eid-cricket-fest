import type { Metadata } from "next";
import Link from "next/link";
import { connection } from "next/server";

import { DataUnavailable } from "@/components/cricket/data-unavailable";
import { PublicPageHeader } from "@/components/cricket/public-page-header";
import { getKnockoutBracket, type KnockoutMatch } from "@/lib/api/knockout";
import {
  matchRouteAction,
  matchStageLabel,
  matchStatusLabel,
} from "@/lib/cricket/match-labels";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";

export const metadata: Metadata = {
  title: "Knockout | Eid Cricket Fest",
  description: "Semi-finals and final bracket for Eid Cricket Fest.",
};

export default async function KnockoutPage() {
  await connection();

  const currentEdition = await getCurrentEditionData();

  if (currentEdition.status === "unavailable") {
    return (
      <main>
        <PublicPageHeader
          description="Knockout matches will appear when the backend is reachable."
          title="Knockout"
        />
        <DataUnavailable message={currentEdition.message} />
      </main>
    );
  }

  const bracket = await getKnockoutBracket(currentEdition.edition.id);

  return (
    <main>
      <PublicPageHeader
        description="Semi-finals and final, including winners and live or scorecard actions."
        edition={currentEdition.edition}
        title="Knockout"
      />
      <section className="mx-auto grid w-full max-w-7xl gap-5 px-4 py-8 sm:px-6 lg:px-8 xl:grid-cols-[1fr_0.8fr]">
        {bracket.ok ? (
          (bracket.data.semiFinals?.length ?? 0) > 0 || bracket.data.finalMatch ? (
            <>
              <div className="grid gap-4">
                {(bracket.data.semiFinals ?? []).map((match, index) => (
                  <KnockoutCard
                    key={match.matchId ?? index}
                    label={`Semi-final ${index + 1}`}
                    match={match}
                  />
                ))}
              </div>
              <KnockoutCard label="Final" match={bracket.data.finalMatch} />
            </>
          ) : (
            <div className="rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground xl:col-span-2">
              Knockout stage has not been generated yet.
            </div>
          )
        ) : (
          <DataUnavailable message="Knockout bracket is temporarily unavailable." />
        )}
      </section>
    </main>
  );
}

function KnockoutCard({
  label,
  match,
}: {
  label: string;
  match?: KnockoutMatch;
}) {
  if (!match) {
    return (
      <article className="rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground">
        {label} has not been generated yet.
      </article>
    );
  }

  const action = matchRouteAction({
    matchId: match.matchId,
    status: match.status,
  });

  return (
    <article className="rounded-sm border border-white/10 bg-card p-5">
      <p className="font-mono text-xs uppercase text-muted-foreground">
        {label} · Match {match.matchNumber ?? "-"} · {matchStageLabel(match.stage)}
      </p>
      <h2 className="mt-3 font-heading text-2xl font-semibold uppercase tracking-normal">
        {teamLabel(match.teamA)} vs {teamLabel(match.teamB)}
      </h2>
      <dl className="mt-4 grid gap-3 text-sm">
        <Detail label="Status" value={matchStatusLabel(match.status)} />
        <Detail label="Winner" value={teamLabel(match.winner)} />
        <Detail label="Source A" value={sourceLabel(match.sourceMatchAId)} />
        <Detail label="Source B" value={sourceLabel(match.sourceMatchBId)} />
      </dl>
      {action ? (
        <Link
          className="mt-4 inline-flex min-h-10 items-center rounded-sm bg-secondary px-3 py-2 text-sm font-semibold text-secondary-foreground transition-colors hover:bg-secondary/85"
          href={action.href}
        >
          {action.label}
        </Link>
      ) : null}
    </article>
  );
}

function teamLabel(team?: KnockoutMatch["teamA"]) {
  if (!team) {
    return "TBD";
  }

  return team.seed ? `${team.teamName ?? "Team"} (Seed ${team.seed})` : team.teamName ?? "Team";
}

function sourceLabel(matchId?: number) {
  return matchId ? `Winner match ${matchId}` : "TBD";
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-5 border-b border-white/10 pb-2 last:border-b-0 last:pb-0">
      <dt className="font-mono text-xs uppercase text-muted-foreground">
        {label}
      </dt>
      <dd className="text-right font-medium">{value}</dd>
    </div>
  );
}
