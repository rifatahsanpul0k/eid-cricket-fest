import { createApiClient } from "@/lib/api/client";
import { networkError, problemError, type ApiResult } from "@/lib/api/errors";
import type { components } from "@/lib/api/schema";

export type KnockoutBracket =
  components["schemas"]["KnockoutBracketResponse"];
export type KnockoutMatch = components["schemas"]["MatchInfo"];

export async function getKnockoutBracket(
  editionId: number
): Promise<ApiResult<KnockoutBracket>> {
  try {
    const { data, error, response } = await createApiClient().GET(
      "/api/v1/tournament-editions/{editionId}/knockout",
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
