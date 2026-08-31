import type {
  DraftPickResponse,
  DraftPoolPlayerResponse,
  DraftStateResponse,
  TournamentTeamResponse,
} from "@/lib/api/schema-helpers";

export const draftStatuses = [
  "PENDING",
  "ORDER_GENERATED",
  "IN_PROGRESS",
  "COMPLETED",
  "CANCELLED",
] as const;

export const draftPickModes = ["SNAKE", "LINEAR"] as const;

export function draftStatusLabel(status?: DraftStateResponse["status"]) {
  switch (status) {
    case "PENDING":
      return "Pending";
    case "ORDER_GENERATED":
      return "Order generated";
    case "IN_PROGRESS":
      return "In progress";
    case "COMPLETED":
      return "Completed";
    case "CANCELLED":
      return "Cancelled";
    default:
      return "Not created";
  }
}

export function rosterStatusLabel(
  status?: TournamentTeamResponse["rosterStatus"]
) {
  return status === "LOCKED" ? "Locked" : "Open";
}

export function availableEditionTeams(
  teams: { id?: number }[],
  editionTeams: { teamId?: number }[]
) {
  const selected = new Set(
    editionTeams
      .map((team) => team.teamId)
      .filter((teamId): teamId is number => teamId !== undefined)
  );

  return teams.filter((team) => team.id !== undefined && !selected.has(team.id));
}

export function poolWithoutPickedPlayers(
  pool: DraftPoolPlayerResponse[],
  picks: DraftPickResponse[]
) {
  const picked = new Set(
    picks
      .map((pick) => pick.registrationId)
      .filter((registrationId): registrationId is number => registrationId !== undefined)
  );

  return pool.filter(
    (player) => player.registrationId !== undefined && !picked.has(player.registrationId)
  );
}

export function picksForTeam(
  picks: DraftPickResponse[],
  tournamentTeamId?: number
) {
  return picks.filter((pick) => pick.tournamentTeamId === tournamentTeamId);
}
