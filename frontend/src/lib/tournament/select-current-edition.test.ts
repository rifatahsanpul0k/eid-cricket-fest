import { describe, expect, it } from "vitest";

import { selectCurrentEdition } from "@/lib/tournament/select-current-edition";
import type { TournamentEdition } from "@/lib/api/tournaments";

describe("selectCurrentEdition", () => {
  it("prefers ongoing over completed", () => {
    expect(
      selectCurrentEdition([
        edition(1, "COMPLETED", "2026-01-01"),
        edition(2, "ONGOING", "2025-01-01"),
      ])?.id
    ).toBe(2);
  });

  it("prefers scheduled over old completed", () => {
    expect(
      selectCurrentEdition([
        edition(1, "COMPLETED", "2026-01-01"),
        edition(2, "SCHEDULED", "2025-01-01"),
      ])?.id
    ).toBe(2);
  });

  it("does not prefer cancelled when another usable edition exists", () => {
    expect(
      selectCurrentEdition([
        edition(1, "CANCELLED", "2027-01-01"),
        edition(2, "DRAFT", "2025-01-01"),
      ])?.id
    ).toBe(2);
  });

  it("selects deterministically within equivalent statuses", () => {
    expect(
      selectCurrentEdition([
        edition(1, "COMPLETED", "2024-01-01"),
        edition(3, "COMPLETED", "2025-01-01"),
        edition(2, "COMPLETED", "2025-01-01"),
      ])?.id
    ).toBe(3);
  });

  it("returns no edition for an empty list", () => {
    expect(selectCurrentEdition([])).toBeUndefined();
  });
});

function edition(
  id: number,
  status: TournamentEdition["status"],
  startDate: string
): TournamentEdition {
  return {
    id,
    status,
    startDate,
  };
}
