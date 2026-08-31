import { describe, expect, it } from "vitest";

import {
  ScoringIntentStore,
  buildDeliveryRequest,
  canCorrectDelivery,
  canUndo,
  deliveryLabel,
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
});
