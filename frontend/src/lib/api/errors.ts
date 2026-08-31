export type ApiResult<T> =
  | {
      ok: true;
      data: T;
    }
  | {
      ok: false;
      error: ApiError;
    };

export type ApiError = {
  kind: "problem" | "network" | "unexpected";
  title: string;
  detail?: string;
  status?: number;
};

export function problemError(error: unknown, status?: number): ApiError {
  if (isProblemLike(error)) {
    return {
      kind: "problem",
      title: error.title,
      detail: error.detail,
      status: error.status ?? status,
    };
  }

  return {
    kind: "unexpected",
    title: "Unexpected API response",
    status,
  };
}

export function networkError(error: unknown): ApiError {
  return {
    kind: "network",
    title: "Backend unavailable",
    detail:
      error instanceof Error
        ? error.message
        : "The backend could not be reached.",
  };
}

function isProblemLike(
  value: unknown
): value is {
  title: string;
  detail?: string;
  status?: number;
} {
  return (
    typeof value === "object" &&
    value !== null &&
    "title" in value &&
    typeof value.title === "string"
  );
}
