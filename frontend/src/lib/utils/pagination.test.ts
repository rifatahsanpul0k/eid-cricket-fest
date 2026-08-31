import { describe, expect, it } from "vitest";

import {
  hasNextPage,
  hasPreviousPage,
  parsePageParam,
  parseSizeParam,
} from "@/lib/utils/pagination";

describe("pagination helpers", () => {
  it("parses zero-indexed page values", () => {
    expect(parsePageParam("0")).toBe(0);
    expect(parsePageParam("3")).toBe(3);
  });

  it("clamps invalid page values to zero", () => {
    expect(parsePageParam("-5")).toBe(0);
    expect(parsePageParam("hello")).toBe(0);
  });

  it("clamps page size to backend bounds", () => {
    expect(parseSizeParam("0")).toBe(1);
    expect(parseSizeParam("100000")).toBe(100);
    expect(parseSizeParam("25")).toBe(25);
  });

  it("calculates pagination boundaries", () => {
    expect(hasPreviousPage(0)).toBe(false);
    expect(hasPreviousPage(2)).toBe(true);
    expect(hasNextPage({ page: 1, totalPages: 3 })).toBe(true);
    expect(hasNextPage({ page: 2, totalPages: 3 })).toBe(false);
  });
});
