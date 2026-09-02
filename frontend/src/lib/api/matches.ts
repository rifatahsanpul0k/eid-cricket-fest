import { createApiClient } from "@/lib/api/client";
import { networkError, problemError, type ApiResult } from "@/lib/api/errors";
import type { components } from "@/lib/api/schema";

export type Match = components["schemas"]["MatchResponse"];
export type LiveMatch = components["schemas"]["LiveMatchResponse"];
export type LiveCentreMatch =
  components["schemas"]["LiveCentreMatchResponse"];
export type MatchPage = components["schemas"]["PageResponseMatchResponse"];
export type MatchStage = NonNullable<Match["stage"]>;
export type MatchStatus = NonNullable<Match["status"]>;

type MatchListOptions = {
  status?: Match["status"];
  stage?: Match["stage"];
  teamId?: number;
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: "asc" | "desc";
};

export async function getMatches(
  editionId: number,
  options: MatchListOptions = {}
): Promise<ApiResult<Match[]>> {
  const result = await getMatchesPage(editionId, options);

  if (!result.ok) {
    return result;
  }

  return {
    ok: true,
    data: result.data.content ?? [],
  };
}

export async function getMatchesPage(
  editionId: number,
  options: MatchListOptions = {}
): Promise<ApiResult<MatchPage>> {
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
            stage: options.stage,
            teamId: options.teamId,
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
        data,
      };
    }

    if (data && !data.content) {
      return {
        ok: true,
        data: {
          ...data,
          content: [],
        },
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

export async function getLiveCentreMatches(): Promise<
  ApiResult<LiveCentreMatch[]>
> {
  try {
    const { data, error, response } = await createApiClient().GET(
      "/api/v1/matches/live-centre"
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

export async function getFriendlyMatches(): Promise<ApiResult<Match[]>> {
  try {
    const { data, error, response } = await createApiClient().GET(
      "/api/v1/friendly-matches"
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
