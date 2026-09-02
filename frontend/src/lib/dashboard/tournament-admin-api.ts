import type {
  CreateTournamentEditionRequest,
  CreateTournamentRequest,
  TournamentEditionResponse,
  TournamentResponse,
  UpdateTournamentEditionRequest,
  UpdateTournamentEditionStatusRequest,
} from "@/lib/api/schema-helpers";
import type { AwardType } from "@/lib/api/awards";
import { backendRequest, jsonInit } from "@/lib/auth/backend";
import type { AwardPlayerOption } from "@/lib/api/awards";

export async function createTournament(body: CreateTournamentRequest) {
  return backendRequest<TournamentResponse>(
    "/api/v1/tournaments",
    jsonInit("POST", body),
    { authenticated: true }
  );
}

export async function createTournamentEdition(
  tournamentId: number,
  body: CreateTournamentEditionRequest
) {
  return backendRequest<TournamentEditionResponse>(
    `/api/v1/tournaments/${tournamentId}/editions`,
    jsonInit("POST", body),
    { authenticated: true }
  );
}

export async function updateTournamentEdition(
  tournamentId: number,
  editionId: number,
  body: UpdateTournamentEditionRequest
) {
  return backendRequest<TournamentEditionResponse>(
    `/api/v1/tournaments/${tournamentId}/editions/${editionId}`,
    jsonInit("PUT", body),
    { authenticated: true }
  );
}

export async function transitionTournamentEditionStatus(
  tournamentId: number,
  editionId: number,
  body: UpdateTournamentEditionStatusRequest
) {
  return backendRequest<TournamentEditionResponse>(
    `/api/v1/tournaments/${tournamentId}/editions/${editionId}/status`,
    jsonInit("PATCH", body),
    { authenticated: true }
  );
}

export async function assignTournamentAward(
  editionId: number,
  body: {
    awardType: AwardType;
    registrationId: number;
    notes?: string;
    title?: string;
  }
) {
  return backendRequest(
    `/api/v1/tournament-editions/${editionId}/awards`,
    jsonInit("POST", body),
    { authenticated: true }
  );
}

export async function getAwardPlayerOptionsForDashboard(
  editionId: number
) {
  return backendRequest<AwardPlayerOption[]>(
    `/api/v1/tournament-editions/${editionId}/awards/player-options`,
    {},
    { authenticated: true }
  );
}
