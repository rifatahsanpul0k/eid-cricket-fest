import { describe, expect, it } from "vitest";

import {
  hasMyCricketData,
  myMatchAction,
  partitionMyMatches,
  playingXiStatus,
} from "@/lib/auth/my-cricket-state";
import type {
  MyEditionStatisticsResponse,
  MyMatchResponse,
} from "@/lib/api/schema-helpers";

describe("my cricket state", () => {
  it("partitions upcoming and completed matches", () => {
    const matches = [
      match({ matchId: 1, status: "SCHEDULED" }),
      match({ matchId: 2, status: "LIVE" }),
      match({ matchId: 3, status: "COMPLETED" }),
      match({ matchId: 4, status: "ABANDONED" }),
    ];

    expect(partitionMyMatches(matches)).toEqual({
      upcoming: [matches[0], matches[1]],
      completed: [matches[2], matches[3]],
    });
  });

  it("returns live, scorecard, or no dead action", () => {
    expect(myMatchAction(match({ matchId: 1, status: "LIVE" }))).toEqual({
      label: "Follow Live",
      href: "/matches/1/live",
    });
    expect(myMatchAction(match({ matchId: 2, status: "COMPLETED" }))).toEqual({
      label: "View Scorecard",
      href: "/matches/2/scorecard",
    });
    expect(myMatchAction(match({ matchId: 3, status: "SCHEDULED" }))).toBeUndefined();
  });

  it("keeps empty match state simple", () => {
    expect(partitionMyMatches([])).toEqual({
      upcoming: [],
      completed: [],
    });
  });

  it("uses neutral XI wording before selection is submitted", () => {
    expect(
      playingXiStatus(
        match({ inPlayingXi: false, myTeamPlayingXiSubmitted: false })
      )
    ).toBe("Selection pending");
    expect(
      playingXiStatus(
        match({ inPlayingXi: false, myTeamPlayingXiSubmitted: true })
      )
    ).toBe("Not in Playing XI");
    expect(
      playingXiStatus(
        match({ inPlayingXi: true, myTeamPlayingXiSubmitted: false })
      )
    ).toBe("In Playing XI");
  });

  it("treats zero statistics as valid data", () => {
    const statistics: MyEditionStatisticsResponse = {
      batting: { balls: 0, dismissals: 0, fours: 0, highestScore: 0, innings: 0, runs: 0, sixes: 0, strikeRate: 0 },
      bowling: { bestBowling: "0/0", economy: 0, legalBalls: 0, overs: "0.0", runsConceded: 0, wickets: 0 },
      editionId: 1,
      fielding: { catches: 0, runOuts: 0, stumpings: 0 },
      matchesPlayed: 0,
      playerId: 1,
      playerName: "Player",
    };

    expect(hasMyCricketData(statistics)).toBe(false);
  });
});

function match(overrides: Partial<MyMatchResponse>): MyMatchResponse {
  return {
    inPlayingXi: false,
    matchId: 1,
    matchNumber: 1,
    myTeamPlayingXiSubmitted: false,
    myTournamentTeamId: 10,
    opponent: { name: "Opposition", tournamentTeamId: 11 },
    oversPerInnings: 5,
    stage: "LEAGUE",
    status: "SCHEDULED",
    teamA: { name: "A", tournamentTeamId: 10 },
    teamB: { name: "B", tournamentTeamId: 11 },
    ...overrides,
  };
}
