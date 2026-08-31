import Link from "next/link";

import type { Match } from "@/lib/api/matches";
import {
  matchAction,
  matchStageLabel,
  matchStatusLabel,
} from "@/lib/cricket/match-labels";
import { formatBangladeshDateTime } from "@/lib/utils/format";

export function FixtureCard({ match }: { match: Match }) {
  const action = matchAction(match);

  return (
    <article className="grid gap-4 rounded-sm border border-white/10 bg-card p-4 md:grid-cols-[1fr_auto] md:items-center">
      <div>
        <div className="flex flex-wrap items-center gap-2 font-mono text-xs uppercase text-muted-foreground">
          <span>Match {match.matchNumber ?? "-"}</span>
          <span>{matchStageLabel(match.stage)}</span>
          <span className="rounded-sm bg-surface-elevated px-2 py-1 text-foreground">
            {matchStatusLabel(match.status)}
          </span>
        </div>
        <h2 className="mt-3 font-heading text-xl font-semibold uppercase tracking-normal">
          {match.teamA?.name ?? "Team A"} vs{" "}
          {match.teamB?.name ?? "Team B"}
        </h2>
        <p className="mt-2 text-sm text-muted-foreground">
          {formatBangladeshDateTime(match.scheduledAt)}
        </p>
        <p className="mt-3 font-mono text-xs uppercase text-muted-foreground">
          {match.venue?.name ?? "Venue TBD"} · {match.oversPerInnings ?? 0} overs
        </p>
      </div>
      {action ? (
        <Link
          className="inline-flex min-h-11 items-center justify-center rounded-sm bg-secondary px-4 py-2 text-sm font-semibold text-secondary-foreground transition-colors hover:bg-secondary/85"
          href={action.href}
        >
          {action.label}
        </Link>
      ) : null}
    </article>
  );
}
