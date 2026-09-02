import type { Metadata } from "next";
import Link from "next/link";
import { ShieldAlertIcon } from "lucide-react";

import { PublicPagination } from "@/components/cricket/public-pagination";
import { ReviewSubmitButton } from "@/components/dashboard/review-submit-button";
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
import {
  getKnockoutBracket,
  getVenues,
  searchDashboardMatches,
} from "@/lib/dashboard/match-admin-api";
import {
  matchStatuses,
  matchStages,
  parseMatchAdminSearch,
  publicMatchHref,
} from "@/lib/dashboard/match-admin-state";
import { getSession } from "@/lib/auth/session";
import { hasOrganizerAccess } from "@/lib/dashboard/roles";
import { getEditionTeams } from "@/lib/dashboard/team-draft-api";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";
import { matchStageLabel, matchStatusLabel } from "@/lib/cricket/match-labels";
import { cn } from "@/lib/utils";
import { formatBangladeshDateTime } from "@/lib/utils/format";

export const metadata: Metadata = {
  title: "Fixtures | Dashboard",
};

type FixturesPageProps = {
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

export default async function DashboardFixturesPage({
  searchParams,
}: FixturesPageProps) {
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

  const [matches, venues, bracket, teams] = await Promise.all([
    searchDashboardMatches(currentEdition.edition.id, search),
    getVenues(),
    getKnockoutBracket(currentEdition.edition.id),
    getEditionTeams(currentEdition.edition.id),
  ]);

  return (
    <main className="flex-1">
      <DashboardHeader
        description={`${currentEdition.edition.name} fixture operations`}
        title="Fixtures"
      />
      <section className="mx-auto grid w-full max-w-7xl gap-6 px-4 py-8 sm:px-6 lg:px-8">
        {params.error ? (
          <p className="rounded-sm border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
            {params.error}
          </p>
        ) : null}
        <div className="grid gap-4 lg:grid-cols-2">
          <VenueForm />
          <GenerateFixtureForm
            editionId={currentEdition.edition.id}
            venues={venues.ok ? venues.data : []}
          />
        </div>
        <KnockoutPanel
          bracket={bracket.ok ? bracket.data : undefined}
          editionId={currentEdition.edition.id}
        />
        <FixtureFilter
          search={search}
          teams={teams.ok ? teams.data : []}
        />
        {matches.ok ? (
          <>
            <FixtureTable matches={matches.data.content ?? []} />
            <PublicPagination
              basePath="/dashboard/fixtures"
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

function VenueForm() {
  return (
    <form
      action="/api/dashboard/fixtures"
      className="grid gap-4 rounded-sm border border-white/10 bg-card p-5 text-sm"
      method="post"
    >
      <input name="action" type="hidden" value="venue" />
      <input name="returnTo" type="hidden" value="/dashboard/fixtures" />
      <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
        Venue
      </h2>
      <label className="grid gap-2">
        Name
        <input
          className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          maxLength={150}
          name="name"
          required
        />
      </label>
      <label className="grid gap-2">
        Address
        <input
          className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          maxLength={1000}
          name="address"
        />
      </label>
      <ReviewSubmitButton>Create venue</ReviewSubmitButton>
    </form>
  );
}

function GenerateFixtureForm({
  editionId,
  venues,
}: {
  editionId: number;
  venues: { id?: number; name?: string }[];
}) {
  return (
    <form
      action="/api/dashboard/fixtures"
      className="grid gap-4 rounded-sm border border-white/10 bg-card p-5 text-sm"
      method="post"
    >
      <input name="action" type="hidden" value="round-robin" />
      <input name="editionId" type="hidden" value={editionId} />
      <input name="returnTo" type="hidden" value="/dashboard/fixtures" />
      <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
        League fixtures
      </h2>
      <label className="grid gap-2">
        Venue
        <select
          className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          name="venueId"
        >
          <option value="">No default venue</option>
          {venues.map((venue) => (
            <option key={venue.id} value={venue.id}>
              {venue.name}
            </option>
          ))}
        </select>
      </label>
      <ReviewSubmitButton>Generate round robin</ReviewSubmitButton>
    </form>
  );
}

function KnockoutPanel({
  bracket,
  editionId,
}: {
  bracket?: { semiFinals?: { matchId?: number; teamA?: { teamName?: string }; teamB?: { teamName?: string }; status?: string }[]; finalMatch?: { matchId?: number; status?: string } };
  editionId: number;
}) {
  return (
    <section className="rounded-sm border border-white/10 bg-card p-5 text-sm">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
          Knockout
        </h2>
        <form action="/api/dashboard/fixtures" method="post">
          <input name="action" type="hidden" value="knockout" />
          <input name="editionId" type="hidden" value={editionId} />
          <input name="returnTo" type="hidden" value="/dashboard/fixtures" />
          <ReviewSubmitButton variant="outline">
            Generate semi-finals
          </ReviewSubmitButton>
        </form>
      </div>
      <div className="mt-4 grid gap-3 md:grid-cols-3">
        {(bracket?.semiFinals ?? []).map((match) => (
          <div
            className="rounded-sm border border-white/10 bg-background p-3"
            key={match.matchId}
          >
            <p className="font-medium">
              {match.teamA?.teamName ?? "TBD"} vs {match.teamB?.teamName ?? "TBD"}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">
              Semi-final #{match.matchId} · {match.status}
            </p>
          </div>
        ))}
        {bracket?.finalMatch ? (
          <div className="rounded-sm border border-white/10 bg-background p-3">
            <p className="font-medium">Final</p>
            <p className="mt-1 text-xs text-muted-foreground">
              Match #{bracket.finalMatch.matchId} · {bracket.finalMatch.status}
            </p>
          </div>
        ) : null}
      </div>
    </section>
  );
}

function FixtureFilter({
  search,
  teams,
}: {
  search: ReturnType<typeof parseMatchAdminSearch>;
  teams: { id?: number; teamName?: string }[];
}) {
  return (
    <form
      action="/dashboard/fixtures"
      className="grid gap-3 rounded-sm border border-white/10 bg-card p-4 md:grid-cols-5"
      method="get"
    >
      <input name="page" type="hidden" value="0" />
      <label className="grid gap-2 text-sm">
        Stage
        <select
          className="min-h-11 rounded-sm border border-white/10 bg-surface px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          defaultValue={search.stage ?? ""}
          name="stage"
        >
          <option value="">All</option>
          {matchStages.map((stage) => (
            <option key={stage} value={stage}>
              {matchStageLabel(stage)}
            </option>
          ))}
        </select>
      </label>
      <label className="grid gap-2 text-sm">
        Status
        <select
          className="min-h-11 rounded-sm border border-white/10 bg-surface px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          defaultValue={search.status ?? ""}
          name="status"
        >
          <option value="">All</option>
          {matchStatuses.map((status) => (
            <option key={status} value={status}>
              {matchStatusLabel(status)}
            </option>
          ))}
        </select>
      </label>
      <label className="grid gap-2 text-sm">
        Team
        <select
          className="min-h-11 rounded-sm border border-white/10 bg-surface px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          defaultValue={search.teamId ?? ""}
          name="teamId"
        >
          <option value="">All</option>
          {teams.map((team) => (
            <option key={team.id} value={team.id}>
              {team.teamName}
            </option>
          ))}
        </select>
      </label>
      <label className="grid gap-2 text-sm">
        Sort
        <select
          className="min-h-11 rounded-sm border border-white/10 bg-surface px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          defaultValue={search.sortBy}
          name="sortBy"
        >
          <option value="matchNumber">Match number</option>
          <option value="scheduledAt">Scheduled time</option>
          <option value="stage">Stage</option>
        </select>
      </label>
      <label className="grid gap-2 text-sm">
        Direction
        <select
          className="min-h-11 rounded-sm border border-white/10 bg-surface px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          defaultValue={search.direction}
          name="direction"
        >
          <option value="asc">Ascending</option>
          <option value="desc">Descending</option>
        </select>
      </label>
      <button
        className="min-h-11 self-end rounded-sm bg-secondary px-4 text-sm font-semibold text-secondary-foreground transition-colors hover:bg-secondary/85"
        type="submit"
      >
        Apply
      </button>
    </form>
  );
}

function FixtureTable({ matches }: { matches: { id?: number; matchNumber?: number; stage?: "LEAGUE" | "SEMI_FINAL" | "FINAL" | "OTHER"; status?: "PLANNED" | "SCHEDULED" | "READY" | "TOSS_COMPLETED" | "LIVE" | "INNINGS_BREAK" | "SUSPENDED" | "COMPLETED" | "POSTPONED" | "ABANDONED" | "CANCELLED"; teamA?: { name?: string }; teamB?: { name?: string }; venue?: { name?: string }; scheduledAt?: string }[] }) {
  if (matches.length === 0) {
    return (
      <div className="rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground">
        No fixtures match this view.
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
                {match.teamA?.name ?? "TBD"} vs {match.teamB?.name ?? "TBD"}
              </TableCell>
              <TableCell>
                {formatBangladeshDateTime(match.scheduledAt)}
                <p className="mt-1 text-xs text-muted-foreground">
                  {match.venue?.name ?? "No venue"}
                </p>
              </TableCell>
              <TableCell>
                <Badge variant="outline">{matchStatusLabel(match.status)}</Badge>
              </TableCell>
              <TableCell>
                {match.id ? (
                  <Link
                    className="text-primary hover:underline"
                    href={publicMatchHref(match) ?? `/dashboard/matches/${match.id}`}
                  >
                    {publicMatchHref(match) ? "Public" : "Manage"}
                  </Link>
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
