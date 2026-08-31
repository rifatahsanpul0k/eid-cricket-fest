import Link from "next/link";

import type { Match } from "@/lib/api/matches";
import {
  matchAction,
  matchStageLabel,
  matchStatusLabel,
} from "@/lib/cricket/match-labels";
import { formatBangladeshDateTime } from "@/lib/utils/format";

export function MatchCard({ match }: { match: Match }) {
  const action = matchAction(match);

  return (
    <article className="rounded-sm border border-white/10 bg-card p-4">
      <p className="font-mono text-xs font-medium uppercase text-muted-foreground">
        Match {match.matchNumber ?? "-"} · {matchStageLabel(match.stage)} ·{" "}
        {matchStatusLabel(match.status)}
      </p>
      <h3 className="mt-3 font-heading text-lg font-semibold uppercase tracking-normal">
        {match.teamA?.teamName ?? "Team A"} vs{" "}
        {match.teamB?.teamName ?? "Team B"}
      </h3>
      <p className="mt-2 text-sm text-muted-foreground">
        {formatBangladeshDateTime(match.scheduledAt)}
      </p>
      <p className="mt-3 font-mono text-xs uppercase">
        {match.venue?.name ?? "Venue TBD"} · {match.oversPerInnings ?? 0} overs
      </p>
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
