import { createApiClient } from "@/lib/api/client";
import { networkError, problemError, type ApiResult } from "@/lib/api/errors";
import type { components } from "@/lib/api/schema";

export type Team = components["schemas"]["TeamResponse"];
export type TournamentTeam = components["schemas"]["TournamentTeamResponse"];

export async function getTeams(): Promise<ApiResult<Team[]>> {
  try {
    const { data, error, response } = await createApiClient().GET(
      "/api/v1/teams"
    );

    if (data) {
      return { ok: true, data };
    }

    return {
      ok: false,
      error: problemError(error, response.status),
    };
  } catch (error) {
    return {
      ok: false,
      error: networkError(error),
    };
  }
}

export async function getEditionTeams(
  editionId: number
): Promise<ApiResult<TournamentTeam[]>> {
  try {
    const { data, error, response } = await createApiClient().GET(
      "/api/v1/tournament-editions/{editionId}/teams",
      {
        params: {
          path: {
            editionId,
          },
        },
      }
    );

    if (data) {
      return { ok: true, data };
    }

    return {
      ok: false,
      error: problemError(error, response.status),
    };
  } catch (error) {
    return {
      ok: false,
      error: networkError(error),
    };
  }
}
