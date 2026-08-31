import { createApiClient } from "@/lib/api/client";
import { networkError, problemError, type ApiResult } from "@/lib/api/errors";
import type { components } from "@/lib/api/schema";

export type TournamentStatistics =
  components["schemas"]["TournamentStatisticsResponse"];
export type BattingLeader = components["schemas"]["BattingLeader"];
export type BowlingLeader = components["schemas"]["BowlingLeader"];

export async function getStatistics(
  editionId: number
): Promise<ApiResult<TournamentStatistics>> {
  try {
    const { data, error, response } = await createApiClient().GET(
      "/api/v1/tournament-editions/{editionId}/statistics",
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
