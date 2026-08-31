import type {
  CreateDraftRequest,
  CreateTeamRequest,
  DraftPickResponse,
  DraftPoolPlayerResponse,
  DraftStateResponse,
  TeamResponse,
  TournamentTeamResponse,
} from "@/lib/api/schema-helpers";
import { backendRequest, jsonInit } from "@/lib/auth/backend";

export async function getPermanentTeams() {
  return backendRequest<TeamResponse[]>("/api/v1/teams");
}

export async function createPermanentTeam(body: CreateTeamRequest) {
  return backendRequest<TeamResponse>(
    "/api/v1/teams",
    jsonInit("POST", body),
    { authenticated: true }
  );
}

export async function getEditionTeams(editionId: number) {
  return backendRequest<TournamentTeamResponse[]>(
    `/api/v1/tournament-editions/${editionId}/teams`
  );
}

export async function addTeamToEdition(editionId: number, teamId: number) {
  return backendRequest<TournamentTeamResponse>(
    `/api/v1/tournament-editions/${editionId}/teams/${teamId}`,
    { method: "POST" },
    { authenticated: true }
  );
}

export async function assignCaptain(
  tournamentTeamId: number,
  registrationId: number
) {
  return backendRequest<TournamentTeamResponse>(
    `/api/v1/tournament-teams/${tournamentTeamId}/captain`,
    jsonInit("PATCH", { registrationId }),
    { authenticated: true }
  );
}

export async function getDraftPool(editionId: number) {
  return backendRequest<DraftPoolPlayerResponse[]>(
    `/api/v1/tournament-editions/${editionId}/draft-pool`,
    {},
    { authenticated: true }
  );
}

export async function getDraftState(editionId: number) {
  const result = await backendRequest<DraftStateResponse>(
    `/api/v1/tournament-editions/${editionId}/draft`
  );

  return result.status === 404 ? undefined : result;
}

export async function createDraft(
  editionId: number,
  pickMode: CreateDraftRequest["pickMode"]
) {
  return backendRequest<DraftStateResponse>(
    `/api/v1/tournament-editions/${editionId}/draft`,
    jsonInit("POST", { pickMode }),
    { authenticated: true }
  );
}

export async function generateDraftLottery(draftId: number) {
  return backendRequest<DraftStateResponse>(
    `/api/v1/drafts/${draftId}/lottery`,
    { method: "POST" },
    { authenticated: true }
  );
}

export async function startDraft(draftId: number) {
  return backendRequest<DraftStateResponse>(
    `/api/v1/drafts/${draftId}/start`,
    { method: "POST" },
    { authenticated: true }
  );
}

export async function getDraftPicks(draftId: number) {
  return backendRequest<DraftPickResponse[]>(`/api/v1/drafts/${draftId}/picks`);
}

export async function makeDraftPick(draftId: number, registrationId: number) {
  return backendRequest<DraftPickResponse>(
    `/api/v1/drafts/${draftId}/picks`,
    jsonInit("POST", { registrationId }),
    { authenticated: true }
  );
}
