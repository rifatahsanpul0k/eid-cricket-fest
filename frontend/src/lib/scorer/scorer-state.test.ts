import { describe, expect, it } from "vitest";

import {
  ScoringIntentStore,
  batterOptions,
  buildByeDelivery,
  buildDeliveryRequest,
  buildLegByeDelivery,
  buildNoBallBatDelivery,
  buildNoBallByeDelivery,
  buildNoBallLegByeDelivery,
  buildWideDelivery,
  canCorrectDelivery,
  canUndo,
  deliveryLabel,
  deterministicDismissedBatter,
  eligibleActiveBatters,
  dismissalOptionsForDelivery,
  dismissalRequiresFielder,
  eligibleIncomingBatters,
  eligibleFielders,
  eligibleNextOverBowlers,
  extraRunOptions,
  runOptions,
  wicketDismissalOptions,
  wideOptions,
  validateDeliveryInput,
} from "@/lib/scorer/scorer-state";
import type { ScorerMatchStateResponse } from "@/lib/api/schema-helpers";

describe("scorer-state", () => {
  it("reuses the same client event id for one pending intended action", () => {
    let next = 0;
    const store = new ScoringIntentStore(() => `event-${++next}`);

    expect(store.begin("delivery:4")).toBe("event-1");
    expect(store.begin("delivery:4")).toBe("event-1");
    expect(store.begin("delivery:1")).toBe("event-2");

    store.finish("delivery:4");

    expect(store.begin("delivery:4")).toBe("event-3");
  });

  it("builds delivery requests with explicit zero defaults", () => {
    expect(buildDeliveryRequest("event-1", { runsOffBat: 4 })).toMatchObject({
      clientEventId: "event-1",
      runsOffBat: 4,
      wideRuns: 0,
      noBallRuns: 0,
      byeRuns: 0,
      legByeRuns: 0,
      penaltyRuns: 0,
    });
  });

  it("guards obvious mutually exclusive extra inputs", () => {
    expect(validateDeliveryInput({ wideRuns: 1, noBallRuns: 1 })).toContain(
      "wide"
    );
    expect(validateDeliveryInput({ byeRuns: 1, legByeRuns: 1 })).toContain(
      "byes"
    );
    expect(validateDeliveryInput({ runsOffBat: 1, byeRuns: 1 })).toContain(
      "Bat runs"
    );
    expect(validateDeliveryInput({ wideRuns: 1, runsOffBat: 1 })).toContain(
      "wide"
    );
    expect(validateDeliveryInput({ noBallRuns: 1, runsOffBat: 1 })).toBeUndefined();
  });

  it("builds quick scoring payload inputs without mixing incompatible extras", () => {
    expect(runOptions()).toEqual([0, 1, 2, 3, 4, 6]);
    expect(wideOptions()).toEqual([1, 2, 3, 4, 5]);
    expect(extraRunOptions()).toEqual([1, 2, 3, 4]);
    expect(buildWideDelivery(2)).toEqual({ wideRuns: 2 });
    expect(buildNoBallBatDelivery(4)).toEqual({
      noBallRuns: 1,
      runsOffBat: 4,
    });
    expect(buildNoBallByeDelivery(2)).toEqual({
      noBallRuns: 1,
      byeRuns: 2,
    });
    expect(buildNoBallLegByeDelivery(1)).toEqual({
      noBallRuns: 1,
      legByeRuns: 1,
    });
    expect(buildByeDelivery(3)).toEqual({ byeRuns: 3 });
    expect(buildLegByeDelivery(4)).toEqual({ legByeRuns: 4 });
  });

  it("filters dismissal choices for ordinary, wide, and no-ball deliveries", () => {
    expect(dismissalOptionsForDelivery({ runsOffBat: 0 })).toContain("BOWLED");
    expect(dismissalOptionsForDelivery({ wideRuns: 1 })).toEqual([
      "RUN_OUT",
      "STUMPED",
      "HIT_WICKET",
      "OBSTRUCTING_FIELD",
    ]);
    expect(dismissalOptionsForDelivery({ noBallRuns: 1 })).toEqual([
      "RUN_OUT",
      "HIT_BALL_TWICE",
      "OBSTRUCTING_FIELD",
    ]);
    expect(dismissalOptionsForDelivery({ noBallRuns: 1 })).not.toContain(
      "CAUGHT"
    );
  });

  it("formats recent-ball labels only from exposed live ball fields", () => {
    expect(deliveryLabel({ deliveryId: 1, sequence: 1, runs: 0, legal: true })).toBe(".");
    expect(deliveryLabel({ deliveryId: 2, sequence: 2, runs: 4, legal: true })).toBe("4");
    expect(deliveryLabel({ deliveryId: 3, sequence: 3, runs: 1, legal: false })).toBe("+1");
  });

  it("uses backend state to determine undo and correction eligibility", () => {
    const state = {
      match: { status: "LIVE" },
      live: {
        innings: { inningsId: 5 },
        recentBalls: [{ deliveryId: 8, sequence: 1, runs: 1, legal: true }],
      },
    } as ScorerMatchStateResponse;

    expect(canUndo(state)).toBe(true);
    expect(canCorrectDelivery(state)).toBe(true);

    expect(canUndo({ ...state, match: { status: "COMPLETED" } })).toBe(false);
  });

  it("filters paired batter options by playing XI id", () => {
    const players = [
      { playingXiId: 1, playerName: "A" },
      { playingXiId: 2, playerName: "B" },
    ] as ScorerMatchStateResponse["teamAPlayingXi"];

    expect(batterOptions(players ?? [], 1).map((player) => player.playingXiId))
      .toEqual([2]);

    expect(batterOptions(players ?? [], 2).map((player) => player.playingXiId))
      .toEqual([1]);
  });

  it("excludes dismissed batters from incoming choices and only offers current batters for wickets", () => {
    const state = {
      match: { status: "LIVE" },
      dismissedPlayingXiIds: [3],
      live: {
        innings: {
          inningsId: 7,
          battingTeam: "Team A",
          striker: { playerId: 101, name: "A" },
          nonStriker: { playerId: 102, name: "B" },
        },
      },
      teamAPlayingXi: [
        { playingXiId: 1, playerId: 101, teamName: "Team A", playerName: "A" },
        { playingXiId: 2, playerId: 102, teamName: "Team A", playerName: "B" },
        { playingXiId: 3, playerId: 103, teamName: "Team A", playerName: "C" },
      ],
    } as ScorerMatchStateResponse;

    expect(eligibleActiveBatters(state).map((player) => player.playingXiId))
      .toEqual([1, 2]);
    expect(eligibleIncomingBatters(state).map((player) => player.playingXiId))
      .toEqual([]);
    expect(wicketDismissalOptions(state).map((player) => player.playingXiId))
      .toEqual([1, 2]);
  });

  it("offers only undismissed non-current batters as incoming options", () => {
    const state = {
      match: { status: "LIVE" },
      dismissedPlayingXiIds: [1],
      live: {
        innings: {
          inningsId: 7,
          battingTeam: "Team A",
          striker: undefined,
          nonStriker: { playerId: 102, name: "B" },
        },
      },
      teamAPlayingXi: [
        { playingXiId: 1, playerId: 101, teamName: "Team A", playerName: "A" },
        { playingXiId: 2, playerId: 102, teamName: "Team A", playerName: "B" },
        { playingXiId: 3, playerId: 103, teamName: "Team A", playerName: "C" },
      ],
    } as ScorerMatchStateResponse;

    expect(eligibleIncomingBatters(state).map((player) => player.playingXiId))
      .toEqual([3]);
  });

  it("defaults deterministic striker dismissals and requests fielders only when needed", () => {
    const state = {
      live: {
        innings: {
          battingTeam: "Team A",
          bowlingTeam: "Team B",
          striker: { playerId: 101, name: "A" },
          nonStriker: { playerId: 102, name: "B" },
        },
      },
      teamAPlayingXi: [
        { playingXiId: 1, playerId: 101, teamName: "Team A", playerName: "A" },
        { playingXiId: 2, playerId: 102, teamName: "Team A", playerName: "B" },
      ],
      teamBPlayingXi: [
        { playingXiId: 10, teamName: "Team B", playerName: "Keeper" },
      ],
    } as ScorerMatchStateResponse;

    expect(deterministicDismissedBatter(state, "BOWLED")).toBe(1);
    expect(deterministicDismissedBatter(state, "LBW")).toBe(1);
    expect(deterministicDismissedBatter(state, "RUN_OUT")).toBeUndefined();
    expect(dismissalRequiresFielder("CAUGHT")).toBe(true);
    expect(dismissalRequiresFielder("STUMPED")).toBe(true);
    expect(dismissalRequiresFielder("BOWLED")).toBe(false);
    expect(eligibleFielders(state).map((player) => player.playingXiId))
      .toEqual([10]);
  });

  it("excludes only the previous-over bowler from next-over choices", () => {
    const state = {
      previousOverBowlerPlayingXiId: 10,
      live: {
        innings: {
          bowlingTeam: "Team B",
        },
      },
      teamBPlayingXi: [
        { playingXiId: 10, teamName: "Team B", playerName: "X" },
        { playingXiId: 11, teamName: "Team B", playerName: "Y" },
        { playingXiId: 12, teamName: "Team B", playerName: "Z" },
      ],
    } as ScorerMatchStateResponse;

    expect(eligibleNextOverBowlers(state).map((player) => player.playingXiId))
      .toEqual([11, 12]);

    expect(
      eligibleNextOverBowlers({
        ...state,
        previousOverBowlerPlayingXiId: 11,
      }).map((player) => player.playingXiId)
    ).toEqual([10, 12]);
  });
});
