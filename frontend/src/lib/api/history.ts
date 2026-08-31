import { createApiClient } from "@/lib/api/client";
import { networkError, problemError, type ApiResult } from "@/lib/api/errors";
import type { components } from "@/lib/api/schema";

export type TournamentHistory = components["schemas"]["TournamentHistoryResponse"];
export type HistoryEdition = components["schemas"]["Edition"];

export async function getTournamentHistory(
  tournamentId: number
): Promise<ApiResult<TournamentHistory>> {
  try {
    const { data, error, response } = await createApiClient().GET(
      "/api/v1/tournaments/{tournamentId}/history",
      {
        params: {
          path: {
            tournamentId,
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
