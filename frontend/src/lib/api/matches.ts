import { createApiClient } from "@/lib/api/client";
import { networkError, problemError, type ApiResult } from "@/lib/api/errors";
import type { components } from "@/lib/api/schema";

export type Match = components["schemas"]["MatchResponse"];
export type LiveMatch = components["schemas"]["LiveMatchResponse"];

export async function getMatches(
  editionId: number,
  options: {
    status?: Match["status"];
    page?: number;
    size?: number;
    sortBy?: string;
    direction?: "asc" | "desc";
  } = {}
): Promise<ApiResult<Match[]>> {
  try {
    const { data, error, response } = await createApiClient().GET(
      "/api/v1/tournament-editions/{editionId}/matches",
      {
        params: {
          path: {
            editionId,
          },
          query: {
            status: options.status,
            page: options.page ?? 0,
            size: options.size ?? 20,
            sortBy: options.sortBy ?? "matchNumber",
            direction: options.direction ?? "asc",
          },
        },
      }
    );

    if (data?.content) {
      return {
        ok: true,
        data: data.content,
      };
    }

    if (data && !data.content) {
      return {
        ok: true,
        data: [],
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

export async function getLiveMatch(
  matchId: number
): Promise<ApiResult<LiveMatch>> {
  try {
    const { data, error, response } = await createApiClient().GET(
      "/api/v1/matches/{matchId}/live",
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
