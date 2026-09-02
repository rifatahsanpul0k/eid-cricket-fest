import Link from "next/link";

import { Badge } from "@/components/ui/badge";
import type { MatchResponse } from "@/lib/api/schema-helpers";
import { matchStatusLabel } from "@/lib/cricket/match-labels";
import { formatBangladeshDateTime } from "@/lib/utils/format";

export function AdministrativeHistory({ match }: { match: MatchResponse }) {
  const history = match.operationHistory ?? [];

  return (
    <section className="rounded-sm border border-white/10 bg-card p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="font-heading text-2xl font-bold uppercase tracking-normal">
            Administrative History
          </h2>
          <p className="mt-2 text-sm text-muted-foreground">
            Private organizer audit trail for recovery actions on this match.
          </p>
        </div>
        <Badge variant="outline">{history.length} entries</Badge>
      </div>

      {history.length === 0 ? (
        <p className="mt-4 text-sm text-muted-foreground">
          No administrative operations have been recorded for this match.
        </p>
      ) : (
        <div className="mt-5 divide-y divide-white/10 rounded-sm border border-white/10">
          {history.map((item) => (
            <article className="grid gap-2 p-4 text-sm" key={item.id}>
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h3 className="font-heading text-lg font-bold uppercase tracking-normal">
                  {operationLabel(item.operationType)}
                </h3>
                <time className="font-mono text-xs uppercase text-muted-foreground">
                  {formatBangladeshDateTime(item.createdAt)}
                </time>
              </div>
              <p className="text-muted-foreground">
                Reason: {item.reason ?? "No reason recorded"}
              </p>
              <div className="flex flex-wrap gap-2 text-xs text-muted-foreground">
                {item.oldStatus || item.newStatus ? (
                  <span>
                    Status: {matchStatusLabel(item.oldStatus)} to{" "}
                    {matchStatusLabel(item.newStatus)}
                  </span>
                ) : null}
                {item.oldResultStatus || item.newResultStatus ? (
                  <span>
                    Result: {resultStatusLabel(item.oldResultStatus)} to{" "}
                    {resultStatusLabel(item.newResultStatus)}
                  </span>
                ) : null}
                {item.metadata ? (
                  <span>{operationMetadata(item.metadata)}</span>
                ) : null}
              </div>
              {item.relatedMatchId ? (
                <Link
                  className="text-sm font-medium text-primary hover:underline"
                  href={`/dashboard/matches/${item.relatedMatchId}`}
                >
                  Related match #{item.relatedMatchId}
                </Link>
              ) : null}
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

export function operationFormAction(
  operation: NonNullable<MatchResponse["availableOperations"]>[number]
) {
  if (operation === "RESCHEDULE") return "operation-reschedule";
  if (operation === "ORDER_REMATCH") return "operation-rematch";
  if (operation === "MARK_UNDER_REVIEW") return "operation-review";
  if (operation === "RESTORE_OFFICIAL") return "operation-restore-result";

  return `operation-${operation.toLowerCase().replaceAll("_", "-")}`;
}

export function operationLabel(
  operation?: NonNullable<MatchResponse["availableOperations"]>[number] | string
) {
  if (operation === "RESET_TOSS") return "Reset Toss";
  if (operation === "MARK_UNDER_REVIEW") return "Mark Under Review";
  if (operation === "RESTORE_OFFICIAL") return "Restore Official Result";
  if (operation === "VOID_RESULT") return "Void Result";
  if (operation === "ORDER_REMATCH") return "Order Rematch";

  return operation
    ? operation
        .toLowerCase()
        .split("_")
        .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
        .join(" ")
    : "Operation";
}

export function resultStatusLabel(status?: MatchResponse["resultStatus"]) {
  return status
    ? status
        .toLowerCase()
        .split("_")
        .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
        .join(" ")
    : "None";
}

function operationMetadata(metadata: string) {
  return metadata
    .split(",")
    .map((part) => part.trim())
    .filter(Boolean)
    .join(" · ");
}
