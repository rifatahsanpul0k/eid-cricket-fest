import type { Metadata } from "next";
import { connection } from "next/server";

import { DataUnavailable } from "@/components/cricket/data-unavailable";
import { PlayerLink } from "@/components/cricket/player-link";
import { PublicPageHeader } from "@/components/cricket/public-page-header";
import { getTournamentHistory, type HistoryEdition } from "@/lib/api/history";
import { formatAwardType } from "@/lib/cricket/formatters";
import { sortHistoryEditions } from "@/lib/cricket/history";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";
import { formatDate } from "@/lib/utils/format";

export const metadata: Metadata = {
  title: "History | Eid Cricket Fest",
  description: "Past Eid Cricket Fest editions and champions.",
};

export default async function HistoryPage() {
  await connection();

  const currentEdition = await getCurrentEditionData();

  if (currentEdition.status !== "ready") {
    return (
      <main>
        <PublicPageHeader
          description="Tournament history will appear when the backend is reachable."
          title="History"
        />
        <DataUnavailable message={currentEdition.message} />
      </main>
    );
  }

  const history = await getTournamentHistory(currentEdition.tournament.id);

  return (
    <main>
      <PublicPageHeader
        description="Previous editions, champions, finalists, performers, and awards."
        kicker={currentEdition.tournament.name ?? "Tournament history"}
        title="History"
      />
      <section className="mx-auto grid w-full max-w-7xl gap-5 px-4 py-8 sm:px-6 lg:px-8">
        {history.ok ? (
          sortHistoryEditions(history.data.editions ?? []).length > 0 ? (
            sortHistoryEditions(history.data.editions ?? []).map((edition) => (
              <HistoryCard edition={edition} key={edition.editionId} />
            ))
          ) : (
            <div className="rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground">
              Tournament history is not available yet.
            </div>
          )
        ) : (
          <DataUnavailable message="Tournament history is temporarily unavailable." />
        )}
      </section>
    </main>
  );
}

function HistoryCard({ edition }: { edition: HistoryEdition }) {
  return (
    <article className="rounded-sm border border-white/10 bg-card p-5">
      <div className="flex flex-col gap-2 md:flex-row md:items-end md:justify-between">
        <div>
          <p className="font-mono text-xs uppercase text-muted-foreground">
            {formatDate(edition.startDate)} - {formatDate(edition.endDate)}
          </p>
          <h2 className="mt-2 font-heading text-2xl font-semibold uppercase tracking-normal">
            {edition.name ?? "Tournament edition"}
          </h2>
        </div>
        <p className="font-mono text-sm uppercase text-secondary">
          {edition.finalResult ?? "Final result TBD"}
        </p>
      </div>
      <dl className="mt-5 grid gap-3 md:grid-cols-2">
        <HistoryDetail label="Champion" value={edition.champion?.name ?? "TBD"} />
        <HistoryDetail label="Runner-up" value={edition.runnerUp?.name ?? "TBD"} />
        <HistoryDetail
          label="Top Runs"
          value={
            <PlayerLink
              name={statLabel(edition.topRunScorer)}
              playerId={edition.topRunScorer?.playerId}
            />
          }
        />
        <HistoryDetail
          label="Top Wickets"
          value={
            <PlayerLink
              name={statLabel(edition.topWicketTaker)}
              playerId={edition.topWicketTaker?.playerId}
            />
          }
        />
      </dl>
      {(edition.awards ?? []).length > 0 ? (
        <div className="mt-5 rounded-sm border border-white/10 bg-surface p-4">
          <h3 className="font-mono text-xs uppercase text-muted-foreground">
            Awards
          </h3>
          <ul className="mt-3 grid gap-2 text-sm">
            {edition.awards?.map((award) => (
              <li
                className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between"
                key={award.id}
              >
                <span>{award.title ?? formatAwardType(award.awardType)}</span>
                <PlayerLink name={award.playerName} playerId={award.playerId} />
              </li>
            ))}
          </ul>
        </div>
      ) : null}
    </article>
  );
}

function HistoryDetail({
  label,
  value,
}: {
  label: string;
  value: React.ReactNode;
}) {
  return (
    <div className="rounded-sm border border-white/10 bg-surface p-3">
      <dt className="font-mono text-xs uppercase text-muted-foreground">
        {label}
      </dt>
      <dd className="mt-2 font-medium">{value}</dd>
    </div>
  );
}

function statLabel(stat?: HistoryEdition["topRunScorer"]) {
  if (!stat?.name) {
    return "TBD";
  }

  return stat.value === undefined ? stat.name : `${stat.name} (${stat.value})`;
}
