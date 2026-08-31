import type { Metadata } from "next";
import Link from "next/link";
import { ShieldAlertIcon } from "lucide-react";

import { PublicPagination } from "@/components/cricket/public-pagination";
import { Badge } from "@/components/ui/badge";
import { buttonVariants } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { MatchResponse } from "@/lib/api/schema-helpers";
import { getSession } from "@/lib/auth/session";
import { hasOrganizerAccess } from "@/lib/dashboard/roles";
import {
  matchStatuses,
  matchStages,
  parseMatchAdminSearch,
  publicMatchHref,
} from "@/lib/dashboard/match-admin-state";
import { searchDashboardMatches } from "@/lib/dashboard/match-admin-api";
import { getEditionTeams } from "@/lib/dashboard/team-draft-api";
import { matchStageLabel, matchStatusLabel } from "@/lib/cricket/match-labels";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";
import { cn } from "@/lib/utils";
import { formatBangladeshDateTime } from "@/lib/utils/format";

export const metadata: Metadata = {
  title: "Matches | Dashboard",
};

type MatchesPageProps = {
  searchParams: Promise<{
    direction?: string;
    error?: string;
    page?: string;
    size?: string;
    sortBy?: string;
    stage?: string;
    status?: string;
    teamId?: string;
  }>;
};

export default async function DashboardMatchesPage({
  searchParams,
}: MatchesPageProps) {
  const params = await searchParams;
  const search = parseMatchAdminSearch(params);
  const [session, currentEdition] = await Promise.all([
    getSession(),
    getCurrentEditionData(),
  ]);

  if (!hasOrganizerAccess(session)) {
    return <ForbiddenDashboard />;
  }

  if (currentEdition.status !== "ready") {
    return <Unavailable message={currentEdition.message} />;
  }

  const [matches, teams] = await Promise.all([
    searchDashboardMatches(currentEdition.edition.id, search),
    getEditionTeams(currentEdition.edition.id),
  ]);

  return (
    <main className="flex-1">
      <DashboardHeader
        description={`${currentEdition.edition.name} match setup and operations`}
        title="Matches"
      />
      <section className="mx-auto grid w-full max-w-7xl gap-6 px-4 py-8 sm:px-6 lg:px-8">
        {params.error ? (
          <p className="rounded-sm border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
            {params.error}
          </p>
        ) : null}
        <MatchFilter search={search} teams={teams.ok ? teams.data : []} />
        {matches.ok ? (
          <>
            <MatchTable matches={matches.data.content ?? []} />
            <PublicPagination
              basePath="/dashboard/matches"
              hasNext={matches.data.hasNext}
              hasPrevious={matches.data.hasPrevious}
              page={matches.data.page ?? search.page}
              params={{
                direction: search.direction,
                size: String(search.size),
                sortBy: search.sortBy,
                stage: search.stage,
                status: search.status,
                teamId: search.teamId ? String(search.teamId) : undefined,
              }}
              totalPages={matches.data.totalPages}
            />
          </>
        ) : (
          <Unavailable message={matches.error.detail ?? matches.error.title} />
        )}
      </section>
    </main>
  );
}

function MatchFilter({
  search,
  teams,
}: {
  search: ReturnType<typeof parseMatchAdminSearch>;
  teams: { id?: number; teamName?: string }[];
}) {
  return (
    <form
      action="/dashboard/matches"
      className="grid gap-3 rounded-sm border border-white/10 bg-card p-4 md:grid-cols-5"
      method="get"
    >
      <input name="page" type="hidden" value="0" />
      <SelectField label="Stage" name="stage" value={search.stage ?? ""}>
        <option value="">All</option>
        {matchStages.map((stage) => (
          <option key={stage} value={stage}>
            {matchStageLabel(stage)}
          </option>
        ))}
      </SelectField>
      <SelectField label="Status" name="status" value={search.status ?? ""}>
        <option value="">All</option>
        {matchStatuses.map((status) => (
          <option key={status} value={status}>
            {matchStatusLabel(status)}
          </option>
        ))}
      </SelectField>
      <SelectField label="Team" name="teamId" value={search.teamId ?? ""}>
        <option value="">All</option>
        {teams.map((team) => (
          <option key={team.id} value={team.id}>
            {team.teamName}
          </option>
        ))}
      </SelectField>
      <SelectField label="Sort" name="sortBy" value={search.sortBy}>
        <option value="matchNumber">Match number</option>
        <option value="scheduledAt">Scheduled time</option>
        <option value="stage">Stage</option>
      </SelectField>
      <SelectField label="Direction" name="direction" value={search.direction}>
        <option value="asc">Ascending</option>
        <option value="desc">Descending</option>
      </SelectField>
      <button
        className="min-h-11 rounded-sm bg-secondary px-4 text-sm font-semibold text-secondary-foreground transition-colors hover:bg-secondary/85 md:col-start-5"
        type="submit"
      >
        Apply
      </button>
    </form>
  );
}

function SelectField({
  children,
  label,
  name,
  value,
}: {
  children: React.ReactNode;
  label: string;
  name: string;
  value: number | string;
}) {
  return (
    <label className="grid gap-2 text-sm">
      {label}
      <select
        className="min-h-11 rounded-sm border border-white/10 bg-surface px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        defaultValue={value}
        name={name}
      >
        {children}
      </select>
    </label>
  );
}

function MatchTable({ matches }: { matches: MatchResponse[] }) {
  if (matches.length === 0) {
    return (
      <div className="rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground">
        No matches match this view.
      </div>
    );
  }

  return (
    <div className="rounded-sm border border-white/10 bg-card p-3">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Match</TableHead>
            <TableHead>Teams</TableHead>
            <TableHead>Schedule</TableHead>
            <TableHead>Venue</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Open</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {matches.map((match) => (
            <TableRow key={match.id}>
              <TableCell>
                #{match.matchNumber} · {matchStageLabel(match.stage)}
              </TableCell>
              <TableCell>
                {match.teamA?.name ?? "TBD"} vs{" "}
                {match.teamB?.name ?? "TBD"}
              </TableCell>
              <TableCell>{formatBangladeshDateTime(match.scheduledAt)}</TableCell>
              <TableCell>{match.venue?.name ?? "No venue"}</TableCell>
              <TableCell>
                <Badge variant="outline">{matchStatusLabel(match.status)}</Badge>
              </TableCell>
              <TableCell>
                {match.id ? (
                  <div className="flex flex-wrap gap-2">
                    <Link
                      className="text-primary hover:underline"
                      href={`/dashboard/matches/${match.id}`}
                    >
                      Manage
                    </Link>
                    {publicMatchHref(match) ? (
                      <Link
                        className="text-muted-foreground hover:text-primary hover:underline"
                        href={publicMatchHref(match) ?? "#"}
                      >
                        Public
                      </Link>
                    ) : null}
                  </div>
                ) : null}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}

function DashboardHeader({
  description,
  title,
}: {
  description: string;
  title: string;
}) {
  return (
    <section className="border-b border-white/10 bg-background">
      <div className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <p className="font-mono text-xs uppercase text-primary">Organizer</p>
        <h1 className="mt-3 font-heading text-4xl font-bold uppercase tracking-normal">
          {title}
        </h1>
        <p className="mt-3 text-sm text-muted-foreground">{description}</p>
      </div>
    </section>
  );
}

function ForbiddenDashboard() {
  return (
    <main className="flex-1">
      <section className="mx-auto grid min-h-[52vh] w-full max-w-2xl place-items-center px-4 py-12 text-center sm:px-6 lg:px-8">
        <div>
          <ShieldAlertIcon className="mx-auto size-10 text-destructive" />
          <h1 className="mt-4 font-heading text-3xl font-bold uppercase tracking-normal">
            Dashboard access denied
          </h1>
          <p className="mt-3 text-sm text-muted-foreground">
            Organizer or admin access is required for match operations.
          </p>
          <Link
            className={cn(buttonVariants({ variant: "outline" }), "mt-6")}
            href="/account"
          >
            Back to account
          </Link>
        </div>
      </section>
    </main>
  );
}

function Unavailable({ message }: { message: string }) {
  return (
    <main className="flex-1">
      <section className="mx-auto w-full max-w-3xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground">
          {message}
        </div>
      </section>
    </main>
  );
}
