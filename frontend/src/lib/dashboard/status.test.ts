import { describe, expect, it } from "vitest";

import {
  paymentMethodLabel,
  paymentStatusLabel,
  registrationStatusLabel,
} from "@/lib/dashboard/status";

describe("dashboard status labels", () => {
  it("humanizes registration statuses", () => {
    expect(registrationStatusLabel("PENDING")).toBe("Pending");
    expect(registrationStatusLabel("APPROVED")).toBe("Approved");
  });

  it("humanizes payment statuses and methods centrally", () => {
    expect(paymentStatusLabel("VERIFIED")).toBe("Verified");
    expect(paymentMethodLabel("BKASH")).toBe("bKash");
    expect(paymentMethodLabel("NAGAD")).toBe("Nagad");
  });
});
