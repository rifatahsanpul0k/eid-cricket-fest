import type { Metadata } from "next";
import Link from "next/link";
import { ShieldAlertIcon } from "lucide-react";

import { ReviewSubmitButton } from "@/components/dashboard/review-submit-button";
import { TournamentFinalization } from "@/components/dashboard/tournament-finalization";
import { Badge } from "@/components/ui/badge";
import { buttonVariants } from "@/components/ui/button";
import {
  getAwards,
  type AwardPlayerOption,
  type PlayerAward,
} from "@/lib/api/awards";
import { getSession } from "@/lib/auth/session";
import { getDashboardRoleLabel, hasOrganizerAccess } from "@/lib/dashboard/roles";
import { lifecycleActions } from "@/lib/dashboard/tournament-lifecycle";
import { getAwardPlayerOptionsForDashboard } from "@/lib/dashboard/tournament-admin-api";
import {
  getTournamentEditions,
  getTournaments,
  type Tournament,
  type TournamentEdition,
} from "@/lib/api/tournaments";
import { editionStatusLabel } from "@/lib/tournament/select-current-edition";
import { cn } from "@/lib/utils";

export const metadata: Metadata = {
  title: "Tournament Setup | Dashboard",
};

type TournamentPageProps = {
  searchParams: Promise<{ error?: string }>;
};

type TournamentWithId = Tournament & { id: number };
type EditionAwardData = {
  awards: PlayerAward[];
  options: AwardPlayerOption[];
};

export default async function DashboardTournamentPage({
  searchParams,
}: TournamentPageProps) {
  const [params, session, tournaments] = await Promise.all([
    searchParams,
    getSession(),
    getTournaments(),
  ]);

  if (!hasOrganizerAccess(session)) {
    return <Forbidden />;
  }

  if (!tournaments.ok) {
    return <Unavailable message={problemText(tournaments.error)} />;
  }

  const tournament = selectTournament(tournaments.data);
  const editions = tournament
    ? await getTournamentEditions(tournament.id)
    : undefined;

  if (editions && !editions.ok) {
    return <Unavailable message={problemText(editions.error)} />;
  }

  const awardDataByEditionId =
    editions?.ok ? await getEditionAwardData(editions.data) : new Map();

  return (
    <main className="flex-1">
      <DashboardHeader
        description="Create editions, registration settings, and lifecycle status without manual database changes."
        roleLabel={getDashboardRoleLabel(session)}
        title="Tournament Setup"
      />
      <section className="mx-auto grid w-full max-w-7xl gap-6 px-4 py-8 sm:px-6 lg:px-8">
        {params.error ? (
          <p className="rounded-sm border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
            {params.error}
          </p>
        ) : null}

        {!tournament ? (
          <CreateTournamentForm />
        ) : (
          <>
            <TournamentSummary tournament={tournament} />
            {editions?.data.length ? (
              <EditionList
                awardDataByEditionId={awardDataByEditionId}
                editions={editions.data}
                tournamentId={tournament.id}
              />
            ) : (
              <CreateEditionForm tournamentId={tournament.id} />
            )}
          </>
        )}
      </section>
    </main>
  );
}

function CreateTournamentForm() {
  return (
    <form
      action="/api/dashboard/tournament"
      className="grid gap-4 rounded-sm border border-white/10 bg-card p-5 text-sm"
      method="post"
    >
      <input name="action" type="hidden" value="create-tournament" />
      <input name="returnTo" type="hidden" value="/dashboard/tournament" />
      <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
        Create Tournament
      </h2>
      <TextField label="Name" maxLength={150} name="name" required />
      <TextAreaField label="Description" maxLength={5000} name="description" />
      <TextField label="Logo URL" maxLength={2000} name="logoUrl" />
      <ReviewSubmitButton>Create tournament</ReviewSubmitButton>
    </form>
  );
}

function TournamentSummary({
  tournament,
}: {
  tournament: TournamentWithId;
}) {
  return (
    <section className="rounded-sm border border-white/10 bg-card p-5">
      <p className="font-mono text-xs uppercase text-muted-foreground">
        Tournament
      </p>
      <h2 className="mt-3 font-heading text-2xl font-bold uppercase tracking-normal">
        {tournament.name}
      </h2>
      {tournament.description ? (
        <p className="mt-3 text-sm text-muted-foreground">
          {tournament.description}
        </p>
      ) : null}
    </section>
  );
}

function EditionList({
  awardDataByEditionId,
  editions,
  tournamentId,
}: {
  awardDataByEditionId: Map<number, EditionAwardData>;
  editions: TournamentEdition[];
  tournamentId: number;
}) {
  return (
    <section className="grid gap-5">
      <CreateEditionForm tournamentId={tournamentId} />
      {editions.map((edition) =>
        edition.id ? (
          <EditionPanel
            awardData={awardDataByEditionId.get(edition.id)}
            edition={edition}
            key={edition.id}
            tournamentId={tournamentId}
          />
        ) : null
      )}
    </section>
  );
}

function CreateEditionForm({
  tournamentId,
}: {
  tournamentId: number;
}) {
  const defaults = defaultEditionValues();

  return (
    <EditionForm
      action="create-edition"
      defaults={defaults}
      submitLabel="Create edition"
      title="Create Tournament Edition"
      tournamentId={tournamentId}
    />
  );
}

function EditionPanel({
  awardData,
  edition,
  tournamentId,
}: {
  awardData?: EditionAwardData;
  edition: TournamentEdition;
  tournamentId: number;
}) {
  const readOnly = edition.status !== "DRAFT";
  const actions = lifecycleActions(edition.status);

  return (
    <article className="grid gap-4 rounded-sm border border-white/10 bg-card p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-mono text-xs uppercase text-muted-foreground">
            Edition
          </p>
          <h2 className="mt-2 font-heading text-2xl font-bold uppercase tracking-normal">
            {edition.name}
          </h2>
        </div>
        <Badge variant="outline">{editionStatusLabel(edition.status)}</Badge>
      </div>

      <EditionFacts edition={edition} />
      <TournamentFinalization awardData={awardData} edition={edition} />

      {readOnly ? (
        <p className="rounded-sm border border-white/10 bg-background p-3 text-sm text-muted-foreground">
          Configuration is read-only after registration opens.
        </p>
      ) : edition.id ? (
        <EditionForm
          action="update-edition"
          defaults={edition}
          editionId={edition.id}
          submitLabel="Save edition"
          title="Edit Configuration"
          tournamentId={tournamentId}
        />
      ) : null}

      {edition.id && actions.length ? (
        <div className="flex flex-wrap gap-2">
          {actions.map((action) => (
            <form
              action="/api/dashboard/tournament"
              key={action.status}
              method="post"
            >
              <input name="action" type="hidden" value="transition-status" />
              <input name="editionId" type="hidden" value={edition.id} />
              <input name="returnTo" type="hidden" value="/dashboard/tournament" />
              <input name="status" type="hidden" value={action.status} />
              <input name="tournamentId" type="hidden" value={tournamentId} />
              <ReviewSubmitButton>{action.label}</ReviewSubmitButton>
            </form>
          ))}
        </div>
      ) : null}

    </article>
  );
}

function EditionForm({
  action,
  defaults,
  editionId,
  submitLabel,
  title,
  tournamentId,
}: {
  action: "create-edition" | "update-edition";
  defaults: Partial<TournamentEdition>;
  editionId?: number;
  submitLabel: string;
  title: string;
  tournamentId: number;
}) {
  return (
    <form
      action="/api/dashboard/tournament"
      className="grid gap-4 rounded-sm border border-white/10 bg-background p-4 text-sm"
      method="post"
    >
      <input name="action" type="hidden" value={action} />
      {editionId ? <input name="editionId" type="hidden" value={editionId} /> : null}
      <input name="returnTo" type="hidden" value="/dashboard/tournament" />
      <input name="tournamentId" type="hidden" value={tournamentId} />
      <h3 className="font-heading text-xl font-bold uppercase tracking-normal">
        {title}
      </h3>
      <div className="grid gap-4 md:grid-cols-2">
        <TextField
          defaultValue={defaults.name}
          label="Name"
          maxLength={150}
          name="name"
          required
        />
        <TextField
          defaultValue={defaults.startDate}
          label="Start date"
          name="startDate"
          type="date"
        />
        <TextField
          defaultValue={defaults.endDate}
          label="End date"
          name="endDate"
          type="date"
        />
        <TextField
          defaultValue={toDateTimeLocal(defaults.registrationStartAt)}
          label="Registration start"
          name="registrationStartAt"
          type="datetime-local"
        />
        <TextField
          defaultValue={toDateTimeLocal(defaults.registrationEndAt)}
          label="Registration end"
          name="registrationEndAt"
          type="datetime-local"
        />
        <NumberField
          defaultValue={defaults.oversPerInnings}
          label="Overs per innings"
          min={1}
          name="oversPerInnings"
          required
        />
        <NumberField
          defaultValue={defaults.squadSize}
          label="Squad size"
          min={2}
          name="squadSize"
          required
        />
        <NumberField
          defaultValue={defaults.playingXiSize}
          label="Playing XI size"
          min={2}
          name="playingXiSize"
          required
        />
        <NumberField
          defaultValue={defaults.registrationFee}
          label="Registration fee"
          min={0}
          name="registrationFee"
          step="0.01"
        />
        <TextField
          defaultValue={defaults.registrationCurrency ?? "BDT"}
          label="Currency"
          maxLength={3}
          name="registrationCurrency"
          pattern="[A-Z]{3}"
        />
        <NumberField
          defaultValue={defaults.winPoints}
          label="Win points"
          min={0}
          name="winPoints"
          step="0.01"
        />
        <NumberField
          defaultValue={defaults.tiePoints}
          label="Tie points"
          min={0}
          name="tiePoints"
          step="0.01"
        />
        <NumberField
          defaultValue={defaults.noResultPoints}
          label="No-result points"
          min={0}
          name="noResultPoints"
          step="0.01"
        />
        <NumberField
          defaultValue={defaults.lossPoints}
          label="Loss points"
          min={0}
          name="lossPoints"
          step="0.01"
        />
      </div>
      <ReviewSubmitButton>{submitLabel}</ReviewSubmitButton>
    </form>
  );
}

function EditionFacts({
  edition,
}: {
  edition: TournamentEdition;
}) {
  const facts = [
    ["Start", edition.startDate ?? "Not set"],
    ["End", edition.endDate ?? "Not set"],
    ["Registration start", formatDateTime(edition.registrationStartAt)],
    ["Registration end", formatDateTime(edition.registrationEndAt)],
    ["Overs", edition.oversPerInnings],
    ["Squad size", edition.squadSize],
    ["Playing XI", edition.playingXiSize],
    [
      "Fee",
      `${edition.registrationFee ?? 0} ${edition.registrationCurrency ?? "BDT"}`,
    ],
    ["Win points", edition.winPoints],
    ["Tie points", edition.tiePoints],
    ["No-result points", edition.noResultPoints],
    ["Loss points", edition.lossPoints],
  ];

  return (
    <dl className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      {facts.map(([label, value]) => (
        <div
          className="rounded-sm border border-white/10 bg-background p-3"
          key={label}
        >
          <dt className="font-mono text-xs uppercase text-muted-foreground">
            {label}
          </dt>
          <dd className="mt-1 text-sm text-foreground">{value}</dd>
        </div>
      ))}
    </dl>
  );
}

function TextField({
  defaultValue,
  label,
  maxLength,
  name,
  pattern,
  required,
  type = "text",
}: {
  defaultValue?: number | string;
  label: string;
  maxLength?: number;
  name: string;
  pattern?: string;
  required?: boolean;
  type?: string;
}) {
  return (
    <label className="grid gap-2">
      {label}
      <input
        className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        defaultValue={defaultValue ?? ""}
        maxLength={maxLength}
        name={name}
        pattern={pattern}
        required={required}
        type={type}
      />
    </label>
  );
}

function TextAreaField({
  label,
  maxLength,
  name,
}: {
  label: string;
  maxLength?: number;
  name: string;
}) {
  return (
    <label className="grid gap-2">
      {label}
      <textarea
        className="min-h-28 rounded-sm border border-white/10 bg-background px-3 py-2 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        maxLength={maxLength}
        name={name}
      />
    </label>
  );
}

function NumberField({
  defaultValue,
  label,
  min,
  name,
  required,
  step = "1",
}: {
  defaultValue?: number | string;
  label: string;
  min: number;
  name: string;
  required?: boolean;
  step?: string;
}) {
  return (
    <label className="grid gap-2">
      {label}
      <input
        className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
        defaultValue={defaultValue ?? ""}
        min={min}
        name={name}
        required={required}
        step={step}
        type="number"
      />
    </label>
  );
}

function DashboardHeader({
  description,
  roleLabel,
  title,
}: {
  description: string;
  roleLabel: string;
  title: string;
}) {
  return (
    <section className="border-b border-white/10 bg-background">
      <div className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <p className="font-mono text-xs uppercase text-primary">{roleLabel}</p>
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
            Organizer or admin access is required for tournament setup.
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
      <section className="mx-auto grid min-h-[52vh] w-full max-w-2xl place-items-center px-4 py-12 text-center sm:px-6 lg:px-8">
        <div>
          <h1 className="font-heading text-3xl font-bold uppercase tracking-normal">
            Tournament setup unavailable
          </h1>
          <p className="mt-3 text-sm text-muted-foreground">{message}</p>
        </div>
      </section>
    </main>
  );
}

function selectTournament(
  tournaments: Tournament[]
): TournamentWithId | undefined {
  const selectable = tournaments.filter(
    (tournament): tournament is TournamentWithId =>
      tournament.id !== undefined
  );

  return (
    selectable.find((tournament) =>
      tournament.name?.toLowerCase().includes("eid cricket fest")
    ) ?? selectable[0]
  );
}

async function getEditionAwardData(editions: TournamentEdition[]) {
  const completedEditions = editions.filter(
    (edition): edition is TournamentEdition & { id: number } =>
      edition.id !== undefined && edition.status === "COMPLETED"
  );

  const entries = await Promise.all(
    completedEditions.map(async (edition) => {
      const [awards, options] = await Promise.all([
        getAwards(edition.id),
        getAwardPlayerOptionsForDashboard(edition.id),
      ]);

      return [
        edition.id,
        {
          awards: awards.ok ? awards.data : [],
          options: options.ok ? options.data : [],
        },
      ] as const;
    })
  );

  return new Map(entries);
}

function defaultEditionValues(): Partial<TournamentEdition> {
  const now = new Date();
  const registrationEnd = new Date(now);
  registrationEnd.setDate(registrationEnd.getDate() + 14);
  const tournamentEnd = new Date(now);
  tournamentEnd.setDate(tournamentEnd.getDate() + 30);

  return {
    endDate: tournamentEnd.toISOString().slice(0, 10),
    lossPoints: 0,
    name: `Edition ${now.getFullYear()}`,
    noResultPoints: 1,
    oversPerInnings: 5,
    playingXiSize: 11,
    registrationCurrency: "BDT",
    registrationEndAt: registrationEnd.toISOString(),
    registrationFee: 0,
    registrationStartAt: now.toISOString(),
    squadSize: 15,
    startDate: now.toISOString().slice(0, 10),
    tiePoints: 1,
    winPoints: 2,
  };
}

function toDateTimeLocal(value?: string) {
  return value ? value.slice(0, 16) : "";
}

function formatDateTime(value?: string) {
  return value ? new Date(value).toLocaleString() : "Not set";
}

function problemText(error: { detail?: string; title?: string }) {
  return error.detail ?? error.title ?? "Tournament data is unavailable.";
}
