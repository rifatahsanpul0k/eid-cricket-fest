const DEFAULT_RETURN_TO = "/account";

export function safeReturnTo(value?: string | null) {
  if (!value) {
    return DEFAULT_RETURN_TO;
  }

  if (!value.startsWith("/") || value.startsWith("//")) {
    return DEFAULT_RETURN_TO;
  }

  try {
    const parsed = new URL(value, "http://localhost");

    if (parsed.origin !== "http://localhost") {
      return DEFAULT_RETURN_TO;
    }

    if (parsed.pathname.startsWith("/api/")) {
      return DEFAULT_RETURN_TO;
    }

    return `${parsed.pathname}${parsed.search}${parsed.hash}`;
  } catch {
    return DEFAULT_RETURN_TO;
  }
}
