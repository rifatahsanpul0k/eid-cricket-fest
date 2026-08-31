const DEVELOPMENT_API_BASE_URL = "http://localhost:8080";

export function getApiBaseUrl() {
  const configured = process.env.API_BASE_URL?.trim();

  if (configured) {
    return configured.replace(/\/+$/, "");
  }

  if (process.env.NODE_ENV === "production") {
    throw new Error("API_BASE_URL is required in production.");
  }

  return DEVELOPMENT_API_BASE_URL;
}
