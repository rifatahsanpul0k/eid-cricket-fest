import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import {
  ScorerConsole,
  buildIncomingBatterMutation,
  buildNextBowlerMutation,
  canUseIncomingBatterTransition,
  transitionButtonDisabled,
} from "@/components/scorer/scorer-console";
import type {
  ScorerMatchStateResponse,
  ScorerPlayingXiPlayer,
} from "@/lib/api/schema-helpers";

describe("ScorerConsole", () => {
  it("server-renders start innings as player buttons without dropdowns", () => {
    const html = renderToStaticMarkup(
      <ScorerConsole initialState={startInningsState()} />
    );

    expect(html).toContain("Start Innings");
    expect(html).toContain("Select striker");
    expect(html).toMatch(/<button[^>]*>\s*Batter One\s*<\/button>/);
    expect(html).not.toContain("<select");
  });

  it("server-renders primary run and extra controls as direct tap buttons", () => {
    const html = renderToStaticMarkup(
      <ScorerConsole initialState={liveScoringState()} />
    );

    expect(html).toContain("Score Ball");
    expect(html).toMatch(/<button[^>]*>\s*0\s*<\/button>/);
    expect(html).toMatch(/<button[^>]*>\s*4\s*<\/button>/);
    expect(html).toMatch(/<button[^>]*>\s*WD\s*<\/button>/);
    expect(html).toMatch(/<button[^>]*>\s*NB\s*<\/button>/);
    expect(html).toMatch(/<button[^>]*>\s*B\s*<\/button>/);
    expect(html).toMatch(/<button[^>]*>\s*LB\s*<\/button>/);
    expect(html).not.toContain("Record Extra");
  });

  it("server-renders one-click eligible bowlers after an over boundary", () => {
    const state = overBoundaryState();

    const html = renderToStaticMarkup(<ScorerConsole initialState={state} />);

    expect(html).toContain("Next bowler");
    expect(html).not.toMatch(/<button[^>]*>\s*Bowler One\s*<\/button>/);
    expect(html).toMatch(/<button[^>]*>\s*Bowler Two\s*<\/button>/);
  });

  it("server-renders one-click incoming batter when one survivor is known", () => {
    const state = postWicketState();

    const html = renderToStaticMarkup(<ScorerConsole initialState={state} />);

    expect(canUseIncomingBatterTransition(state)).toBe(true);
    expect(html).toContain("Incoming batter");
    expect(html).not.toMatch(/<button[^>]*>\s*Batter One\s*<\/button>/);
    expect(html).not.toMatch(/<button[^>]*>\s*Batter Two\s*<\/button>/);
    expect(html).toMatch(/<button[^>]*>\s*Batter Three\s*<\/button>/);
  });

  it("keeps manual batter controls for ambiguous wicket transitions", () => {
    const state = {
      ...postWicketState(),
      live: {
        ...postWicketState().live,
        innings: {
          ...postWicketState().live?.innings,
          striker: undefined,
          nonStriker: undefined,
        },
      },
    } satisfies ScorerMatchStateResponse;

    const html = renderToStaticMarkup(<ScorerConsole initialState={state} />);

    expect(canUseIncomingBatterTransition(state)).toBe(false);
    expect(html).not.toContain("Incoming batter");
    expect(html).toContain("Select striker");
  });

  it("builds one next-bowler mutation with a stable transition key", () => {
    const result = buildNextBowlerMutation(5, {
      playingXiId: 22,
      playerName: "Bowler Two",
    } as ScorerPlayingXiPlayer);

    expect(result).toEqual({
      actionKey: "set-bowler:5:22",
      body: {
        action: "set-bowler",
        inningsId: 5,
        payload: {
          bowlerPlayingXiId: 22,
        },
      },
    });
  });

  it("builds one deterministic incoming-batter mutation with the survivor preserved", () => {
    const result = buildIncomingBatterMutation(5, postWicketState(), {
      playingXiId: 13,
      playerName: "Batter Three",
    } as ScorerPlayingXiPlayer);

    expect(result).toEqual({
      actionKey: "set-batter:5:13",
      body: {
        action: "set-batters",
        inningsId: 5,
        payload: {
          strikerPlayingXiId: 13,
          nonStrikerPlayingXiId: 12,
        },
      },
    });
  });

  it("disables transition buttons while busy or locally pending", () => {
    expect(transitionButtonDisabled(false, false, false)).toBe(false);
    expect(transitionButtonDisabled(true, false, false)).toBe(true);
    expect(transitionButtonDisabled(false, true, false)).toBe(true);
    expect(transitionButtonDisabled(false, false, true)).toBe(true);
  });
});

function startInningsState() {
  return {
    match: {
      id: 10,
      matchNumber: 3,
      status: "TOSS_COMPLETED",
      teamA: { tournamentTeamId: 1, name: "Team A" },
      teamB: { tournamentTeamId: 2, name: "Team B" },
    },
    nextInningsBattingTeamId: 1,
    nextInningsBowlingTeamId: 2,
    teamAPlayingXi: [
      batter(11, 101, "Batter One"),
      batter(12, 102, "Batter Two"),
    ],
    teamBPlayingXi: [
      bowler(21, 201, "Bowler One"),
      bowler(22, 202, "Bowler Two"),
    ],
  } satisfies ScorerMatchStateResponse;
}

function liveScoringState() {
  return {
    match: {
      id: 10,
      matchNumber: 3,
      status: "LIVE",
      teamA: { tournamentTeamId: 1, name: "Team A" },
      teamB: { tournamentTeamId: 2, name: "Team B" },
    },
    live: {
      matchId: 10,
      status: "LIVE",
      teamA: "Team A",
      teamB: "Team B",
      innings: {
        inningsId: 5,
        inningsNumber: 1,
        battingTeam: "Team A",
        bowlingTeam: "Team B",
        scoreRevision: 1,
        striker: { playerId: 101, name: "Batter One" },
        nonStriker: { playerId: 102, name: "Batter Two" },
        bowler: { playerId: 201, name: "Bowler One" },
      },
      recentBalls: [{ deliveryId: 1, sequence: 1, runs: 4, legal: true }],
    },
    teamAPlayingXi: [
      batter(11, 101, "Batter One"),
      batter(12, 102, "Batter Two"),
    ],
    teamBPlayingXi: [
      bowler(21, 201, "Bowler One"),
      bowler(22, 202, "Bowler Two"),
    ],
  } satisfies ScorerMatchStateResponse;
}

function overBoundaryState() {
  return {
    match: {
      id: 10,
      matchNumber: 3,
      status: "LIVE",
      teamA: { tournamentTeamId: 1, name: "Team A" },
      teamB: { tournamentTeamId: 2, name: "Team B" },
    },
    live: {
      matchId: 10,
      status: "LIVE",
      teamA: "Team A",
      teamB: "Team B",
      innings: {
        inningsId: 5,
        inningsNumber: 1,
        battingTeam: "Team A",
        bowlingTeam: "Team B",
        scoreRevision: 6,
        striker: { playerId: 101, name: "Batter One" },
        nonStriker: { playerId: 102, name: "Batter Two" },
      },
      recentBalls: [],
    },
    previousOverBowlerPlayingXiId: 21,
    teamAPlayingXi: [
      batter(11, 101, "Batter One"),
      batter(12, 102, "Batter Two"),
    ],
    teamBPlayingXi: [
      bowler(21, 201, "Bowler One"),
      bowler(22, 202, "Bowler Two"),
    ],
  } satisfies ScorerMatchStateResponse;
}

function postWicketState() {
  return {
    match: {
      id: 10,
      matchNumber: 3,
      status: "LIVE",
      teamA: { tournamentTeamId: 1, name: "Team A" },
      teamB: { tournamentTeamId: 2, name: "Team B" },
    },
    live: {
      matchId: 10,
      status: "LIVE",
      teamA: "Team A",
      teamB: "Team B",
      innings: {
        inningsId: 5,
        inningsNumber: 1,
        battingTeam: "Team A",
        bowlingTeam: "Team B",
        scoreRevision: 2,
        striker: undefined,
        nonStriker: { playerId: 102, name: "Batter Two" },
        bowler: { playerId: 201, name: "Bowler One" },
      },
      recentBalls: [],
    },
    dismissedPlayingXiIds: [11],
    teamAPlayingXi: [
      batter(11, 101, "Batter One"),
      batter(12, 102, "Batter Two"),
      batter(13, 103, "Batter Three"),
    ],
    teamBPlayingXi: [
      bowler(21, 201, "Bowler One"),
      bowler(22, 202, "Bowler Two"),
    ],
  } satisfies ScorerMatchStateResponse;
}

function batter(
  playingXiId: number,
  playerId: number,
  playerName: string
): ScorerPlayingXiPlayer {
  return {
    playingXiId,
    tournamentTeamId: 1,
    teamName: "Team A",
    playerId,
    playerName,
  };
}

function bowler(
  playingXiId: number,
  playerId: number,
  playerName: string
): ScorerPlayingXiPlayer {
  return {
    playingXiId,
    tournamentTeamId: 2,
    teamName: "Team B",
    playerId,
    playerName,
  };
}
