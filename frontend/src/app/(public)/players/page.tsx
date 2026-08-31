import type { Metadata } from "next";
import Link from "next/link";
import { connection } from "next/server";

import { DataUnavailable } from "@/components/cricket/data-unavailable";
import { PublicPageHeader } from "@/components/cricket/public-page-header";
import { PublicPagination } from "@/components/cricket/public-pagination";
import { searchPlayers, type Player, type PlayerSort } from "@/lib/api/players";
import { parsePageParam, parseSizeParam } from "@/lib/utils/pagination";

export const metadata: Metadata = {
  title: "Players | Eid Cricket Fest",
  description: "Search the Eid Cricket Fest player directory.",
};

type PlayersPageProps = {
  searchParams: Promise<{
    category?: string;
    page?: string;
    q?: string;
    size?: string;
    sort?: string;
  }>;
};

export default async function PlayersPage({ searchParams }: PlayersPageProps) {
  await connection();

  const params = await searchParams;
  const page = parsePageParam(params.page);
  const size = parseSizeParam(params.size, 20);
  const sortBy = parsePlayerSort(params.sort);
  const query = params.q?.trim() || undefined;
  const category = params.category?.trim() || undefined;
  const players = await searchPlayers({
    category,
    page,
    q: query,
    size,
    sortBy,
  });

  return (
    <main>
      <PublicPageHeader
        description="Search public player profiles and career records."
        kicker="Player directory"
        title="Players"
      />
      <section className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <PlayerSearchForm query={query} sortBy={sortBy} />
        {players.ok ? (
          <>
            <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {(players.data.content ?? []).length > 0 ? (
                players.data.content?.map((player) => (
                  <PlayerCard key={player.id} player={player} />
                ))
              ) : (
                <div className="rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground md:col-span-2 xl:col-span-3">
                  No players match this search.
                </div>
              )}
            </div>
            <PublicPagination
              basePath="/players"
              hasNext={players.data.hasNext}
              hasPrevious={players.data.hasPrevious}
              page={players.data.page ?? page}
              params={{ q: query, size: String(size), sort: sortBy }}
              totalPages={players.data.totalPages}
            />
          </>
        ) : (
          <DataUnavailable message="Players are temporarily unavailable." />
        )}
      </section>
    </main>
  );
}

function PlayerSearchForm({
  query,
  sortBy,
}: {
  query?: string;
  sortBy: PlayerSort;
}) {
  return (
    <form
      action="/players"
      className="grid gap-3 rounded-sm border border-white/10 bg-card p-4 md:grid-cols-[1fr_auto_auto]"
      method="get"
    >
      <label className="grid gap-2 text-sm">
        <span className="font-mono text-xs uppercase text-muted-foreground">
          Search by name
        </span>
        <input
          className="min-h-11 rounded-sm border border-white/10 bg-surface px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          defaultValue={query}
          name="q"
          placeholder="Search players"
          type="search"
        />
      </label>
      <label className="grid gap-2 text-sm">
        <span className="font-mono text-xs uppercase text-muted-foreground">
          Sort
        </span>
        <select
          className="min-h-11 rounded-sm border border-white/10 bg-surface px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          defaultValue={sortBy}
          name="sort"
        >
          <option value="name">Name A-Z</option>
          <option value="createdAt">Newest</option>
        </select>
      </label>
      <button
        className="min-h-11 self-end rounded-sm bg-secondary px-4 text-sm font-semibold text-secondary-foreground transition-colors hover:bg-secondary/85"
        type="submit"
      >
        Search
      </button>
    </form>
  );
}

function PlayerCard({ player }: { player: Player }) {
  const initials = player.fullName
    ?.split(" ")
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();

  return (
    <article className="rounded-sm border border-white/10 bg-card p-5">
      <div className="flex items-start gap-4">
        <div className="grid size-14 shrink-0 place-items-center rounded-sm bg-surface-elevated font-heading text-lg font-semibold uppercase text-secondary">
          {initials || "P"}
        </div>
        <div>
          <h2 className="font-heading text-xl font-semibold uppercase tracking-normal">
            <Link className="hover:text-secondary" href={`/players/${player.id}`}>
              {player.fullName ?? "Player"}
            </Link>
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">
            {player.primaryCategory?.name ?? "Category TBD"}
          </p>
        </div>
      </div>
      <dl className="mt-4 grid gap-3 text-sm">
        <PlayerDetail label="Batting" value={player.battingStyle ?? "TBD"} />
        <PlayerDetail label="Bowling" value={player.bowlingStyle ?? "TBD"} />
      </dl>
    </article>
  );
}

function PlayerDetail({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-5 border-b border-white/10 pb-2 last:border-b-0 last:pb-0">
      <dt className="font-mono text-xs uppercase text-muted-foreground">
        {label}
      </dt>
      <dd className="text-right font-medium">{value}</dd>
    </div>
  );
}

function parsePlayerSort(value?: string): PlayerSort {
  return value === "createdAt" ? "createdAt" : "name";
}
