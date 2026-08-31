import { describe, expect, it } from "vitest";

import {
  formatAwardType,
  formatNetRunRate,
  formatPoints,
} from "@/lib/cricket/formatters";

describe("cricket formatters", () => {
  it("formats standings points safely", () => {
    expect(formatPoints(8)).toBe("8.0");
    expect(formatPoints(undefined)).toBe("0.0");
  });

  it("formats net run rate with signs", () => {
    expect(formatNetRunRate(1.245)).toBe("NRR +1.245");
    expect(formatNetRunRate(-0.438)).toBe("NRR -0.438");
  });

  it("formats award enum labels", () => {
    expect(formatAwardType("PLAYER_OF_TOURNAMENT")).toBe(
      "Player of the Tournament"
    );
    expect(formatAwardType("CUSTOM")).toBe("Custom");
  });
});
