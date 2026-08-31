import { createApiClient } from "@/lib/api/client";
import { networkError, problemError, type ApiResult } from "@/lib/api/errors";
import type { components } from "@/lib/api/schema";

export type Tournament = components["schemas"]["TournamentResponse"];
export type TournamentEdition =
  components["schemas"]["TournamentEditionResponse"];

export async function getTournaments(): Promise<ApiResult<Tournament[]>> {
  try {
    const { data, error, response } = await createApiClient().GET(
      "/api/v1/tournaments"
    );

    if (data) {
      return {
        ok: true,
        data,
      };
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

export async function getTournamentEditions(
  tournamentId: number
): Promise<ApiResult<TournamentEdition[]>> {
  try {
    const { data, error, response } = await createApiClient().GET(
      "/api/v1/tournaments/{tournamentId}/editions",
      {
        params: {
          path: {
            tournamentId,
          },
        },
      }
    );

    if (data) {
      return {
        ok: true,
        data,
      };
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
