import { describe, expect, it } from "vitest";

import {
  ACCESS_TOKEN_COOKIE,
  REFRESH_TOKEN_COOKIE,
  accessTokenCookieOptions,
  refreshTokenCookieOptions,
} from "@/lib/auth/cookies";
import { clearAuthCookies, setAuthCookies } from "@/lib/auth/backend";

describe("auth cookies", () => {
  it("uses HttpOnly SameSite cookies for backend tokens", () => {
    expect(accessTokenCookieOptions(120)).toMatchObject({
      httpOnly: true,
      sameSite: "lax",
      path: "/",
      maxAge: 120,
    });
    expect(refreshTokenCookieOptions()).toMatchObject({
      httpOnly: true,
      sameSite: "lax",
      path: "/",
    });
  });

  it("creates and clears session cookies without exposing token names elsewhere", () => {
    const writes: { name: string; value: string; options: unknown }[] = [];
    const store = {
      set(name: string, value: string, options: unknown) {
        writes.push({ name, value, options });
      },
    };

    setAuthCookies(store as never, {
      accessToken: "access-token",
      refreshToken: "refresh-token",
      tokenType: "Bearer",
      expiresIn: 60,
    });
    clearAuthCookies(store as never);

    expect(writes.map((write) => write.name)).toEqual([
      ACCESS_TOKEN_COOKIE,
      REFRESH_TOKEN_COOKIE,
      ACCESS_TOKEN_COOKIE,
      REFRESH_TOKEN_COOKIE,
    ]);
    expect(writes[0]?.options).toMatchObject({ httpOnly: true });
    expect(writes[2]?.options).toMatchObject({ maxAge: 0 });
  });
});
