import type { components } from "@/lib/api/schema";

export type AuthResponse = components["schemas"]["AuthResponse"];

type RefreshFn = (refreshToken: string) => Promise<AuthResponse | undefined>;

export function createRefreshCoordinator(refreshFn: RefreshFn) {
  const inFlight = new Map<string, Promise<AuthResponse | undefined>>();

  return async function refreshOnce(refreshToken: string) {
    const existing = inFlight.get(refreshToken);

    if (existing) {
      return existing;
    }

    const request = refreshFn(refreshToken).finally(() => {
      inFlight.delete(refreshToken);
    });

    inFlight.set(refreshToken, request);

    return request;
  };
}
