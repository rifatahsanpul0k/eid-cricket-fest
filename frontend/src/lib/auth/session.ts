import { cookies } from "next/headers";

import { ACCESS_TOKEN_COOKIE, REFRESH_TOKEN_COOKIE } from "@/lib/auth/cookies";
import type { components } from "@/lib/api/schema";

export type SessionUser = components["schemas"]["AuthResponse"]["user"] & {
  id: number;
};

export type Session = {
  accessToken: string;
  refreshToken?: string;
  user?: SessionUser;
};

export async function getSession(): Promise<Session | undefined> {
  const store = await cookies();
  const accessToken = store.get(ACCESS_TOKEN_COOKIE)?.value;

  if (!accessToken) {
    return undefined;
  }

  return {
    accessToken,
    refreshToken: store.get(REFRESH_TOKEN_COOKIE)?.value,
    user: decodeSessionUser(accessToken),
  };
}

export function decodeSessionUser(token: string): SessionUser | undefined {
  const [, payload] = token.split(".");

  if (!payload) {
    return undefined;
  }

  try {
    const json = JSON.parse(
      Buffer.from(base64UrlToBase64(payload), "base64").toString("utf8")
    ) as {
      sub?: string;
      displayName?: string;
      email?: string;
      phone?: string;
      roles?: SessionUser["roles"];
    };
    const id = Number(json.sub);

    if (!Number.isFinite(id)) {
      return undefined;
    }

    return {
      id,
      displayName: json.displayName,
      email: json.email,
      phone: json.phone,
      roles: json.roles,
    };
  } catch {
    return undefined;
  }
}

function base64UrlToBase64(value: string) {
  const padded = value.padEnd(value.length + ((4 - (value.length % 4)) % 4), "=");

  return padded.replace(/-/g, "+").replace(/_/g, "/");
}
