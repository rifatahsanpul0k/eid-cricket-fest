import createClient from "openapi-fetch";

import { getApiBaseUrl } from "@/lib/api/env";
import type { paths } from "@/lib/api/schema";

const API_TIMEOUT_MS = 5_000;

export function createApiClient() {
  return createClient<paths>({
    baseUrl: getApiBaseUrl(),
    fetch: timeoutFetch,
  });
}

async function timeoutFetch(input: RequestInfo | URL, init?: RequestInit) {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), API_TIMEOUT_MS);

  try {
    return await fetch(input, {
      ...init,
      signal: init?.signal ?? controller.signal,
    });
  } finally {
    clearTimeout(timeoutId);
  }
}
