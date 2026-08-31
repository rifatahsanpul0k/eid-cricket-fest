import { headers } from "next/headers";
import type { NextRequest } from "next/server";

export async function assertSameOriginRequest(request: NextRequest) {
  const origin = request.headers.get("origin");

  if (!origin) {
    return;
  }

  const headerStore = await headers();
  const host = headerStore.get("host");

  if (!host) {
    throw new Error("Missing host header");
  }

  const expectedOrigin = `${request.nextUrl.protocol}//${host}`;

  if (origin !== expectedOrigin) {
    throw new Error("Invalid request origin");
  }
}
