export const ACCESS_TOKEN_COOKIE = "ecf_access_token";
export const REFRESH_TOKEN_COOKIE = "ecf_refresh_token";

const SHARED_COOKIE_OPTIONS = {
  httpOnly: true,
  sameSite: "lax" as const,
  secure: process.env.NODE_ENV === "production",
  path: "/",
};

export function accessTokenCookieOptions(expiresInSeconds?: number) {
  return {
    ...SHARED_COOKIE_OPTIONS,
    maxAge: Math.max(1, Math.floor(expiresInSeconds ?? 60)),
  };
}

export function refreshTokenCookieOptions() {
  return SHARED_COOKIE_OPTIONS;
}

export function clearAuthCookieOptions() {
  return {
    ...SHARED_COOKIE_OPTIONS,
    maxAge: 0,
  };
}
