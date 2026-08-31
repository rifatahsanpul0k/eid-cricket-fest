import type { Metadata } from "next";
import { connection } from "next/server";

import { DataUnavailable } from "@/components/cricket/data-unavailable";
import { PublicPageHeader } from "@/components/cricket/public-page-header";
import { getEditionTeams, type TournamentTeam } from "@/lib/api/teams";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";

export const metadata: Metadata = {
  title: "Teams | Eid Cricket Fest",
  description: "Tournament teams for the current Eid Cricket Fest edition.",
};

export default async function TeamsPage() {
  await connection();

  const currentEdition = await getCurrentEditionData();

  if (currentEdition.status === "unavailable") {
    return (
      <main>
        <PublicPageHeader
          description="Edition teams will appear when the backend is reachable."
          title="Teams"
        />
        <DataUnavailable message={currentEdition.message} />
      </main>
    );
  }

  const teams = await getEditionTeams(currentEdition.edition.id);

  return (
    <main>
      <PublicPageHeader
        description="Current edition teams with captains and roster status."
        edition={currentEdition.edition}
        title="Teams"
      />
      <section className="mx-auto grid w-full max-w-7xl gap-4 px-4 py-8 sm:px-6 md:grid-cols-2 lg:grid-cols-3 lg:px-8">
        {teams.ok ? (
          teams.data.length > 0 ? (
            teams.data.map((team) => <TeamCard key={team.id} team={team} />)
          ) : (
            <EmptyTeams />
          )
        ) : (
          <DataUnavailable message="Teams are temporarily unavailable." />
        )}
      </section>
    </main>
  );
}

function TeamCard({ team }: { team: TournamentTeam }) {
  return (
    <article className="rounded-sm border border-white/10 bg-card p-5">
      <p className="font-mono text-xs uppercase text-muted-foreground">
        {team.shortName ?? "Team"}
      </p>
      <h2 className="mt-3 font-heading text-2xl font-semibold uppercase tracking-normal">
        {team.teamName ?? "Team name"}
      </h2>
      <dl className="mt-4 grid gap-3 text-sm">
        <TeamDetail label="Captain" value={team.captain?.name ?? "Captain not assigned"} />
        <TeamDetail label="Roster" value={rosterStatusLabel(team.rosterStatus)} />
      </dl>
    </article>
  );
}

function TeamDetail({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-5 border-b border-white/10 pb-2 last:border-b-0 last:pb-0">
      <dt className="font-mono text-xs uppercase text-muted-foreground">
        {label}
      </dt>
      <dd className="text-right font-medium">{value}</dd>
    </div>
  );
}

function rosterStatusLabel(status?: TournamentTeam["rosterStatus"]) {
  return status === "LOCKED" ? "Locked" : status === "OPEN" ? "Open" : "TBD";
}

function EmptyTeams() {
  return (
    <div className="rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground md:col-span-2 lg:col-span-3">
      Teams have not been added to this edition yet.
    </div>
  );
}
