import { describe, expect, it } from "vitest";

import { sortHistoryEditions } from "@/lib/cricket/history";

describe("sortHistoryEditions", () => {
  it("orders editions newest first with deterministic id fallback", () => {
    expect(
      sortHistoryEditions([
        { editionId: 1, startDate: "2025-01-01" },
        { editionId: 3, startDate: "2026-01-01" },
        { editionId: 2, startDate: "2026-01-01" },
      ]).map((edition) => edition.editionId)
    ).toEqual([3, 2, 1]);
  });
});
