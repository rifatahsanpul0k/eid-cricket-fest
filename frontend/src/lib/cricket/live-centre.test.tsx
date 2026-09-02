import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { LiveCentreCard } from "@/components/cricket/live/live-centre-client";
import type { LiveCentreMatch } from "@/lib/api/matches";
import {
  liveCentreDetailText,
  liveCentreEyebrow,
  liveCentrePrimaryText,
  liveCentreSections,
  resultText,
} from "@/lib/cricket/live-centre";

describe("live centre", () => {
  it("groups every public live centre state", () => {
    const sections = liveCentreSections([
      match({ status: "LIVE" }),
      match({ status: "TOSS_COMPLETED" }),
      match({ status: "INNINGS_BREAK" }),
      match({ status: "SUSPENDED" }),
      match({ status: "COMPLETED" }),
    ]);

    expect(sections.map((section) => [section.title, section.matches.length]))
      .toEqual([
        ["LIVE NOW", 1],
        ["TOSS COMPLETED", 1],
        ["INNINGS BREAK", 1],
        ["SUSPENDED", 1],
        ["RECENT RESULTS", 1],
      ]);
  });

  it("renders tournament card metadata", () => {
    const html = renderToStaticMarkup(
      <LiveCentreCard
        match={match({
          matchNumber: 4,
          matchType: "TOURNAMENT",
          stage: "LEAGUE",
          status: "LIVE",
        })}
      />
    );

    expect(html).toContain("TOURNAMENT · LEAGUE · MATCH 4");
    expect(html).toContain("Alpha XI vs Bravo XI");
    expect(html).toContain("67/3 · 7.4 overs");
    expect(html).toContain("Watch Live");
  });

  it("renders friendly card without fake tournament metadata", () => {
    const html = renderToStaticMarkup(
      <LiveCentreCard
        match={match({
          matchNumber: undefined,
          matchType: "FRIENDLY",
          stage: undefined,
          status: "LIVE",
          teamA: { matchSideId: 1, name: "Thunder XI" },
          teamB: { matchSideId: 2, name: "Warriors XI" },
        })}
      />
    );

    expect(html).toContain("FRIENDLY");
    expect(html).toContain("Thunder XI vs Warriors XI");
    expect(html).not.toContain("MATCH");
    expect(html).not.toContain("LEAGUE");
  });

  it("describes toss completed without fake score", () => {
    const tossMatch = match({
      innings: undefined,
      status: "TOSS_COMPLETED",
    });

    expect(liveCentrePrimaryText(tossMatch)).toBe("Match starting shortly");
    expect(liveCentreDetailText(tossMatch)).toBe(
      "Alpha XI won the toss and chose to bat first."
    );
  });

  it("describes innings break and suspended states from persisted score", () => {
    expect(
      liveCentreDetailText(
        match({
          status: "INNINGS_BREAK",
          innings: {
            ...innings(),
            battingTeam: "Alpha XI",
            runs: 67,
            wickets: 3,
            target: 68,
          },
        })
      )
    ).toBe("Alpha XI finished. Target 68.");

    expect(
      liveCentreDetailText(
        match({
          status: "SUSPENDED",
          innings: {
            ...innings(),
            battingTeam: "Alpha XI",
            runs: 67,
            wickets: 3,
          },
        })
      )
    ).toBe("Suspended with Alpha XI at 67/3");
  });

  it("handles completed result statuses", () => {
    expect(
      resultText(match({ status: "COMPLETED", resultStatus: "OFFICIAL" }))
    ).toBe("Alpha XI won by 12 runs");

    expect(
      resultText(match({ status: "COMPLETED", resultStatus: "UNDER_REVIEW" }))
    ).toBe("Result under review");

    expect(resultText(match({ status: "COMPLETED", resultStatus: "VOID" })))
      .toBe("Result voided");

    expect(
      resultText(match({ status: "COMPLETED", resultStatus: "SUPERSEDED" }))
    ).toBe("Result superseded");
  });

  it("keeps one live match renderable instead of redirect-only", () => {
    const sections = liveCentreSections([match({ status: "LIVE" })]);

    expect(sections[0]?.matches).toHaveLength(1);
    expect(liveCentreEyebrow(sections[0]!.matches[0]!)).toContain(
      "TOURNAMENT"
    );
  });
});

function match(overrides: Partial<LiveCentreMatch> = {}): LiveCentreMatch {
  return {
    matchId: 10,
    matchType: "TOURNAMENT",
    status: "LIVE",
    matchNumber: 3,
    stage: "LEAGUE",
    scheduledAt: "2026-12-16T04:00:00Z",
    oversPerInnings: 10,
    resultStatus: "OFFICIAL",
    rematchOfMatchId: undefined,
    supersededByMatchId: undefined,
    teamA: { matchSideId: 1, tournamentTeamId: 11, name: "Alpha XI" },
    teamB: { matchSideId: 2, tournamentTeamId: 12, name: "Bravo XI" },
    venue: { id: 1, name: "Mirpur" },
    toss: {
      winnerMatchSideId: 1,
      winnerName: "Alpha XI",
      decision: "BAT",
    },
    innings: innings(),
    resultText: "Alpha XI won by 12 runs",
    winner: { matchSideId: 1, tournamentTeamId: 11, name: "Alpha XI" },
    ...overrides,
  };
}

function innings(): NonNullable<LiveCentreMatch["innings"]> {
  return {
    inningsId: 5,
    inningsNumber: 1,
    battingTeam: "Alpha XI",
    bowlingTeam: "Bravo XI",
    runs: 67,
    wickets: 3,
    overs: "7.4",
    target: undefined,
    runsRequired: undefined,
    ballsRemaining: undefined,
    currentRunRate: 8.74,
    requiredRunRate: undefined,
    striker: { playerId: 101, name: "Pulok" },
    nonStriker: { playerId: 102, name: "Nitol" },
    bowler: { playerId: 201, name: "Limon" },
  };
}
