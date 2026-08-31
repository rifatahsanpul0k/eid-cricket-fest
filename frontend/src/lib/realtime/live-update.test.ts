import { describe, expect, it } from "vitest";

import type { LiveMatch } from "@/lib/api/matches";
import {
  parseLiveUpdate,
  shouldAcceptLiveUpdate,
} from "@/lib/realtime/live-update";

describe("shouldAcceptLiveUpdate", () => {
  it("accepts the first update", () => {
    expect(shouldAcceptLiveUpdate(undefined, liveMatch(1, 10, 2))).toBe(true);
  });

  it("rejects updates for a different match", () => {
    expect(shouldAcceptLiveUpdate(liveMatch(1, 10, 2), liveMatch(2, 10, 3))).toBe(
      false
    );
  });

  it("rejects same-innings updates with an older revision", () => {
    expect(shouldAcceptLiveUpdate(liveMatch(1, 10, 4), liveMatch(1, 10, 3))).toBe(
      false
    );
  });

  it("rejects same-innings updates with an equal revision", () => {
    expect(shouldAcceptLiveUpdate(liveMatch(1, 10, 4), liveMatch(1, 10, 4))).toBe(
      false
    );
  });

  it("accepts same-innings updates with a newer revision", () => {
    expect(shouldAcceptLiveUpdate(liveMatch(1, 10, 4), liveMatch(1, 10, 5))).toBe(
      true
    );
  });

  it("accepts a new innings even when its revision is lower", () => {
    expect(shouldAcceptLiveUpdate(liveMatch(1, 10, 8), liveMatch(1, 11, 1))).toBe(
      true
    );
  });

  it("handles null innings safely", () => {
    expect(
      shouldAcceptLiveUpdate({ matchId: 1 }, { matchId: 1, innings: undefined })
    ).toBe(true);
  });
});

describe("parseLiveUpdate", () => {
  it("ignores malformed JSON", () => {
    expect(parseLiveUpdate("{bad")).toBeUndefined();
  });

  it("ignores messages without a numeric match id", () => {
    expect(parseLiveUpdate(JSON.stringify({ innings: {} }))).toBeUndefined();
  });
});

function liveMatch(
  matchId: number,
  inningsId: number,
  scoreRevision: number
): LiveMatch {
  return {
    matchId,
    innings: {
      inningsId,
      scoreRevision,
    },
  };
}
