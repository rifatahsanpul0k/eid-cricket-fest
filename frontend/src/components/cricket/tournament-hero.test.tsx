import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import { TournamentHero } from "@/components/cricket/tournament-hero";
import type { TournamentEdition } from "@/lib/api/tournaments";

describe("TournamentHero", () => {
  it("renders product name, edition name, and raw current edition status separately", () => {
    const edition: TournamentEdition = {
      id: 1,
      tournamentId: 1,
      name: "Season 1.0",
      status: "DRAFT",
      startDate: "2026-03-01",
      endDate: "2026-03-31",
      oversPerInnings: 10,
    };

    const html = renderToStaticMarkup(<TournamentHero edition={edition} />);

    expect(html).toContain("Eid Cricket Fest");
    expect(html).toContain("Season 1.0");
    expect(html).toContain("DRAFT");
    expect(html).not.toContain("Kandapara Eid Cricket Fest");
  });
});
