import type { MyMatchResponse, MyEditionStatisticsResponse } from "@/lib/api/schema-helpers";

const ACTIVE_STATUSES = new Set(["LIVE", "INNINGS_BREAK"]);
const COMPLETED_STATUSES = new Set(["COMPLETED", "ABANDONED"]);

export function partitionMyMatches(matches: MyMatchResponse[]) {
  return {
    upcoming: matches.filter(
      (match) => !COMPLETED_STATUSES.has(match.status ?? "")
    ),
    completed: matches.filter((match) =>
      COMPLETED_STATUSES.has(match.status ?? "")
    ),
  };
}

export function myMatchAction(match: MyMatchResponse) {
  if (!match.matchId) {
    return undefined;
  }

  if (ACTIVE_STATUSES.has(match.status ?? "")) {
    return {
      label: "Follow Live",
      href: `/matches/${match.matchId}/live`,
    };
  }

  if (COMPLETED_STATUSES.has(match.status ?? "")) {
    return {
      label: "View Scorecard",
      href: `/matches/${match.matchId}/scorecard`,
    };
  }

  return undefined;
}

export function playingXiStatus(match: MyMatchResponse) {
  if (match.inPlayingXi) {
    return "In Playing XI";
  }

  return match.myTeamPlayingXiSubmitted
    ? "Not in Playing XI"
    : "Selection pending";
}

export function hasMyCricketData(
  statistics?: MyEditionStatisticsResponse
) {
  return Boolean(
    statistics &&
      ((statistics.matchesPlayed ?? 0) > 0 ||
        (statistics.batting?.runs ?? 0) > 0 ||
        (statistics.bowling?.wickets ?? 0) > 0 ||
        (statistics.fielding?.catches ?? 0) > 0 ||
        (statistics.fielding?.stumpings ?? 0) > 0 ||
        (statistics.fielding?.runOuts ?? 0) > 0)
  );
}
