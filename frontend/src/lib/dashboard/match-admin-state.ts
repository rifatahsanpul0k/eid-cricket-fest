import type {
  DraftPickResponse,
  MatchResponse,
  TournamentTeamResponse,
} from "@/lib/api/schema-helpers";
import { parseIntegerParam } from "@/lib/utils/pagination";

export const matchStatuses = [
  "PLANNED",
  "SCHEDULED",
  "READY",
  "TOSS_COMPLETED",
  "LIVE",
  "INNINGS_BREAK",
  "COMPLETED",
  "POSTPONED",
  "ABANDONED",
  "CANCELLED",
] as const;

export const matchStages = ["LEAGUE", "SEMI_FINAL", "FINAL", "OTHER"] as const;

export type RosterCandidate = {
  playerName: string;
  registrationId: number;
};

export type MatchAdminSearch = {
  direction: "asc" | "desc";
  page: number;
  size: number;
  sortBy: string;
  stage?: MatchResponse["stage"];
  status?: MatchResponse["status"];
  teamId?: number;
};

export function parseMatchAdminSearch(params: {
  direction?: string;
  page?: string;
  size?: string;
  sortBy?: string;
  stage?: string;
  status?: string;
  teamId?: string;
}): MatchAdminSearch {
  return {
    direction: params.direction === "desc" ? "desc" : "asc",
    page: parseIntegerParam(params.page, 0, { min: 0, max: Number.MAX_SAFE_INTEGER }),
    size: parseIntegerParam(params.size, 20, { min: 1, max: 100 }),
    sortBy: matchSort(params.sortBy),
    stage: enumValue(params.stage, matchStages),
    status: enumValue(params.status, matchStatuses),
    teamId: optionalInteger(params.teamId),
  };
}

export function isSetupStatus(status?: MatchResponse["status"]) {
  return status === "PLANNED" || status === "SCHEDULED" || status === "READY";
}

export function canRecordToss(status?: MatchResponse["status"]) {
  return status === "READY";
}

export function publicMatchHref(match: MatchResponse) {
  if (!match.id) {
    return undefined;
  }

  if (match.status === "LIVE" || match.status === "INNINGS_BREAK") {
    return `/matches/${match.id}/live`;
  }

  if (match.status === "COMPLETED") {
    return `/matches/${match.id}/scorecard`;
  }

  return undefined;
}

export function rosterCandidatesForTeam({
  picks,
  team,
}: {
  picks: DraftPickResponse[];
  team?: TournamentTeamResponse;
}): RosterCandidate[] {
  const captain = team?.captain
    ? [{
        playerName: team.captain.name,
        registrationId: team.captain.registrationId,
      }]
    : [];
  const drafted = picks
    .filter((pick) => pick.tournamentTeamId === team?.id)
    .map((pick) => ({
      playerName: pick.playerName,
      registrationId: pick.registrationId,
    }));

  return [...captain, ...drafted].filter(
    (candidate): candidate is { playerName: string; registrationId: number } =>
      Boolean(candidate.playerName && candidate.registrationId)
  );
}

export function selectedCountLabel(selected: number, required?: number) {
  return `${selected}/${required ?? 0} selected`;
}

function enumValue<T extends string>(value: string | undefined, values: readonly T[]) {
  return values.includes(value as T) ? (value as T) : undefined;
}

function optionalInteger(value?: string) {
  const parsed = Number.parseInt(value ?? "", 10);

  return Number.isFinite(parsed) ? parsed : undefined;
}

function matchSort(value?: string) {
  return value === "scheduledAt" || value === "stage" || value === "createdAt"
    ? value
    : "matchNumber";
}
