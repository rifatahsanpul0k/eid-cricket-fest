export function getWebSocketUrl() {
  const configuredUrl = process.env.NEXT_PUBLIC_WS_URL?.trim();

  if (configuredUrl) {
    return configuredUrl;
  }

  if (process.env.NODE_ENV !== "production") {
    return "ws://localhost:8080/ws";
  }

  throw new Error("NEXT_PUBLIC_WS_URL is required in production.");
}
