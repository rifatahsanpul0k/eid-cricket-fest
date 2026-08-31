import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { connection } from "next/server";

import { DataUnavailable } from "@/components/cricket/data-unavailable";
import { PublicPageHeader } from "@/components/cricket/public-page-header";
import {
  getPlayer,
  getPlayerCareer,
  type Player,
  type PlayerCareer,
} from "@/lib/api/players";
import { formatRunRate } from "@/lib/cricket/formatters";

export const metadata: Metadata = {
  title: "Player Profile | Eid Cricket Fest",
  description: "Public Eid Cricket Fest player profile and career summary.",
};

type PlayerPageProps = {
  params: Promise<{
    playerId: string;
  }>;
};

export default async function PlayerPage({ params }: PlayerPageProps) {
  await connection();

  const playerId = parsePlayerId((await params).playerId);

  if (!playerId) {
    notFound();
  }

  const [player, career] = await Promise.all([
    getPlayer(playerId),
    getPlayerCareer(playerId),
  ]);

  if (!player.ok) {
    if (player.error.status === 404) {
      notFound();
    }

    return (
      <main>
        <PublicPageHeader
          description="Player profile data will appear when the backend is reachable."
          title="Player Profile"
        />
        <DataUnavailable message="Player profile is temporarily unavailable." />
      </main>
    );
  }

  return (
    <main>
      <PublicPageHeader
        description="Public player identity and career statistics."
        kicker={player.data.primaryCategory?.name ?? "Player profile"}
        title={player.data.fullName ?? "Player"}
      />
      <section className="mx-auto grid w-full max-w-7xl gap-5 px-4 py-8 sm:px-6 lg:grid-cols-[0.7fr_1.3fr] lg:px-8">
        <ProfileCard player={player.data} />
        {career.ok ? (
          <CareerCard career={career.data} />
        ) : (
          <DataUnavailable message="Career statistics are temporarily unavailable." />
        )}
      </section>
    </main>
  );
}

function ProfileCard({ player }: { player: Player }) {
  return (
    <article className="rounded-sm border border-white/10 bg-card p-5">
      <h2 className="font-heading text-2xl font-semibold uppercase tracking-normal">
        Profile
      </h2>
      <dl className="mt-5 grid gap-3 text-sm">
        <Detail label="Category" value={player.primaryCategory?.name ?? "TBD"} />
        <Detail label="Batting" value={player.battingStyle ?? "TBD"} />
        <Detail label="Bowling" value={player.bowlingStyle ?? "TBD"} />
      </dl>
    </article>
  );
}

function CareerCard({ career }: { career: PlayerCareer }) {
  return (
    <article className="rounded-sm border border-white/10 bg-card p-5">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="font-mono text-xs uppercase text-muted-foreground">
            Career Summary
          </p>
          <h2 className="font-heading text-2xl font-semibold uppercase tracking-normal">
            {career.playerName ?? "Player"}
          </h2>
        </div>
        <p className="font-mono text-sm uppercase text-muted-foreground">
          {career.matchesPlayed ?? 0} matches · {career.editionsPlayed ?? 0} editions
        </p>
      </div>
      <div className="mt-5 grid gap-5 xl:grid-cols-2">
        <StatGrid
          stats={[
            ["Innings", career.batting?.innings ?? 0],
            ["Runs", career.batting?.runs ?? 0],
            ["Balls", career.batting?.balls ?? 0],
            ["Highest", career.batting?.highestScore ?? 0],
            ["4s", career.batting?.fours ?? 0],
            ["6s", career.batting?.sixes ?? 0],
            ["Dismissals", career.batting?.dismissals ?? 0],
            ["Average", formatRunRate(career.batting?.average)],
            ["Strike Rate", formatRunRate(career.batting?.strikeRate)],
          ]}
          title="Batting"
        />
        <StatGrid
          stats={[
            ["Overs", career.bowling?.overs ?? "0.0"],
            ["Legal Balls", career.bowling?.legalBalls ?? 0],
            ["Runs", career.bowling?.runsConceded ?? 0],
            ["Wickets", career.bowling?.wickets ?? 0],
            ["Best", career.bowling?.bestBowling ?? "-"],
            ["Average", formatRunRate(career.bowling?.average)],
            ["Economy", formatRunRate(career.bowling?.economy)],
          ]}
          title="Bowling"
        />
      </div>
    </article>
  );
}

function StatGrid({
  stats,
  title,
}: {
  stats: [string, string | number][];
  title: string;
}) {
  return (
    <div className="rounded-sm border border-white/10 bg-surface p-4">
      <h3 className="font-heading text-xl font-semibold uppercase tracking-normal">
        {title}
      </h3>
      <dl className="mt-4 grid grid-cols-2 gap-3">
        {stats.map(([label, value]) => (
          <div
            className="rounded-sm border border-white/10 bg-card p-3"
            key={label}
          >
            <dt className="font-mono text-xs uppercase text-muted-foreground">
              {label}
            </dt>
            <dd className="mt-2 font-mono text-lg font-bold">{value}</dd>
          </div>
        ))}
      </dl>
    </div>
  );
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

function parsePlayerId(value: string) {
  const parsed = Number.parseInt(value, 10);

  return Number.isFinite(parsed) && parsed > 0 ? parsed : undefined;
}
