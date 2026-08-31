import { MatchCard } from "@/components/cricket/match-card";
import type { Match } from "@/lib/api/matches";

export function UpcomingMatches({ matches }: { matches: Match[] }) {
  return (
    <section className="border-y border-white/10 bg-surface py-12" id="fixtures">
      <div className="mx-auto w-full max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mb-5">
          <p className="font-mono text-xs font-medium uppercase text-primary">
            Fixtures
          </p>
          <h2 className="font-heading text-3xl font-bold uppercase tracking-normal">
            Upcoming matches
          </h2>
        </div>
        {matches.length > 0 ? (
          <div className="grid gap-4 md:grid-cols-3">
            {matches.map((match) => (
              <MatchCard key={match.id} match={match} />
            ))}
          </div>
        ) : (
          <div className="rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground">
            No upcoming matches are scheduled yet.
          </div>
        )}
      </div>
    </section>
  );
}
