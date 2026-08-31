import { describe, expect, it } from "vitest";

import { safeReturnTo } from "@/lib/auth/return-url";

describe("safeReturnTo", () => {
  it("allows same-origin relative account paths", () => {
    expect(safeReturnTo("/account/profile?tab=form")).toBe(
      "/account/profile?tab=form"
    );
  });

  it("rejects open redirects", () => {
    expect(safeReturnTo("https://evil.example/account")).toBe("/account");
    expect(safeReturnTo("//evil.example/account")).toBe("/account");
  });

  it("rejects api return paths", () => {
    expect(safeReturnTo("/api/auth/logout")).toBe("/account");
  });
});
