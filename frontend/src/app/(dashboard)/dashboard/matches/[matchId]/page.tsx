import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ShieldAlertIcon } from "lucide-react";

import { PlayingXiForm } from "@/components/dashboard/playing-xi-form";
import { ReviewSubmitButton } from "@/components/dashboard/review-submit-button";
import { Badge } from "@/components/ui/badge";
import { buttonVariants } from "@/components/ui/button";
import type {
  MatchResponse,
  MatchSetupDetailsResponse,
  TournamentTeamResponse,
  Venue,
} from "@/lib/api/schema-helpers";
import { getSession } from "@/lib/auth/session";
import { matchStageLabel, matchStatusLabel } from "@/lib/cricket/match-labels";
import {
  getDashboardMatch,
  getDraftPicksForMatchAdmin,
  getMatchSetupDetails,
  getScorers,
  getVenues,
} from "@/lib/dashboard/match-admin-api";
import {
  canRecordToss,
  publicMatchHref,
  rosterCandidatesForTeam,
} from "@/lib/dashboard/match-admin-state";
import { hasOrganizerAccess } from "@/lib/dashboard/roles";
import {
  getDraftState,
  getEditionTeams,
} from "@/lib/dashboard/team-draft-api";
import { getCurrentEditionData } from "@/lib/tournament/current-edition";
import { cn } from "@/lib/utils";
import { formatBangladeshDateTime } from "@/lib/utils/format";

export const metadata: Metadata = {
  title: "Match Administration | Dashboard",
};

type MatchAdminPageProps = {
  params: Promise<{ matchId: string }>;
  searchParams: Promise<{ error?: string }>;
};

export default async function DashboardMatchAdminPage({
  params,
  searchParams,
}: MatchAdminPageProps) {
  const [{ matchId }, query, session, currentEdition] = await Promise.all([
    params,
    searchParams,
    getSession(),
    getCurrentEditionData(),
  ]);

  if (!hasOrganizerAccess(session)) {
    return <ForbiddenDashboard />;
  }

  if (currentEdition.status !== "ready") {
    return <Unavailable message={currentEdition.message} />;
  }

  const id = Number(matchId);

  if (!Number.isFinite(id)) {
    notFound();
  }

  const [matchResult, venues, scorers, teams, draft] = await Promise.all([
    getDashboardMatch(currentEdition.edition.id, id),
    getVenues(),
    getScorers(),
    getEditionTeams(currentEdition.edition.id),
    getDraftState(currentEdition.edition.id),
  ]);

  if (!matchResult.ok) {
    if (matchResult.status === 404) {
      notFound();
    }

    return <Unavailable message={matchResult.error.detail ?? matchResult.error.title} />;
  }

  const persistedMatchId = matchResult.data.id ?? id;
  const [draftPicks, setupDetails] = await Promise.all([
    draft?.ok && draft.data.id
      ? getDraftPicksForMatchAdmin(draft.data.id)
      : undefined,
    getMatchSetupDetails(persistedMatchId),
  ]);
  const action = `/api/dashboard/matches/${persistedMatchId}`;
  const returnTo = `/dashboard/matches/${persistedMatchId}`;
  const editionTeams = teams.ok ? teams.data : [];
  const teamA = findEditionTeam(editionTeams, matchResult.data.teamA?.tournamentTeamId);
  const teamB = findEditionTeam(editionTeams, matchResult.data.teamB?.tournamentTeamId);
  const picks = draftPicks?.ok ? draftPicks.data : [];
  const playingXiSize = currentEdition.edition.playingXiSize ?? 11;
  const publicHref = publicMatchHref(matchResult.data);
  const setup = setupDetails.ok ? setupDetails.data : undefined;

  return (
    <main className="flex-1">
      <DashboardHeader
        description={`${currentEdition.edition.name} match setup`}
        title={`Match #${matchResult.data.matchNumber ?? matchResult.data.id}`}
      />
      <section className="mx-auto grid w-full max-w-7xl gap-6 px-4 py-8 sm:px-6 lg:px-8">
        {query.error ? (
          <p className="rounded-sm border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
            {query.error}
          </p>
        ) : null}
        <MatchSummary match={matchResult.data} publicHref={publicHref} />
        <ReadinessPanel match={matchResult.data} />
        <div className="grid gap-6 lg:grid-cols-2">
          <SchedulePanel
            action={action}
            match={matchResult.data}
            returnTo={returnTo}
            venues={venues.ok ? venues.data : []}
          />
          <ScorerPanel
            action={action}
            returnTo={returnTo}
            setup={setup}
            scorers={scorers.ok ? scorers.data : []}
          />
        </div>
        <section className="rounded-sm border border-white/10 bg-card p-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
                Playing XI
              </h2>
              <p className="mt-2 text-sm text-muted-foreground">
                Valid roster members come from captains and completed draft picks.
              </p>
            </div>
            <Badge variant="outline">{playingXiSize} required</Badge>
          </div>
          {draftPicks && !draftPicks.ok ? (
            <p className="mt-4 rounded-sm border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
              {draftPicks.error.detail ?? draftPicks.error.title}
            </p>
          ) : null}
          <div className="mt-5 grid gap-5 lg:grid-cols-2">
            <TeamXiPanel
              action={action}
              playingXiSize={playingXiSize}
              returnTo={returnTo}
              setup={setup?.teamAPlayingXi}
              team={teamA}
              candidates={rosterCandidatesForTeam({ picks, team: teamA })}
            />
            <TeamXiPanel
              action={action}
              playingXiSize={playingXiSize}
              returnTo={returnTo}
              setup={setup?.teamBPlayingXi}
              team={teamB}
              candidates={rosterCandidatesForTeam({ picks, team: teamB })}
            />
          </div>
        </section>
        <div className="grid gap-6 lg:grid-cols-2">
          <TossPanel
            action={action}
            match={matchResult.data}
            returnTo={returnTo}
          />
          <LifecyclePanel
            action={action}
            match={matchResult.data}
            returnTo={returnTo}
          />
        </div>
      </section>
    </main>
  );
}

function MatchSummary({
  match,
  publicHref,
}: {
  match: MatchResponse;
  publicHref?: string;
}) {
  return (
    <section className="rounded-sm border border-white/10 bg-card p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-mono text-xs uppercase text-muted-foreground">
            {matchStageLabel(match.stage)}
          </p>
          <h2 className="mt-2 font-heading text-3xl font-bold uppercase tracking-normal">
            {match.teamA?.name ?? "TBD"} vs {match.teamB?.name ?? "TBD"}
          </h2>
        </div>
        <Badge variant="outline">{matchStatusLabel(match.status)}</Badge>
      </div>
      <dl className="mt-5 grid gap-3 text-sm sm:grid-cols-4">
        <Info label="Scheduled" value={formatBangladeshDateTime(match.scheduledAt)} />
        <Info label="Venue" value={match.venue?.name ?? "No venue"} />
        <Info label="Overs" value={match.oversPerInnings ?? "TBD"} />
        <Info label="Round" value={match.roundNumber ?? "TBD"} />
      </dl>
      <div className="mt-5 flex flex-wrap gap-3">
        <Link
          className={cn(buttonVariants({ variant: "outline" }), "h-9")}
          href="/dashboard/matches"
        >
          Back to matches
        </Link>
        {publicHref ? (
          <Link className={cn(buttonVariants(), "h-9")} href={publicHref}>
            Public view
          </Link>
        ) : null}
      </div>
    </section>
  );
}

function ReadinessPanel({ match }: { match: MatchResponse }) {
  const playingXisSubmitted =
    Boolean(match.teamAPlayingXiSubmitted) &&
    Boolean(match.teamBPlayingXiSubmitted);

  return (
    <section className="rounded-sm border border-white/10 bg-card p-5">
      <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
        Readiness
      </h2>
      <div className="mt-4 grid gap-2 text-sm sm:grid-cols-5">
        <ReadinessItem done={Boolean(match.venue)} label="Venue" />
        <ReadinessItem done={Boolean(match.scheduledAt)} label="Schedule" />
        <ReadinessItem done={Boolean(match.scorerAssigned)} label="Scorer" />
        <ReadinessItem done={playingXisSubmitted} label="Playing XIs" />
        <ReadinessItem done={Boolean(match.tossCompleted)} label="Toss" />
      </div>
      <p className="mt-4 text-sm text-muted-foreground">
        Readiness is based on saved match setup: venue, schedule, scorer, both
        Playing XIs, and toss.
      </p>
    </section>
  );
}

function ReadinessItem({ done, label }: { done: boolean; label: string }) {
  return (
    <div className="rounded-sm border border-white/10 bg-background p-3">
      <p className="font-medium">{label}</p>
      <p className={done ? "mt-1 text-primary" : "mt-1 text-muted-foreground"}>
        {done ? "Complete" : "Pending"}
      </p>
    </div>
  );
}

function SchedulePanel({
  action,
  match,
  returnTo,
  venues,
}: {
  action: string;
  match: MatchResponse;
  returnTo: string;
  venues: Venue[];
}) {
  return (
    <section className="rounded-sm border border-white/10 bg-card p-5">
      <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
        Schedule / Venue
      </h2>
      <form action={action} className="mt-4 grid gap-3" method="post">
        <input name="action" type="hidden" value="schedule" />
        <input name="returnTo" type="hidden" value={returnTo} />
        <label className="grid gap-2 text-sm">
          Time
          <input
            className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            defaultValue={toDhakaDateTimeLocal(match.scheduledAt)}
            name="scheduledAt"
            required
            type="datetime-local"
          />
        </label>
        <label className="grid gap-2 text-sm">
          Venue
          <select
            className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            defaultValue={match.venue?.id ?? ""}
            disabled={venues.length === 0}
            name="venueId"
            required
          >
            <option value="">Select venue</option>
            {venues.map((venue) => (
              <option key={venue.id} value={venue.id}>
                {venue.name}
              </option>
            ))}
          </select>
        </label>
        <ReviewSubmitButton>Save schedule</ReviewSubmitButton>
      </form>
    </section>
  );
}

function ScorerPanel({
  action,
  returnTo,
  setup,
  scorers,
}: {
  action: string;
  returnTo: string;
  setup?: MatchSetupDetailsResponse;
  scorers: { displayName?: string; email?: string; id?: number }[];
}) {
  const selectedScorer =
    setup?.scorers?.find((scorer) => scorer.primary) ?? setup?.scorers?.[0];
  const scorerOptions = mergeScorerOptions(scorers, setup?.scorers ?? []);

  return (
    <section className="rounded-sm border border-white/10 bg-card p-5">
      <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
        Scorer
      </h2>
      <form action={action} className="mt-4 grid gap-3" method="post">
        <input name="action" type="hidden" value="scorer" />
        <input name="returnTo" type="hidden" value={returnTo} />
        <label className="grid gap-2 text-sm">
          User
          <select
            className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            defaultValue={selectedScorer?.userId ?? ""}
            disabled={scorerOptions.length === 0}
            name="scorerUserId"
            required
          >
            <option value="">Select scorer</option>
            {scorerOptions.map((scorer) => (
              <option key={scorer.id} value={scorer.id}>
                {scorer.displayName ?? scorer.email ?? `User ${scorer.id}`}
              </option>
            ))}
          </select>
        </label>
        <label className="flex items-center gap-2 text-sm">
          <input
            defaultChecked={selectedScorer?.primary ?? true}
            name="primary"
            type="checkbox"
          />
          Primary scorer
        </label>
        <ReviewSubmitButton>Assign scorer</ReviewSubmitButton>
      </form>
      <p className="mt-3 text-sm text-muted-foreground">
        The backend validates scorer role membership before assignment.
      </p>
    </section>
  );
}

function TeamXiPanel({
  action,
  candidates,
  playingXiSize,
  returnTo,
  setup,
  team,
}: {
  action: string;
  candidates: ReturnType<typeof rosterCandidatesForTeam>;
  playingXiSize: number;
  returnTo: string;
  setup?: MatchSetupDetailsResponse["teamAPlayingXi"];
  team?: TournamentTeamResponse;
}) {
  if (!team?.id) {
    return (
      <div className="rounded-sm border border-white/10 bg-background p-4 text-sm text-muted-foreground">
        Team has not been assigned to this match.
      </div>
    );
  }

  if (candidates.length < playingXiSize) {
    return (
      <div className="rounded-sm border border-white/10 bg-background p-4 text-sm text-muted-foreground">
        {team.teamName ?? "Team"} has {candidates.length}/{playingXiSize} roster
        members available for XI submission.
      </div>
    );
  }

  return (
    <PlayingXiForm
      action={action}
      candidates={candidates}
      initialRegistrationIds={setup?.registrationIds}
      initialWicketkeeperRegistrationId={
        setup?.wicketkeeperRegistrationId ?? undefined
      }
      playingXiSize={playingXiSize}
      returnTo={returnTo}
      teamName={team.teamName ?? `Team ${team.id}`}
      tournamentTeamId={team.id}
    />
  );
}

function TossPanel({
  action,
  match,
  returnTo,
}: {
  action: string;
  match: MatchResponse;
  returnTo: string;
}) {
  const disabled =
    !canRecordToss(match.status) ||
    !match.teamA?.tournamentTeamId ||
    !match.teamB?.tournamentTeamId;

  if (disabled) {
    return (
      <section className="rounded-sm border border-white/10 bg-card p-5">
        <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
          Toss
        </h2>
        <p className="mt-3 text-sm text-muted-foreground">
          Toss can be recorded only when both teams are assigned and the backend
          status is ready.
        </p>
      </section>
    );
  }

  return (
    <section className="rounded-sm border border-white/10 bg-card p-5">
      <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
        Toss
      </h2>
      <form action={action} className="mt-4 grid gap-3" method="post">
        <input name="action" type="hidden" value="toss" />
        <input name="returnTo" type="hidden" value={returnTo} />
        <label className="grid gap-2 text-sm">
          Winner
          <select
            className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            name="winnerTournamentTeamId"
            required
          >
            <option value="">Select winner</option>
            <option value={match.teamA?.tournamentTeamId}>
              {match.teamA?.name ?? "Team A"}
            </option>
            <option value={match.teamB?.tournamentTeamId}>
              {match.teamB?.name ?? "Team B"}
            </option>
          </select>
        </label>
        <label className="grid gap-2 text-sm">
          Decision
          <select
            className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            name="decision"
            required
          >
            <option value="BAT">Bat</option>
            <option value="BOWL">Bowl</option>
          </select>
        </label>
        <ReviewSubmitButton>Record toss</ReviewSubmitButton>
      </form>
    </section>
  );
}

function LifecyclePanel({
  action,
  match,
  returnTo,
}: {
  action: string;
  match: MatchResponse;
  returnTo: string;
}) {
  const isKnockout = match.stage === "SEMI_FINAL" || match.stage === "FINAL";

  return (
    <section className="rounded-sm border border-white/10 bg-card p-5">
      <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
        Administrative Result
      </h2>
      <form action={action} className="mt-4 grid gap-3" method="post">
        <input name="action" type="hidden" value="no-result" />
        <input name="returnTo" type="hidden" value={returnTo} />
        <label className="grid gap-2 text-sm">
          No-result reason
          <textarea
            className="min-h-24 rounded-sm border border-white/10 bg-background px-3 py-2 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
            name="reason"
            required
          />
        </label>
        <ReviewSubmitButton variant="outline">Mark no-result</ReviewSubmitButton>
      </form>
      {isKnockout ? (
        <form action={action} className="mt-6 grid gap-3" method="post">
          <input name="action" type="hidden" value="knockout-winner" />
          <input name="returnTo" type="hidden" value={returnTo} />
          <label className="grid gap-2 text-sm">
            Winner
            <select
              className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
              name="winnerTournamentTeamId"
              required
            >
              <option value="">Select winner</option>
              <option value={match.teamA?.tournamentTeamId}>
                {match.teamA?.name ?? "Team A"}
              </option>
              <option value={match.teamB?.tournamentTeamId}>
                {match.teamB?.name ?? "Team B"}
              </option>
            </select>
          </label>
          <label className="grid gap-2 text-sm">
            Resolution
            <select
              className="min-h-11 rounded-sm border border-white/10 bg-background px-3 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
              name="resolutionType"
            >
              <option value="TIEBREAKER">Tiebreaker</option>
              <option value="FORFEIT">Forfeit</option>
            </select>
          </label>
          <label className="grid gap-2 text-sm">
            Reason
            <textarea
              className="min-h-24 rounded-sm border border-white/10 bg-background px-3 py-2 text-foreground outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
              name="reason"
              required
            />
          </label>
          <ReviewSubmitButton>Resolve knockout winner</ReviewSubmitButton>
        </form>
      ) : (
        <p className="mt-4 text-sm text-muted-foreground">
          Tiebreaker and forfeit winner resolution is available only for
          knockout matches.
        </p>
      )}
    </section>
  );
}

function Info({ label, value }: { label: string; value?: number | string }) {
  return (
    <div>
      <dt className="font-mono text-xs uppercase text-muted-foreground">
        {label}
      </dt>
      <dd className="mt-1 font-medium">{value ?? "TBD"}</dd>
    </div>
  );
}

function findEditionTeam(
  teams: TournamentTeamResponse[],
  tournamentTeamId?: number
) {
  return teams.find((team) => team.id === tournamentTeamId);
}

function mergeScorerOptions(
  scorers: { displayName?: string; email?: string; id?: number }[],
  savedScorers: NonNullable<MatchSetupDetailsResponse["scorers"]>
) {
  const byId = new Map<number, { displayName?: string; email?: string; id?: number }>();

  for (const scorer of scorers) {
    if (scorer.id !== undefined) {
      byId.set(scorer.id, scorer);
    }
  }

  for (const scorer of savedScorers) {
    if (scorer.userId !== undefined && !byId.has(scorer.userId)) {
      byId.set(scorer.userId, {
        displayName: scorer.displayName,
        email: scorer.email,
        id: scorer.userId,
      });
    }
  }

  return Array.from(byId.values());
}

function toDhakaDateTimeLocal(value?: string) {
  if (!value) {
    return "";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "";
  }

  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Dhaka",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).formatToParts(date);
  const part = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((item) => item.type === type)?.value ?? "";

  return `${part("year")}-${part("month")}-${part("day")}T${part("hour")}:${part("minute")}`;
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
