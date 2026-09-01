import type { Metadata } from "next";
import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import { buttonVariants } from "@/components/ui/button";
import type { MyMatchResponse } from "@/lib/api/schema-helpers";
import { getMyMatches } from "@/lib/auth/my-cricket-api";
import {
  myMatchAction,
  partitionMyMatches,
  playingXiStatus,
} from "@/lib/auth/my-cricket-state";
import { MATCH_STAGE_LABELS, MATCH_STATUS_LABELS } from "@/lib/cricket/match-labels";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";
import { cn } from "@/lib/utils";
import { formatBangladeshDateTime } from "@/lib/utils/format";

export const metadata: Metadata = {
  title: "My Matches",
};

export default async function MyMatchesPage() {
  const currentEdition = await getCurrentEditionData();
  const matches =
    currentEdition.status === "ready"
      ? await getMyMatches(currentEdition.edition.id)
      : [];
  const grouped = partitionMyMatches(matches);

  return (
    <main className="flex-1">
      <section className="border-b border-white/10 bg-background">
        <div className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
          <p className="font-mono text-xs uppercase text-primary">
            My Cricket
          </p>
          <h1 className="mt-3 font-heading text-4xl font-bold uppercase tracking-normal">
            My Matches
          </h1>
          {currentEdition.status === "ready" ? (
            <p className="mt-3 text-sm text-muted-foreground">
              {currentEdition.edition.name}
            </p>
          ) : null}
        </div>
      </section>
      <section className="mx-auto grid w-full max-w-7xl gap-6 px-4 py-8 sm:px-6 lg:px-8">
        {currentEdition.status !== "ready" ? (
          <EmptyPanel message={currentEdition.message} />
        ) : matches.length === 0 ? (
          <EmptyPanel message="Your team fixtures will appear here after scheduling." />
        ) : (
          <>
            <MatchSection matches={grouped.upcoming} title="Upcoming" />
            <MatchSection matches={grouped.completed} title="Completed" />
          </>
        )}
        <Link className={cn(buttonVariants({ variant: "outline" }), "w-fit")} href="/account">
          Back to account
        </Link>
      </section>
    </main>
  );
}

function MatchSection({
  matches,
  title,
}: {
  matches: MyMatchResponse[];
  title: string;
}) {
  return (
    <div>
      <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
        {title}
      </h2>
      {matches.length > 0 ? (
        <div className="mt-4 grid gap-3">
          {matches.map((match) => (
            <MatchItem key={match.matchId} match={match} />
          ))}
        </div>
      ) : (
        <p className="mt-3 rounded-sm border border-white/10 bg-card p-4 text-sm text-muted-foreground">
          No {title.toLowerCase()} matches.
        </p>
      )}
    </div>
  );
}

function MatchItem({ match }: { match: MyMatchResponse }) {
  const action = myMatchAction(match);

  return (
    <article className="grid gap-4 rounded-sm border border-white/10 bg-card p-4 text-sm md:grid-cols-[1fr_auto] md:items-center">
      <div>
        <div className="flex flex-wrap items-center gap-2 font-mono text-xs uppercase text-muted-foreground">
          <span>Match {match.matchNumber ?? "-"}</span>
          <span>{match.stage ? MATCH_STAGE_LABELS[match.stage] : "Fixture"}</span>
          <Badge variant="outline">
            {match.status ? MATCH_STATUS_LABELS[match.status] : "Fixture"}
          </Badge>
          <Badge variant={match.inPlayingXi ? "default" : "secondary"}>
            {playingXiStatus(match)}
          </Badge>
        </div>
        <h3 className="mt-3 font-heading text-xl font-semibold uppercase tracking-normal">
          {match.teamA?.name ?? "Team A"} vs {match.teamB?.name ?? "Team B"}
        </h3>
        <p className="mt-2 text-muted-foreground">
          {formatBangladeshDateTime(match.scheduledAt)}
        </p>
        <p className="mt-3 font-mono text-xs uppercase text-muted-foreground">
          {match.venue?.name ?? "Venue TBD"} · {match.oversPerInnings ?? 0} overs
          {match.opponent?.name ? ` · vs ${match.opponent.name}` : ""}
        </p>
        {match.resultSummary ? (
          <p className="mt-3 text-muted-foreground">{match.resultSummary}</p>
        ) : null}
      </div>
      {action ? (
        <Link className={cn(buttonVariants(), "w-fit")} href={action.href}>
          {action.label}
        </Link>
      ) : null}
    </article>
  );
}

function EmptyPanel({ message }: { message: string }) {
  return (
    <div className="rounded-sm border border-white/10 bg-card p-5 text-sm">
      <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
        Matches unavailable
      </h2>
      <p className="mt-3 text-muted-foreground">{message}</p>
    </div>
  );
}
