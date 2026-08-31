import type {
  AssignScorerRequest,
  CreateVenueRequest,
  DraftPickResponse,
  KnockoutBracketResponse,
  MatchResponse,
  NoResultRequest,
  PageResponseMatchResponse,
  RecordTossRequest,
  ResolveKnockoutMatchRequest,
  ScheduleMatchRequest,
  SubmitPlayingXiRequest,
  UserOptionResponse,
  Venue,
} from "@/lib/api/schema-helpers";
import { backendRequest, jsonInit } from "@/lib/auth/backend";

export type MatchSearch = {
  direction?: "asc" | "desc";
  page?: number;
  size?: number;
  sortBy?: string;
  stage?: MatchResponse["stage"];
  status?: MatchResponse["status"];
  teamId?: number;
};

export async function searchDashboardMatches(
  editionId: number,
  search: MatchSearch
) {
  return backendRequest<PageResponseMatchResponse>(
    `/api/v1/tournament-editions/${editionId}/matches${query(search)}`
  );
}

export async function getDashboardMatch(editionId: number, matchId: number) {
  const matches = await searchDashboardMatches(editionId, {
    page: 0,
    size: 100,
  });

  if (!matches.ok) {
    return matches;
  }

  const match = matches.data.content?.find((item) => item.id === matchId);

  return match
    ? { ok: true as const, data: match, status: 200 }
    : {
        ok: false as const,
        error: {
          title: "Match not found",
          detail: "The match is not part of the current edition.",
          status: 404,
        },
        status: 404,
      };
}

export async function getVenues() {
  return backendRequest<Venue[]>("/api/v1/venues", {}, { authenticated: true });
}

export async function createVenue(body: CreateVenueRequest) {
  return backendRequest<Venue>(
    "/api/v1/venues",
    jsonInit("POST", body),
    { authenticated: true }
  );
}

export async function getScorers() {
  return backendRequest<UserOptionResponse[]>(
    "/api/v1/users/scorers",
    {},
    { authenticated: true }
  );
}

export async function generateRoundRobin(editionId: number, venueId?: number) {
  return backendRequest<MatchResponse[]>(
    `/api/v1/tournament-editions/${editionId}/fixtures/round-robin`,
    jsonInit("POST", { venueId }),
    { authenticated: true }
  );
}

export async function scheduleMatch(matchId: number, body: ScheduleMatchRequest) {
  return backendRequest<MatchResponse>(
    `/api/v1/matches/${matchId}/schedule`,
    jsonInit("PATCH", body),
    { authenticated: true }
  );
}

export async function assignMatchScorer(
  matchId: number,
  body: AssignScorerRequest
) {
  return backendRequest<void>(
    `/api/v1/matches/${matchId}/scorers`,
    jsonInit("POST", body),
    { authenticated: true }
  );
}

export async function submitPlayingXi(
  matchId: number,
  tournamentTeamId: number,
  body: SubmitPlayingXiRequest
) {
  return backendRequest<void>(
    `/api/v1/matches/${matchId}/teams/${tournamentTeamId}/playing-xi`,
    jsonInit("PUT", body),
    { authenticated: true }
  );
}

export async function recordToss(matchId: number, body: RecordTossRequest) {
  return backendRequest<void>(
    `/api/v1/matches/${matchId}/toss`,
    jsonInit("POST", body),
    { authenticated: true }
  );
}

export async function markNoResult(matchId: number, body: NoResultRequest) {
  return backendRequest<void>(
    `/api/v1/matches/${matchId}/no-result`,
    jsonInit("PATCH", body),
    { authenticated: true }
  );
}

export async function resolveKnockoutWinner(
  matchId: number,
  body: ResolveKnockoutMatchRequest
) {
  return backendRequest<void>(
    `/api/v1/matches/${matchId}/knockout-winner`,
    jsonInit("PATCH", body),
    { authenticated: true }
  );
}

export async function getKnockoutBracket(editionId: number) {
  return backendRequest<KnockoutBracketResponse>(
    `/api/v1/tournament-editions/${editionId}/knockout`
  );
}

export async function generateSemiFinals(editionId: number) {
  return backendRequest<KnockoutBracketResponse>(
    `/api/v1/tournament-editions/${editionId}/knockout/semi-finals`,
    { method: "POST" },
    { authenticated: true }
  );
}

export async function getDraftPicksForMatchAdmin(draftId?: number) {
  return draftId
    ? backendRequest<DraftPickResponse[]>(`/api/v1/drafts/${draftId}/picks`)
    : undefined;
}

function query(params: Record<string, number | string | undefined>) {
  const search = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      search.set(key, String(value));
    }
  });

  const value = search.toString();

  return value ? `?${value}` : "";
}
