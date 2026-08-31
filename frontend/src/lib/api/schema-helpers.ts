import type { components } from "@/lib/api/schema";

export type AuthResponse = components["schemas"]["AuthResponse"];
export type LoginRequest = components["schemas"]["LoginRequest"];
export type RegisterRequest = components["schemas"]["RegisterRequest"];
export type PlayerResponse = components["schemas"]["PlayerResponse"];
export type CreatePlayerRequest = components["schemas"]["CreatePlayerRequest"];
export type RegistrationResponse = components["schemas"]["RegistrationResponse"];
export type CreateRegistrationRequest =
  components["schemas"]["CreateRegistrationRequest"];
export type PaymentResponse = components["schemas"]["PaymentResponse"];
export type SubmitPaymentRequest = components["schemas"]["SubmitPaymentRequest"];
