import { createApiClient } from "@/lib/api/client";
import { networkError, problemError, type ApiResult } from "@/lib/api/errors";
import type { components } from "@/lib/api/schema";

export type Player = components["schemas"]["PlayerResponse"];
export type PlayerPage = components["schemas"]["PageResponsePlayerResponse"];
export type PlayerCareer = components["schemas"]["PlayerCareerResponse"];

export type PlayerSort = "name" | "createdAt";
export type SortDirection = "asc" | "desc";

export async function searchPlayers(
  options: {
    category?: string;
    direction?: SortDirection;
    page?: number;
    q?: string;
    size?: number;
    sortBy?: PlayerSort;
  } = {}
): Promise<ApiResult<PlayerPage>> {
  try {
    const { data, error, response } = await createApiClient().GET(
      "/api/v1/players",
      {
        params: {
          query: {
            category: options.category,
            direction: options.direction ?? "asc",
            page: options.page ?? 0,
            q: options.q,
            size: options.size ?? 20,
            sortBy: options.sortBy ?? "name",
          },
        },
      }
    );

    if (data) {
      return {
        ok: true,
        data: {
          ...data,
          content: data.content ?? [],
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

export async function getPlayer(playerId: number): Promise<ApiResult<Player>> {
  try {
    const { data, error, response } = await createApiClient().GET(
      "/api/v1/players/{id}",
      {
        params: {
          path: {
            id: playerId,
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

export async function getPlayerCareer(
  playerId: number
): Promise<ApiResult<PlayerCareer>> {
  try {
    const { data, error, response } = await createApiClient().GET(
      "/api/v1/players/{playerId}/career",
      {
        params: {
          path: {
            playerId,
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
