"use client";

import { useEffect, useState } from "react";

import type { LiveMatch } from "@/lib/api/matches";
import { createLiveMatchClient } from "@/lib/realtime/stomp-client";
import {
  shouldAcceptLiveUpdate,
  type LiveConnectionState,
} from "@/lib/realtime/live-update";

export function LiveMatchClient({ initialMatch }: { initialMatch: LiveMatch }) {
  const [match, setMatch] = useState(initialMatch);
  const [connectionState, setConnectionState] =
    useState<LiveConnectionState>("connecting");

  useEffect(() => {
    if (!initialMatch.matchId) {
      return;
    }

    const liveClient = createLiveMatchClient({
      matchId: initialMatch.matchId,
      onConnectionState: setConnectionState,
      onUpdate: (incoming) => {
        setMatch((current) =>
          shouldAcceptLiveUpdate(current, incoming) ? incoming : current
        );
      },
    });

    liveClient.activate();

    return () => {
      liveClient.deactivate();
    };
  }, [initialMatch]);

  return <LiveMatchView connectionState={connectionState} match={match} />;
}

function LiveMatchView({
  connectionState,
  match,
}: {
  connectionState: LiveConnectionState;
  match: LiveMatch;
}) {
  const innings = match.innings;

  return (
    <section className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="font-mono text-xs uppercase text-live">Live Match</p>
          <h1 className="font-heading text-4xl font-bold uppercase tracking-normal">
            {match.teamA ?? "Team A"} vs {match.teamB ?? "Team B"}
          </h1>
        </div>
        <ConnectionBadge state={connectionState} />
      </div>
      <div className="grid gap-4 lg:grid-cols-[1.4fr_0.6fr]">
        <div className="rounded-sm border border-white/10 bg-card p-5">
          <p className="font-mono text-xs uppercase text-muted-foreground">
            Match {match.matchNumber ?? "-"} · {match.status ?? "LIVE"}
          </p>
          {innings ? (
            <>
              <p className="mt-5 font-heading text-xl font-semibold uppercase tracking-normal">
                {innings.battingTeam ?? "Batting team"}
              </p>
              <div className="mt-2 flex flex-wrap items-end gap-4">
                <p className="font-mono text-6xl font-bold tracking-normal text-secondary">
                  {innings.runs ?? 0}/{innings.wickets ?? 0}
                </p>
                <p className="pb-2 font-mono text-sm uppercase text-muted-foreground">
                  {innings.overs ?? "0.0"} overs
                </p>
              </div>
              <dl className="mt-6 grid gap-3 sm:grid-cols-3">
                <LiveMetric label="Run rate" value={innings.currentRunRate} />
                <LiveMetric label="Required" value={innings.requiredRunRate} />
                <LiveMetric label="Target" value={innings.target} />
              </dl>
            </>
          ) : (
            <p className="mt-5 rounded-sm border border-white/10 bg-surface p-4 text-sm text-muted-foreground">
              Live score has not started for this match yet.
            </p>
          )}
        </div>
        <aside className="rounded-sm border border-white/10 bg-card p-5">
          <h2 className="font-heading text-xl font-semibold uppercase tracking-normal">
            Crease
          </h2>
          <dl className="mt-4 grid gap-3 text-sm">
            <LiveDetail label="Striker" value={innings?.striker?.name} />
            <LiveDetail label="Non-striker" value={innings?.nonStriker?.name} />
            <LiveDetail label="Bowler" value={innings?.bowler?.name} />
            <LiveDetail label="Bowling" value={innings?.bowlingTeam} />
            <LiveDetail
              label="Revision"
              value={innings?.scoreRevision?.toString()}
            />
          </dl>
        </aside>
      </div>
      <RecentBalls balls={match.recentBalls ?? []} />
    </section>
  );
}

function ConnectionBadge({ state }: { state: LiveConnectionState }) {
  const label =
    state === "connected"
      ? "Connected"
      : state === "connecting"
        ? "Connecting"
        : state === "reconnecting"
          ? "Reconnecting"
          : state === "error"
            ? "Unavailable"
            : "Disconnected";

  return (
    <span className="rounded-full border border-white/10 bg-surface-elevated px-3 py-2 font-mono text-xs uppercase text-muted-foreground">
      {label}
    </span>
  );
}

function LiveMetric({
  label,
  value,
}: {
  label: string;
  value?: number;
}) {
  return (
    <div className="rounded-sm border border-white/10 bg-surface p-3">
      <dt className="font-mono text-xs uppercase text-muted-foreground">
        {label}
      </dt>
      <dd className="mt-2 font-mono text-xl font-bold">
        {value ?? "-"}
      </dd>
    </div>
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
    <div className="flex items-center justify-between gap-5 border-b border-white/10 pb-2 last:border-b-0 last:pb-0">
      <dt className="font-mono text-xs uppercase text-muted-foreground">
        {label}
      </dt>
      <dd className="text-right font-medium">{value ?? "TBD"}</dd>
    </div>
  );
}

function RecentBalls({ balls }: { balls: LiveMatch["recentBalls"] }) {
  return (
    <section className="mt-4 rounded-sm border border-white/10 bg-card p-5">
      <h2 className="font-heading text-xl font-semibold uppercase tracking-normal">
        Recent Balls
      </h2>
      {balls && balls.length > 0 ? (
        <div className="mt-4 flex flex-wrap gap-2">
          {balls.map((ball) => (
            <span
              className="grid size-10 place-items-center rounded-full bg-surface-elevated font-mono text-sm font-bold"
              key={ball.deliveryId ?? ball.sequence}
              title={ball.commentary}
            >
              {ball.runs ?? 0}
            </span>
          ))}
        </div>
      ) : (
        <p className="mt-4 text-sm text-muted-foreground">
          Recent deliveries will appear as scoring updates arrive.
        </p>
      )}
    </section>
  );
}
