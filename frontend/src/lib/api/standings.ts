import { createApiClient } from "@/lib/api/client";
import { networkError, problemError, type ApiResult } from "@/lib/api/errors";
import type { components } from "@/lib/api/schema";

export type Standings = components["schemas"]["StandingsResponse"];
export type StandingRow = components["schemas"]["Row"];

export async function getStandings(
  editionId: number
): Promise<ApiResult<Standings>> {
  try {
    const { data, error, response } = await createApiClient().GET(
      "/api/v1/tournament-editions/{editionId}/standings",
      {
        params: {
          path: {
            editionId,
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
