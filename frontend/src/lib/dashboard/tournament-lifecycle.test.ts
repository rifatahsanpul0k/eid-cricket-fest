import { describe, expect, it } from "vitest";

import { lifecycleActions } from "@/lib/dashboard/tournament-lifecycle";

describe("lifecycleActions", () => {
  it("exposes the draft actions", () => {
    expect(lifecycleActions("DRAFT")).toEqual([
      { label: "Open Registration", status: "REGISTRATION_OPEN" },
      { label: "Cancel Edition", status: "CANCELLED" },
    ]);
  });

  it("exposes the registration open actions", () => {
    expect(lifecycleActions("REGISTRATION_OPEN")).toEqual([
      { label: "Close Registration", status: "REGISTRATION_CLOSED" },
      { label: "Cancel Edition", status: "CANCELLED" },
    ]);
  });

  it("exposes the registration closed actions", () => {
    expect(lifecycleActions("REGISTRATION_CLOSED")).toEqual([
      { label: "Start Drafting", status: "DRAFTING" },
      { label: "Cancel Edition", status: "CANCELLED" },
    ]);
  });

  it("exposes the drafting actions", () => {
    expect(lifecycleActions("DRAFTING")).toEqual([
      { label: "Mark Scheduled", status: "SCHEDULED" },
      { label: "Cancel Edition", status: "CANCELLED" },
    ]);
  });

  it("exposes the scheduled actions", () => {
    expect(lifecycleActions("SCHEDULED")).toEqual([
      { label: "Start Tournament", status: "ONGOING" },
      { label: "Cancel Edition", status: "CANCELLED" },
    ]);
  });

  it("does not expose terminal transitions", () => {
    expect(lifecycleActions("COMPLETED")).toEqual([]);
    expect(lifecycleActions("CANCELLED")).toEqual([]);
  });
});
