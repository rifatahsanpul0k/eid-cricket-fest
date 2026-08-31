import type { Match } from "@/lib/api/matches";
import { formatDateTime } from "@/lib/utils/format";

export function MatchCard({ match }: { match: Match }) {
  return (
    <article className="rounded-sm border border-white/10 bg-card p-4">
      <p className="font-mono text-xs font-medium uppercase text-muted-foreground">
        Match {match.matchNumber ?? "-"} · {match.stage ?? "Fixture"}
      </p>
      <h3 className="mt-3 font-heading text-lg font-semibold uppercase tracking-normal">
        {match.teamA?.teamName ?? "Team A"} vs{" "}
        {match.teamB?.teamName ?? "Team B"}
      </h3>
      <p className="mt-2 text-sm text-muted-foreground">
        {formatDateTime(match.scheduledAt)}
      </p>
      <p className="mt-3 font-mono text-xs uppercase">
        {match.venue?.name ?? "Venue TBD"} · {match.oversPerInnings ?? 0} overs
      </p>
    </article>
  );
}
