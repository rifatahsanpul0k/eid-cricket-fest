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
  DraftPickResponse,
  DraftPoolPlayerResponse,
  DraftStateResponse,
  TournamentTeamResponse,
} from "@/lib/api/schema-helpers";
import { getSession } from "@/lib/auth/session";
import { hasOrganizerAccess } from "@/lib/dashboard/roles";
import {
  getDraftPicks,
  getDraftPool,
  getDraftState,
  getEditionTeams,
} from "@/lib/dashboard/team-draft-api";
import {
  draftPickModes,
  draftStatusLabel,
  picksForTeam,
  poolWithoutPickedPlayers,
  rosterStatusLabel,
} from "@/lib/dashboard/team-draft-state";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";
import { cn } from "@/lib/utils";
import { formatDateTime } from "@/lib/utils/format";

export const metadata: Metadata = {
  title: "Draft | Dashboard",
};

type DraftPageProps = {
  searchParams: Promise<{ error?: string }>;
};

export default async function DashboardDraftPage({
  searchParams,
}: DraftPageProps) {
  const [params, session, currentEdition] = await Promise.all([
    searchParams,
    getSession(),
    getCurrentEditionData(),
  ]);

  if (!hasOrganizerAccess(session)) {
    return <Forbidden />;
  }

  if (currentEdition.status !== "ready") {
    return <Unavailable message={currentEdition.message} />;
  }

  const [draft, pool, editionTeams] = await Promise.all([
    getDraftState(currentEdition.edition.id),
    getDraftPool(currentEdition.edition.id),
    getEditionTeams(currentEdition.edition.id),
  ]);
  const picks =
    draft && "ok" in draft && draft.ok && draft.data.id
      ? await getDraftPicks(draft.data.id)
      : undefined;

  return (
    <main className="flex-1">
      <DashboardHeader
        description={`${currentEdition.edition.name} draft control`}
        title="Draft"
      />
      <section className="mx-auto grid w-full max-w-7xl gap-6 px-4 py-8 sm:px-6 lg:px-8">
        {params.error ? (
          <p className="rounded-sm border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
            {params.error}
          </p>
        ) : null}
        {draft && "ok" in draft && !draft.ok ? (
          <Unavailable message={draft.error.detail ?? draft.error.title} />
        ) : null}
        {pool.ok && editionTeams.ok ? (
          <>
            <DraftControl
              draft={draft && "ok" in draft && draft.ok ? draft.data : undefined}
              editionId={currentEdition.edition.id}
              editionTeams={editionTeams.data}
              eligiblePool={pool.data}
              picks={picks?.ok ? picks.data : []}
            />
            <RosterOverview
              editionTeams={editionTeams.data}
              picks={picks?.ok ? picks.data : []}
            />
          </>
        ) : (
          <Unavailable
            message={
              !pool.ok
                ? pool.error.detail ?? pool.error.title
                : !editionTeams.ok
                  ? editionTeams.error.detail ?? editionTeams.error.title
                  : "Draft data is unavailable."
            }
          />
        )}
      </section>
    </main>
  );
}

function DraftControl({
  draft,
  editionId,
  editionTeams,
  eligiblePool,
  picks,
}: {
  draft?: DraftStateResponse;
  editionId: number;
  editionTeams: TournamentTeamResponse[];
  eligiblePool: DraftPoolPlayerResponse[];
  picks: DraftPickResponse[];
}) {
  const selectablePool = poolWithoutPickedPlayers(eligiblePool, picks);
  const allCaptainsAssigned =
    editionTeams.length >= 2 && editionTeams.every((team) => team.captain);

  return (
    <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(320px,420px)]">
      <section className="rounded-sm border border-white/10 bg-card p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="font-mono text-xs uppercase text-muted-foreground">
              Status
            </p>
            <h2 className="mt-2 font-heading text-2xl font-bold uppercase tracking-normal">
              {draftStatusLabel(draft?.status)}
            </h2>
          </div>
          {draft?.status ? (
            <Badge variant="outline">{draft.status}</Badge>
          ) : null}
        </div>
        {draft ? (
          <dl className="mt-5 grid gap-3 text-sm sm:grid-cols-3">
            <Info label="Mode" value={draft.pickMode} />
            <Info
              label="Progress"
              value={`${draft.completedPicks ?? 0}/${draft.totalRequiredPicks ?? 0}`}
            />
            <Info
              label="Current turn"
              value={
                draft.currentTurn?.teamName
                  ? `${draft.currentTurn.teamName}, pick ${draft.currentTurn.pickNumber}`
                  : "Not active"
              }
            />
          </dl>
        ) : (
          <p className="mt-4 text-sm text-muted-foreground">
            Create a draft after teams and captains are ready.
          </p>
        )}
        <DraftLifecycleActions
          allCaptainsAssigned={allCaptainsAssigned}
          draft={draft}
          editionId={editionId}
        />
        <DraftOrder order={draft?.order ?? []} />
        <PickHistory picks={picks} />
      </section>
      <section className="rounded-sm border border-white/10 bg-card p-5">
        <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
          Eligible pool
        </h2>
        <p className="mt-2 text-sm text-muted-foreground">
          Approved, undrafted registrations supplied by the backend.
        </p>
        {draft?.status === "IN_PROGRESS" && draft.id ? (
          <MakePickForm
            draftId={draft.id}
            pool={selectablePool}
            turn={draft.currentTurn}
          />
        ) : null}
        <div className="mt-5 grid gap-2">
          {selectablePool.length > 0 ? (
            selectablePool.map((player) => (
              <div
                className="rounded-sm border border-white/10 bg-background p-3 text-sm"
                key={player.registrationId}
              >
                <p className="font-medium">{player.playerName}</p>
                <p className="mt-1 text-xs text-muted-foreground">
                  Registration #{player.registrationId} · {player.categoryName}
                </p>
              </div>
            ))
          ) : (
            <p className="rounded-sm border border-white/10 bg-background p-3 text-sm text-muted-foreground">
              No eligible players are currently available.
            </p>
          )}
        </div>
      </section>
    </div>
  );
}

function DraftLifecycleActions({
  allCaptainsAssigned,
  draft,
  editionId,
}: {
  allCaptainsAssigned: boolean;
  draft?: DraftStateResponse;
  editionId: number;
}) {
  return (
    <div className="mt-5 flex flex-wrap gap-3">
      {!draft ? (
        <form action="/api/dashboard/draft" method="post">
          <input name="action" type="hidden" value="create" />
          <input name="editionId" type="hidden" value={editionId} />
          <input name="returnTo" type="hidden" value="/dashboard/draft" />
          <select
            className="mr-2 min-h-8 rounded-sm border border-white/10 bg-background px-2 text-sm text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            name="pickMode"
          >
            {draftPickModes.map((mode) => (
              <option key={mode} value={mode}>
                {mode}
              </option>
            ))}
          </select>
          <ReviewSubmitButton>Create draft</ReviewSubmitButton>
        </form>
      ) : null}
      {draft?.status === "PENDING" && draft.id ? (
        <form action="/api/dashboard/draft" method="post">
          <input name="action" type="hidden" value="lottery" />
          <input name="draftId" type="hidden" value={draft.id} />
          <input name="returnTo" type="hidden" value="/dashboard/draft" />
          <ReviewSubmitButton variant="outline">
            {allCaptainsAssigned ? "Generate lottery" : "Generate lottery"}
          </ReviewSubmitButton>
        </form>
      ) : null}
      {draft?.status === "ORDER_GENERATED" && draft.id ? (
        <form action="/api/dashboard/draft" method="post">
          <input name="action" type="hidden" value="start" />
          <input name="draftId" type="hidden" value={draft.id} />
          <input name="returnTo" type="hidden" value="/dashboard/draft" />
          <ReviewSubmitButton>Start draft</ReviewSubmitButton>
        </form>
      ) : null}
      {!allCaptainsAssigned ? (
        <Link
          className={cn(buttonVariants({ variant: "outline" }), "h-8")}
          href="/dashboard/teams"
        >
          Assign captains
        </Link>
      ) : null}
    </div>
  );
}

function MakePickForm({
  draftId,
  pool,
  turn,
}: {
  draftId: number;
  pool: DraftPoolPlayerResponse[];
  turn?: DraftStateResponse["currentTurn"];
}) {
  return (
    <form action="/api/dashboard/draft" className="mt-5 grid gap-3" method="post">
      <input name="action" type="hidden" value="pick" />
      <input name="draftId" type="hidden" value={draftId} />
      <input name="returnTo" type="hidden" value="/dashboard/draft" />
      <label className="grid gap-2 text-sm">
        {turn?.teamName ? `${turn.teamName} pick` : "Player"}
        <select
          className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          disabled={pool.length === 0}
          name="registrationId"
          required
        >
          <option value="">Select player</option>
          {pool.map((player) => (
            <option key={player.registrationId} value={player.registrationId}>
              {player.playerName} - {player.categoryName}
            </option>
          ))}
        </select>
      </label>
      <ReviewSubmitButton>Make pick</ReviewSubmitButton>
    </form>
  );
}

function DraftOrder({
  order,
}: {
  order: NonNullable<DraftStateResponse["order"]>;
}) {
  if (order.length === 0) {
    return (
      <p className="mt-5 rounded-sm border border-white/10 bg-background p-3 text-sm text-muted-foreground">
        Draft order has not been generated.
      </p>
    );
  }

  return (
    <div className="mt-6">
      <h3 className="font-heading text-lg font-bold uppercase tracking-normal">
        Draft order
      </h3>
      <div className="mt-3 grid gap-2 sm:grid-cols-2">
        {order.map((item) => (
          <div
            className="rounded-sm border border-white/10 bg-background p-3 text-sm"
            key={`${item.position}-${item.tournamentTeamId}`}
          >
            <span className="font-mono text-xs text-muted-foreground">
              Position {item.position}
            </span>
            <p className="mt-1 font-medium">{item.teamName}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

function PickHistory({ picks }: { picks: DraftPickResponse[] }) {
  return (
    <div className="mt-6">
      <h3 className="font-heading text-lg font-bold uppercase tracking-normal">
        Pick history
      </h3>
      <div className="mt-3 grid gap-2">
        {picks.length > 0 ? (
          picks.map((pick) => (
            <div
              className="rounded-sm border border-white/10 bg-background p-3 text-sm"
              key={pick.id}
            >
              <p className="font-medium">
                Pick {pick.pickNumber}: {pick.playerName}
              </p>
              <p className="mt-1 text-xs text-muted-foreground">
                {pick.teamName} · Round {pick.roundNumber} ·{" "}
                {formatDateTime(pick.selectedAt)}
              </p>
            </div>
          ))
        ) : (
          <p className="rounded-sm border border-white/10 bg-background p-3 text-sm text-muted-foreground">
            No picks have been made.
          </p>
        )}
      </div>
    </div>
  );
}

function RosterOverview({
  editionTeams,
  picks,
}: {
  editionTeams: TournamentTeamResponse[];
  picks: DraftPickResponse[];
}) {
  return (
    <section className="rounded-sm border border-white/10 bg-card p-5">
      <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
        Rosters
      </h2>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Team</TableHead>
            <TableHead>Captain</TableHead>
            <TableHead>Draft picks</TableHead>
            <TableHead>Status</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {editionTeams.map((team) => {
            const teamPicks = picksForTeam(picks, team.id);

            return (
              <TableRow key={team.id}>
                <TableCell>{team.teamName}</TableCell>
                <TableCell>
                  {team.captain?.name ?? (
                    <span className="text-muted-foreground">Not assigned</span>
                  )}
                </TableCell>
                <TableCell>
                  {teamPicks.length > 0 ? (
                    <ul className="grid gap-1">
                      {teamPicks.map((pick) => (
                        <li key={pick.id}>{pick.playerName}</li>
                      ))}
                    </ul>
                  ) : (
                    <span className="text-muted-foreground">No draft picks</span>
                  )}
                </TableCell>
                <TableCell>{rosterStatusLabel(team.rosterStatus)}</TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </section>
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

function Info({ label, value }: { label: string; value?: number | string }) {
  return (
    <div>
      <dt className="font-mono text-xs uppercase text-muted-foreground">
        {label}
      </dt>
      <dd className="mt-1 font-medium text-foreground">{value ?? "TBD"}</dd>
    </div>
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
