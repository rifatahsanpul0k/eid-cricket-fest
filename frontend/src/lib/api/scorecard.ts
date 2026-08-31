import { createApiClient } from "@/lib/api/client";
import { networkError, problemError, type ApiResult } from "@/lib/api/errors";
import type { components } from "@/lib/api/schema";

export type Scorecard = components["schemas"]["ScorecardResponse"];
export type InningsScorecard = components["schemas"]["InningsScorecard"];
export type BattingRow = components["schemas"]["BattingRow"];
export type BowlingRow = components["schemas"]["BowlingRow"];

export async function getScorecard(
  matchId: number
): Promise<ApiResult<Scorecard>> {
  try {
    const { data, error, response } = await createApiClient().GET(
      "/api/v1/matches/{matchId}/scorecard",
      {
        params: {
          path: {
            matchId,
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
