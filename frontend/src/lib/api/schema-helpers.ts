import type { components } from "@/lib/api/schema";

export type AuthResponse = components["schemas"]["AuthResponse"];
export type LoginRequest = components["schemas"]["LoginRequest"];
export type RegisterRequest = components["schemas"]["RegisterRequest"];
export type PlayerResponse = components["schemas"]["PlayerResponse"];
export type CreatePlayerRequest = components["schemas"]["CreatePlayerRequest"];
export type MyTeamResponse = components["schemas"]["MyTeamResponse"];
export type MyMatchResponse = components["schemas"]["MyMatchResponse"];
export type MyEditionStatisticsResponse =
  components["schemas"]["MyEditionStatisticsResponse"];
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
export type CreateTournamentRequest =
  components["schemas"]["CreateTournamentRequest"];
export type CreateTournamentEditionRequest =
  components["schemas"]["CreateTournamentEditionRequest"];
export type UpdateTournamentEditionRequest =
  components["schemas"]["UpdateTournamentEditionRequest"];
export type UpdateTournamentEditionStatusRequest =
  components["schemas"]["UpdateTournamentEditionStatusRequest"];
export type TournamentEditionResponse =
  components["schemas"]["TournamentEditionResponse"];
export type TournamentResponse = components["schemas"]["TournamentResponse"];
export type MatchResponse = components["schemas"]["MatchResponse"];
export type MatchSetupDetailsResponse =
  components["schemas"]["MatchSetupDetailsResponse"];
export type CreateFriendlyMatchRequest =
  components["schemas"]["CreateFriendlyMatchRequest"];
export type FriendlyPlayerOptionResponse =
  components["schemas"]["FriendlyPlayerOptionResponse"];
export type PageResponseMatchResponse =
  components["schemas"]["PageResponseMatchResponse"];
export type Venue = components["schemas"]["Venue"];
export type CreateVenueRequest = components["schemas"]["CreateVenueRequest"];
export type ScheduleMatchRequest =
  components["schemas"]["ScheduleMatchRequest"];
export type RescheduleMatchOperationRequest =
  components["schemas"]["RescheduleMatchOperationRequest"];
export type OrderRematchRequest =
  components["schemas"]["OrderRematchRequest"];
export type AssignScorerRequest =
  components["schemas"]["AssignScorerRequest"];
export type SubmitPlayingXiRequest =
  components["schemas"]["SubmitPlayingXiRequest"];
export type RecordTossRequest = components["schemas"]["RecordTossRequest"];
export type NoResultRequest = components["schemas"]["NoResultRequest"];
export type ResolveKnockoutMatchRequest =
  components["schemas"]["ResolveKnockoutMatchRequest"];
export type KnockoutBracketResponse =
  components["schemas"]["KnockoutBracketResponse"];
export type UserOptionResponse = components["schemas"]["UserOptionResponse"];
export type LiveMatchResponse = components["schemas"]["LiveMatchResponse"];
export type InningsResponse = components["schemas"]["InningsResponse"];
export type StartInningsRequest =
  components["schemas"]["StartInningsRequest"];
export type SetBattersRequest = components["schemas"]["SetBattersRequest"];
export type SetBowlerRequest = components["schemas"]["SetBowlerRequest"];
export type RecordDeliveryRequest =
  components["schemas"]["RecordDeliveryRequest"];
export type UndoDeliveryRequest =
  components["schemas"]["UndoDeliveryRequest"];
export type CorrectDeliveryRequest =
  components["schemas"]["CorrectDeliveryRequest"];
export type ScorerMatchResponse =
  components["schemas"]["ScorerMatchResponse"];
export type ScorerMatchStateResponse =
  components["schemas"]["ScorerMatchStateResponse"];
export type ScorerPlayingXiPlayer =
  components["schemas"]["PlayingXiPlayer"];
