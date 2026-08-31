import { describe, expect, it } from "vitest";

import { createRefreshCoordinator } from "@/lib/auth/refresh";

describe("createRefreshCoordinator", () => {
  it("deduplicates concurrent refreshes for the same token", async () => {
    let calls = 0;
    const refreshOnce = createRefreshCoordinator(async () => {
      calls++;
      await new Promise((resolve) => setTimeout(resolve, 5));

      return {
        accessToken: "rotated-access",
        refreshToken: "rotated-refresh",
        tokenType: "Bearer",
        expiresIn: 60,
      };
    });

    const [first, second] = await Promise.all([
      refreshOnce("same-refresh-token"),
      refreshOnce("same-refresh-token"),
    ]);

    expect(calls).toBe(1);
    expect(first?.refreshToken).toBe("rotated-refresh");
    expect(second?.refreshToken).toBe("rotated-refresh");
  });
});
