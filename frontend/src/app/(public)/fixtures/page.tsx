import type { Metadata } from "next";
import { connection } from "next/server";

import { DataUnavailable } from "@/components/cricket/data-unavailable";
import { FixtureCard } from "@/components/cricket/fixtures/fixture-card";
import { FixtureFilterNav } from "@/components/cricket/fixtures/fixture-filter-nav";
import { PaginationNav } from "@/components/cricket/fixtures/pagination-nav";
import { getMatchesPage } from "@/lib/api/matches";
import {
  parseMatchStage,
  parseMatchStatus,
} from "@/lib/cricket/match-labels";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";

export const metadata: Metadata = {
  title: "Fixtures | Eid Cricket Fest",
  description: "Browse Eid Cricket Fest fixtures, live matches, and results.",
};

type FixturesPageProps = {
  searchParams: Promise<{
    page?: string;
    stage?: string;
    status?: string;
  }>;
};

export default async function FixturesPage({
  searchParams,
}: FixturesPageProps) {
  await connection();

  const params = await searchParams;
  const page = parsePage(params.page);
  const status = parseMatchStatus(params.status);
  const stage = parseMatchStage(params.stage);
  const currentEdition = await getCurrentEditionData();

  if (currentEdition.status !== "ready") {
    return (
      <main>
        <FixturesHero />
        <DataUnavailable message={currentEdition.message} />
      </main>
    );
  }

  const matches = await getMatchesPage(currentEdition.edition.id, {
    direction: "asc",
    page,
    size: 12,
    sortBy: "scheduledAt",
    stage,
    status,
  });

  return (
    <main>
      <FixturesHero editionName={currentEdition.edition.name} />
      <section className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <FixtureFilterNav stage={stage} status={status} />
        {matches.ok ? (
          <>
            <div className="mt-6 grid gap-4">
              {(matches.data.content ?? []).length > 0 ? (
                matches.data.content?.map((match) => (
                  <FixtureCard key={match.id} match={match} />
                ))
              ) : (
                <div className="rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground">
                  No fixtures match the selected filters.
                </div>
              )}
            </div>
            <PaginationNav
              hasNext={matches.data.hasNext}
              hasPrevious={matches.data.hasPrevious}
              page={matches.data.page ?? page}
              stage={stage}
              status={status}
              totalPages={matches.data.totalPages}
            />
          </>
        ) : (
          <DataUnavailable message={matches.error.detail ?? matches.error.title} />
        )}
      </section>
    </main>
  );
}

function FixturesHero({ editionName }: { editionName?: string }) {
  return (
    <section className="border-b border-white/10 bg-background">
      <div className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <p className="font-mono text-xs uppercase text-primary">
          {editionName ?? "Current edition"}
        </p>
        <h1 className="mt-3 font-heading text-4xl font-bold uppercase tracking-normal">
          Fixtures
        </h1>
        <p className="mt-3 max-w-2xl text-sm leading-6 text-muted-foreground">
          Match centre for upcoming fixtures, live games, innings breaks, and
          completed scorecards.
        </p>
      </div>
    </section>
  );
}

function parsePage(value?: string) {
  const parsed = Number.parseInt(value ?? "0", 10);

  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
}
