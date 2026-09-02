import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";

import {
  AdministrativeHistory,
  operationFormAction,
} from "@/components/dashboard/match-administrative-history";
import type { MatchResponse } from "@/lib/api/schema-helpers";

describe("dashboard match operations view", () => {
  it("maps result operation controls to route actions", () => {
    expect(operationFormAction("MARK_UNDER_REVIEW")).toBe("operation-review");
    expect(operationFormAction("RESTORE_OFFICIAL")).toBe(
      "operation-restore-result"
    );
    expect(operationFormAction("VOID_RESULT")).toBe("operation-void-result");
  });

  it("renders private administrative history with state, metadata, and link", () => {
    const html = renderToStaticMarkup(
      <AdministrativeHistory
        match={{
          operationHistory: [
            {
              id: 1,
              operationType: "ORDER_REMATCH",
              actorUserId: 4,
              actorName: "Organizer",
              reason: "Scoring dispute",
              oldStatus: "COMPLETED",
              newStatus: "COMPLETED",
              oldResultStatus: "OFFICIAL",
              newResultStatus: "SUPERSEDED",
              metadata: "rematchId=22",
              relatedMatchId: 22,
              createdAt: "2026-09-02T09:20:00Z",
            },
          ],
        } as MatchResponse}
      />
    );

    expect(html).toContain("Administrative History");
    expect(html).toContain("Order Rematch");
    expect(html).toContain("Reason: Scoring dispute");
    expect(html).toContain("Result: Official to Superseded");
    expect(html).toContain("rematchId=22");
    expect(html).toContain("/dashboard/matches/22");
  });
});
