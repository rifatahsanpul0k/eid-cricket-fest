import { describe, expect, it } from "vitest";

import {
  availableEditionTeams,
  draftStatusLabel,
  picksForTeam,
  poolWithoutPickedPlayers,
  rosterStatusLabel,
} from "@/lib/dashboard/team-draft-state";

describe("team and draft state helpers", () => {
  it("distinguishes permanent teams from edition teams", () => {
    expect(
      availableEditionTeams(
        [{ id: 1 }, { id: 2 }, { id: 3 }],
        [{ teamId: 2 }]
      )
    ).toEqual([{ id: 1 }, { id: 3 }]);
  });

  it("removes picked registrations from the draft pool", () => {
    expect(
      poolWithoutPickedPlayers(
        [{ registrationId: 10, playerName: "One" }, { registrationId: 11, playerName: "Two" }],
        [{ registrationId: 10, playerName: "One" }]
      )
    ).toEqual([{ registrationId: 11, playerName: "Two" }]);
  });

  it("groups picks by tournament team", () => {
    expect(
      picksForTeam(
        [{ tournamentTeamId: 3, playerName: "A" }, { tournamentTeamId: 4, playerName: "B" }],
        3
      )
    ).toEqual([{ tournamentTeamId: 3, playerName: "A" }]);
  });

  it("labels backend states centrally", () => {
    expect(draftStatusLabel("ORDER_GENERATED")).toBe("Order generated");
    expect(rosterStatusLabel("LOCKED")).toBe("Locked");
  });
});
