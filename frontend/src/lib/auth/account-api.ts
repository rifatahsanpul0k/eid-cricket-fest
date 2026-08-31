import type {
  PaymentResponse,
  PlayerResponse,
  RegistrationResponse,
} from "@/lib/api/schema-helpers";
import { backendRequest } from "@/lib/auth/backend";

export async function getMyProfile() {
  const result = await backendRequest<PlayerResponse>(
    "/api/v1/players/me",
    {},
    { authenticated: true, retryOnUnauthorized: false }
  );

  return result.status === 404 ? undefined : result;
}

export async function getMyRegistration(editionId: number) {
  const result = await backendRequest<RegistrationResponse>(
    `/api/v1/tournament-editions/${editionId}/registrations/me`,
    {},
    { authenticated: true, retryOnUnauthorized: false }
  );

  return result.status === 404 ? undefined : result;
}

export async function getMyPayments(registrationId: number) {
  const result = await backendRequest<PaymentResponse[]>(
    `/api/v1/registrations/${registrationId}/payments/me`,
    {},
    { authenticated: true, retryOnUnauthorized: false }
  );

  return result.ok ? result.data : [];
}
