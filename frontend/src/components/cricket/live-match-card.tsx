import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import type { LiveMatch } from "@/lib/api/matches";

export function LiveMatchCard({
  liveMatch,
}: {
  liveMatch?: LiveMatch;
}) {
  const innings = liveMatch?.innings;

  return (
    <section className="mx-auto w-full max-w-7xl px-4 py-12 sm:px-6 lg:px-8" id="live">
      <div className="mb-5 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="font-mono text-xs font-medium uppercase text-live">
            Live Match
          </p>
          <h2 className="font-heading text-3xl font-bold uppercase tracking-normal">
            Score now
          </h2>
        </div>
        <Badge
          className={
            innings
              ? "rounded-full bg-live font-mono text-live-foreground"
              : "rounded-full bg-muted font-mono text-muted-foreground"
          }
        >
          {innings ? "LIVE" : "No match is live"}
        </Badge>
      </div>
      <div className="rounded-sm border border-white/10 bg-card p-5">
        {innings ? (
          <div className="grid gap-6 lg:grid-cols-[1fr_auto] lg:items-center">
            <div>
              <p className="font-mono text-xs uppercase text-muted-foreground">
                Match {liveMatch.matchNumber ?? "-"} · {liveMatch.teamA} vs{" "}
                {liveMatch.teamB}
              </p>
              <p className="mt-4 font-heading text-xl font-semibold uppercase tracking-normal text-foreground">
                {innings.battingTeam}
              </p>
              <h3 className="mt-2 font-mono text-5xl font-bold tracking-normal text-secondary sm:text-6xl">
                {innings.runs ?? 0}/{innings.wickets ?? 0}
              </h3>
              <p className="mt-3 font-mono text-sm uppercase text-muted-foreground">
                {innings.overs ?? "0.0"} overs
                {innings.target ? ` · Target ${innings.target}` : ""}
              </p>
            </div>
            <dl className="grid min-w-64 gap-3 text-sm">
              <LiveDetail label="Bowling" value={innings.bowlingTeam} />
              <LiveDetail label="Striker" value={innings.striker?.name} />
              <LiveDetail label="Bowler" value={innings.bowler?.name} />
              <LiveDetail
                label="Revision"
                value={innings.scoreRevision?.toString()}
              />
            </dl>
            {liveMatch?.matchId ? (
              <Link
                className="inline-flex min-h-11 items-center justify-center rounded-sm bg-secondary px-4 py-2 text-sm font-semibold text-secondary-foreground transition-colors hover:bg-secondary/85 lg:col-span-2 lg:justify-self-start"
                href={`/matches/${liveMatch.matchId}/live`}
              >
                Follow Live
              </Link>
            ) : null}
          </div>
        ) : (
          <p className="rounded-sm border border-white/10 bg-surface p-4 text-sm text-muted-foreground">
            No match is live right now. Fixtures and standings remain available
            below when tournament data is available.
          </p>
        )}
      </div>
    </section>
  );
}

function LiveDetail({
  label,
  value,
}: {
  label: string;
  value?: string;
}) {
  return (
    <div className="flex items-center justify-between gap-6 border-b border-white/10 pb-2 last:border-b-0 last:pb-0">
      <dt className="font-mono text-xs uppercase text-muted-foreground">
        {label}
      </dt>
      <dd className="font-medium">{value ?? "TBD"}</dd>
    </div>
  );
}
