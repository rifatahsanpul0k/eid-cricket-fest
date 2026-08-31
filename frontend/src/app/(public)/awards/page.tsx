import type { Metadata } from "next";
import { connection } from "next/server";

import { DataUnavailable } from "@/components/cricket/data-unavailable";
import { PlayerLink } from "@/components/cricket/player-link";
import { PublicPageHeader } from "@/components/cricket/public-page-header";
import { getAwards, type PlayerAward } from "@/lib/api/awards";
import { formatAwardType } from "@/lib/cricket/formatters";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";

export const metadata: Metadata = {
  title: "Awards | Eid Cricket Fest",
  description: "Tournament awards for Eid Cricket Fest.",
};

export default async function AwardsPage() {
  await connection();

  const currentEdition = await getCurrentEditionData();

  if (currentEdition.status === "unavailable") {
    return (
      <main>
        <PublicPageHeader
          description="Awards will appear when the backend is reachable."
          title="Awards"
        />
        <DataUnavailable message={currentEdition.message} />
      </main>
    );
  }

  const awards = await getAwards(currentEdition.edition.id);

  return (
    <main>
      <PublicPageHeader
        description="Tournament awards and recognized players."
        edition={currentEdition.edition}
        title="Awards"
      />
      <section className="mx-auto grid w-full max-w-7xl gap-4 px-4 py-8 sm:px-6 md:grid-cols-2 lg:grid-cols-3 lg:px-8">
        {awards.ok ? (
          awards.data.length > 0 ? (
            awards.data.map((award) => <AwardCard award={award} key={award.id} />)
          ) : (
            <div className="rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground md:col-span-2 lg:col-span-3">
              No awards have been announced yet.
            </div>
          )
        ) : (
          <DataUnavailable message="Awards are temporarily unavailable." />
        )}
      </section>
    </main>
  );
}

function AwardCard({ award }: { award: PlayerAward }) {
  return (
    <article className="rounded-sm border border-white/10 bg-card p-5">
      <p className="font-mono text-xs uppercase text-muted-foreground">
        {formatAwardType(award.awardType)}
      </p>
      <h2 className="mt-3 font-heading text-2xl font-semibold uppercase tracking-normal">
        {award.title ?? formatAwardType(award.awardType)}
      </h2>
      <p className="mt-3 text-sm">
        <PlayerLink name={award.playerName} playerId={award.playerId} />
      </p>
      {award.notes ? (
        <p className="mt-4 text-sm leading-6 text-muted-foreground">
          {award.notes}
        </p>
      ) : null}
    </article>
  );
}
