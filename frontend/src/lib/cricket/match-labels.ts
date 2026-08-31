import type { Match, MatchStage, MatchStatus } from "@/lib/api/matches";

export type { MatchStage, MatchStatus };

export const MATCH_STATUS_LABELS: Record<MatchStatus, string> = {
  PLANNED: "Planned",
  SCHEDULED: "Upcoming",
  READY: "Ready",
  TOSS_COMPLETED: "Toss Complete",
  LIVE: "Live",
  INNINGS_BREAK: "Innings Break",
  COMPLETED: "Completed",
  POSTPONED: "Postponed",
  ABANDONED: "Abandoned",
  CANCELLED: "Cancelled",
};

export const MATCH_STAGE_LABELS: Record<MatchStage, string> = {
  LEAGUE: "League",
  SEMI_FINAL: "Semi-final",
  FINAL: "Final",
  OTHER: "Other",
};

const ACTIVE_STATUSES = new Set<MatchStatus>(["LIVE", "INNINGS_BREAK"]);

export function matchStatusLabel(status?: Match["status"]) {
  return status ? MATCH_STATUS_LABELS[status] : "Fixture";
}

export function matchStageLabel(stage?: Match["stage"]) {
  return stage ? MATCH_STAGE_LABELS[stage] : "Fixture";
}

export function isActiveMatchStatus(status?: Match["status"]) {
  return status ? ACTIVE_STATUSES.has(status) : false;
}

export function matchAction(match: Match) {
  if (!match.id) {
    return undefined;
  }

  if (isActiveMatchStatus(match.status)) {
    return {
      label: "Follow Live",
      href: `/matches/${match.id}/live`,
    };
  }

  if (match.status === "COMPLETED") {
    return {
      label: "View Scorecard",
      href: `/matches/${match.id}/scorecard`,
    };
  }

  return undefined;
}

export function matchRouteAction({
  matchId,
  status,
}: {
  matchId?: number;
  status?: Match["status"];
}) {
  if (!matchId) {
    return undefined;
  }

  if (isActiveMatchStatus(status)) {
    return {
      label: "Follow Live",
      href: `/matches/${matchId}/live`,
    };
  }

  if (status === "COMPLETED") {
    return {
      label: "Scorecard",
      href: `/matches/${matchId}/scorecard`,
    };
  }

  return undefined;
}

export function parseMatchStatus(value?: string) {
  return value && value in MATCH_STATUS_LABELS
    ? (value as MatchStatus)
    : undefined;
}

export function parseMatchStage(value?: string) {
  return value && value in MATCH_STAGE_LABELS ? (value as MatchStage) : undefined;
}
