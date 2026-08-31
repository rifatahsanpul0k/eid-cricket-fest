import type {
  CorrectDeliveryRequest,
  InningsResponse,
  RecordDeliveryRequest,
  ScorerMatchResponse,
  ScorerMatchStateResponse,
  SetBattersRequest,
  SetBowlerRequest,
  StartInningsRequest,
  UndoDeliveryRequest,
} from "@/lib/api/schema-helpers";
import { backendRequest, jsonInit } from "@/lib/auth/backend";

export async function getScorerMatches() {
  return backendRequest<ScorerMatchResponse[]>(
    "/api/v1/scorer/matches",
    {},
    { authenticated: true }
  );
}

export async function getScorerMatchState(matchId: number) {
  return backendRequest<ScorerMatchStateResponse>(
    `/api/v1/scorer/matches/${matchId}`,
    {},
    { authenticated: true }
  );
}

export async function startScorerInnings(
  matchId: number,
  body: StartInningsRequest
) {
  return backendRequest<InningsResponse>(
    `/api/v1/matches/${matchId}/innings`,
    jsonInit("POST", body),
    { authenticated: true }
  );
}

export async function setScorerBatters(
  inningsId: number,
  body: SetBattersRequest
) {
  return backendRequest<InningsResponse>(
    `/api/v1/innings/${inningsId}/batters`,
    jsonInit("PUT", body),
    { authenticated: true }
  );
}

export async function setScorerBowler(
  inningsId: number,
  body: SetBowlerRequest
) {
  return backendRequest<InningsResponse>(
    `/api/v1/innings/${inningsId}/bowler`,
    jsonInit("PUT", body),
    { authenticated: true }
  );
}

export async function recordScorerDelivery(
  inningsId: number,
  body: RecordDeliveryRequest
) {
  return backendRequest<InningsResponse>(
    `/api/v1/innings/${inningsId}/deliveries`,
    jsonInit("POST", body),
    { authenticated: true }
  );
}

export async function undoScorerDelivery(
  inningsId: number,
  body: UndoDeliveryRequest
) {
  return backendRequest<InningsResponse>(
    `/api/v1/innings/${inningsId}/deliveries/undo`,
    jsonInit("POST", body),
    { authenticated: true }
  );
}

export async function correctScorerDelivery(
  deliveryId: number,
  body: CorrectDeliveryRequest
) {
  return backendRequest<InningsResponse>(
    `/api/v1/deliveries/${deliveryId}`,
    jsonInit("PATCH", body),
    { authenticated: true }
  );
}
