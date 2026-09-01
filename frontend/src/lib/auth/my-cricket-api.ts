import type {
  MyEditionStatisticsResponse,
  MyMatchResponse,
  MyTeamResponse,
} from "@/lib/api/schema-helpers";
import { backendRequest } from "@/lib/auth/backend";

export async function getMyTeam(editionId: number) {
  const result = await backendRequest<MyTeamResponse>(
    `/api/v1/players/me/team?editionId=${editionId}`,
    {},
    { authenticated: true, retryOnUnauthorized: false }
  );

  return result.status === 404 ? undefined : result;
}

export async function getMyMatches(editionId: number) {
  const result = await backendRequest<MyMatchResponse[]>(
    `/api/v1/players/me/matches?editionId=${editionId}`,
    {},
    { authenticated: true, retryOnUnauthorized: false }
  );

  return result.ok ? result.data : [];
}

export async function getMyStatistics(editionId: number) {
  return backendRequest<MyEditionStatisticsResponse>(
    `/api/v1/players/me/statistics?editionId=${editionId}`,
    {},
    { authenticated: true, retryOnUnauthorized: false }
  );
}
