import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { buttonVariants } from "@/components/ui/button";
import type { InningsScorecard, Scorecard } from "@/lib/api/scorecard";
import { cn } from "@/lib/utils";
import Link from "next/link";

export function ScorecardView({ scorecard }: { scorecard: Scorecard }) {
  const innings = scorecard.innings ?? [];
  const state = scorecardState(scorecard);

  return (
    <section className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="mb-6">
        <p className="font-mono text-xs uppercase text-primary">
          Match {scorecard.matchId ?? "-"}
        </p>
        <h1 className="font-heading text-4xl font-bold uppercase tracking-normal">
          Scorecard
        </h1>
        {state ? (
          <div className="mt-4 rounded-sm border border-white/10 bg-card p-4">
            <p className="font-heading text-xl font-bold uppercase tracking-normal text-secondary">
              {state.title}
            </p>
            {state.detail ? (
              <p className="mt-1 text-sm text-muted-foreground">
                {state.detail}
              </p>
            ) : null}
            {state.rematchId ? (
              <Link
                className={cn(buttonVariants({ variant: "outline" }), "mt-3 h-9")}
                href={`/matches/${state.rematchId}/scorecard`}
              >
                Rematch scorecard
              </Link>
            ) : null}
          </div>
        ) : null}
      </div>
      {innings.length > 0 ? (
        <div className="grid gap-5">
          {innings.map((item) => (
            <InningsCard innings={item} key={item.inningsNumber} />
          ))}
        </div>
      ) : (
        <div className="rounded-sm border border-white/10 bg-card p-5 text-sm text-muted-foreground">
          Scorecard rows are not available for this match yet.
        </div>
      )}
    </section>
  );
}

function scorecardState(scorecard: Scorecard) {
  if (scorecard.status === "ABANDONED") {
    return {
      title: "MATCH ABANDONED",
      detail: scorecard.resultText ?? "Recorded score remains available.",
    };
  }

  if (scorecard.status === "SUSPENDED") {
    return {
      title: "MATCH SUSPENDED",
      detail: "Recorded score is preserved until play resumes.",
    };
  }

  if (scorecard.resultStatus === "UNDER_REVIEW") {
    return {
      title: "RESULT UNDER REVIEW",
      detail: "This result is temporarily excluded from standings and statistics.",
    };
  }

  if (scorecard.resultStatus === "VOID") {
    return {
      title: "RESULT VOIDED",
      detail: scorecard.resultText ?? "This result does not count in standings or statistics.",
    };
  }

  if (scorecard.resultStatus === "SUPERSEDED") {
    return {
      title: "RESULT SUPERSEDED",
      detail: "A rematch has replaced this result.",
      rematchId: scorecard.supersededByMatchId,
    };
  }

  return undefined;
}

function InningsCard({ innings }: { innings: InningsScorecard }) {
  return (
    <article className="rounded-sm border border-white/10 bg-card p-5">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="font-mono text-xs uppercase text-muted-foreground">
            Innings {innings.inningsNumber ?? "-"}
          </p>
          <h2 className="font-heading text-2xl font-semibold uppercase tracking-normal">
            {innings.battingTeam ?? "Batting team"}
          </h2>
        </div>
        <p className="font-mono text-3xl font-bold text-secondary">
          {innings.runs ?? 0}/{innings.wickets ?? 0}
          <span className="ml-2 text-sm font-medium text-muted-foreground">
            {innings.overs ?? "0.0"} overs
          </span>
        </p>
      </div>
      <div className="mt-5 grid gap-5 xl:grid-cols-2">
        <ScoreTable
          columns={["Batter", "R", "B", "4s", "6s", "SR", "How out"]}
          rows={(innings.batting ?? []).map((row) => [
            row.playerName ?? "Player",
            row.runs ?? 0,
            row.balls ?? 0,
            row.fours ?? 0,
            row.sixes ?? 0,
            row.strikeRate ?? "-",
            row.dismissal ?? "not out",
          ])}
          title="Batting"
        />
        <ScoreTable
          columns={["Bowler", "O", "R", "W", "Econ"]}
          rows={(innings.bowling ?? []).map((row) => [
            row.playerName ?? "Player",
            row.overs ?? "0.0",
            row.runs ?? 0,
            row.wickets ?? 0,
            row.economy ?? "-",
          ])}
          title="Bowling"
        />
      </div>
    </article>
  );
}

function ScoreTable({
  columns,
  rows,
  title,
}: {
  columns: string[];
  rows: (string | number)[][];
  title: string;
}) {
  return (
    <div className="overflow-hidden rounded-sm border border-white/10">
      <h3 className="bg-surface px-3 py-2 font-mono text-xs uppercase text-muted-foreground">
        {title}
      </h3>
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow>
              {columns.map((column) => (
                <TableHead key={column}>{column}</TableHead>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.length > 0 ? (
              rows.map((row, rowIndex) => (
                <TableRow key={`${title}-${rowIndex}`}>
                  {row.map((cell, cellIndex) => (
                    <TableCell key={`${title}-${rowIndex}-${cellIndex}`}>
                      {cell}
                    </TableCell>
                  ))}
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={columns.length}>
                  No rows available.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
