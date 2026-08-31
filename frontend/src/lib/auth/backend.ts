import { cookies } from "next/headers";

import { getApiBaseUrl } from "@/lib/api/env";
import {
  ACCESS_TOKEN_COOKIE,
  REFRESH_TOKEN_COOKIE,
  accessTokenCookieOptions,
  clearAuthCookieOptions,
  refreshTokenCookieOptions,
} from "@/lib/auth/cookies";
import { createRefreshCoordinator, type AuthResponse } from "@/lib/auth/refresh";
import type { components } from "@/lib/api/schema";

export type BackendProblem = {
  title: string;
  detail?: string;
  status?: number;
  errors?: { field?: string; message?: string }[];
};

export type BackendResult<T> =
  | { ok: true; data: T; status: number }
  | { ok: false; error: BackendProblem; status: number };

export type RegisterBody = components["schemas"]["RegisterRequest"];
export type LoginBody = components["schemas"]["LoginRequest"];
export type CreatePlayerBody = components["schemas"]["CreatePlayerRequest"];
export type CreateRegistrationBody =
  components["schemas"]["CreateRegistrationRequest"];
export type SubmitPaymentBody = components["schemas"]["SubmitPaymentRequest"];

const refreshOnce = createRefreshCoordinator(refreshSession);

export async function backendRequest<T>(
  path: string,
  init: RequestInit = {},
  options: { authenticated?: boolean; retryOnUnauthorized?: boolean } = {}
): Promise<BackendResult<T>> {
  const authenticated = options.authenticated ?? false;
  const retryOnUnauthorized = options.retryOnUnauthorized ?? authenticated;
  const store = await cookies();
  const accessToken = store.get(ACCESS_TOKEN_COOKIE)?.value;

  const first = await rawBackendRequest<T>(path, init, accessToken);

  if (!authenticated || first.status !== 401 || !retryOnUnauthorized) {
    return first;
  }

  const refreshToken = store.get(REFRESH_TOKEN_COOKIE)?.value;

  if (!refreshToken) {
    clearAuthCookies(store);
    return first;
  }

  const refreshed = await refreshOnce(refreshToken);

  if (!refreshed?.accessToken || !refreshed.refreshToken) {
    clearAuthCookies(store);
    return first;
  }

  setAuthCookies(store, refreshed);

  return rawBackendRequest<T>(
    path,
    init,
    refreshed.accessToken
  );
}

export async function login(body: LoginBody) {
  return backendRequest<AuthResponse>("/api/v1/auth/login", jsonInit("POST", body));
}

export async function register(body: RegisterBody) {
  return backendRequest<AuthResponse>(
    "/api/v1/auth/register",
    jsonInit("POST", body)
  );
}

export async function logout(refreshToken: string) {
  await backendRequest<void>(
    "/api/v1/auth/logout",
    jsonInit("POST", { refreshToken })
  );
}

export function jsonInit(method: string, body: unknown): RequestInit {
  return {
    method,
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  };
}

export function setAuthCookies(
  store: Awaited<ReturnType<typeof cookies>>,
  auth: AuthResponse
) {
  if (auth.accessToken) {
    store.set(
      ACCESS_TOKEN_COOKIE,
      auth.accessToken,
      accessTokenCookieOptions(auth.expiresIn)
    );
  }

  if (auth.refreshToken) {
    store.set(
      REFRESH_TOKEN_COOKIE,
      auth.refreshToken,
      refreshTokenCookieOptions()
    );
  }
}

export function clearAuthCookies(store: Awaited<ReturnType<typeof cookies>>) {
  store.set(ACCESS_TOKEN_COOKIE, "", clearAuthCookieOptions());
  store.set(REFRESH_TOKEN_COOKIE, "", clearAuthCookieOptions());
}

async function refreshSession(refreshToken: string) {
  const result = await rawBackendRequest<AuthResponse>(
    "/api/v1/auth/refresh",
    jsonInit("POST", { refreshToken })
  );

  return result.ok ? result.data : undefined;
}

async function rawBackendRequest<T>(
  path: string,
  init: RequestInit,
  accessToken?: string
): Promise<BackendResult<T>> {
  const headers = new Headers(init.headers);

  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  try {
    const response = await fetch(`${getApiBaseUrl()}${path}`, {
      ...init,
      headers,
      cache: "no-store",
    });

    if (response.status === 204) {
      return { ok: true, data: undefined as T, status: response.status };
    }

    const payload = await response.json().catch(() => undefined);

    if (response.ok) {
      return { ok: true, data: payload as T, status: response.status };
    }

    return {
      ok: false,
      error: normalizeProblem(payload, response.status),
      status: response.status,
    };
  } catch (error) {
    return {
      ok: false,
      error: {
        title: "Backend unavailable",
        detail:
          error instanceof Error
            ? error.message
            : "The backend could not be reached.",
        status: 503,
      },
      status: 503,
    };
  }
}

function normalizeProblem(value: unknown, status: number): BackendProblem {
  if (typeof value === "object" && value !== null && "title" in value) {
    return value as BackendProblem;
  }

  return {
    title: "Request failed",
    status,
  };
}
