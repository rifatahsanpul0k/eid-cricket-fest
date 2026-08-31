import type {
  PaymentResponse,
  RegistrationResponse,
} from "@/lib/api/schema-helpers";
import { backendRequest, jsonInit } from "@/lib/auth/backend";
import type { BackendResult } from "@/lib/auth/backend";
import type { components } from "@/lib/api/schema";

export type RegistrationPageResponse =
  components["schemas"]["PageResponseRegistrationResponse"];
export type PaymentPageResponse =
  components["schemas"]["PageResponsePaymentResponse"];

export type RegistrationSearch = {
  category?: string;
  direction?: string;
  page?: number;
  q?: string;
  size?: number;
  sortBy?: string;
  status?: RegistrationResponse["status"];
};

export type PaymentSearch = {
  direction?: string;
  method?: PaymentResponse["paymentMethod"];
  page?: number;
  q?: string;
  size?: number;
  sortBy?: string;
  status?: PaymentResponse["status"];
};

export async function searchOrganizerRegistrations(
  editionId: number,
  search: RegistrationSearch
): Promise<BackendResult<RegistrationPageResponse>> {
  return backendRequest<RegistrationPageResponse>(
    `/api/v1/tournament-editions/${editionId}/registrations${query(search)}`,
    {},
    { authenticated: true }
  );
}

export async function searchOrganizerPayments(
  editionId: number,
  search: PaymentSearch
): Promise<BackendResult<PaymentPageResponse>> {
  return backendRequest<PaymentPageResponse>(
    `/api/v1/tournament-editions/${editionId}/payments${query(search)}`,
    {},
    { authenticated: true }
  );
}

export async function approveRegistration(registrationId: number) {
  return backendRequest(
    `/api/v1/registrations/${registrationId}/approve`,
    jsonInit("PATCH", undefined),
    { authenticated: true }
  );
}

export async function rejectRegistration(registrationId: number, reason: string) {
  return backendRequest(
    `/api/v1/registrations/${registrationId}/reject`,
    jsonInit("PATCH", { reason }),
    { authenticated: true }
  );
}

export async function verifyPayment(paymentId: number) {
  return backendRequest(
    `/api/v1/registration-payments/${paymentId}/verify`,
    jsonInit("PATCH", undefined),
    { authenticated: true }
  );
}

export async function rejectPayment(paymentId: number, reason: string) {
  return backendRequest(
    `/api/v1/registration-payments/${paymentId}/reject`,
    jsonInit("PATCH", { reason }),
    { authenticated: true }
  );
}

function query(params: Record<string, number | string | undefined>) {
  const search = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      search.set(key, String(value));
    }
  });

  const value = search.toString();

  return value ? `?${value}` : "";
}
