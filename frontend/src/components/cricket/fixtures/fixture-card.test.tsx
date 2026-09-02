import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { FixtureCard } from "@/components/cricket/fixtures/fixture-card";
import type { Match } from "@/lib/api/matches";

describe("FixtureCard", () => {
  it("shows postponed and cancelled labels without live actions", () => {
    const postponed = renderToStaticMarkup(
      <FixtureCard match={match({ status: "POSTPONED" })} />
    );
    const cancelled = renderToStaticMarkup(
      <FixtureCard match={match({ status: "CANCELLED" })} />
    );

    expect(postponed).toContain("Postponed");
    expect(postponed).not.toContain("Follow Live");
    expect(cancelled).toContain("Cancelled");
    expect(cancelled).not.toContain("Follow Live");
  });
});

function match(overrides: Partial<Match> = {}): Match {
  return {
    id: 7,
    matchType: "TOURNAMENT",
    matchNumber: 2,
    roundNumber: 1,
    stage: "LEAGUE",
    status: "SCHEDULED",
    resultStatus: undefined,
    rematchOfMatchId: undefined,
    supersededByMatchId: undefined,
    teamA: { matchSideId: 1, tournamentTeamId: 11, name: "Alpha XI" },
    teamB: { matchSideId: 2, tournamentTeamId: 12, name: "Bravo XI" },
    oversPerInnings: 10,
    venue: { id: 1, name: "Mirpur" },
    scheduledAt: "2026-12-16T04:00:00Z",
    scorerAssigned: false,
    teamAPlayingXiSubmitted: false,
    teamBPlayingXiSubmitted: false,
    tossCompleted: false,
    availableOperations: [],
    operationHistory: [],
    ...overrides,
  };
}
