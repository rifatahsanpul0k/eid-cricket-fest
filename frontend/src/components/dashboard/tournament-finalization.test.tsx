import { renderToStaticMarkup } from "react-dom/server";
import type React from "react";
import { describe, expect, it, vi } from "vitest";

import { TournamentFinalization } from "@/components/dashboard/tournament-finalization";
import type { TournamentEdition } from "@/lib/api/tournaments";

vi.mock("@/components/dashboard/review-submit-button", () => ({
  ReviewSubmitButton: ({
    children,
    disabled,
  }: {
    children: React.ReactNode;
    disabled?: boolean;
  }) => <button disabled={disabled}>{children}</button>,
}));

describe("TournamentFinalization", () => {
  it("renders completed champion summary and award assignment options", () => {
    const html = renderToStaticMarkup(
      <TournamentFinalization
        awardData={{
          awards: [
            {
              id: 5,
              awardType: "PLAYER_OF_TOURNAMENT",
              playerName: "Rafi Ahmed",
              registrationId: 21,
              teamName: "Alpha XI",
              title: "Tournament MVP",
            },
          ],
          options: [
            {
              playerName: "Rafi Ahmed",
              registrationId: 21,
              teamName: "Alpha XI",
            },
          ],
        }}
        edition={edition({
          champion: {
            name: "Alpha XI",
            tournamentTeamId: 1,
          },
          completedAt: "2026-09-02T08:00:00Z",
          finalMatchId: 88,
          runnerUp: {
            name: "Beta XI",
            tournamentTeamId: 2,
          },
          status: "COMPLETED",
        })}
      />
    );

    expect(html).toContain("Tournament Complete");
    expect(html).toContain("Alpha XI");
    expect(html).toContain("Beta XI");
    expect(html).toContain("/dashboard/matches/88");
    expect(html).toContain("Manage Awards");
    expect(html).toContain("Tournament MVP");
    expect(html).toContain("name=\"action\"");
    expect(html).toContain("value=\"assign-award\"");
    expect(html).toContain("value=\"PLAYER_OF_TOURNAMENT\"");
    expect(html).toContain("value=\"21\"");
  });

  it("keeps award assignment hidden before completion", () => {
    const html = renderToStaticMarkup(
      <TournamentFinalization
        awardData={{
          awards: [],
          options: [],
        }}
        edition={edition({
          finalMatchId: undefined,
          status: "ONGOING",
        })}
      />
    );

    expect(html).toContain("Knockout");
    expect(html).toContain("Final");
    expect(html).not.toContain("Manage Awards");
    expect(html).not.toContain("assign-award");
  });
});

function edition(overrides: Partial<TournamentEdition>): TournamentEdition {
  return {
    endDate: "2026-09-05",
    id: 7,
    name: "Edition 2026",
    oversPerInnings: 5,
    playingXiSize: 3,
    registrationCurrency: "BDT",
    registrationFee: 0,
    squadSize: 3,
    startDate: "2026-09-01",
    status: "DRAFT",
    tournamentId: 1,
    ...overrides,
  };
}
