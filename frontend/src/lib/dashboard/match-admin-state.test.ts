import { describe, expect, it } from "vitest";

import {
  canRecordToss,
  parseMatchAdminSearch,
  publicMatchHref,
  rosterCandidatesForTeam,
  selectedCountLabel,
} from "@/lib/dashboard/match-admin-state";

describe("match admin state helpers", () => {
  it("parses safe match filters", () => {
    expect(
      parseMatchAdminSearch({
        direction: "desc",
        page: "2",
        stage: "LEAGUE",
        status: "READY",
        teamId: "7",
      })
    ).toMatchObject({
      direction: "desc",
      page: 2,
      stage: "LEAGUE",
      status: "READY",
      teamId: 7,
    });
  });

  it("guards public match links by status", () => {
    expect(publicMatchHref({ id: 3, status: "LIVE" })).toBe("/matches/3/live");
    expect(publicMatchHref({ id: 4, status: "COMPLETED" })).toBe(
      "/matches/4/scorecard"
    );
    expect(publicMatchHref({ id: 5, status: "READY" })).toBeUndefined();
  });

  it("exposes toss only when ready", () => {
    expect(canRecordToss("READY")).toBe(true);
    expect(canRecordToss("SCHEDULED")).toBe(false);
  });

  it("builds roster candidates from backend captain and picks", () => {
    expect(
      rosterCandidatesForTeam({
        team: {
          id: 9,
          captain: {
            name: "Captain",
            playerId: 1,
            registrationId: 11,
          },
        },
        picks: [
          { tournamentTeamId: 9, playerName: "Pick", registrationId: 12 },
          { tournamentTeamId: 10, playerName: "Other", registrationId: 13 },
        ],
      })
    ).toEqual([
      { playerName: "Captain", registrationId: 11 },
      { playerName: "Pick", registrationId: 12 },
    ]);
  });

  it("shows selected count against edition size", () => {
    expect(selectedCountLabel(2, 3)).toBe("2/3 selected");
  });
});
