import { describe, expect, it } from "vitest";

import {
  paymentStatusMessage,
  registrationStatusMessage,
  shouldOfferPayment,
} from "@/lib/auth/account-state";

describe("account state presentation", () => {
  it("directs players without profiles to create one first", () => {
    expect(registrationStatusMessage(undefined, false)).toContain("profile");
  });

  it("shows persisted registration status", () => {
    expect(
      registrationStatusMessage({ status: "APPROVED" }, true)
    ).toBe("Your registration is approved.");
  });

  it("does not offer payment for zero-fee registrations", () => {
    expect(shouldOfferPayment({ status: "PENDING", feeAmount: 0 })).toBe(false);
  });

  it("offers payment when pending amount remains", () => {
    expect(
      shouldOfferPayment({ status: "PENDING", feeAmount: 100 }, [
        { amount: 25, status: "PENDING" },
      ])
    ).toBe(true);
  });

  it("summarizes latest payment status", () => {
    expect(paymentStatusMessage([{ status: "VERIFIED" }])).toBe(
      "Latest payment is verified."
    );
  });
});
