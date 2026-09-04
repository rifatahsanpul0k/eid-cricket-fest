import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { TournamentInfo } from "@/components/cricket/tournament-info";
import type { TournamentEdition } from "@/lib/api/tournaments";

describe("TournamentInfo", () => {
  it("uses the product name and current edition fields separately", () => {
    const edition: TournamentEdition = {
      id: 1,
      tournamentId: 1,
      name: "Season 1.0",
      status: "DRAFT",
      startDate: "2026-03-01",
      endDate: "2026-03-31",
    };

    const html = renderToStaticMarkup(<TournamentInfo edition={edition} />);

    expect(html).toContain("Eid Cricket Fest");
    expect(html).toContain("Season 1.0");
    expect(html).toContain("DRAFT");
    expect(html).not.toContain("Kandapara Eid Cricket Fest");
  });
});
