"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import type { LiveCentreMatch } from "@/lib/api/matches";
import {
  liveCentreAction,
  liveCentreDetailText,
  liveCentreEyebrow,
  liveCentrePrimaryText,
  liveCentreSections,
  resultText,
} from "@/lib/cricket/live-centre";
import { formatBangladeshDateTime } from "@/lib/utils/format";

const REFRESH_MS = 7_000;

export function LiveCentreClient({
  initialMatches,
}: {
  initialMatches: LiveCentreMatch[];
}) {
  const [matches, setMatches] = useState(initialMatches);

  useEffect(() => {
    let active = true;

    async function refresh() {
      try {
        const response = await fetch("/api/live-centre", {
          cache: "no-store",
        });

        if (!response.ok) {
          return;
        }

        const incoming = (await response.json()) as LiveCentreMatch[];

        if (active) {
          setMatches(incoming);
        }
      } catch {
        // Keep the last good server-rendered data when a refresh misses.
      }
    }

    const intervalId = window.setInterval(refresh, REFRESH_MS);

    return () => {
      active = false;
      window.clearInterval(intervalId);
    };
  }, []);

  const sections = liveCentreSections(matches);
  const hasMatches = matches.length > 0;

  return (
    <section className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      {hasMatches ? (
        <div className="grid gap-8">
          {sections.map((section) =>
            section.matches.length > 0 ? (
              <section key={section.id}>
                <div className="mb-3 flex items-center justify-between gap-4">
                  <h2 className="font-heading text-2xl font-semibold uppercase tracking-normal">
                    {section.title}
                  </h2>
                  <span className="font-mono text-xs uppercase text-muted-foreground">
                    {section.matches.length}
                  </span>
                </div>
                <div className="grid gap-4 md:grid-cols-2">
                  {section.matches.map((match) => (
                    <LiveCentreCard key={match.matchId} match={match} />
                  ))}
                </div>
              </section>
            ) : null
          )}
        </div>
      ) : (
        <div className="rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground">
          No match is live right now.
        </div>
      )}
    </section>
  );
}

export function LiveCentreCard({ match }: { match: LiveCentreMatch }) {
  const action = liveCentreAction(match);
  const innings = match.innings;
  const result = resultText(match);

  return (
    <article className="rounded-sm border border-white/10 bg-card p-5">
      <div className="flex flex-wrap items-center gap-2 font-mono text-xs uppercase text-muted-foreground">
        <span>{liveCentreEyebrow(match)}</span>
        <span className="rounded-sm bg-surface-elevated px-2 py-1 text-foreground">
          {statusLabel(match.status)}
        </span>
      </div>
      <h3 className="mt-3 font-heading text-xl font-semibold uppercase tracking-normal">
        {match.teamA?.name ?? "Team A"} vs {match.teamB?.name ?? "Team B"}
      </h3>
      <p className="mt-3 font-mono text-3xl font-bold tracking-normal text-secondary">
        {liveCentrePrimaryText(match)}
      </p>
      <p className="mt-2 min-h-5 text-sm text-muted-foreground">
        {liveCentreDetailText(match)}
      </p>
      {innings?.target ? (
        <dl className="mt-4 grid gap-2 text-sm sm:grid-cols-3">
          <Metric label="Target" value={innings.target.toString()} />
          <Metric
            label="CRR"
            value={innings.currentRunRate?.toString() ?? "-"}
          />
          <Metric
            label="RRR"
            value={innings.requiredRunRate?.toString() ?? "-"}
          />
        </dl>
      ) : null}
      <div className="mt-5 flex flex-wrap items-center gap-3">
        {action ? (
          <Link
            className="inline-flex min-h-11 items-center justify-center rounded-sm bg-secondary px-4 py-2 text-sm font-semibold text-secondary-foreground transition-colors hover:bg-secondary/85"
            href={action.href}
          >
            {action.label}
          </Link>
        ) : null}
        {match.resultStatus === "SUPERSEDED" && match.supersededByMatchId ? (
          <Link
            className="inline-flex min-h-11 items-center justify-center rounded-sm border border-white/10 bg-surface px-4 py-2 text-sm font-semibold transition-colors hover:bg-surface-elevated"
            href={`/matches/${match.supersededByMatchId}/live`}
          >
            Rematch
          </Link>
        ) : null}
      </div>
      <p className="mt-4 font-mono text-xs uppercase text-muted-foreground">
        {match.venue?.name ?? "Venue TBD"} ·{" "}
        {formatBangladeshDateTime(match.scheduledAt)}
      </p>
      {result && match.status === "COMPLETED" ? (
        <p className="mt-3 text-sm font-medium text-foreground">{result}</p>
      ) : null}
    </article>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-sm border border-white/10 bg-surface p-3">
      <dt className="font-mono text-xs uppercase text-muted-foreground">
        {label}
      </dt>
      <dd className="mt-1 font-mono text-lg font-bold">{value}</dd>
    </div>
  );
}

function statusLabel(status?: LiveCentreMatch["status"]) {
  return status?.replace("_", " ") ?? "Match";
}
