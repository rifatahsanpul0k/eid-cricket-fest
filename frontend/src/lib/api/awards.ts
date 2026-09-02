import { createApiClient } from "@/lib/api/client";
import { networkError, problemError, type ApiResult } from "@/lib/api/errors";
import type { components } from "@/lib/api/schema";

export type PlayerAward = components["schemas"]["PlayerAwardResponse"];
export type AwardType = NonNullable<PlayerAward["awardType"]>;
export type AwardPlayerOption =
  components["schemas"]["AwardPlayerOptionResponse"];

export async function getAwards(
  editionId: number
): Promise<ApiResult<PlayerAward[]>> {
  try {
    const { data, error, response } = await createApiClient().GET(
      "/api/v1/tournament-editions/{editionId}/awards",
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

export async function getAwardPlayerOptions(
  editionId: number
): Promise<ApiResult<AwardPlayerOption[]>> {
  try {
    const { data, error, response } = await createApiClient().GET(
      "/api/v1/tournament-editions/{editionId}/awards/player-options",
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
