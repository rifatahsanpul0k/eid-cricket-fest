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
export type TeamResponse = components["schemas"]["TeamResponse"];
export type TournamentTeamResponse =
  components["schemas"]["TournamentTeamResponse"];
export type CreateTeamRequest = components["schemas"]["CreateTeamRequest"];
export type AssignCaptainRequest =
  components["schemas"]["AssignCaptainRequest"];
export type DraftStateResponse = components["schemas"]["DraftStateResponse"];
export type DraftPickResponse = components["schemas"]["DraftPickResponse"];
export type DraftPoolPlayerResponse =
  components["schemas"]["DraftPoolPlayerResponse"];
export type CreateDraftRequest = components["schemas"]["CreateDraftRequest"];
export type DraftPickRequest = components["schemas"]["DraftPickRequest"];
