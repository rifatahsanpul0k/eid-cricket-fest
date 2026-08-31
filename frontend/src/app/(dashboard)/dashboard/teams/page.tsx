import type { Metadata } from "next";
import Link from "next/link";
import { ShieldAlertIcon } from "lucide-react";

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
import type {
  DraftPoolPlayerResponse,
  TeamResponse,
  TournamentTeamResponse,
} from "@/lib/api/schema-helpers";
import { getSession } from "@/lib/auth/session";
import { hasOrganizerAccess } from "@/lib/dashboard/roles";
import {
  getEditionTeams,
  getDraftPool,
  getPermanentTeams,
} from "@/lib/dashboard/team-draft-api";
import {
  availableEditionTeams,
  rosterStatusLabel,
} from "@/lib/dashboard/team-draft-state";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";
import { cn } from "@/lib/utils";

export const metadata: Metadata = {
  title: "Teams | Dashboard",
};

type TeamsPageProps = {
  searchParams: Promise<{ error?: string }>;
};

export default async function DashboardTeamsPage({
  searchParams,
}: TeamsPageProps) {
  const [params, session, currentEdition, permanentTeams] = await Promise.all([
    searchParams,
    getSession(),
    getCurrentEditionData(),
    getPermanentTeams(),
  ]);

  if (!hasOrganizerAccess(session)) {
    return <Forbidden />;
  }

  if (currentEdition.status !== "ready") {
    return <Unavailable message={currentEdition.message} />;
  }

  const [editionTeams, captainCandidates] = await Promise.all([
    getEditionTeams(currentEdition.edition.id),
    getDraftPool(currentEdition.edition.id),
  ]);

  if (!editionTeams.ok || !permanentTeams.ok) {
    const message = !editionTeams.ok
      ? editionTeams.error.detail ?? editionTeams.error.title
      : !permanentTeams.ok
        ? permanentTeams.error.detail ?? permanentTeams.error.title
        : "Teams are unavailable.";

    return (
      <Unavailable message={message} />
    );
  }

  const approved = captainCandidates.ok ? captainCandidates.data : [];
  const selectable = availableEditionTeams(
    permanentTeams.data,
    editionTeams.data
  );

  return (
    <main className="flex-1">
      <DashboardHeader
        description={`${currentEdition.edition.name} team setup`}
        title="Teams"
      />
      <section className="mx-auto grid w-full max-w-7xl gap-6 px-4 py-8 sm:px-6 lg:px-8">
        {params.error ? (
          <p className="rounded-sm border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
            {params.error}
          </p>
        ) : null}
        <div className="grid gap-4 lg:grid-cols-2">
          <CreateTeamForm />
          <AddEditionTeamForm
            editionId={currentEdition.edition.id}
            teams={selectable}
          />
        </div>
        <EditionTeamsTable
          captainCandidates={approved}
          editionTeams={editionTeams.data}
        />
        <p className="rounded-sm border border-white/10 bg-card p-4 text-sm text-muted-foreground">
          Full roster membership is exposed through draft picks and captain
          assignment in the current API. There is no dedicated roster list
          endpoint yet, so this page does not fabricate extra roster rows.
        </p>
      </section>
    </main>
  );
}

function CreateTeamForm() {
  return (
    <form
      action="/api/dashboard/teams"
      className="grid gap-4 rounded-sm border border-white/10 bg-card p-5 text-sm"
      method="post"
    >
      <input name="action" type="hidden" value="create" />
      <input name="returnTo" type="hidden" value="/dashboard/teams" />
      <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
        Permanent team
      </h2>
      <label className="grid gap-2">
        Name
        <input
          className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          maxLength={120}
          name="name"
          required
        />
      </label>
      <label className="grid gap-2">
        Short name
        <input
          className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          maxLength={20}
          name="shortName"
        />
      </label>
      <ReviewSubmitButton>Create team</ReviewSubmitButton>
    </form>
  );
}

function AddEditionTeamForm({
  editionId,
  teams,
}: {
  editionId: number;
  teams: TeamResponse[];
}) {
  return (
    <form
      action="/api/dashboard/teams"
      className="grid gap-4 rounded-sm border border-white/10 bg-card p-5 text-sm"
      method="post"
    >
      <input name="action" type="hidden" value="add-to-edition" />
      <input name="editionId" type="hidden" value={editionId} />
      <input name="returnTo" type="hidden" value="/dashboard/teams" />
      <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
        Edition team
      </h2>
      <label className="grid gap-2">
        Permanent team
        <select
          className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          disabled={teams.length === 0}
          name="teamId"
          required
        >
          <option value="">Select team</option>
          {teams.map((team) => (
            <option key={team.id} value={team.id}>
              {team.name}
              {team.shortName ? ` (${team.shortName})` : ""}
            </option>
          ))}
        </select>
      </label>
      <ReviewSubmitButton>Add to edition</ReviewSubmitButton>
    </form>
  );
}

function EditionTeamsTable({
  captainCandidates,
  editionTeams,
}: {
  captainCandidates: DraftPoolPlayerResponse[];
  editionTeams: TournamentTeamResponse[];
}) {
  if (editionTeams.length === 0) {
    return (
      <div className="rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground">
        No teams have been added to this edition.
      </div>
    );
  }

  return (
    <div className="rounded-sm border border-white/10 bg-card p-3">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Edition team</TableHead>
            <TableHead>Permanent team ID</TableHead>
            <TableHead>Captain</TableHead>
            <TableHead>Roster</TableHead>
            <TableHead>Assign captain</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {editionTeams.map((team) => (
            <TableRow key={team.id}>
              <TableCell>
                <p className="font-medium">{team.teamName}</p>
                <p className="mt-1 text-xs text-muted-foreground">
                  Tournament team #{team.id}
                </p>
              </TableCell>
              <TableCell>{team.teamId}</TableCell>
              <TableCell>
                {team.captain?.name ? (
                  <>
                    <p>{team.captain.name}</p>
                    <Link
                      className="text-xs text-primary hover:underline"
                      href={`/players/${team.captain.playerId}`}
                    >
                      Player #{team.captain.playerId}
                    </Link>
                  </>
                ) : (
                  <span className="text-muted-foreground">Not assigned</span>
                )}
              </TableCell>
              <TableCell>
                <Badge variant="outline">
                  {rosterStatusLabel(team.rosterStatus)}
                </Badge>
              </TableCell>
              <TableCell>
                <form
                  action="/api/dashboard/teams"
                  className="flex min-w-64 flex-col gap-2"
                  method="post"
                >
                  <input name="action" type="hidden" value="assign-captain" />
                  <input name="returnTo" type="hidden" value="/dashboard/teams" />
                  <input
                    name="tournamentTeamId"
                    type="hidden"
                    value={team.id}
                  />
                  <select
                    className="min-h-10 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                    name="registrationId"
                    required
                  >
                    <option value="">Approved registration</option>
                    {captainCandidates.map((registration) => (
                      <option
                        key={registration.registrationId}
                        value={registration.registrationId}
                      >
                        {registration.playerName} - {registration.categoryName}
                      </option>
                    ))}
                  </select>
                  <ReviewSubmitButton variant="outline">
                    Save captain
                  </ReviewSubmitButton>
                </form>
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

function Forbidden() {
  return (
    <main className="flex-1">
      <section className="mx-auto grid min-h-[52vh] w-full max-w-2xl place-items-center px-4 py-12 text-center sm:px-6 lg:px-8">
        <div>
          <ShieldAlertIcon className="mx-auto size-10 text-destructive" />
          <h1 className="mt-4 font-heading text-3xl font-bold uppercase tracking-normal">
            Dashboard access denied
          </h1>
          <p className="mt-3 text-sm text-muted-foreground">
            Organizer or admin access is required.
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
